package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import kotlinx.coroutines.launch

@Composable
fun DeviceToolsScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val batteryPct by viewModel.currentBatteryPct.collectAsStateWithLifecycle()
    val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()

    var isFlashlightActive by remember { mutableStateOf(false) }
    var alarmHour by remember { mutableStateOf("07") }
    var alarmMinute by remember { mutableStateOf("00") }
    var timerSeconds by remember { mutableStateOf("300") }
    var clipboardText by remember { mutableStateOf("") }

    val stat = remember {
        val s = StatFs(Environment.getDataDirectory().path)
        val free = (s.availableBlocksLong * s.blockSizeLong) / (1024 * 1024 * 1024)
        val total = (s.blockCountLong * s.blockSizeLong) / (1024 * 1024 * 1024)
        free to total
    }

    LaunchedEffect(Unit) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = cm?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            clipboardText = clip.getItemAt(0).text?.toString() ?: ""
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Flashlight & Battery Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Flashlight Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, if (isFlashlightActive) MyraAccentAmber else MyraRedBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashlightOn,
                            contentDescription = null,
                            tint = if (isFlashlightActive) MyraAccentAmber else MyraTextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("FLASHLIGHT", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val newState = !isFlashlightActive
                                isFlashlightActive = newState
                                coroutineScope.launch {
                                    viewModel.sendUserMessage(if (newState) "flashlight on karo" else "flashlight off karo")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFlashlightActive) MyraAccentAmber else MyraDarkSurface
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("toggle_flashlight_btn")
                        ) {
                            Text(if (isFlashlightActive) "TURNOFF" else "TURN ON", fontSize = 11.sp)
                        }
                    }
                }

                // Battery Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Default.Bolt else Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = if (batteryPct > 20) MyraAccentGreen else MyraRedPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("BATTERY: $batteryPct%", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isCharging) "Charging ⚡" else "Discharging", color = MyraTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Health: Normal", color = MyraTextTertiary, fontSize = 10.sp)
                    }
                }
            }
        }

        // Storage Metrics
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("INTERNAL STORAGE", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${stat.first} GB FREE / ${stat.second} GB", color = MyraRedPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = if (stat.second > 0) ((stat.second - stat.first).toFloat() / stat.second.toFloat()) else 0.5f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MyraRedPrimary,
                        trackColor = MyraDarkSurface
                    )
                }
            }
        }

        // Alarms & Timers
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ALARMS & TIMERS", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = alarmHour,
                            onValueChange = { alarmHour = it },
                            label = { Text("Hour (24h)") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                        )
                        OutlinedTextField(
                            value = alarmMinute,
                            onValueChange = { alarmMinute = it },
                            label = { Text("Minute") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.sendUserMessage("Set alarm for $alarmHour:$alarmMinute")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("set_alarm_btn")
                    ) {
                        Text("SET SYSTEM ALARM")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("COUNTDOWN TIMER", color = MyraTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = timerSeconds,
                            onValueChange = { timerSeconds = it },
                            label = { Text("Seconds") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.sendUserMessage("Start timer for $timerSeconds seconds")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraDarkSurface),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Text("START")
                        }
                    }
                }
            }
        }

        // System Settings Shortcuts
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYSTEM HARDWARE SHORTCUTS", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Wi-Fi")
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bluetooth")
                        }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Display")
                        }
                    }
                }
            }
        }
    }
}
