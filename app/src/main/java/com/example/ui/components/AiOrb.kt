package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.voice.VoiceState
import kotlin.math.sin

@Composable
fun AiOrb(
    modifier: Modifier = Modifier,
    voiceState: VoiceState = VoiceState.IDLE,
    rmsDb: Float = 0f,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Dynamic color glow based on Voice State
    val coreGlowColor = when (voiceState) {
        VoiceState.LISTENING -> MyraAccentCyan
        VoiceState.PROCESSING -> MyraAccentAmber
        VoiceState.SPEAKING -> MyraRedPrimary
        VoiceState.ERROR -> Color.Gray
        VoiceState.IDLE -> MyraRedGlow
    }

    val statusText = when (voiceState) {
        VoiceState.LISTENING -> "LISTENING"
        VoiceState.PROCESSING -> "PROCESSING"
        VoiceState.SPEAKING -> "SPEAKING"
        VoiceState.ERROR -> "SYSTEM ERROR"
        VoiceState.IDLE -> "MYRA AI"
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.minDimension / 2.5f

            // Dynamic audio wave amplitude expansion
            val audioBoost = if (voiceState == VoiceState.LISTENING) (rmsDb.coerceAtLeast(0f) * 2.5f) else 0f
            val dynamicRadius = (radius * pulseScale) + audioBoost

            // Outer ethereal radiation glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreGlowColor.copy(alpha = 0.45f),
                        MyraRedDark.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.5f
                ),
                radius = dynamicRadius * 1.5f,
                center = center
            )

            // Cybernetic Orbital Rings
            drawCircle(
                color = coreGlowColor.copy(alpha = 0.8f),
                radius = dynamicRadius * 1.15f,
                center = center,
                style = Stroke(width = 2.5f)
            )

            drawCircle(
                color = MyraRedPrimary.copy(alpha = 0.4f),
                radius = dynamicRadius * 1.35f,
                center = center,
                style = Stroke(width = 1.2f)
            )

            // Inner Quantum Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        coreGlowColor,
                        MyraRedDark,
                        MyraBlack
                    ),
                    center = center,
                    radius = dynamicRadius
                ),
                radius = dynamicRadius * 0.85f,
                center = center
            )
        }

        // Center Emblem / Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MYRA",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Text(
                text = statusText,
                color = coreGlowColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
