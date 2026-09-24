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

@Composable
fun MemoryScreen(
    viewModel: MyraViewModel
) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MyraCardBg,
            title = { Text("REMEMBER NEW FACT", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Topic / Key (e.g. Brother's Name)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Details / Fact (e.g. Rahul, Raipur)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newValue.isNotBlank()) {
                            viewModel.saveMemory(newKey, newValue)
                            newKey = ""
                            newValue = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary)
                ) {
                    Text("SAVE TO MEMORY")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) { Text("CANCEL") }
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
                    Text("PERSONAL MEMORY CORE", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Persistent facts remembered by MYRA", color = MyraTextSecondary, fontSize = 12.sp)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                    modifier = Modifier.testTag("add_memory_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("REMEMBER")
                }
            }
        }

        if (memories.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MyraCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No personal facts stored yet. Tell MYRA: \"Remember my brother's name is Rahul\" or tap '+ REMEMBER' above.",
                        color = MyraTextTertiary,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(memories) { mem ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mem.key, color = MyraRedPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(mem.value, color = MyraTextPrimary, fontSize = 13.sp)
                        }
                        IconButton(onClick = { viewModel.deleteMemory(mem) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Forget", tint = MyraTextTertiary)
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.clearAllMemories() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MyraRedPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FORGET ALL FACTS")
                }
            }
        }
    }
}
