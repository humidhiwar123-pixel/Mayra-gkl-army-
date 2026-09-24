package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import kotlinx.coroutines.launch

enum class AiToolCategory(val title: String, val icon: ImageVector) {
    WRITER("AI Writer", Icons.Default.EditNote),
    TRANSLATOR("Translator", Icons.Default.Translate),
    SUMMARIZER("Summarizer", Icons.Default.Compress),
    CODE("Code Assistant", Icons.Default.Code),
    WEB_BUILDER("Website Builder", Icons.Default.Web),
    IMAGE_GEN("Image Gen", Icons.Default.Image),
    CALCULATOR("Calculator", Icons.Default.Calculate)
}

@Composable
fun AiToolsScreen(
    viewModel: MyraViewModel,
    onNavigate: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(AiToolCategory.WRITER) }
    var toolInput by remember { mutableStateOf("") }
    var toolOutput by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp)
    ) {
        // Top Tools Horizontal Selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AiToolCategory.values()) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MyraRedPrimary else MyraCardBg,
                    modifier = Modifier
                        .clickable {
                            selectedCategory = cat
                            toolInput = ""
                            toolOutput = ""
                        }
                        .border(1.dp, if (isSelected) MyraRedGlow else MyraRedBorder, RoundedCornerShape(12.dp))
                        .testTag("ai_tool_tab_${cat.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.title,
                            tint = if (isSelected) Color.White else MyraTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.title,
                            color = if (isSelected) Color.White else MyraTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tool Content Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = when (selectedCategory) {
                        AiToolCategory.WRITER -> "AI Writer: Draft professional emails, blogs, or essays"
                        AiToolCategory.TRANSLATOR -> "AI Translator: Translate naturally between Hindi & English"
                        AiToolCategory.SUMMARIZER -> "AI Summarizer: Paste long text, reports or notes"
                        AiToolCategory.CODE -> "Code Assistant: Generate Kotlin, Python, or SQL code"
                        AiToolCategory.WEB_BUILDER -> "Website Builder: Generate modern responsive HTML/CSS"
                        AiToolCategory.IMAGE_GEN -> "AI Image Prompt Studio: Design visual concepts"
                        AiToolCategory.CALCULATOR -> "Smart Calculator: Compute expressions & math questions"
                    },
                    color = MyraTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                OutlinedTextField(
                    value = toolInput,
                    onValueChange = { toolInput = it },
                    placeholder = {
                        Text(
                            text = when (selectedCategory) {
                                AiToolCategory.WRITER -> "Enter topic e.g. 'Leave email to boss for 2 days' or 'Blog on quantum computing'"
                                AiToolCategory.TRANSLATOR -> "Enter text to translate (Hindi or English)..."
                                AiToolCategory.SUMMARIZER -> "Paste long text here to summarize..."
                                AiToolCategory.CODE -> "Enter programming request e.g. 'Room database migration in Kotlin'..."
                                AiToolCategory.WEB_BUILDER -> "Describe landing page e.g. 'Cyberpunk portfolio with hero banner'..."
                                AiToolCategory.IMAGE_GEN -> "Describe visual scene e.g. 'Cybernetic assistant in neon rain'..."
                                AiToolCategory.CALCULATOR -> "e.g. '1250 * 18 / 100' or 'monthly payment for 50000 loan at 8% for 3 years'..."
                            },
                            color = MyraTextTertiary,
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MyraRedPrimary,
                        unfocusedBorderColor = MyraRedBorder,
                        focusedTextColor = MyraTextPrimary,
                        unfocusedTextColor = MyraTextPrimary,
                        focusedContainerColor = MyraCardBg,
                        unfocusedContainerColor = MyraCardBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("ai_tool_input")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (toolInput.isNotBlank()) {
                                isRunning = true
                                coroutineScope.launch {
                                    val prompt = when (selectedCategory) {
                                        AiToolCategory.WRITER -> "Write the following in a polished, ready-to-use format: $toolInput"
                                        AiToolCategory.TRANSLATOR -> "Translate the following accurately. If it is in Hindi/Hinglish, translate to English. If English, translate to Hindi:\n$toolInput"
                                        AiToolCategory.SUMMARIZER -> "Summarize the key points of the following text with bullet points:\n$toolInput"
                                        AiToolCategory.CODE -> "Write clean, production-grade code for: $toolInput. Provide code blocks with brief comments."
                                        AiToolCategory.WEB_BUILDER -> "Build a complete standalone single-file HTML & CSS web layout for: $toolInput."
                                        AiToolCategory.IMAGE_GEN -> "Generate a detailed creative prompt and visual description for image generation: $toolInput"
                                        AiToolCategory.CALCULATOR -> "Calculate and explain step by step: $toolInput"
                                    }

                                    viewModel.sendUserMessage(prompt)
                                    toolOutput = "Output synthesized and appended to Chat History! Check response below:\n\nProcessing request with MYRA Neural Core..."
                                    isRunning = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                        modifier = Modifier.testTag("ai_tool_generate_button")
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GENERATE WITH MYRA")
                    }

                    if (toolOutput.isNotBlank()) {
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(toolOutput)) }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = MyraAccentCyan)
                        }
                    }
                }
            }

            // Quick Shortcut Card to Deep Research & Photo Vision
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MyraCardBg,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(Screen.DeepResearch.route) }
                            .border(1.dp, MyraRedBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.TravelExplore, contentDescription = null, tint = MyraAccentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Deep Research", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Web sources & facts", color = MyraTextTertiary, fontSize = 10.sp)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MyraCardBg,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(Screen.Photos.route) }
                            .border(1.dp, MyraRedBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = MyraRedPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Photo OCR & AI", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Image understanding", color = MyraTextTertiary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
