package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.MyraApplication
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MyraViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = (context.applicationContext as MyraApplication).preferencesManager

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val voiceLang by viewModel.voiceLanguage.collectAsStateWithLifecycle()
    val autoSpeak by viewModel.autoSpeak.collectAsStateWithLifecycle()
    val devMode by viewModel.developerMode.collectAsStateWithLifecycle()
    val customKey by prefs.customApiKey.collectAsState(initial = null)

    var nameInput by remember { mutableStateOf(userName) }
    var keyInput by remember { mutableStateOf(customKey ?: "") }

    LaunchedEffect(userName) { nameInput = userName }
    LaunchedEffect(customKey) { keyInput = customKey ?: "" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("MYRA SETTINGS & PREFERENCES", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        // Profile Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("USER PROFILE", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Your Preferred Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("settings_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.setUserName(nameInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("UPDATE NAME")
                    }
                }
            }
        }

        // Voice Language & Audio
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("VOICE SYNTHESIS & RECOGNITION", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Language Model Mode", color = MyraTextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("hinglish" to "Hinglish", "hindi" to "Hindi", "english" to "English").forEach { (code, label) ->
                            FilterChip(
                                selected = voiceLang == code,
                                onClick = { viewModel.setVoiceLanguage(code) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MyraRedPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Speak Voice Responses", color = MyraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("MYRA speaks answer aloud automatically", color = MyraTextTertiary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = autoSpeak,
                            onCheckedChange = { viewModel.setAutoSpeak(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MyraRedPrimary)
                        )
                    }
                }
            }
        }

        // Custom API Key Override
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GEMINI AI KEY OVERRIDE", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Default key is auto-loaded from build environment. You can optionally paste a custom key below:", color = MyraTextTertiary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Custom Gemini API Key") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch { prefs.setCustomApiKey(keyInput.ifBlank { null }) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SAVE KEY")
                        }
                        if (!customKey.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        prefs.setCustomApiKey(null)
                                        keyInput = ""
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("RESET")
                            }
                        }
                    }
                }
            }
        }

        // Subsystem Links
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingLinkRow("Personal Memory Core", Icons.Default.Psychology) { onNavigate(Screen.Memory.route) }
                    SettingLinkRow("Security & Audit Logs", Icons.Default.Security) { onNavigate(Screen.Security.route) }
                    SettingLinkRow("Developer Hub & Diagnostics", Icons.Default.Terminal) { onNavigate(Screen.Developer.route) }
                    SettingLinkRow("About MYRA AI", Icons.Default.Info) { onNavigate(Screen.About.route) }
                }
            }
        }
    }
}

@Composable
fun SettingLinkRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MyraRedPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = MyraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MyraTextTertiary)
    }
}
