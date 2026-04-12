package net.gijun.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files

private const val LAUNCHER_VERSION = "1.0.2"
private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/Park-GiJun/lol-event/releases"
private const val MAIN_APP_DISPLAY_NAME = "LoL-Collector"
private const val MAIN_APP_EXE = "LoL-Collector.exe"
private const val DESKTOP_TAG_PREFIX = "desktop-v"

@Serializable
private data class GithubAsset(
    val name: String = "",
    val browser_download_url: String = "",
)

@Serializable
private data class GithubRelease(
    val tag_name: String = "",
    val name: String = "",
    val body: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

private enum class Phase { CHECKING, DOWNLOADING, INSTALLING, LAUNCHING, ERROR }

private class LauncherState {
    var phase by mutableStateOf(Phase.CHECKING)
    var statusText by mutableStateOf("업데이트 확인 중...")
    var progress by mutableStateOf(0)
    var errorMessage by mutableStateOf<String?>(null)
}

fun main() = application {
    val state = remember { LauncherState() }
    val scope = rememberCoroutineScope()

    val windowState = rememberWindowState(
        size = DpSize(440.dp, 240.dp),
        position = WindowPosition(Alignment.Center),
    )

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                runLauncher(state)
            } finally {
                delay(if (state.phase == Phase.ERROR) 5_000 else 400)
                exitApplication()
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "LoL 수집기 런처",
        resizable = false,
    ) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A1428)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        "LoL 수집기",
                        color = Color(0xFFC8AA6E),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        state.statusText,
                        color = Color(0xFFA09B8C),
                        fontSize = 13.sp,
                    )
                    if (state.phase == Phase.DOWNLOADING) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.width(300.dp).height(6.dp),
                            color = Color(0xFFC8AA6E),
                            trackColor = Color(0xFF1E2328),
                        )
                        Text(
                            "${state.progress}%",
                            color = Color(0xFFC8AA6E),
                            fontSize = 11.sp,
                        )
                    } else if (state.phase != Phase.ERROR) {
                        LinearProgressIndicator(
                            modifier = Modifier.width(300.dp).height(6.dp),
                            color = Color(0xFFC8AA6E),
                            trackColor = Color(0xFF1E2328),
                        )
                    }
                    state.errorMessage?.let { msg ->
                        Text(
                            msg,
                            color = Color(0xFFE84057),
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        "Launcher v$LAUNCHER_VERSION",
                        color = Color(0xFF5B5A56),
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

private suspend fun runLauncher(state: LauncherState) {
    val httpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
    try {
        val installedVersion = withContext(Dispatchers.IO) { readInstalledVersion() }

        state.statusText = if (installedVersion != null) {
            "현재 v$installedVersion — 최신 버전 확인 중..."
        } else {
            "본체 미설치 — 다운로드 준비 중..."
        }

        val release = withContext(Dispatchers.IO) {
            try {
                fetchLatestDesktopRelease(httpClient)
            } catch (_: Exception) {
                null
            }
        }

        val msiAsset = release?.assets?.firstOrNull { it.name.endsWith(".msi", ignoreCase = true) }
        val latestVersion = release?.tag_name?.removePrefix(DESKTOP_TAG_PREFIX)?.trim()

        val needsUpdate = when {
            release == null -> false
            msiAsset == null -> false
            installedVersion == null -> true
            latestVersion == null -> false
            isNewer(latestVersion, installedVersion) -> true
            else -> false
        }

        if (needsUpdate && msiAsset != null && latestVersion != null) {
            state.phase = Phase.DOWNLOADING
            state.statusText = "v$latestVersion 다운로드 중..."
            val msiFile = withContext(Dispatchers.IO) {
                downloadMsi(httpClient, msiAsset.browser_download_url, msiAsset.name) { pct ->
                    state.progress = pct
                }
            }

            state.phase = Phase.INSTALLING
            state.statusText = "v$latestVersion 설치 중..."
            val rc = withContext(Dispatchers.IO) { runMsiInstall(msiFile) }
            if (rc != 0) {
                state.phase = Phase.ERROR
                state.statusText = "설치 실패"
                state.errorMessage = "msiexec 종료 코드: $rc"
                return
            }
        } else if (release == null && installedVersion == null) {
            state.phase = Phase.ERROR
            state.statusText = "본체가 설치되어 있지 않고 GitHub에서 받을 수도 없습니다"
            state.errorMessage = "네트워크 또는 GitHub Releases 확인 불가"
            return
        }

        state.phase = Phase.LAUNCHING
        state.statusText = "본체 실행 중..."
        val launched = withContext(Dispatchers.IO) { launchMainApp() }
        if (!launched) {
            state.phase = Phase.ERROR
            state.statusText = "본체 실행 실패"
            state.errorMessage = "$MAIN_APP_EXE 위치를 찾을 수 없습니다"
        }
    } finally {
        httpClient.close()
    }
}

private suspend fun fetchLatestDesktopRelease(client: HttpClient): GithubRelease? {
    val all: List<GithubRelease> = client.get(GITHUB_RELEASES_URL) {
        header("User-Agent", "LoL-Collector-Launcher")
        header("Accept", "application/vnd.github+json")
    }.body()
    return all.firstOrNull { r ->
        !r.draft && !r.prerelease && r.tag_name.startsWith(DESKTOP_TAG_PREFIX)
    }
}

private fun isNewer(remote: String, local: String): Boolean {
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

private suspend fun downloadMsi(
    client: HttpClient,
    url: String,
    fileName: String,
    onProgress: (Int) -> Unit,
): File {
    val tempDir = Files.createTempDirectory("lol-collector-launcher").toFile()
    val target = File(tempDir, fileName.ifBlank { "LoL-Collector-update.msi" })

    client.prepareGet(url) {
        header("User-Agent", "LoL-Collector-Launcher")
    }.execute { response ->
        val total = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        val channel: ByteReadChannel = response.bodyAsChannel()
        var downloaded = 0L
        target.outputStream().use { output ->
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    output.write(bytes)
                    downloaded += bytes.size
                    if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                }
            }
        }
    }
    onProgress(100)
    return target
}

private fun runMsiInstall(msi: File): Int {
    // PowerShell Start-Process -Verb RunAs를 통해 UAC 프롬프트와 함께 msiexec를 elevated 권한으로 실행한다.
    // /qb는 기본 진행률 UI를 띄워 사용자가 설치 진행을 볼 수 있게 한다 (/qn으로는 권한 부족 시 1603 에러).
    val msiPath = msi.absolutePath.replace("'", "''")
    val psCommand = buildString {
        append("\$p = Start-Process msiexec ")
        append("-ArgumentList '/i','\"")
        append(msiPath)
        append("\"','/qb','/norestart' ")
        append("-Verb RunAs -Wait -PassThru; ")
        append("exit \$p.ExitCode")
    }
    val proc = ProcessBuilder("powershell", "-NoProfile", "-Command", psCommand)
        .redirectErrorStream(true)
        .start()
    return proc.waitFor()
}

private fun launchMainApp(): Boolean {
    val exe = findMainExe() ?: return false
    return try {
        ProcessBuilder(exe.absolutePath)
            .directory(exe.parentFile)
            .start()
        true
    } catch (_: Exception) {
        false
    }
}

private fun findMainExe(): File? {
    registryInstallLocation()?.let { dir ->
        val exe = File(dir, MAIN_APP_EXE)
        if (exe.exists()) return exe
    }
    val candidates = listOfNotNull(
        System.getenv("ProgramFiles")?.let { File(it, "$MAIN_APP_DISPLAY_NAME\\$MAIN_APP_EXE") },
        System.getenv("ProgramFiles(x86)")?.let { File(it, "$MAIN_APP_DISPLAY_NAME\\$MAIN_APP_EXE") },
        System.getenv("LOCALAPPDATA")?.let { File(it, "$MAIN_APP_DISPLAY_NAME\\$MAIN_APP_EXE") },
    )
    return candidates.firstOrNull { it.exists() }
}

private fun readInstalledVersion(): String? {
    val key = registryUninstallKey() ?: return null
    return queryRegistryValue(key, "DisplayVersion")
}

private fun registryInstallLocation(): String? {
    val key = registryUninstallKey() ?: return null
    return queryRegistryValue(key, "InstallLocation")
}

private val UNINSTALL_ROOTS = listOf(
    "HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
    "HKLM\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
    "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
)

private fun registryUninstallKey(): String? {
    for (root in UNINSTALL_ROOTS) {
        try {
            val proc = ProcessBuilder("reg", "query", root, "/s", "/f", MAIN_APP_DISPLAY_NAME, "/d")
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            // 키 라인은 root\<GUID> 형태로 나오고, 그 다음 줄에 DisplayName 매치 라인이 옴.
            val lines = output.lines()
            var currentKey: String? = null
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith(root, ignoreCase = true)) {
                    currentKey = trimmed
                } else if (currentKey != null && trimmed.contains("DisplayName") && trimmed.contains(MAIN_APP_DISPLAY_NAME)) {
                    // 본체 키 외 (예: Launcher) 와 구분: 정확히 "LoL-Collector"인지 확인
                    val nameValue = Regex("DisplayName\\s+REG_SZ\\s+(.+)").find(trimmed)?.groupValues?.get(1)?.trim()
                    if (nameValue == MAIN_APP_DISPLAY_NAME) {
                        return currentKey
                    }
                }
            }
        } catch (_: Exception) {
            // 다음 루트 시도
        }
    }
    return null
}

private fun queryRegistryValue(key: String, valueName: String): String? {
    return try {
        val proc = ProcessBuilder("reg", "query", key, "/v", valueName)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        Regex("$valueName\\s+REG_SZ\\s+(.+?)\\s*$", RegexOption.MULTILINE)
            .find(output)
            ?.groupValues
            ?.get(1)
            ?.trim()
    } catch (_: Exception) {
        null
    }
}
