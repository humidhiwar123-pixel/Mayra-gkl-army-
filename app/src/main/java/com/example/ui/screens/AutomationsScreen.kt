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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AutomationsScreen(
    viewModel: MyraViewModel
) {
    val automations by viewModel.automations.collectAsStateWithLifecycle()
    val history by viewModel.automationHistory.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var triggerType by remember { mutableStateOf("BATTERY") }
    var triggerValue by remember { mutableStateOf("20") }
    var actionType by remember { mutableStateOf("NOTIFICATION") }
    var actionPayload by remember { mutableStateOf("Battery low! Connect charger now.") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = MyraCardBg,
            title = {
                Text("NEW AUTOMATION ROUTINE", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Routine Title") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                    )

                    Text("TRIGGER TYPE", color = MyraTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("BATTERY", "DAILY", "TIME").forEach { type ->
                            FilterChip(
                                selected = triggerType == type,
                                onClick = {
                                    triggerType = type
                                    if (type == "BATTERY") triggerValue = "20"
                                    else if (type == "DAILY") triggerValue = "20:00"
                                    else triggerValue = "07:00"
                                },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = triggerValue,
                        onValueChange = { triggerValue = it },
                        label = { Text("Trigger Value (e.g. 20% or 08:00)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                    )

                    Text("ACTION TYPE", color = MyraTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("NOTIFICATION", "SPEAK", "LAUNCH_APP").forEach { action ->
                            FilterChip(
                                selected = actionType == action,
                                onClick = { actionType = action },
                                label = { Text(action, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = actionPayload,
                        onValueChange = { actionPayload = it },
                        label = { Text("Action Payload (Message / Package)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.isNotBlank()) {
                            viewModel.addAutomation(titleInput, triggerType, triggerValue, actionType, actionPayload)
                            showCreateDialog = false
                            titleInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary)
                ) {
                    Text("SAVE ROUTINE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
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
                    Text("AUTOMATION ENGINE", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Background routines, rules, and system triggers", color = MyraTextSecondary, fontSize = 12.sp)
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                    modifier = Modifier.testTag("add_automation_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD")
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
                        text = "No automations registered. Tap '+ ADD' above or ask MYRA in chat.",
                        color = MyraTextTertiary,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(automations) { auto ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (auto.isEnabled) MyraRedBorder else Color.DarkGray, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(auto.title, color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Switch(
                                checked = auto.isEnabled,
                                onCheckedChange = { viewModel.toggleAutomation(auto) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MyraRedPrimary)
                            )
                        }

                        Text("Trigger: ${auto.triggerType} ${auto.triggerValue}", color = MyraAccentAmber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Action: ${auto.actionType} -> ${auto.actionPayload}", color = MyraTextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.runAutomationNow(auto) },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("RUN NOW", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteAutomation(auto) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MyraRedPrimary)
                            }
                        }
                    }
                }
            }
        }

        // History Log
        item {
            Text(
                text = "EXECUTION HISTORY (${history.size})",
                color = MyraTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(history.take(15)) { h ->
            val fmt = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(h.executedAt))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MyraCardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (h.status == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (h.status == "SUCCESS") MyraAccentGreen else MyraRedPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(h.title, color = MyraTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(h.detail, color = MyraTextTertiary, fontSize = 11.sp)
                    }
                    Text(fmt, color = MyraTextTertiary, fontSize = 10.sp)
                }
            }
        }
    }
}
