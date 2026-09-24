package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "myra_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_VOICE_LANG = stringPreferencesKey("voice_lang") // "hinglish", "hindi", "english"
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val KEY_PARKING_LAT = stringPreferencesKey("parking_lat")
        val KEY_PARKING_LNG = stringPreferencesKey("parking_lng")
        val KEY_PARKING_TIME = stringPreferencesKey("parking_time")
        val KEY_PARKING_NOTE = stringPreferencesKey("parking_note")
        val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val KEY_PC_SYNC_PORT = stringPreferencesKey("pc_sync_port")
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "Sohan"
    }

    val voiceLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOICE_LANG] ?: "hinglish"
    }

    val speechRate: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_RATE] ?: 1.0f
    }

    val speechPitch: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_PITCH] ?: 1.0f
    }

    val autoSpeak: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SPEAK] ?: true
    }

    val appLockPin: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_LOCK_PIN]
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val developerMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVELOPER_MODE] ?: false
    }

    val parkingLocation: Flow<Triple<Double?, Double?, String?>> = context.dataStore.data.map { prefs ->
        val lat = prefs[KEY_PARKING_LAT]?.toDoubleOrNull()
        val lng = prefs[KEY_PARKING_LNG]?.toDoubleOrNull()
        val note = prefs[KEY_PARKING_NOTE]
        Triple(lat, lng, note)
    }

    val customApiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_API_KEY]
    }

    val pcSyncPort: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PC_SYNC_PORT] ?: "8088"
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH] ?: true
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setVoiceLanguage(lang: String) {
        context.dataStore.edit { it[KEY_VOICE_LANG] = lang }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[KEY_SPEECH_PITCH] = pitch }
    }

    suspend fun setAutoSpeak(auto: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SPEAK] = auto }
    }

    suspend fun setAppLockPin(pin: String?) {
        context.dataStore.edit {
            if (pin == null) it.remove(KEY_APP_LOCK_PIN)
            else it[KEY_APP_LOCK_PIN] = pin
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DEVELOPER_MODE] = enabled }
    }

    suspend fun saveParkingLocation(lat: Double, lng: Double, note: String) {
        context.dataStore.edit {
            it[KEY_PARKING_LAT] = lat.toString()
            it[KEY_PARKING_LNG] = lng.toString()
            it[KEY_PARKING_TIME] = System.currentTimeMillis().toString()
            it[KEY_PARKING_NOTE] = note
        }
    }

    suspend fun clearParkingLocation() {
        context.dataStore.edit {
            it.remove(KEY_PARKING_LAT)
            it.remove(KEY_PARKING_LNG)
            it.remove(KEY_PARKING_TIME)
            it.remove(KEY_PARKING_NOTE)
        }
    }

    suspend fun setCustomApiKey(key: String?) {
        context.dataStore.edit {
            if (key == null || key.isBlank()) it.remove(KEY_CUSTOM_API_KEY)
            else it[KEY_CUSTOM_API_KEY] = key
        }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { it[KEY_FIRST_LAUNCH] = false }
    }
}
