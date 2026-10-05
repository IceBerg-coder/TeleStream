package com.telestream.app.data.stream

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.telestream.app.data.local.AppPreferences
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class LocalStreamProxy(
    private val context: Context,
    private val preferences: AppPreferences
) {
    private val tag = "LocalStreamProxy"
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val proxyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    var port: Int = preferences.streamServerPort
        private set

    fun start() {
        if (isRunning) return
        isRunning = true

        proxyScope.launch {
            try {
                // Try configured port, fallback to any available port if busy
                serverSocket = try {
                    ServerSocket(port)
                } catch (e: Exception) {
                    Log.w(tag, "Port $port in use, picking random open port")
                    ServerSocket(0).also { port = it.localPort }
                }

                Log.i(tag, "Local Telegram Stream Proxy started on 127.0.0.1:$port")

                while (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
                    val clientSocket = serverSocket!!.accept()
                    launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(tag, "Error in stream proxy server: ${e.message}", e)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    fun getStreamUrl(directUrl: String = "", fileId: String = ""): String {
        return if (fileId.isNotEmpty()) {
            "http://127.0.0.1:$port/stream?fileId=${java.net.URLEncoder.encode(fileId, "UTF-8")}"
        } else {
            "http://127.0.0.1:$port/stream?url=${java.net.URLEncoder.encode(directUrl, "UTF-8")}"
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        var input: BufferedReader? = null
        var output: OutputStream? = null

        try {
            input = BufferedReader(InputStreamReader(socket.getInputStream()))
            output = socket.getOutputStream()

            val requestLine = input.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val rawUri = parts[1]
            val rangeHeader = extractHeader(input, "Range")

            val (urlParam, fileIdParam) = parseQueryParams(rawUri)

            // 1. Check if this is a TDLib native file ID (User Mode / Private Channel)
            val tdlibId = fileIdParam.toIntOrNull()
            if (tdlibId != null) {
                com.telestream.app.TeleStreamApp.instance.authManager.startFileDownload(tdlibId)
                val localPath = getTdlibFilePath(tdlibId)
                if (!localPath.isNullOrEmpty()) {
                    val localFile = File(localPath)
                    if (localFile.exists() && localFile.length() > 0) {
                        serveLocalFile(localFile, rangeHeader, output)
                        return@withContext
                    }
                }
            }

            // 2. Fallback to Bot API or Direct URL
            val targetUrl = when {
                fileIdParam.isNotEmpty() -> resolveTelegramFileUrl(fileIdParam)
                urlParam.isNotEmpty() -> urlParam
                else -> null
            }

            if (targetUrl == null) {
                writeNotFound(output)
                return@withContext
            }

            proxyRequest(targetUrl, rangeHeader, output)
        } catch (e: Exception) {
            Log.d(tag, "Connection handled or closed: ${e.message}")
        } finally {
            try { output?.flush() } catch (_: Exception) {}
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun extractHeader(reader: BufferedReader, headerName: String): String? {
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line.isNullOrEmpty()) break
            if (line!!.startsWith("$headerName:", ignoreCase = true)) {
                return line!!.substring(headerName.length + 1).trim()
            }
        }
        return null
    }

    private fun parseQueryParams(uri: String): Pair<String, String> {
        var url = ""
        var fileId = ""
        val queryStart = uri.indexOf('?')
        if (queryStart != -1) {
            val query = uri.substring(queryStart + 1)
            val pairs = query.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                if (idx != -1) {
                    val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                    val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                    if (key == "url") url = value
                    if (key == "fileId") fileId = value
                }
            }
        }
        return Pair(url, fileId)
    }

    private fun resolveTelegramFileUrl(fileId: String): String? {
        val botToken = preferences.botToken
        if (botToken.isEmpty()) return null

        try {
            val infoUrl = "https://api.telegram.org/bot$botToken/getFile?file_id=$fileId"
            val request = Request.Builder().url(infoUrl).build()
            val response: Response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null

            val json: JsonObject = JsonParser.parseString(body).asJsonObject
            if (json.get("ok").asBoolean) {
                val filePath = json.getAsJsonObject("result").get("file_path").asString
                return "https://api.telegram.org/file/bot$botToken/$filePath"
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to resolve file path for $fileId: ${e.message}")
        }
        return null
    }

    private fun proxyRequest(targetUrl: String, rangeHeader: String?, clientOutput: OutputStream) {
        val requestBuilder = Request.Builder().url(targetUrl)

        if (!rangeHeader.isNullOrEmpty()) {
            requestBuilder.header("Range", rangeHeader)
        }

        val upstreamResponse = httpClient.newCall(requestBuilder.build()).execute()
        val responseCode = upstreamResponse.code
        val responseBody = upstreamResponse.body ?: return

        val printWriter = PrintWriter(clientOutput, false)
        val statusLine = if (responseCode == 206) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK"
        printWriter.println(statusLine)

        // Forward essential streaming headers
        val contentLength = responseBody.contentLength()
        if (contentLength >= 0) {
            printWriter.println("Content-Length: $contentLength")
        }
        printWriter.println("Accept-Ranges: bytes")
        upstreamResponse.header("Content-Range")?.let {
            printWriter.println("Content-Range: $it")
        }
        val contentType = upstreamResponse.header("Content-Type") ?: "video/mp4"
        printWriter.println("Content-Type: $contentType")
        printWriter.println("Connection: close")
        printWriter.println()
        printWriter.flush()

        // Stream byte chunks directly to ExoPlayer
        val buffer = ByteArray(64 * 1024)
        val upstreamStream = responseBody.byteStream()
        var read: Int
        while (upstreamStream.read(buffer).also { read = it } != -1) {
            clientOutput.write(buffer, 0, read)
        }
        clientOutput.flush()
    }

    private fun getTdlibFilePath(fileId: Int): String? {
        var path: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        com.telestream.app.TeleStreamApp.instance.authManager.getFile(fileId) { file ->
            path = file?.local?.path
            latch.countDown()
        }
        latch.await(2, TimeUnit.SECONDS)
        return path
    }

    private fun serveLocalFile(file: File, rangeHeader: String?, clientOutput: OutputStream) {
        val raf = RandomAccessFile(file, "r")
        val fileLength = raf.length()
        var start = 0L
        var end = fileLength - 1

        if (!rangeHeader.isNullOrEmpty() && rangeHeader.startsWith("bytes=")) {
            val parts = rangeHeader.removePrefix("bytes=").split("-")
            start = parts[0].toLongOrNull() ?: 0L
            if (parts.size > 1 && parts[1].isNotEmpty()) {
                end = parts[1].toLongOrNull() ?: (fileLength - 1)
            }
        }

        val contentLength = end - start + 1
        val pw = PrintWriter(clientOutput, false)
        pw.println(if (rangeHeader != null) "HTTP/1.1 206 Partial Content" else "HTTP/1.1 200 OK")
        pw.println("Content-Type: video/mp4")
        pw.println("Accept-Ranges: bytes")
        pw.println("Content-Range: bytes $start-$end/$fileLength")
        pw.println("Content-Length: $contentLength")
        pw.println("Connection: close")
        pw.println()
        pw.flush()

        raf.seek(start)
        val buffer = ByteArray(64 * 1024)
        var remaining = contentLength
        while (remaining > 0) {
            val toRead = minOf(buffer.size.toLong(), remaining).toInt()
            val read = raf.read(buffer, 0, toRead)
            if (read == -1) break
            clientOutput.write(buffer, 0, read)
            remaining -= read
        }
        raf.close()
        clientOutput.flush()
    }

    private fun writeNotFound(output: OutputStream) {
        val pw = PrintWriter(output, true)
        pw.println("HTTP/1.1 404 Not Found")
        pw.println("Content-Type: text/plain")
        pw.println("Content-Length: 9")
        pw.println()
        pw.print("Not Found")
        pw.flush()
    }
}
