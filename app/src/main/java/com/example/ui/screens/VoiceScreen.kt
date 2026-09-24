package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AiOrb
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import com.example.voice.VoiceState

@Composable
fun VoiceScreen(
    viewModel: MyraViewModel
) {
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val liveTranscript by viewModel.liveTranscript.collectAsStateWithLifecycle()
    val rmsDb by viewModel.rmsDb.collectAsStateWithLifecycle()
    val voiceLang by viewModel.voiceLanguage.collectAsStateWithLifecycle()
    val autoSpeak by viewModel.autoSpeak.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Language Switcher Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val languages = listOf("hinglish" to "Hinglish", "hindi" to "हिन्दी", "english" to "English")
            languages.forEach { (code, label) ->
                val isSelected = voiceLang == code
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setVoiceLanguage(code) },
                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MyraRedPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = MyraCardBg,
                        labelColor = MyraTextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp).testTag("lang_chip_$code")
                )
            }
        }

        // Animated Core Orb
        Box(
            modifier = Modifier.padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            AiOrb(
                modifier = Modifier.size(230.dp),
                voiceState = voiceState,
                rmsDb = rmsDb,
                onClick = {
                    if (voiceState == VoiceState.LISTENING) {
                        viewModel.stopListening()
                    } else if (voiceState == VoiceState.SPEAKING) {
                        viewModel.interruptSpeech()
                    } else {
                        viewModel.startListening()
                    }
                }
            )
        }

        // Live Transcript / Spoken Output Box
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MyraCardBg,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(vertical = 12.dp)
                .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> "Listening to your voice..."
                        VoiceState.PROCESSING -> "Processing command & checking tools..."
                        VoiceState.SPEAKING -> "MYRA is speaking..."
                        VoiceState.ERROR -> "Voice system idle"
                        VoiceState.IDLE -> "Tap microphone or Orb to speak"
                    },
                    color = when (voiceState) {
                        VoiceState.LISTENING -> MyraAccentCyan
                        VoiceState.PROCESSING -> MyraAccentAmber
                        VoiceState.SPEAKING -> MyraRedPrimary
                        else -> MyraTextSecondary
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (liveTranscript.isNotBlank()) "\"$liveTranscript\"" else "e.g. \"MYRA battery kitni hai?\", \"WhatsApp kholo\", \"Torch jalao\", \"Kal subah alarm lagao\"",
                    color = if (liveTranscript.isNotBlank()) MyraTextPrimary else MyraTextTertiary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Controls (Mic + Interrupt)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interrupt button
                AnimatedVisibility(visible = voiceState == VoiceState.SPEAKING) {
                    OutlinedButton(
                        onClick = { viewModel.interruptSpeech() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MyraRedPrimary),
                        modifier = Modifier.padding(end = 16.dp).testTag("voice_interrupt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Interrupt")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP SPEECH")
                    }
                }

                // Main Mic Button
                IconButton(
                    onClick = {
                        if (voiceState == VoiceState.LISTENING) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (voiceState == VoiceState.LISTENING) MyraRedPrimary else MyraCardBg)
                        .border(2.dp, MyraRedGlow, CircleShape)
                        .testTag("voice_main_mic_button")
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Voice Trigger",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (voiceState == VoiceState.LISTENING) "TAP TO STOP LISTENING" else "TAP MIC TO SPEAK",
                color = MyraTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
