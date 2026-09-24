package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.MyraDatabase
import com.example.data.local.PreferencesManager

class MyraApplication : Application() {

    lateinit var database: MyraDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = MyraDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val automationChannel = NotificationChannel(
                AUTOMATION_CHANNEL_ID,
                "MYRA Automations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggered alerts from MYRA automation routines"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(automationChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "myra_assistant_channel"
        const val AUTOMATION_CHANNEL_ID = "myra_automation_channel"

        lateinit var instance: MyraApplication
            private set
    }
}
