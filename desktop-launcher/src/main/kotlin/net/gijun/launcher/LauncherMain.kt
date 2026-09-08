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

/**
 * 본체 부트스트랩.
 *
 * 본체가 Kotlin/MSI 에서 Rust 단일 exe 로 바뀌면서 런처가 할 일이 줄었다.
 * 예전에는 MSI 를 받아 msiexec 로 설치했고, 그때마다 UAC 승격 프롬프트와
 * "알 수 없는 게시자" 경고가 떴다. 이제는 그냥 exe 를 받아 실행한다.
 *
 *  - 본체가 이미 깔려 있으면(`%LOCALAPPDATA%\LoL-Collector`) 그걸 실행하고 끝.
 *  - 없으면 GitHub Releases 에서 exe 를 받아 임시 폴더에서 한 번 실행한다.
 *    그 다음은 본체가 알아서 자기를 설치 위치로 복사하고 바로가기를 만든다.
 *
 * 그래서 여기에는 버전 비교가 없다. 업데이트는 전적으로 본체가 스스로 한다.
 */
private const val LAUNCHER_VERSION = "2.0.0"
private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/Park-GiJun/lol-event/releases"
private const val DESKTOP_TAG_PREFIX = "desktop-v"

/** 본체 릴리즈 자산 이름. Rust 쪽 `updater::ASSET_NAME` 과 반드시 같아야 한다. */
private const val ASSET_NAME = "LoL-Collector.exe"

/** 본체 설치 위치. Rust 쪽 `install::install_dir()` 과 반드시 같아야 한다. */
private const val INSTALL_DIR_NAME = "LoL-Collector"

@Serializable
private data class GithubAsset(
    val name: String = "",
    val browser_download_url: String = "",
)

@Serializable
private data class GithubRelease(
    val tag_name: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

private enum class Phase { CHECKING, DOWNLOADING, LAUNCHING, ERROR }

private class LauncherState {
    var phase by mutableStateOf(Phase.CHECKING)
    var statusText by mutableStateOf("본체 확인 중...")
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
                delay(if (state.phase == Phase.ERROR) 6_000 else 400)
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
                    // 본체 웹과 같은 밝은 배경. 예전 다크 테마에서 옮겨 왔다.
                    .background(Color(0xFFF9FAFB)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        "LoL 수집기",
                        color = Color(0xFF191F28),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.statusText,
                        color = Color(0xFF4E5968),
                        fontSize = 13.sp,
                    )
                    if (state.phase == Phase.DOWNLOADING) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.width(300.dp).height(6.dp),
                            color = Color(0xFF3182F6),
                            trackColor = Color(0xFFE5E8EB),
                        )
                        Text(
                            "${state.progress}%",
                            color = Color(0xFF3182F6),
                            fontSize = 11.sp,
                        )
                    } else if (state.phase != Phase.ERROR) {
                        LinearProgressIndicator(
                            modifier = Modifier.width(300.dp).height(6.dp),
                            color = Color(0xFF3182F6),
                            trackColor = Color(0xFFE5E8EB),
                        )
                    }
                    state.errorMessage?.let { msg ->
                        Text(msg, color = Color(0xFFE5484D), fontSize = 11.sp)
                    }
                    Text(
                        "Launcher v$LAUNCHER_VERSION",
                        color = Color(0xFFB0B8C1),
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

private suspend fun runLauncher(state: LauncherState) {
    // 이미 깔려 있으면 그대로 띄운다. 업데이트는 본체가 알아서 한다.
    val installed = installedExe()
    if (installed != null) {
        state.phase = Phase.LAUNCHING
        state.statusText = "본체 실행 중..."
        if (launch(installed, installed.parentFile)) return

        state.phase = Phase.ERROR
        state.statusText = "본체 실행 실패"
        state.errorMessage = installed.absolutePath
        return
    }

    val httpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000
            connectTimeoutMillis = 15_000
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
    try {
        state.statusText = "본체 미설치 — 최신 버전 확인 중..."
        val asset = withContext(Dispatchers.IO) {
            try {
                findLatestExeAsset(httpClient)
            } catch (_: Exception) {
                null
            }
        }
        if (asset == null) {
            state.phase = Phase.ERROR
            state.statusText = "본체를 받을 수 없습니다"
            state.errorMessage = "네트워크 또는 GitHub Releases 확인 불가"
            return
        }

        state.phase = Phase.DOWNLOADING
        state.statusText = "본체 다운로드 중..."
        val downloaded = withContext(Dispatchers.IO) {
            download(httpClient, asset.browser_download_url) { pct -> state.progress = pct }
        }

        state.phase = Phase.LAUNCHING
        state.statusText = "본체 설치 중..."
        // 본체가 스스로 %LOCALAPPDATA% 로 복사하고 바로가기를 만든 뒤 다시 뜬다.
        if (!launch(downloaded, downloaded.parentFile)) {
            state.phase = Phase.ERROR
            state.statusText = "본체 실행 실패"
            state.errorMessage = downloaded.absolutePath
        }
    } catch (e: Exception) {
        state.phase = Phase.ERROR
        state.statusText = "설치 실패"
        state.errorMessage = e.message
    } finally {
        httpClient.close()
    }
}

private fun installedExe(): File? {
    val local = System.getenv("LOCALAPPDATA") ?: return null
    val exe = File(local, "$INSTALL_DIR_NAME\\$ASSET_NAME")
    return exe.takeIf { it.exists() }
}

private suspend fun findLatestExeAsset(client: HttpClient): GithubAsset? {
    val all: List<GithubRelease> = client.get(GITHUB_RELEASES_URL) {
        header("User-Agent", "LoL-Collector-Launcher")
        header("Accept", "application/vnd.github+json")
    }.body()
    return all
        .firstOrNull { !it.draft && !it.prerelease && it.tag_name.startsWith(DESKTOP_TAG_PREFIX) }
        ?.assets
        ?.firstOrNull { it.name.equals(ASSET_NAME, ignoreCase = true) }
}

private suspend fun download(
    client: HttpClient,
    url: String,
    onProgress: (Int) -> Unit,
): File {
    val dir = File(System.getProperty("java.io.tmpdir"), "lol-collector-bootstrap")
    dir.mkdirs()
    val target = File(dir, ASSET_NAME)

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

private fun launch(exe: File, workingDir: File?): Boolean = try {
    ProcessBuilder(exe.absolutePath).directory(workingDir).start()
    true
} catch (_: Exception) {
    false
}
