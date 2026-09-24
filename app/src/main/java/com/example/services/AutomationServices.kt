package com.example.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.MainActivity
import com.example.MyraApplication
import com.example.R
import com.example.data.local.AutomationEntity
import com.example.data.local.AutomationHistoryEntity
import com.example.data.local.MyraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AutomationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        val automationId = intent?.getLongExtra("automation_id", -1L) ?: -1L

        CoroutineScope(Dispatchers.IO).launch {
            val db = MyraDatabase.getDatabase(context)
            if (automationId != -1L) {
                val auto = db.automationDao().getAutomationById(automationId)
                if (auto != null && auto.isEnabled) {
                    executeAutomation(context, db, auto)
                }
            } else if (Intent.ACTION_BATTERY_LOW == action || Intent.ACTION_BATTERY_CHANGED == action) {
                checkBatteryAutomations(context, db, intent)
            }
        }
    }

    private suspend fun checkBatteryAutomations(context: Context, db: MyraDatabase, intent: Intent?) {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0) return
        val pct = (level * 100) / scale

        val automations = db.automationDao().getActiveAutomations()
        for (auto in automations) {
            if (auto.triggerType.equals("BATTERY", ignoreCase = true)) {
                val targetPct = auto.triggerValue.toIntOrNull() ?: 20
                if (pct <= targetPct) {
                    executeAutomation(context, db, auto)
                }
            }
        }
    }

    private suspend fun executeAutomation(context: Context, db: MyraDatabase, auto: AutomationEntity) {
        try {
            when (auto.actionType.uppercase()) {
                "NOTIFICATION" -> {
                    showAutomationNotification(context, auto.title, auto.actionPayload)
                }
                "LAUNCH_APP" -> {
                    val pm = context.packageManager
                    val launchIntent = pm.getLaunchIntentForPackage(auto.actionPayload)
                    if (launchIntent != null) {
                        launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(launchIntent)
                    }
                }
                "SPEAK" -> {
                    showAutomationNotification(context, "MYRA Automation", auto.actionPayload)
                }
            }

            db.automationDao().updateAutomation(auto.copy(lastRunAt = System.currentTimeMillis()))
            db.automationDao().insertHistory(
                AutomationHistoryEntity(
                    automationId = auto.id,
                    title = auto.title,
                    status = "SUCCESS",
                    detail = "Executed ${auto.actionType}: ${auto.actionPayload}"
                )
            )
        } catch (e: Exception) {
            db.automationDao().insertHistory(
                AutomationHistoryEntity(
                    automationId = auto.id,
                    title = auto.title,
                    status = "FAILED",
                    detail = "Error: ${e.localizedMessage}"
                )
            )
        }
    }

    private fun showAutomationNotification(context: Context, title: String, text: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 10000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MyraApplication.AUTOMATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.myra_icon)
            .setContentTitle("⚡ MYRA: $title")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}

class AutomationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = MyraDatabase.getDatabase(appContext)
        val activeAutomations = db.automationDao().getActiveAutomations()

        for (auto in activeAutomations) {
            // Periodic background check
            if (auto.triggerType.equals("DAILY", ignoreCase = true) || auto.triggerType.equals("SCHEDULE", ignoreCase = true)) {
                // If not run today, run and record history
                val lastRun = auto.lastRunAt ?: 0L
                val oneDayMs = 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastRun > oneDayMs) {
                    val intent = Intent(appContext, AutomationAlarmReceiver::class.java).apply {
                        putExtra("automation_id", auto.id)
                    }
                    appContext.sendBroadcast(intent)
                }
            }
        }
        return Result.success()
    }

    companion object {
        fun schedulePeriodicCheck(context: Context) {
            try {
                val workRequest = PeriodicWorkRequestBuilder<AutomationWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(false)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    "myra_automation_routine",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
