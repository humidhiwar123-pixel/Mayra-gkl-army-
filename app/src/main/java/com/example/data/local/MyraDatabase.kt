package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        AutomationEntity::class,
        AutomationHistoryEntity::class,
        MemoryEntity::class,
        ActionLogEntity::class,
        ConnectedDeviceEntity::class,
        ConnectorEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MyraDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun automationDao(): AutomationDao
    abstract fun memoryDao(): MemoryDao
    abstract fun actionLogDao(): ActionLogDao
    abstract fun deviceDao(): DeviceDao
    abstract fun connectorDao(): ConnectorDao

    companion object {
        @Volatile
        private var INSTANCE: MyraDatabase? = null

        fun getDatabase(context: Context): MyraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyraDatabase::class.java,
                    "myra_assistant_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
