package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AiOrb
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import com.example.voice.VoiceState

@Composable
fun HomeScreen(
    viewModel: MyraViewModel,
    onNavigate: (String) -> Unit
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val batteryPct by viewModel.currentBatteryPct.collectAsStateWithLifecycle()
    val isCharging by viewModel.isCharging.collectAsStateWithLifecycle()
    val automations by viewModel.automations.collectAsStateWithLifecycle()
    val recentChats by viewModel.chatMessages.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val rmsDb by viewModel.rmsDb.collectAsStateWithLifecycle()

    var quickInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. Header & Greeting
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HELLO, ${userName.uppercase()}",
                        color = MyraTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "MYRA Neural Core • Online",
                        color = MyraRedPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Battery Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MyraCardBg,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MyraRedBorder, Color.Transparent)))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Default.Bolt else Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery",
                            tint = if (batteryPct > 20) MyraAccentGreen else MyraRedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$batteryPct%",
                            color = MyraTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Centerpiece AI Orb
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                AiOrb(
                    voiceState = voiceState,
                    rmsDb = rmsDb,
                    onClick = {
                        if (voiceState == VoiceState.LISTENING) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    }
                )
            }
        }

        // 3. Command Input Bar
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MyraCardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = quickInput,
                        onValueChange = { quickInput = it },
                        placeholder = { Text("Ask MYRA (Hindi / English / Hinglish)...", color = MyraTextTertiary, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_command_input")
                    )

                    IconButton(
                        onClick = {
                            if (quickInput.isNotBlank()) {
                                viewModel.sendUserMessage(quickInput)
                                quickInput = ""
                                onNavigate(Screen.Chat.route)
                            }
                        },
                        modifier = Modifier.testTag("home_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MyraRedPrimary
                        )
                    }

                    IconButton(
                        onClick = { onNavigate(Screen.Voice.route) },
                        modifier = Modifier.testTag("home_voice_screen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Assistant",
                            tint = MyraAccentCyan
                        )
                    }
                }
            }
        }

        // 4. Quick Actions Hub
        item {
            Text(
                text = "SYSTEM MODULES",
                color = MyraTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val hubs = listOf(
                    Triple("WhatsApp", Icons.Default.Message, Screen.Communication.route),
                    Triple("Camera Vision", Icons.Default.PhotoCamera, Screen.Photos.route),
                    Triple("Device Tools", Icons.Default.Build, Screen.DeviceTools.route),
                    Triple("PC Connect", Icons.Default.Laptop, Screen.PcConnect.route),
                    Triple("Automations", Icons.Default.Schedule, Screen.Automations.route),
                    Triple("Deep Research", Icons.Default.TravelExplore, Screen.DeepResearch.route),
                    Triple("My World Maps", Icons.Default.Navigation, Screen.Maps.route),
                    Triple("File Manager", Icons.Default.Folder, Screen.FileManager.route)
                )

                items(hubs) { (label, icon, route) ->
                    QuickHubCard(label = label, icon = icon, onClick = { onNavigate(route) })
                }
            }
        }

        // 5. Active Automations
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE AUTOMATIONS (${automations.filter { it.isEnabled }.size})",
                    color = MyraTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = { onNavigate(Screen.Automations.route) }) {
                    Text("MANAGE", color = MyraRedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (automations.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MyraCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No automations registered. Say: \"Kal 7 baje alarm laga do\" or \"When battery is 20% notify me\".",
                        color = MyraTextTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(automations.take(2)) { auto ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MyraCardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (auto.isEnabled) MyraRedBorder else Color.DarkGray, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(auto.title, color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Trigger: ${auto.triggerType} ${auto.triggerValue} • Action: ${auto.actionType}",
                                color = MyraTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = auto.isEnabled,
                            onCheckedChange = { viewModel.toggleAutomation(auto) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MyraRedPrimary
                            )
                        )
                    }
                }
            }
        }

        // 6. Recent Conversation Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT CONVERSATIONS",
                    color = MyraTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = { onNavigate(Screen.Chat.route) }) {
                    Text("VIEW ALL", color = MyraRedPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(recentChats.takeLast(3).reversed()) { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (msg.sender == "user") MyraElevatedCard else MyraCardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Screen.Chat.route) }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (msg.sender == "user") userName else "MYRA AI",
                            color = if (msg.sender == "user") MyraAccentAmber else MyraRedPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (msg.toolName != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MyraRedSubtle
                            ) {
                                Text(
                                    text = "⚡ ${msg.toolName}",
                                    color = MyraRedPrimary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg.text.take(90) + if (msg.text.length > 90) "..." else "",
                        color = MyraTextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickHubCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MyraCardBg,
        modifier = Modifier
            .size(width = 110.dp, height = 85.dp)
            .clickable { onClick() }
            .border(1.dp, MyraRedBorder, RoundedCornerShape(14.dp))
            .testTag("hub_card_$label")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MyraRedPrimary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = MyraTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
