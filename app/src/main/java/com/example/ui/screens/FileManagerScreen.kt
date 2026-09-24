package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MyraViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FileManagerScreen(
    viewModel: MyraViewModel
) {
    val context = LocalContext.current
    var currentDir by remember { mutableStateOf(context.filesDir) }
    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var newFolderName by remember { mutableStateOf("") }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    fun refreshFiles() {
        fileList = currentDir.listFiles()?.toList()?.sortedWith(
            compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
        ) ?: emptyList()
    }

    LaunchedEffect(currentDir) {
        refreshFiles()
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = MyraCardBg,
            title = { Text("CREATE FOLDER", color = MyraTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("Folder Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MyraTextPrimary, unfocusedTextColor = MyraTextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            File(currentDir, newFolderName).mkdirs()
                            newFolderName = ""
                            showNewFolderDialog = false
                            refreshFiles()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MyraRedPrimary)
                ) {
                    Text("CREATE")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewFolderDialog = false }) { Text("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MyraBlack)
            .padding(16.dp)
    ) {
        // Path Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MyraCardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LOCAL STORAGE EXPLORER", color = MyraRedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(currentDir.path.replace(context.filesDir.path, "~"), color = MyraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Row {
                    if (currentDir != context.filesDir && currentDir.parentFile != null) {
                        IconButton(onClick = { currentDir = currentDir.parentFile!! }) {
                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Up", tint = MyraAccentCyan)
                        }
                    }
                    IconButton(onClick = { showNewFolderDialog = true }, modifier = Modifier.testTag("create_folder_btn")) {
                        Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = MyraRedPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (fileList.isEmpty()) {
                item {
                    Text(
                        text = "Folder is empty.",
                        color = MyraTextTertiary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(fileList) { file ->
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(file.lastModified()))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MyraCardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (file.isDirectory) {
                                currentDir = file
                            }
                        }
                        .border(1.dp, MyraRedBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (file.isDirectory) MyraAccentAmber else MyraAccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.name, color = MyraTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                if (file.isDirectory) "Directory • $dateStr" else "${file.length() / 1024} KB • $dateStr",
                                color = MyraTextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = {
                            file.deleteRecursively()
                            refreshFiles()
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MyraTextTertiary)
                        }
                    }
                }
            }
        }
    }
}
