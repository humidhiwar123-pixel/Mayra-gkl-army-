package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.services.MyraNotificationListenerService
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationCenterScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    val notifications by MyraNotificationListenerService.notifications.collectAsStateWithLifecycle()
    val isConnected by MyraNotificationListenerService.isListenerConnected.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(notifications, searchQuery) {
        if (searchQuery.isBlank()) notifications
        else notifications.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.text.contains(searchQuery, ignoreCase = true) ||
            it.appTitle.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NOTIFICATION CENTER", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Intercept, filter, and summarize system notifications", color = MyraTextSecondary, fontSize = 12.sp)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isConnected) MyraAccentGreen.copy(alpha = 0.2f) else MyraRedSubtle
                ) {
                    Text(
                        text = if (isConnected) "SERVICE ACTIVE" else "ACCESS REQUIRED",
                        color = if (isConnected) MyraAccentGreen else MyraRedPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (!isConnected) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedPrimary, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Enable Notification Access", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "To let MYRA read and summarize your notifications, grant Notification Listener permission in Android settings.",
                            color = MyraTextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                            modifier = Modifier.testTag("enable_notification_access_btn")
                        ) {
                            Text("OPEN SETTINGS")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter notifications...", color = MyraTextTertiary, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val texts = filteredList.take(5).joinToString("\n") { "${it.appTitle}: ${it.title} - ${it.text}" }
                        viewModel.sendUserMessage("Summarize my recent notifications:\n$texts")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary)
                ) {
                    Text("SUMMARIZE", fontSize = 11.sp)
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Text(
                    text = if (isConnected) "No active notifications intercepted." else "Connect service to view notifications.",
                    color = MyraTextTertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(filteredList) { notif ->
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(notif.timestamp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(notif.appTitle, color = MyraRedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(timeStr, color = MyraTextTertiary, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notif.title, color = MyraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(notif.text, color = MyraTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
