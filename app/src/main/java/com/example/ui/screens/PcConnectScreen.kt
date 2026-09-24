package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pc.PcCompanionManager
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel

@Composable
fun PcConnectScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    val serverStatus by PcCompanionManager.serverStatus.collectAsStateWithLifecycle()
    val localIp by PcCompanionManager.localIp.collectAsStateWithLifecycle()
    val pairingPin by PcCompanionManager.pairingPin.collectAsStateWithLifecycle()
    val connectedPcName by PcCompanionManager.connectedPcName.collectAsStateWithLifecycle()
    val transferHistory by PcCompanionManager.transferHistory.collectAsStateWithLifecycle()

    var textToSend by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        PcCompanionManager.startServer(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Connection Card
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Laptop, contentDescription = null, tint = MyraRedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PC WORKSTATION LINK", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (connectedPcName != null) MyraAccentGreen.copy(alpha = 0.2f) else MyraDarkSurface
                        ) {
                            Text(
                                text = if (connectedPcName != null) "CONNECTED: $connectedPcName" else "WAITING FOR PC",
                                color = if (connectedPcName != null) MyraAccentGreen else MyraTextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Device IP: http://$localIp:8088", color = MyraTextSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text("Server Status: $serverStatus", color = MyraTextTertiary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 6-digit PIN Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MyraElevatedCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SECURE PAIRING PIN", color = MyraTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = pairingPin,
                                    color = MyraRedPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 6.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Button(
                                onClick = { PcCompanionManager.generateNewPin() },
                                colors = ButtonDefaults.buttonColors(containerColor = MyraDarkSurface)
                            ) {
                                Text("REFRESH PIN", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Send Text / Clipboard to PC
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DISPATCH TO PC", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textToSend,
                        onValueChange = { textToSend = it },
                        placeholder = { Text("Type text or link to send to PC clipboard...", color = MyraTextTertiary, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedBorderColor = MyraRedPrimary,
                            unfocusedBorderColor = MyraRedBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("pc_text_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (textToSend.isNotBlank()) {
                                PcCompanionManager.sendTextToPc(textToSend)
                                textToSend = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("pc_send_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SEND TO PC")
                    }
                }
            }
        }

        // Companion Script Instructions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("HOW TO RUN PC COMPANION", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Ensure PC and Phone are on the same Wi-Fi.\n2. On PC, run: python pc_companion/myra_pc.py\n3. Enter IP: $localIp and the 6-digit PIN above.\n4. You can now transfer files and sync seamlessly.",
                        color = MyraTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Transfer History
        item {
            Text(
                text = "TRANSFER ACTIVITY (${transferHistory.size})",
                color = MyraTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (transferHistory.isEmpty()) {
            item {
                Text("No transfers recorded yet.", color = MyraTextTertiary, fontSize = 12.sp)
            }
        } else {
            items(transferHistory) { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MyraCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.type == "FILE") Icons.Default.InsertDriveFile else Icons.Default.Notes,
                            contentDescription = null,
                            tint = if (item.sender == "PC") MyraAccentCyan else MyraRedPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${item.sender}: ${item.content}", color = MyraTextPrimary, fontSize = 13.sp)
                            Text(item.type, color = MyraTextTertiary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
