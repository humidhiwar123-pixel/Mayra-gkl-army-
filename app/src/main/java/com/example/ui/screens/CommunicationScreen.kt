package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import java.net.URLEncoder

@Composable
fun CommunicationScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var emailRecipient by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }
    var showSosConfirmDialog by remember { mutableStateOf(false) }

    if (showSosConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSosConfirmDialog = false },
            containerColor = MyraCardBg,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MyraRedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EMERGENCY SOS DIAL", color = MyraTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to dial emergency helpline 112? This will open the phone dialer immediately.",
                    color = MyraTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmDialog = false
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary)
                ) {
                    Text("DIAL 112 NOW")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSosConfirmDialog = false }) {
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Emergency SOS Banner
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MyraRedDark.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedPrimary, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EMERGENCY SHORTCUT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text("Instant dialer access with explicit confirmation", color = MyraTextSecondary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { showSosConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                        modifier = Modifier.testTag("sos_dial_button")
                    ) {
                        Icon(imageVector = Icons.Default.Emergency, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SOS")
                    }
                }
            }
        }

        // WhatsApp Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = MyraAccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WHATSAPP MESSENGER", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number (with country code e.g. +91...)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedBorderColor = MyraRedPrimary,
                            unfocusedBorderColor = MyraRedBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("comm_phone_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Message Text") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedBorderColor = MyraRedPrimary,
                            unfocusedBorderColor = MyraRedBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("comm_message_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val cleanPhone = phoneInput.replace("[^0-9+]".toRegex(), "")
                                val encoded = URLEncoder.encode(messageInput, "UTF-8")
                                val uri = if (cleanPhone.isNotBlank()) {
                                    Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encoded")
                                } else {
                                    Uri.parse("https://api.whatsapp.com/send?text=$encoded")
                                }
                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                            modifier = Modifier.weight(1f).testTag("comm_send_whatsapp")
                        ) {
                            Text("SEND WHATSAPP")
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneInput")).apply {
                                    putExtra("sms_body", messageInput)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraDarkSurface),
                            modifier = Modifier.weight(1f).testTag("comm_send_sms")
                        ) {
                            Text("COMPOSE SMS")
                        }
                    }
                }
            }
        }

        // Email Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MyraAccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EMAIL DISPATCHER", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = emailRecipient,
                        onValueChange = { emailRecipient = it },
                        label = { Text("Recipient Email") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedBorderColor = MyraRedPrimary,
                            unfocusedBorderColor = MyraRedBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailSubject,
                        onValueChange = { emailSubject = it },
                        label = { Text("Subject") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedBorderColor = MyraRedPrimary,
                            unfocusedBorderColor = MyraRedBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailBody,
                        onValueChange = { emailBody = it },
                        label = { Text("Message Body") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedBorderColor = MyraRedPrimary,
                            unfocusedBorderColor = MyraRedBorder
                        ),
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailRecipient))
                                putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                                putExtra(Intent.EXTRA_TEXT, emailBody)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("comm_send_email")
                    ) {
                        Text("OPEN IN EMAIL CLIENT")
                    }
                }
            }
        }
    }
}
