package com.example.pc

import android.content.Context
import com.example.data.local.ConnectedDeviceEntity
import com.example.data.local.MyraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

data class PcTransferMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "Android" or "PC"
    val content: String,
    val type: String = "TEXT", // "TEXT" or "FILE"
    val timestamp: Long = System.currentTimeMillis()
)

object PcCompanionManager {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _serverStatus = MutableStateFlow("Stopped")
    val serverStatus: StateFlow<String> = _serverStatus.asStateFlow()

    private val _localIp = MutableStateFlow("127.0.0.1")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _pairingPin = MutableStateFlow("849201")
    val pairingPin: StateFlow<String> = _pairingPin.asStateFlow()

    private val _transferHistory = MutableStateFlow<List<PcTransferMessage>>(emptyList())
    val transferHistory: StateFlow<List<PcTransferMessage>> = _transferHistory.asStateFlow()

    private val _connectedPcName = MutableStateFlow<String?>(null)
    val connectedPcName: StateFlow<String?> = _connectedPcName.asStateFlow()

    fun generateNewPin(): String {
        val newPin = String.format("%06d", (100000..999999).random())
        _pairingPin.value = newPin
        return newPin
    }

    fun startServer(context: Context, port: Int = 8088) {
        if (serverSocket != null && serverSocket?.isClosed == false) return

        _localIp.value = resolveLocalIp()

        serverJob = scope.launch {
            try {
                val sSocket = ServerSocket(port)
                serverSocket = sSocket
                _serverStatus.value = "Running on port $port"

                while (isActive && !sSocket.isClosed) {
                    try {
                        val clientSocket = sSocket.accept()
                        scope.launch {
                            handleClientConnection(context, clientSocket)
                        }
                    } catch (e: Exception) {
                        if (sSocket.isClosed) break
                    }
                }
            } catch (e: Exception) {
                _serverStatus.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun stopServer() {
        try {
            serverJob?.cancel()
            serverJob = null
            serverSocket?.close()
            serverSocket = null
            _serverStatus.value = "Stopped"
            _connectedPcName.value = null
        } catch (_: Exception) {}
    }

    fun sendTextToPc(text: String) {
        val msg = PcTransferMessage(sender = "Android", content = text, type = "TEXT")
        val list = _transferHistory.value.toMutableList()
        list.add(0, msg)
        _transferHistory.value = list
    }

    private fun resolveLocalIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address.hostAddress?.indexOf(':') == -1) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private suspend fun handleClientConnection(context: Context, socket: Socket) {
        try {
            socket.soTimeout = 10000
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
            val requestLine = reader.readLine() ?: run {
                socket.close()
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                sendHttpResponse(output, 400, "Bad Request", "text/plain", "Invalid HTTP Request".toByteArray())
                socket.close()
                return
            }

            val method = parts[0].uppercase()
            val uri = parts[1]

            val headers = mutableMapOf<String, String>()
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
                line = reader.readLine()
            }

            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0

            when {
                uri.startsWith("/status") -> {
                    val json = JSONObject().apply {
                        put("app", "MYRA AI Companion")
                        put("version", "1.0")
                        put("status", "READY")
                        put("paired_device", _connectedPcName.value ?: "NONE")
                    }
                    sendHttpResponse(output, 200, "OK", "application/json", json.toString().toByteArray())
                }

                uri.startsWith("/pair") && method == "POST" -> {
                    val body = readBodyString(reader, contentLength)
                    val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                    val pin = json.optString("pin", "")
                    val pcName = json.optString("pc_name", "Desktop-Workstation")

                    val responseJson = JSONObject()
                    val success = (pin == _pairingPin.value)
                    if (success) {
                        _connectedPcName.value = pcName
                        val token = UUID.randomUUID().toString()
                        responseJson.put("success", true)
                        responseJson.put("token", token)
                        responseJson.put("message", "Paired successfully with MYRA Android")

                        val db = MyraDatabase.getDatabase(context)
                        db.deviceDao().insertDevice(
                            ConnectedDeviceEntity(
                                deviceId = UUID.randomUUID().toString(),
                                deviceName = pcName,
                                deviceType = "PC",
                                ipAddress = socket.inetAddress.hostAddress ?: "unknown",
                                token = token,
                                isPaired = true
                            )
                        )
                    } else {
                        responseJson.put("success", false)
                        responseJson.put("error", "Invalid Pairing PIN")
                    }

                    val code = if (success) 200 else 401
                    val msg = if (success) "OK" else "Unauthorized"
                    sendHttpResponse(output, code, msg, "application/json", responseJson.toString().toByteArray())
                }

                uri.startsWith("/message") && method == "POST" -> {
                    val body = readBodyString(reader, contentLength)
                    val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                    val text = json.optString("text", "")
                    val sender = json.optString("sender", "PC")

                    if (text.isNotBlank()) {
                        val msg = PcTransferMessage(sender = sender, content = text, type = "TEXT")
                        val list = _transferHistory.value.toMutableList()
                        list.add(0, msg)
                        _transferHistory.value = list
                    }

                    val resp = JSONObject().put("status", "RECEIVED").toString().toByteArray()
                    sendHttpResponse(output, 200, "OK", "application/json", resp)
                }

                uri.startsWith("/file") && method == "POST" -> {
                    val fileName = headers["x-file-name"] ?: "pc_transfer_${System.currentTimeMillis()}.bin"
                    val dir = File(context.filesDir, "pc_downloads").apply { if (!exists()) mkdirs() }
                    val destFile = File(dir, fileName)

                    // Read remaining bytes directly from the reader
                    val charBuffer = CharArray(4096)
                    var totalRead = 0
                    FileWriter(destFile).use { writer ->
                        while (totalRead < contentLength) {
                            val toRead = minOf(charBuffer.size, contentLength - totalRead)
                            val read = reader.read(charBuffer, 0, toRead)
                            if (read == -1) break
                            writer.write(charBuffer, 0, read)
                            totalRead += read
                        }
                    }

                    val msg = PcTransferMessage(
                        sender = "PC",
                        content = "Received file: $fileName (${destFile.length()} bytes)",
                        type = "FILE"
                    )
                    val list = _transferHistory.value.toMutableList()
                    list.add(0, msg)
                    _transferHistory.value = list

                    val resp = JSONObject().put("status", "SAVED").put("path", destFile.absolutePath).toString().toByteArray()
                    sendHttpResponse(output, 200, "OK", "application/json", resp)
                }

                else -> {
                    sendHttpResponse(output, 404, "Not Found", "text/plain", "Endpoint not found".toByteArray())
                }
            }

            output.flush()
        } catch (_: Exception) {
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun readBodyString(reader: BufferedReader, length: Int): String {
        if (length <= 0) return ""
        val buffer = CharArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = reader.read(buffer, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return String(buffer, 0, totalRead)
    }

    private fun sendHttpResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray
    ) {
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.print("HTTP/1.1 $statusCode $statusText\r\n")
        writer.print("Content-Type: $contentType\r\n")
        writer.print("Content-Length: ${body.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        output.write(body)
        output.flush()
    }
}
