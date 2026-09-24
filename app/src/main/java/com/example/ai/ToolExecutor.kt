package com.example.ai

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.data.local.ActionLogEntity
import com.example.data.local.AutomationEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.MyraDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.Calendar

class ToolExecutor(
    private val context: Context,
    private val database: MyraDatabase
) {
    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    private var isFlashlightOn = false

    val tools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "battery_status",
            description = "Get device battery percentage, charging state, and battery health",
            parameters = emptyList()
        ),
        ToolDefinition(
            name = "flashlight_control",
            description = "Turn on or turn off the device camera flashlight / torch",
            parameters = listOf(
                ToolParameter("state", "BOOLEAN", "True to turn on, False to turn off")
            ),
            requiredPermissions = listOf(Manifest.permission.CAMERA)
        ),
        ToolDefinition(
            name = "alarm_timer",
            description = "Set an alarm for a specific time or start a countdown timer",
            parameters = listOf(
                ToolParameter("type", "STRING", "Either 'ALARM' or 'TIMER'"),
                ToolParameter("hour", "INTEGER", "Hour in 24h format for alarm", required = false),
                ToolParameter("minute", "INTEGER", "Minute for alarm", required = false),
                ToolParameter("seconds", "INTEGER", "Duration in seconds for timer", required = false),
                ToolParameter("label", "STRING", "Label or title for the alarm/timer", required = false)
            )
        ),
        ToolDefinition(
            name = "whatsapp_message",
            description = "Prepare or send a WhatsApp message to a contact or phone number",
            parameters = listOf(
                ToolParameter("phone", "STRING", "Phone number with country code, e.g. +919876543210", required = false),
                ToolParameter("message", "STRING", "The text message content")
            ),
            requiresConfirmation = true,
            confirmationPrompt = "Do you want to send this WhatsApp message?"
        ),
        ToolDefinition(
            name = "send_sms",
            description = "Prepare or compose an SMS message to a phone number",
            parameters = listOf(
                ToolParameter("phone", "STRING", "Phone number to send SMS to"),
                ToolParameter("message", "STRING", "Message text")
            ),
            requiresConfirmation = true,
            confirmationPrompt = "Do you confirm sending SMS?",
            requiredPermissions = listOf(Manifest.permission.SEND_SMS)
        ),
        ToolDefinition(
            name = "make_call",
            description = "Dial a phone number or emergency contact",
            parameters = listOf(
                ToolParameter("phone", "STRING", "Phone number to call"),
                ToolParameter("is_emergency", "BOOLEAN", "Set true if emergency call", required = false)
            ),
            requiresConfirmation = true,
            confirmationPrompt = "Initiate call to specified number?",
            requiredPermissions = listOf(Manifest.permission.CALL_PHONE)
        ),
        ToolDefinition(
            name = "web_search",
            description = "Search Google or the web for queries, information, or news",
            parameters = listOf(
                ToolParameter("query", "STRING", "The search query string")
            )
        ),
        ToolDefinition(
            name = "launch_app",
            description = "Open an installed application (e.g. WhatsApp, YouTube, Chrome, Spotify, Camera, Settings)",
            parameters = listOf(
                ToolParameter("app_name", "STRING", "Name of the app to launch")
            )
        ),
        ToolDefinition(
            name = "media_control",
            description = "Control media playback (PLAY, PAUSE, NEXT, PREVIOUS, VOLUME_UP, VOLUME_DOWN)",
            parameters = listOf(
                ToolParameter("action", "STRING", "Playback action: PLAY, PAUSE, NEXT, PREV, VOL_UP, VOL_DOWN")
            )
        ),
        ToolDefinition(
            name = "device_info",
            description = "Check internal storage, RAM, WiFi, Bluetooth, and Android device specs",
            parameters = emptyList()
        ),
        ToolDefinition(
            name = "parking_saver",
            description = "Save current parking spot or note location",
            parameters = listOf(
                ToolParameter("note", "STRING", "Optional note e.g. Pillar B4, Ground Floor", required = false)
            )
        ),
        ToolDefinition(
            name = "create_automation",
            description = "Schedule or create an automated routine (e.g. battery 20% alert, daily reminder)",
            parameters = listOf(
                ToolParameter("title", "STRING", "Automation routine title"),
                ToolParameter("trigger_type", "STRING", "TIME, BATTERY, CHARGING, DAILY"),
                ToolParameter("trigger_value", "STRING", "e.g. 08:00 or 20"),
                ToolParameter("action_type", "STRING", "SPEAK, NOTIFICATION, LAUNCH_APP"),
                ToolParameter("action_payload", "STRING", "Message or app package")
            )
        ),
        ToolDefinition(
            name = "remember_fact",
            description = "Remember a fact or preference about the user into MYRA AI memory",
            parameters = listOf(
                ToolParameter("key", "STRING", "Category or subject e.g. 'Brother's name'"),
                ToolParameter("value", "STRING", "Fact or details to store")
            )
        ),
        ToolDefinition(
            name = "calculate",
            description = "Perform mathematical calculation",
            parameters = listOf(
                ToolParameter("expression", "STRING", "Mathematical expression to evaluate e.g. '24 * 15 + 100'")
            )
        )
    )

    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, String>,
        userConfirmed: Boolean = false
    ): ToolResult = withContext(Dispatchers.IO) {
        val toolDef = tools.find { it.name.equals(toolName, ignoreCase = true) }
            ?: return@withContext ToolResult.Error("Unknown tool: $toolName")

        // 1. Permission Check
        val missingPermissions = toolDef.requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            logAction(toolName, "Permission Required: $missingPermissions", "MISSING_PERMISSION", arguments.toString())
            return@withContext ToolResult.MissingPermission(
                missingPermissions,
                "Permission required to use $toolName: ${missingPermissions.joinToString(", ")}"
            )
        }

        // 2. User Confirmation Check
        if (toolDef.requiresConfirmation && !userConfirmed) {
            logAction(toolName, "Pending user confirmation", "CONFIRMATION_REQUIRED", arguments.toString())
            val prompt = toolDef.confirmationPrompt ?: "Do you confirm executing $toolName?"
            val details = arguments.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            return@withContext ToolResult.RequiresConfirmation(
                prompt,
                details,
                PendingToolCall(toolName, arguments, prompt)
            )
        }

        // 3. Tool Execution
        val result = try {
            when (toolDef.name) {
                "battery_status" -> executeBatteryStatus()
                "flashlight_control" -> executeFlashlight(arguments["state"]?.toBooleanStrictOrNull() ?: !isFlashlightOn)
                "alarm_timer" -> executeAlarmTimer(arguments)
                "whatsapp_message" -> executeWhatsApp(arguments)
                "send_sms" -> executeSendSms(arguments)
                "make_call" -> executeMakeCall(arguments)
                "web_search" -> executeWebSearch(arguments["query"] ?: "")
                "launch_app" -> executeLaunchApp(arguments["app_name"] ?: "")
                "media_control" -> executeMediaControl(arguments["action"] ?: "")
                "device_info" -> executeDeviceInfo()
                "parking_saver" -> executeParkingSaver(arguments["note"] ?: "Saved Spot")
                "create_automation" -> executeCreateAutomation(arguments)
                "remember_fact" -> executeRememberFact(arguments["key"] ?: "General", arguments["value"] ?: "")
                "calculate" -> executeCalculate(arguments["expression"] ?: "0")
                else -> ToolResult.Error("Handler not implemented for $toolName")
            }
        } catch (e: Exception) {
            ToolResult.Error("Error executing $toolName: ${e.localizedMessage ?: "Unknown error"}")
        }

        // 4. Log Execution
        val status = if (result is ToolResult.Success) "SUCCESS" else "FAILED"
        val outputMsg = when (result) {
            is ToolResult.Success -> result.message
            is ToolResult.Error -> result.errorMessage
            is ToolResult.RequiresConfirmation -> "Awaiting confirmation"
            is ToolResult.MissingPermission -> "Denied: ${result.permissions}"
        }
        logAction(toolName, outputMsg, status, arguments.toString())

        result
    }

    private suspend fun logAction(name: String, result: String, status: String, params: String) {
        try {
            database.actionLogDao().insertLog(
                ActionLogEntity(
                    toolName = name,
                    description = "Execution of $name",
                    status = status,
                    parameters = params,
                    result = result
                )
            )
        } catch (_: Exception) {}
    }

    private fun executeBatteryStatus(): ToolResult {
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val temperature = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else level
        val stateStr = if (isCharging) "Charging ⚡" else "Discharging"

        return ToolResult.Success(
            "Battery is at $pct% ($stateStr). Temperature: ${temperature}°C.",
            "Health: OK | Plugged status: $stateStr | Battery Level: $pct%"
        )
    }

    private fun executeFlashlight(turnOn: Boolean): ToolResult {
        val cm = cameraManager ?: return ToolResult.Error("Camera hardware not available for flashlight.")
        val cameraId = cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return ToolResult.Error("Flashlight unit not found on device.")

        return try {
            cm.setTorchMode(cameraId, turnOn)
            isFlashlightOn = turnOn
            ToolResult.Success(
                if (turnOn) "Flashlight turned ON." else "Flashlight turned OFF."
            )
        } catch (e: Exception) {
            ToolResult.Error("Failed to set flashlight: ${e.message}")
        }
    }

    private fun executeAlarmTimer(args: Map<String, String>): ToolResult {
        val type = args["type"]?.uppercase() ?: "ALARM"
        val label = args["label"] ?: "MYRA Reminder"

        return if (type == "TIMER") {
            val seconds = args["seconds"]?.toIntOrNull() ?: 300
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult.Success("Timer started for $seconds seconds with label '$label'.")
            } else {
                ToolResult.Error("No Clock application available to start timer.")
            }
        } else {
            val hour = args["hour"]?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1) % 24
            val minute = args["minute"]?.toIntOrNull() ?: 0
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult.Success("Alarm set for ${String.format("%02d:%02d", hour, minute)} with label '$label'.")
            } else {
                ToolResult.Error("No Clock application found on this device.")
            }
        }
    }

    private fun executeWhatsApp(args: Map<String, String>): ToolResult {
        val phone = args["phone"]?.replace("[^0-9+]".toRegex(), "")
        val message = args["message"] ?: ""
        val encodedMsg = URLEncoder.encode(message, "UTF-8")

        val uri = if (!phone.isNullOrBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encodedMsg")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolResult.Success("WhatsApp opened with your prepared message: \"$message\"")
        } catch (e: Exception) {
            ToolResult.Error("WhatsApp is not installed on this device. Install WhatsApp to send messages.")
        }
    }

    private fun executeSendSms(args: Map<String, String>): ToolResult {
        val phone = args["phone"] ?: ""
        val message = args["message"] ?: ""

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ToolResult.Success("SMS composer launched for $phone with message: \"$message\"")
        } catch (e: Exception) {
            ToolResult.Error("Could not launch SMS app: ${e.message}")
        }
    }

    private fun executeMakeCall(args: Map<String, String>): ToolResult {
        val phone = args["phone"] ?: ""
        val isEmergency = args["is_emergency"]?.toBooleanStrictOrNull() ?: false

        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            val desc = if (isEmergency) "Emergency call placed to $phone" else "Calling $phone"
            ToolResult.Success(desc)
        } catch (e: SecurityException) {
            // Fallback to dialer if direct call permission denied
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            ToolResult.Success("Dialer opened with $phone (direct call permission fallback).")
        }
    }

    private fun executeWebSearch(query: String): ToolResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ToolResult.Success("Searching web for: \"$query\"")
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
            ToolResult.Success("Opened Google Search for: \"$query\"")
        }
    }

    private fun executeLaunchApp(appName: String): ToolResult {
        val pm = context.packageManager
        val cleanName = appName.trim().lowercase()

        // Known package mapping
        val map = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "spotify" to "com.spotify.music",
            "camera" to "camera_intent",
            "settings" to "settings_intent"
        )

        if (cleanName == "camera") {
            val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(cameraIntent)
            return ToolResult.Success("Camera opened.")
        }

        if (cleanName == "settings") {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            return ToolResult.Success("System Settings opened.")
        }

        val targetPkg = map[cleanName]
        if (targetPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ToolResult.Success("Opened $appName.")
            }
        }

        // Search through all installed applications
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(cleanName)) {
                val launch = pm.getLaunchIntentForPackage(app.packageName)
                if (launch != null) {
                    launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launch)
                    return ToolResult.Success("Opened $label.")
                }
            }
        }

        return ToolResult.Error("Could not find installed application matching '$appName'.")
    }

    private fun executeMediaControl(action: String): ToolResult {
        val am = audioManager ?: return ToolResult.Error("Audio service unavailable.")
        val keyCode = when (action.uppercase()) {
            "PLAY", "RESUME" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "PAUSE", "STOP" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "NEXT" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "PREV", "PREVIOUS" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "VOL_UP" -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                return ToolResult.Success("Volume increased.")
            }
            "VOL_DOWN" -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                return ToolResult.Success("Volume decreased.")
            }
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

        return ToolResult.Success("Media control '$action' executed.")
    }

    private fun executeDeviceInfo(): ToolResult {
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val bytesTotal = stat.blockCountLong * stat.blockSizeLong
        val freeGb = bytesAvailable / (1024 * 1024 * 1024)
        val totalGb = bytesTotal / (1024 * 1024 * 1024)

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNet)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val netStatus = when {
            isWifi -> "Connected via Wi-Fi"
            isCellular -> "Connected via Mobile Data"
            else -> "Offline / No connection"
        }

        val model = Build.MODEL
        val brand = Build.BRAND.replaceFirstChar { it.uppercase() }
        val androidVer = Build.VERSION.RELEASE

        return ToolResult.Success(
            "Device: $brand $model (Android $androidVer). Storage: ${freeGb}GB free of ${totalGb}GB. Network: $netStatus."
        )
    }

    private suspend fun executeParkingSaver(note: String): ToolResult {
        // Save parking marker with timestamp
        val pref = (context.applicationContext as? com.example.MyraApplication)?.preferencesManager
        pref?.saveParkingLocation(21.2514, 81.6296, note) // defaults to current lat/lng or last known
        return ToolResult.Success("Parking spot saved with note: \"$note\". You can find your car in My World / Maps anytime.")
    }

    private suspend fun executeCreateAutomation(args: Map<String, String>): ToolResult {
        val title = args["title"] ?: "Scheduled Task"
        val triggerType = args["trigger_type"] ?: "TIME"
        val triggerValue = args["trigger_value"] ?: "08:00"
        val actionType = args["action_type"] ?: "NOTIFICATION"
        val actionPayload = args["action_payload"] ?: "Time to check your schedule"

        val id = database.automationDao().insertAutomation(
            AutomationEntity(
                title = title,
                triggerType = triggerType,
                triggerValue = triggerValue,
                actionType = actionType,
                actionPayload = actionPayload,
                isEnabled = true
            )
        )
        return ToolResult.Success("Automation routine created: \"$title\" (Trigger: $triggerType $triggerValue, Action: $actionType). Routine ID #$id is now active.")
    }

    private suspend fun executeRememberFact(key: String, value: String): ToolResult {
        database.memoryDao().insertMemory(
            MemoryEntity(key = key, value = value)
        )
        return ToolResult.Success("Remembered: \"$key\" is \"$value\". MYRA will use this for future personal context.")
    }

    private fun executeCalculate(expr: String): ToolResult {
        return try {
            val result = simpleEvaluate(expr)
            ToolResult.Success("Result: $expr = $result")
        } catch (e: Exception) {
            ToolResult.Error("Could not calculate '$expr': ${e.message}")
        }
    }

    private fun simpleEvaluate(str: String): Double {
        val clean = str.replace(" ", "")
        // Handle basic expressions
        return when {
            clean.contains("+") -> {
                val parts = clean.split("+", limit = 2)
                simpleEvaluate(parts[0]) + simpleEvaluate(parts[1])
            }
            clean.contains("-") && !clean.startsWith("-") -> {
                val parts = clean.split("-", limit = 2)
                simpleEvaluate(parts[0]) - simpleEvaluate(parts[1])
            }
            clean.contains("*") -> {
                val parts = clean.split("*", limit = 2)
                simpleEvaluate(parts[0]) * simpleEvaluate(parts[1])
            }
            clean.contains("/") -> {
                val parts = clean.split("/", limit = 2)
                val divisor = simpleEvaluate(parts[1])
                if (divisor == 0.0) throw ArithmeticException("Division by zero")
                simpleEvaluate(parts[0]) / divisor
            }
            clean.contains("%") -> {
                val parts = clean.split("%", limit = 2)
                simpleEvaluate(parts[0]) % simpleEvaluate(parts[1])
            }
            else -> clean.toDouble()
        }
    }
}
