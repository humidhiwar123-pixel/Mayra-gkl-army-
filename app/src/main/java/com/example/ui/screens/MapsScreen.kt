package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MyraApplication
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import kotlinx.coroutines.launch

@Composable
fun MapsScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = (context.applicationContext as MyraApplication).preferencesManager
    val parkingTriple by prefs.parkingLocation.collectAsState(initial = Triple(null, null, null))

    var placeSearchQuery by remember { mutableStateOf("") }
    var parkingNote by remember { mutableStateOf("") }

    val nearbyCategories = listOf(
        Pair("Hospitals", "hospital"),
        Pair("Police", "police station"),
        Pair("Fuel & EV", "gas station"),
        Pair("Food & Dine", "restaurants"),
        Pair("ATMs", "atm"),
        Pair("Pharmacies", "pharmacy")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Launch Google Maps
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MyraCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MyraRedBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WORLD NAVIGATION & MAPS", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = placeSearchQuery,
                        onValueChange = { placeSearchQuery = it },
                        placeholder = { Text("Search location, route, or address...", color = MyraTextTertiary, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MyraTextPrimary,
                            unfocusedTextColor = MyraTextPrimary,
                            focusedBorderColor = MyraRedPrimary,
                            unfocusedBorderColor = MyraRedBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("maps_search_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val query = placeSearchQuery.ifBlank { "Raipur" }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                            modifier = Modifier.weight(1f).testTag("open_maps_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("OPEN MAPS")
                        }

                        Button(
                            onClick = {
                                val query = placeSearchQuery.ifBlank { "Home" }
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(query)}")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraDarkSurface),
                            modifier = Modifier.weight(1f).testTag("start_nav_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NAVIGATE")
                        }
                    }
                }
            }
        }

        // Nearby Quick Search Categories
        item {
            Text("NEARBY EMERGENCIES & SERVICES", color = MyraTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(nearbyCategories) { (label, keyword) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MyraCardBg,
                        modifier = Modifier
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$keyword")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                            .border(1.dp, MyraRedBorder, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = label,
                            color = MyraTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Parking Location Saver & Finder
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
                        Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = MyraAccentAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PARKING ASSISTANT / FIND MY CAR", color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (parkingTriple.first != null && parkingTriple.second != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MyraElevatedCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("SAVED SPOT RECORDED", color = MyraAccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Coordinates: ${parkingTriple.first}, ${parkingTriple.second}", color = MyraTextSecondary, fontSize = 12.sp)
                                if (!parkingTriple.third.isNullOrBlank()) {
                                    Text("Note: ${parkingTriple.third}", color = MyraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val lat = parkingTriple.first
                                    val lng = parkingTriple.second
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng")).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("WALK TO CAR")
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch { prefs.clearParkingLocation() }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CLEAR")
                            }
                        }
                    } else {
                        Text("No car location saved. Save your current parking coordinate below:", color = MyraTextTertiary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = parkingNote,
                            onValueChange = { parkingNote = it },
                            placeholder = { Text("Note: e.g. Floor 2, Pillar B7", color = MyraTextTertiary, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    prefs.saveParkingLocation(21.2514, 81.6296, parkingNote.ifBlank { "Saved Spot" })
                                    parkingNote = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("save_parking_spot_btn")
                        ) {
                            Text("SAVE CURRENT PARKING SPOT")
                        }
                    }
                }
            }
        }
    }
}
