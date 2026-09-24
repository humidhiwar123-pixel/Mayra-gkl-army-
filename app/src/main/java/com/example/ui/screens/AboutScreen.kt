package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AiOrb
import com.example.ui.theme.*

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AiOrb(modifier = Modifier.size(160.dp))

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "MYRA AI",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )

        Text(
            text = "\"Your AI Companion, Your Personal Assistant\"",
            color = MyraRedPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MyraCardBg),
            modifier = Modifier.fillMaxWidth().border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Version: 1.0.0 (Production Core)", color = MyraTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Architecture: Clean Architecture + MVVM + Room DB", color = MyraTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Theme: Futuristic Black & Red Quantum Core", color = MyraTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Voice: Multilingual (Hindi, English, Hinglish)", color = MyraTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("PC Connect: Local Encrypted Workstation Protocol", color = MyraTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
