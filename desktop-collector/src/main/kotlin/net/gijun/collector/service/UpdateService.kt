package net.gijun.collector.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files

@Serializable
data class UpdateInfo(
    val version: String = "",
    val url: String = "",
    val notes: String = "",
)

enum class UpdateState {
    IDLE, CHECKING, NOT_AVAILABLE, AVAILABLE, DOWNLOADING, READY, INSTALLING, ERROR
}

class UpdateService(
    private val currentVersion: String,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val UPDATE_URL = "https://gijun.net/downloads/desktop-latest.json"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val httpClient = HttpClient(OkHttp) {
        install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    }

    var state: UpdateState = UpdateState.IDLE
        private set
    var updateInfo: UpdateInfo? = null
        private set
    var downloadProgress: Int = 0
        private set
    var installerPath: File? = null
        private set
    var errorMessage: String? = null
        private set
    var onStateChanged: (() -> Unit)? = null

    private fun notifyChange() { onStateChanged?.invoke() }

    fun checkForUpdates() {
        scope.launch {
            state = UpdateState.CHECKING
            errorMessage = null
            notifyChange()
            try {
                val response: HttpResponse = httpClient.get(UPDATE_URL)
                if (!response.status.isSuccess()) {
                    state = UpdateState.NOT_AVAILABLE
                    notifyChange()
                    return@launch
                }
                val text: String = response.body()
                val raw = json.decodeFromString<UpdateInfo>(text)
                val info = raw.copy(version = raw.version.trim().lines().first(), url = raw.url.trim())

                if (info.version.isBlank() || info.url.isBlank()) {
                    state = UpdateState.NOT_AVAILABLE
                    notifyChange()
                    return@launch
                }

                if (isNewerVersion(info.version, currentVersion)) {
                    updateInfo = info
                    state = UpdateState.AVAILABLE
                    notifyChange()
                    downloadUpdate(info)
                } else {
                    state = UpdateState.NOT_AVAILABLE
                    notifyChange()
                }
            } catch (_: Exception) {
                state = UpdateState.NOT_AVAILABLE
                notifyChange()
            }
        }
    }

    private suspend fun downloadUpdate(info: UpdateInfo) {
        state = UpdateState.DOWNLOADING
        downloadProgress = 0
        notifyChange()
        try {
            val tempDir = Files.createTempDirectory("lol-collector-update").toFile()
            val fileName = info.url.substringAfterLast("/").ifEmpty { "lol-collector-setup.msi" }
            val targetFile = File(tempDir, fileName)

            httpClient.prepareGet(info.url).execute { response ->
                val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                val channel: ByteReadChannel = response.bodyAsChannel()
                var downloaded = 0L

                targetFile.outputStream().use { output ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                        while (!packet.isEmpty) {
                            val bytes = packet.readBytes()
                            output.write(bytes)
                            downloaded += bytes.size
                            if (contentLength > 0) {
                                downloadProgress = ((downloaded * 100) / contentLength).toInt()
                                notifyChange()
                            }
                        }
                    }
                }
            }

            installerPath = targetFile
            downloadProgress = 100
            state = UpdateState.READY
            notifyChange()
        } catch (e: Exception) {
            errorMessage = "다운로드 실패: ${e.message ?: "알 수 없는 오류"}"
            state = UpdateState.ERROR
            notifyChange()
        }
    }

    fun retryDownload() {
        val info = updateInfo ?: return
        scope.launch { downloadUpdate(info) }
    }

    fun installUpdate() {
        val path = installerPath ?: return
        state = UpdateState.INSTALLING
        notifyChange()
        scope.launch {
            delay(500)
            try {
                val ext = path.extension.lowercase()
                val command = when (ext) {
                    "msi" -> listOf("msiexec", "/i", path.absolutePath, "/passive")
                    "exe" -> listOf(path.absolutePath, "/S")
                    else -> listOf("cmd", "/c", "start", path.absolutePath)
                }
                ProcessBuilder(command).start()
                delay(1_500)
                Runtime.getRuntime().exit(0)
            } catch (e: Exception) {
                errorMessage = "설치 실패: ${e.message ?: "알 수 없는 오류"}"
                state = UpdateState.ERROR
                notifyChange()
            }
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val r = remote.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val l = local.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}
