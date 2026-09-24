package com.example.services

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedNotification(
    val key: String,
    val packageName: String,
    val appTitle: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val replyPendingIntent: PendingIntent? = null
)

class MyraNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        extractAndStore(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val currentList = _notifications.value.toMutableList()
        currentList.removeAll { it.key == sbn.key }
        _notifications.value = currentList
    }

    private fun extractAndStore(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val appLabel = try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        if (title.isBlank() && text.isBlank()) return

        // Extract direct reply pending intent if available
        var replyIntent: PendingIntent? = null
        sbn.notification.actions?.forEach { action ->
            if (action.remoteInputs != null && action.remoteInputs.isNotEmpty()) {
                replyIntent = action.actionIntent
            }
        }

        val captured = CapturedNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appTitle = appLabel,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            replyPendingIntent = replyIntent
        )

        val list = _notifications.value.toMutableList()
        list.removeAll { it.key == sbn.key }
        list.add(0, captured)
        _notifications.value = list.take(100)
    }

    private fun refreshActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            for (sbn in active) {
                extractAndStore(sbn)
            }
        } catch (_: Exception) {}
    }

    companion object {
        private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        fun clearNotifications() {
            _notifications.value = emptyList()
        }
    }
}
