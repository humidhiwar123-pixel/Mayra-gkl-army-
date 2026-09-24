package com.example.ui.screens

import android.content.Intent
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
import com.example.services.MyraAccessibilityService
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel

@Composable
fun ScreenAutomationScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    val isServiceActive by MyraAccessibilityService.isServiceActive.collectAsStateWithLifecycle()
    val activePkg by MyraAccessibilityService.activePackage.collectAsStateWithLifecycle()
    val screenTexts by MyraAccessibilityService.visibleScreenTexts.collectAsStateWithLifecycle()

    var clickTargetText by remember { mutableStateOf("") }
    var inputTargetText by remember { mutableStateOf("") }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SCREEN AUTOMATION", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Accessibility-driven assistive actions", color = MyraTextSecondary, fontSize = 12.sp)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isServiceActive) MyraAccentGreen.copy(alpha = 0.2f) else MyraRedSubtle
                ) {
                    Text(
                        text = if (isServiceActive) "ACCESSIBILITY ON" else "SERVICE OFF",
                        color = if (isServiceActive) MyraAccentGreen else MyraRedPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Safety Architecture Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = MyraAccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MANDATORY SAFETY BOUNDARIES", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Passwords and OTP entry are hardware-blocked.\n• Financial and banking payments are never automated.\n• Runs only when explicitly approved in Android Accessibility Settings.\n• Instant emergency cancel button available at all times.",
                        color = MyraTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        if (!isServiceActive) {
            item {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("open_accessibility_settings_btn")
                ) {
                    Text("OPEN ACCESSIBILITY SETTINGS")
                }
            }
        } else {
            // Live Screen Inspector
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("CURRENT FOREGROUND APP", color = MyraTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(if (activePkg.isNotBlank()) activePkg else "None active", color = MyraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("VISIBLE ELEMENTS DETECTED (${screenTexts.size})", color = MyraTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        screenTexts.take(4).forEach { t ->
                            Text("• $t", color = MyraTextSecondary, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tap button by text
                        OutlinedTextField(
                            value = clickTargetText,
                            onValueChange = { clickTargetText = it },
                            label = { Text("Target element text to tap") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val s = MyraAccessibilityService.instance
                                val res = s?.clickNodeByText(clickTargetText) ?: false
                                statusFeedback = if (res) "Tapped on \"$clickTargetText\"" else "Element not found or safety-blocked"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("EXECUTE ASSISTED TAP")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scroll buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    MyraAccessibilityService.instance?.performScrollDown()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SCROLL DOWN")
                            }

                            OutlinedButton(
                                onClick = {
                                    MyraAccessibilityService.instance?.performScrollUp()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SCROLL UP")
                            }
                        }

                        if (statusFeedback != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(statusFeedback!!, color = MyraAccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
