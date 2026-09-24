package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel

@Composable
fun DeepResearchScreen(
    viewModel: MyraViewModel
) {
    var researchTopic by remember { mutableStateOf("") }
    val researchResult by viewModel.deepResearchResult.collectAsStateWithLifecycle()
    val isResearching by viewModel.isDeepResearching.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("DEEP RESEARCH ENGINE", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Synthesizes multi-source facts, market analysis, and comprehensive reports", color = MyraTextSecondary, fontSize = 12.sp)
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = researchTopic,
                        onValueChange = { researchTopic = it },
                        label = { Text("Research Subject / Topic") },
                        placeholder = { Text("e.g. 'Future of humanoid robotics 2026-2030' or 'EV infrastructure in India'") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedBorderColor = MyraRedPrimary,
                            unfocusedBorderColor = MyraRedBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("deep_research_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (researchTopic.isNotBlank()) {
                                viewModel.performDeepResearch(researchTopic)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("start_research_btn")
                    ) {
                        if (isResearching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(imageVector = Icons.Default.TravelExplore, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isResearching) "SYNTHESIZING REPORT..." else "COMMENCE DEEP RESEARCH")
                    }
                }
            }
        }

        if (researchResult != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SYNTHESIS REPORT", color = MyraRedPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(researchResult ?: "")) }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MyraAccentCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = researchResult!!,
                            color = MyraTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
