package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.ai.AssistantResponse
import com.example.ai.GeminiAssistantService
import com.example.ai.ToolExecutor
import com.example.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MyraRepository(
    private val context: Context,
    private val database: MyraDatabase,
    val preferences: PreferencesManager
) {
    val toolExecutor = ToolExecutor(context, database)
    val geminiService = GeminiAssistantService(context, toolExecutor)

    val chatMessages: Flow<List<ChatMessageEntity>> = database.chatDao().getMessages()
    val automations: Flow<List<AutomationEntity>> = database.automationDao().getAllAutomations()
    val automationHistory: Flow<List<AutomationHistoryEntity>> = database.automationDao().getAutomationHistory()
    val memories: Flow<List<MemoryEntity>> = database.memoryDao().getAllMemories()
    val actionLogs: Flow<List<ActionLogEntity>> = database.actionLogDao().getAllLogs()
    val connectedDevices: Flow<List<ConnectedDeviceEntity>> = database.deviceDao().getAllDevices()
    val connectors: Flow<List<ConnectorEntity>> = database.connectorDao().getAllConnectors()

    suspend fun saveUserMessage(text: String, imageUrl: String? = null): Long {
        return database.chatDao().insertMessage(
            ChatMessageEntity(
                sender = "user",
                text = text,
                imageUrl = imageUrl
            )
        )
    }

    suspend fun saveMyraMessage(
        text: String,
        toolName: String? = null,
        toolStatus: String? = null,
        toolOutput: String? = null
    ): Long {
        return database.chatDao().insertMessage(
            ChatMessageEntity(
                sender = "myra",
                text = text,
                toolName = toolName,
                toolStatus = toolStatus,
                toolOutput = toolOutput
            )
        )
    }

    suspend fun processQuery(query: String, imageBitmap: Bitmap? = null): AssistantResponse {
        val history = chatMessages.first()
        val mems = memories.first()
        val customKey = preferences.customApiKey.first()

        val response = geminiService.processUserQuery(
            query = query,
            recentHistory = history,
            memories = mems,
            customApiKey = customKey,
            imageBitmap = imageBitmap
        )

        saveMyraMessage(
            text = response.textResponse,
            toolName = response.executedTool,
            toolStatus = if (response.toolResult != null) "EXECUTED" else null,
            toolOutput = response.toolResult?.let { it.toString() }
        )

        return response
    }

    suspend fun clearChat() {
        database.chatDao().clearConversation()
    }

    suspend fun addAutomation(
        title: String,
        triggerType: String,
        triggerValue: String,
        actionType: String,
        actionPayload: String
    ): Long {
        return database.automationDao().insertAutomation(
            AutomationEntity(
                title = title,
                triggerType = triggerType,
                triggerValue = triggerValue,
                actionType = actionType,
                actionPayload = actionPayload
            )
        )
    }

    suspend fun toggleAutomation(automation: AutomationEntity) {
        database.automationDao().updateAutomation(
            automation.copy(isEnabled = !automation.isEnabled)
        )
    }

    suspend fun deleteAutomation(automation: AutomationEntity) {
        database.automationDao().deleteAutomation(automation)
    }

    suspend fun saveMemory(key: String, value: String) {
        database.memoryDao().insertMemory(MemoryEntity(key = key, value = value))
    }

    suspend fun deleteMemory(memory: MemoryEntity) {
        database.memoryDao().deleteMemory(memory)
    }

    suspend fun clearAllMemories() {
        database.memoryDao().clearAllMemories()
    }

    suspend fun clearLogs() {
        database.actionLogDao().clearLogs()
    }
}
