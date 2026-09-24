package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MyraApplication
import com.example.ai.AssistantResponse
import com.example.ai.PendingToolCall
import com.example.ai.ToolResult
import com.example.data.local.*
import com.example.data.repository.MyraRepository
import com.example.voice.VoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class MyraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MyraRepository(
        context = application.applicationContext,
        database = (application as MyraApplication).database,
        preferences = (application as MyraApplication).preferencesManager
    )

    // UI States
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automations: StateFlow<List<AutomationEntity>> = repository.automations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automationHistory: StateFlow<List<AutomationHistoryEntity>> = repository.automationHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.memories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionLogs: StateFlow<List<ActionLogEntity>> = repository.actionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userName: StateFlow<String> = repository.preferences.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Boss")

    val voiceLanguage: StateFlow<String> = repository.preferences.voiceLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "hinglish")

    val autoSpeak: StateFlow<Boolean> = repository.preferences.autoSpeak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val developerMode: StateFlow<Boolean> = repository.preferences.developerMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFirstLaunch: StateFlow<Boolean> = repository.preferences.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _currentBatteryPct = MutableStateFlow(85)
    val currentBatteryPct: StateFlow<Int> = _currentBatteryPct.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    // Deep research state
    private val _deepResearchResult = MutableStateFlow<String?>(null)
    val deepResearchResult: StateFlow<String?> = _deepResearchResult.asStateFlow()

    private val _isDeepResearching = MutableStateFlow(false)
    val isDeepResearching: StateFlow<Boolean> = _isDeepResearching.asStateFlow()

    // Voice Manager
    val voiceManager = VoiceManager(application.applicationContext) { spokenText ->
        onVoiceInputReceived(spokenText)
    }

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val liveTranscript: StateFlow<String> = voiceManager.liveTranscript
    val rmsDb: StateFlow<Float> = voiceManager.rmsDb

    init {
        updateBatteryState()
        viewModelScope.launch {
            repository.preferences.speechRate.collect { rate ->
                val pitch = repository.preferences.speechPitch.first()
                val lang = repository.preferences.voiceLanguage.first()
                val loc = when (lang) {
                    "hindi" -> Locale("hi", "IN")
                    "english" -> Locale.US
                    else -> Locale("hi", "IN")
                }
                voiceManager.applyVoiceSettings(loc, rate, pitch)
            }
        }
    }

    fun updateBatteryState() {
        try {
            val filter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryIntent = getApplication<Application>().registerReceiver(null, filter)
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            if (level >= 0 && scale > 0) {
                _currentBatteryPct.value = (level * 100) / scale
            }
            _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {}
    }

    fun sendUserMessage(text: String, imageBitmap: Bitmap? = null) {
        if (text.isBlank() && imageBitmap == null) return
        viewModelScope.launch {
            _isProcessing.value = true
            repository.saveUserMessage(text)

            val response = repository.processQuery(text, imageBitmap)
            handleAssistantResponse(response)
            _isProcessing.value = false
        }
    }

    private fun onVoiceInputReceived(spokenText: String) {
        if (spokenText.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            repository.saveUserMessage(spokenText)

            val response = repository.processQuery(spokenText)
            handleAssistantResponse(response)
            _isProcessing.value = false

            if (autoSpeak.value) {
                voiceManager.speak(response.textResponse)
            }
        }
    }

    private fun handleAssistantResponse(response: AssistantResponse) {
        if (response.toolResult is ToolResult.RequiresConfirmation) {
            _pendingConfirmation.value = PendingConfirmation(
                title = response.toolResult.title,
                details = response.toolResult.details,
                pendingCall = response.toolResult.pendingCall
            )
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.toolExecutor.executeTool(
                toolName = pending.pendingCall.toolName,
                arguments = pending.pendingCall.arguments,
                userConfirmed = true
            )
            val msg = when (result) {
                is ToolResult.Success -> "✅ Confirmed: ${result.message}"
                is ToolResult.Error -> "❌ Failed: ${result.errorMessage}"
                else -> result.toString()
            }
            repository.saveMyraMessage(msg, toolName = pending.pendingCall.toolName, toolStatus = "CONFIRMED")
            _isProcessing.value = false
            if (autoSpeak.value) {
                voiceManager.speak(msg)
            }
        }
    }

    fun cancelPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            repository.saveMyraMessage(
                "Action \"${pending.pendingCall.toolName}\" was cancelled by user.",
                toolName = pending.pendingCall.toolName,
                toolStatus = "CANCELLED"
            )
        }
    }

    fun startListening() {
        val lang = voiceLanguage.value
        voiceManager.startListening(lang)
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun interruptSpeech() {
        voiceManager.stopSpeaking()
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            repository.preferences.setUserName(name)
        }
    }

    fun setVoiceLanguage(lang: String) {
        viewModelScope.launch {
            repository.preferences.setVoiceLanguage(lang)
            val loc = when (lang) {
                "hindi" -> Locale("hi", "IN")
                "english" -> Locale.US
                else -> Locale("hi", "IN")
            }
            voiceManager.applyVoiceSettings(loc)
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            repository.preferences.setSpeechRate(rate)
        }
    }

    fun setAutoSpeak(auto: Boolean) {
        viewModelScope.launch {
            repository.preferences.setAutoSpeak(auto)
        }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.preferences.setDeveloperMode(enabled)
        }
    }

    fun addAutomation(title: String, triggerType: String, triggerValue: String, actionType: String, actionPayload: String) {
        viewModelScope.launch {
            repository.addAutomation(title, triggerType, triggerValue, actionType, actionPayload)
        }
    }

    fun toggleAutomation(auto: AutomationEntity) {
        viewModelScope.launch {
            repository.toggleAutomation(auto)
        }
    }

    fun deleteAutomation(auto: AutomationEntity) {
        viewModelScope.launch {
            repository.deleteAutomation(auto)
        }
    }

    fun runAutomationNow(auto: AutomationEntity) {
        viewModelScope.launch {
            val intent = Intent(getApplication(), com.example.services.AutomationAlarmReceiver::class.java).apply {
                putExtra("automation_id", auto.id)
            }
            getApplication<Application>().sendBroadcast(intent)
        }
    }

    fun saveMemory(key: String, value: String) {
        viewModelScope.launch {
            repository.saveMemory(key, value)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            repository.deleteMemory(memory)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun setFirstLaunchDone() {
        viewModelScope.launch {
            repository.preferences.setFirstLaunchCompleted()
        }
    }

    fun performDeepResearch(topic: String) {
        viewModelScope.launch {
            _isDeepResearching.value = true
            _deepResearchResult.value = null
            val prompt = """
                Perform deep research on: "$topic".
                Provide:
                1. Executive Summary
                2. Key Facts & Technical Breakdown
                3. Market / Practical Implications
                4. Primary Sources & Recommended Next Steps
                Write clearly in a structured, professional format.
            """.trimIndent()

            val res = repository.processQuery(prompt)
            _deepResearchResult.value = res.textResponse
            _isDeepResearching.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}

data class PendingConfirmation(
    val title: String,
    val details: String,
    val pendingCall: PendingToolCall
)
