package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    val logs by viewModel.actionLogs.collectAsStateWithLifecycle()

    val permissions = listOf(
        Pair("Audio Recording (Microphone)", Manifest.permission.RECORD_AUDIO),
        Pair("Camera (Vision & Torch)", Manifest.permission.CAMERA),
        Pair("Fine Location (Navigation/Parking)", Manifest.permission.ACCESS_FINE_LOCATION),
        Pair("Phone Calls (Dialer)", Manifest.permission.CALL_PHONE),
        Pair("Send SMS", Manifest.permission.SEND_SMS)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("SECURITY & PERMISSIONS CENTER", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Verify hardware access, privacy bounds, and audit action records", color = MyraTextSecondary, fontSize = 12.sp)
        }

        // Hardware Permissions Dashboard
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DEVICE ACCESS STATUS", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    permissions.forEach { (label, perm) ->
                        val isGranted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = MyraTextSecondary, fontSize = 12.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isGranted) MyraAccentGreen.copy(alpha = 0.2f) else MyraDarkSurface
                            ) {
                                Text(
                                    text = if (isGranted) "GRANTED" else "NOT GRANTED",
                                    color = if (isGranted) MyraAccentGreen else MyraTextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Audit Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AUDIT LOGS (${logs.size})", color = MyraTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                TextButton(onClick = { viewModel.clearLogs() }) {
                    Text("CLEAR LOGS", color = MyraRedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Text("No actions logged yet.", color = MyraTextTertiary, fontSize = 12.sp)
            }
        } else {
            items(logs.take(30)) { log ->
                val timeStr = SimpleDateFormat("MMM dd, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tool: ${log.toolName}", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                log.status,
                                color = if (log.status == "SUCCESS") MyraAccentGreen else MyraRedPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Text(log.result, color = MyraTextSecondary, fontSize = 11.sp)
                        Text(timeStr, color = MyraTextTertiary, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
