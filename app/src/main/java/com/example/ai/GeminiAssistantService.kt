package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiAssistantService(
    private val context: Context,
    private val toolExecutor: ToolExecutor
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun processUserQuery(
        query: String,
        recentHistory: List<ChatMessageEntity> = emptyList(),
        memories: List<MemoryEntity> = emptyList(),
        customApiKey: String? = null,
        imageBitmap: Bitmap? = null
    ): AssistantResponse = withContext(Dispatchers.IO) {
        val trimmed = query.trim()

        // 1. Fast local intent extraction for instant offline / device action triggers (Hindi, English, Hinglish)
        val localToolMatch = matchLocalIntent(trimmed)
        if (localToolMatch != null) {
            val toolRes = toolExecutor.executeTool(
                toolName = localToolMatch.first,
                arguments = localToolMatch.second
            )
            return@withContext AssistantResponse(
                textResponse = when (toolRes) {
                    is ToolResult.Success -> toolRes.message
                    is ToolResult.Error -> "${toolRes.errorMessage}\n${toolRes.recoverySuggestion ?: ""}".trim()
                    is ToolResult.RequiresConfirmation -> "Confirmation required: ${toolRes.title}\n${toolRes.details}"
                    is ToolResult.MissingPermission -> "Permission needed: ${toolRes.reason}"
                },
                toolResult = toolRes,
                executedTool = localToolMatch.first
            )
        }

        // 2. Call Gemini 3.5 Flash REST API
        val apiKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent fallback response if API key is not configured in Secrets panel
            val fallback = generateSmartLocalResponse(trimmed, memories)
            return@withContext AssistantResponse(
                textResponse = fallback,
                toolResult = null,
                executedTool = null
            )
        }

        try {
            val requestJson = buildGeminiRequest(trimmed, recentHistory, memories, imageBitmap)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AssistantResponse(
                    textResponse = "API Error (${response.code}): Unable to contact Gemini. ${extractErrorMessage(responseBody)}",
                    toolResult = null,
                    executedTool = null
                )
            }

            parseGeminiResponse(responseBody)
        } catch (e: Exception) {
            val fallback = generateSmartLocalResponse(trimmed, memories)
            AssistantResponse(
                textResponse = "$fallback\n\n(Note: Cloud connection offline: ${e.localizedMessage})",
                toolResult = null,
                executedTool = null
            )
        }
    }

    private suspend fun parseGeminiResponse(responseBody: String): AssistantResponse {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return AssistantResponse("No response generated.", null, null)
        val firstCandidate = candidates.optJSONObject(0) ?: return AssistantResponse("No response generated.", null, null)
        val content = firstCandidate.optJSONObject("content") ?: return AssistantResponse("No response generated.", null, null)
        val parts = content.optJSONArray("parts") ?: return AssistantResponse("No response generated.", null, null)

        var finalSpeechText = ""
        var pendingResult: ToolResult? = null
        var executedToolName: String? = null

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("text")) {
                finalSpeechText += part.getString("text") + " "
            }
            if (part.has("functionCall")) {
                val call = part.getJSONObject("functionCall")
                val name = call.getString("name")
                val argsObj = call.optJSONObject("args") ?: JSONObject()
                val argsMap = mutableMapOf<String, String>()
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = argsObj.optString(k, "")
                }

                executedToolName = name
                pendingResult = toolExecutor.executeTool(name, argsMap)
            }
        }

        val toolMessage = when (pendingResult) {
            is ToolResult.Success -> "\n\n⚡ ${pendingResult.message}"
            is ToolResult.Error -> "\n\n❌ ${pendingResult.errorMessage}"
            is ToolResult.RequiresConfirmation -> "\n\n⚠️ ${pendingResult.title}\n${pendingResult.details}"
            is ToolResult.MissingPermission -> "\n\n🔒 ${pendingResult.reason}"
            null -> ""
        }

        return AssistantResponse(
            textResponse = (finalSpeechText.trim() + toolMessage).trim(),
            toolResult = pendingResult,
            executedTool = executedToolName
        )
    }

    private fun buildGeminiRequest(
        query: String,
        history: List<ChatMessageEntity>,
        memories: List<MemoryEntity>,
        imageBitmap: Bitmap?
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val sysInstruction = JSONObject()
        val sysParts = JSONArray()
        val memoryContext = if (memories.isNotEmpty()) {
            "User facts remembered:\n" + memories.joinToString("\n") { "- ${it.key}: ${it.value}" }
        } else ""

        val systemPrompt = """
            You are MYRA AI ("Your AI Companion, Your Personal Assistant").
            You are smart, polite, highly capable, loyal, and proactive.
            You naturally understand and speak Hindi, English, and Hinglish.
            Adapt to the user's preferred language seamlessly.
            When a user asks to perform an action on their Android device, call the appropriate function tool.
            Do not make up fake URLs or simulate fake success.
            $memoryContext
        """.trimIndent()

        sysParts.put(JSONObject().put("text", systemPrompt))
        sysInstruction.put("parts", sysParts)
        root.put("systemInstruction", sysInstruction)

        // Contents
        val contentsArray = JSONArray()

        // Add last 6 turns of history for conversational context
        val contextHistory = history.takeLast(6)
        for (item in contextHistory) {
            val msgObj = JSONObject()
            msgObj.put("role", if (item.sender == "user") "user" else "model")
            val pArray = JSONArray()
            pArray.put(JSONObject().put("text", item.text))
            msgObj.put("parts", pArray)
            contentsArray.put(msgObj)
        }

        // Current turn
        val currentTurn = JSONObject()
        currentTurn.put("role", "user")
        val currentParts = JSONArray()
        currentParts.put(JSONObject().put("text", query))

        if (imageBitmap != null) {
            val stream = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            val inlineData = JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", base64Data)
            currentParts.put(JSONObject().put("inlineData", inlineData))
        }

        currentTurn.put("parts", currentParts)
        contentsArray.put(currentTurn)
        root.put("contents", contentsArray)

        // Tools / Function declarations
        val toolsArray = JSONArray()
        val funcDecls = JSONArray()

        for (t in toolExecutor.tools) {
            val f = JSONObject()
            f.put("name", t.name)
            f.put("description", t.description)
            val paramsObj = JSONObject()
            paramsObj.put("type", "OBJECT")
            val propObj = JSONObject()
            val reqArray = JSONArray()
            for (p in t.parameters) {
                val pObj = JSONObject()
                pObj.put("type", p.type)
                pObj.put("description", p.description)
                propObj.put(p.name, pObj)
                if (p.required) reqArray.put(p.name)
            }
            paramsObj.put("properties", propObj)
            paramsObj.put("required", reqArray)
            f.put("parameters", paramsObj)
            funcDecls.put(f)
        }

        val toolDeclObj = JSONObject()
        toolDeclObj.put("functionDeclarations", funcDecls)
        toolsArray.put(toolDeclObj)
        root.put("tools", toolsArray)

        return root
    }

    private fun matchLocalIntent(query: String): Pair<String, Map<String, String>>? {
        val q = query.lowercase().trim()

        // Battery
        if (q.contains("battery") || q.contains("charge kitna") || q.contains("battery kitni") || q.contains("battery status")) {
            return "battery_status" to emptyMap()
        }

        // Flashlight ON
        if (q.contains("flashlight on") || q.contains("torch on") || q.contains("flashlight chalao") || q.contains("torch jalao")) {
            return "flashlight_control" to mapOf("state" to "true")
        }

        // Flashlight OFF
        if (q.contains("flashlight off") || q.contains("torch off") || q.contains("flashlight band") || q.contains("torch band")) {
            return "flashlight_control" to mapOf("state" to "false")
        }

        // WhatsApp open or message
        if (q.startsWith("whatsapp kholo") || q == "whatsapp" || q.contains("open whatsapp")) {
            return "launch_app" to mapOf("app_name" to "whatsapp")
        }

        // Web search
        if (q.contains("search karo") || q.contains("google par") || q.startsWith("search ")) {
            val cleanQuery = q.replace("google par", "")
                .replace("search karo", "")
                .replace("search", "")
                .trim()
            return "web_search" to mapOf("query" to cleanQuery)
        }

        // Media control
        if (q == "play" || q.contains("gaana chalao") || q.contains("play music") || q.contains("song chalao")) {
            return "media_control" to mapOf("action" to "PLAY")
        }
        if (q == "pause" || q.contains("gaana roko") || q.contains("stop music") || q.contains("pause song")) {
            return "media_control" to mapOf("action" to "PAUSE")
        }
        if (q.contains("volume badhao") || q.contains("volume up")) {
            return "media_control" to mapOf("action" to "VOL_UP")
        }
        if (q.contains("volume kam") || q.contains("volume down")) {
            return "media_control" to mapOf("action" to "VOL_DOWN")
        }

        // Device info
        if (q.contains("storage kitna") || q.contains("device info") || q.contains("phone details") || q.contains("system status")) {
            return "device_info" to emptyMap()
        }

        // Parking saver
        if (q.contains("car park") || q.contains("parking save") || q.contains("gadi park")) {
            return "parking_saver" to mapOf("note" to "Saved spot via voice command")
        }

        return null
    }

    private fun generateSmartLocalResponse(query: String, memories: List<MemoryEntity>): String {
        val q = query.lowercase()
        return when {
            q.contains("who are you") || q.contains("tum kaun ho") || q.contains("your name") ->
                "Main MYRA hoon — aapki personal AI companion aur smart assistant. Main aapke device actions, voice commands, automations, aur PC connectivity ko manage karti hoon."
            q.contains("namaste") || q.contains("hello") || q.contains("hi myra") || q.contains("hey") ->
                "Namaste! Main MYRA hoon. Main aapki kya madad kar sakti hoon? Aap mujhe koi bhi command de sakte hain, jaise battery check karna, alarm lagana, ya WhatsApp kholna."
            q.contains("kaise ho") || q.contains("how are you") ->
                "Main bilkul badhiya hoon! Sabhi system protocols active hain aur aapke aadesh ke liye taiyar hoon. Aap bataiye?"
            q.contains("help") || q.contains("madad") ->
                "Aap mujhse pooch sakte hain:\n• 'MYRA battery kitni hai?'\n• 'Flashlight on karo'\n• 'WhatsApp kholo'\n• 'Kal 7 baje alarm laga do'\n• 'Google par Raipur search karo'\n• Photo scan aur AI analysis\n• PC Connect file transfer"
            else ->
                "Maine aapka command note kar liya hai: \"$query\". Sabhi standard tools aur Android system features active hain."
        }
    }

    private fun extractErrorMessage(body: String): String {
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: body
        } catch (_: Exception) {
            body
        }
    }
}

data class AssistantResponse(
    val textResponse: String,
    val toolResult: ToolResult?,
    val executedTool: String?
)
