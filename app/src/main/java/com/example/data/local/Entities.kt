package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String = "default",
    val sender: String, // "user" or "myra" or "system"
    val text: String,
    val imageUrl: String? = null,
    val toolName: String? = null,
    val toolStatus: String? = null, // "PENDING", "SUCCESS", "ERROR"
    val toolOutput: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerType: String, // "TIME", "BATTERY", "WIFI", "DAILY", "CHARGING"
    val triggerValue: String, // e.g. "08:00", "20", "SSID"
    val actionType: String, // "SPEAK", "NOTIFICATION", "LAUNCH_APP", "DEVICE_ACTION"
    val actionPayload: String, // e.g. text to speak, package name
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null
)

@Entity(tableName = "automation_history")
data class AutomationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val automationId: Long,
    val title: String,
    val status: String, // "SUCCESS", "FAILED"
    val detail: String,
    val executedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String, // Category or topic e.g. "Brother's Name", "Favorite Food"
    val value: String,
    val isConfirmed: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolName: String,
    val description: String,
    val status: String, // "SUCCESS", "FAILED", "CONFIRMATION_REQUIRED", "CANCELLED"
    val parameters: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "connected_devices")
data class ConnectedDeviceEntity(
    @PrimaryKey
    val deviceId: String,
    val deviceName: String,
    val deviceType: String, // "PC", "BROWSER"
    val ipAddress: String,
    val token: String,
    val isPaired: Boolean = true,
    val lastSeenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "connectors")
data class ConnectorEntity(
    @PrimaryKey
    val id: String, // "gemini", "google_drive", "github", "custom"
    val name: String,
    val isConnected: Boolean = false,
    val apiKeyOrToken: String? = null,
    val endpointUrl: String? = null,
    val scope: String? = null,
    val lastSyncAt: Long? = null
)
