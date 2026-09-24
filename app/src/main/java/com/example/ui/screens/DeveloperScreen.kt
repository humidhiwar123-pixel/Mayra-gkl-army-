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
import com.example.MyraApplication
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import kotlinx.coroutines.launch

@Composable
fun DeveloperScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = (context.applicationContext as MyraApplication).database
    val toolExec = remember { com.example.ai.ToolExecutor(context, repo) }

    var testToolName by remember { mutableStateOf("battery_status") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isExecuting by remember { mutableStateOf(false) }

    val automations by viewModel.automations.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val logs by viewModel.actionLogs.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = MyraRedPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DEVELOPER DIAGNOSTICS & SYSTEM CONSOLE", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Database Metrics
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ROOM DATABASE METRICS", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Chat Messages: ${messages.size}", color = MyraTextSecondary, fontSize = 12.sp)
                    Text("• Active Automations: ${automations.size}", color = MyraTextSecondary, fontSize = 12.sp)
                    Text("• Stored Memories: ${memories.size}", color = MyraTextSecondary, fontSize = 12.sp)
                    Text("• Action Logs: ${logs.size}", color = MyraTextSecondary, fontSize = 12.sp)
                }
            }
        }

        // Direct Tool Execution Console
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("DIRECT TOOL EXECUTION HARNESS", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Select a tool to test without Gemini mediation:", color = MyraTextTertiary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    val toolNames = listOf("battery_status", "device_info", "flashlight_control", "calculate")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        toolNames.forEach { t ->
                            FilterChip(
                                selected = testToolName == t,
                                onClick = { testToolName = t },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isExecuting = true
                            coroutineScope.launch {
                                val args = if (testToolName == "calculate") mapOf("expression" to "1024 * 768") else emptyMap()
                                val r = toolExec.executeTool(testToolName, args, userConfirmed = true)
                                testResult = when (r) {
                                    is com.example.ai.ToolResult.Success -> "SUCCESS: ${r.message}"
                                    is com.example.ai.ToolResult.Error -> "ERROR: ${r.errorMessage}"
                                    else -> r.toString()
                                }
                                isExecuting = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("exec_test_tool_btn")
                    ) {
                        Text("EXECUTE $testToolName")
                    }

                    if (testResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MyraElevatedCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testResult!!,
                                color = MyraAccentGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
