package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessages(convId: String = "default"): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :convId")
    suspend fun clearConversation(convId: String = "default")

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMessage(): Flow<ChatMessageEntity?>
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun getAllAutomations(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE isEnabled = 1")
    suspend fun getActiveAutomations(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getAutomationById(id: Long): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: AutomationEntity): Long

    @Update
    suspend fun updateAutomation(automation: AutomationEntity)

    @Delete
    suspend fun deleteAutomation(automation: AutomationEntity)

    @Query("SELECT * FROM automation_history ORDER BY executedAt DESC LIMIT 50")
    fun getAutomationHistory(): Flow<List<AutomationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AutomationHistoryEntity): Long
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()
}

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActionLogEntity): Long

    @Query("DELETE FROM action_logs")
    suspend fun clearLogs()
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM connected_devices ORDER BY lastSeenAt DESC")
    fun getAllDevices(): Flow<List<ConnectedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: ConnectedDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: ConnectedDeviceEntity)

    @Query("SELECT * FROM connected_devices WHERE isPaired = 1 LIMIT 1")
    suspend fun getPrimaryDevice(): ConnectedDeviceEntity?
}

@Dao
interface ConnectorDao {
    @Query("SELECT * FROM connectors")
    fun getAllConnectors(): Flow<List<ConnectorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnector(connector: ConnectorEntity)

    @Query("SELECT * FROM connectors WHERE id = :id")
    suspend fun getConnector(id: String): ConnectorEntity?
}
