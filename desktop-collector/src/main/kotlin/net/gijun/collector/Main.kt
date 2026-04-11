package net.gijun.collector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import net.gijun.collector.lcu.LcuClient
import net.gijun.collector.lcu.LcuStatus
import net.gijun.collector.service.*
import net.gijun.collector.ui.AppIcon
import net.gijun.collector.ui.components.Grid16
import net.gijun.collector.ui.components.Page
import net.gijun.collector.ui.components.Sidebar
import net.gijun.collector.ui.components.Titlebar
import net.gijun.collector.ui.components.colSpan
import net.gijun.collector.ui.pages.*
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.LolTheme
import java.awt.Desktop
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import javax.swing.JOptionPane

private const val APP_VERSION = "1.0.1"
private const val SINGLE_INSTANCE_PORT = 47632

fun main() {
    // 빌드 시 사용할 ICO 파일 자동 생성 (없으면)
    val icoFile = File("src/main/resources/icon.ico")
    if (!icoFile.exists()) {
        icoFile.parentFile.mkdirs()
        try { AppIcon.writeIcoFile(icoFile) } catch (_: Exception) {}
    }

    // 싱글 인스턴스 락
    try {
        ServerSocket(SINGLE_INSTANCE_PORT, 0, InetAddress.getByName("127.0.0.1"))
    } catch (_: Exception) {
        JOptionPane.showMessageDialog(null, "이미 실행 중입니다.", "LoL 수집기", JOptionPane.ERROR_MESSAGE)
        return
    }

    application {
        val windowState = rememberWindowState(
            size = DpSize(960.dp, 800.dp),
            position = WindowPosition(Alignment.Center),
        )
        var isVisible by remember { mutableStateOf(true) }
        val trayIcon = remember { AppIcon.createBitmapPainter(32) }
        val windowIcon = remember { AppIcon.createBitmapPainter(48) }
        var startupRegistered by remember { mutableStateOf(StartupService.isRegistered()) }

        // 시스템 트레이
        Tray(
            icon = trayIcon,
            tooltip = "LoL 수집기 v$APP_VERSION",
            onAction = { isVisible = true },
            menu = {
                Item("창 열기") { isVisible = true }
                Item("웹사이트") {
                    try { Desktop.getDesktop().browse(URI("https://gijun.net")) } catch (_: Exception) {}
                }
                if (StartupService.isPackagedApp()) {
                    Separator()
                    Item(
                        if (startupRegistered) "시작 프로그램 해제" else "시작 프로그램 등록",
                    ) {
                        startupRegistered = StartupService.toggle()
                    }
                }
                Separator()
                Item("종료") { exitApplication() }
            },
        )

        Window(
            onCloseRequest = { isVisible = false },
            visible = isVisible,
            state = windowState,
            title = "LoL 수집기",
            icon = windowIcon,
            undecorated = true,
            resizable = true,
        ) {
            window.minimumSize = java.awt.Dimension(760, 680)
            // AWT 윈도우 아이콘 (태스크바)
            LaunchedEffect(Unit) {
                window.iconImages = listOf(
                    AppIcon.createAwtImage(16),
                    AppIcon.createAwtImage(32),
                    AppIcon.createAwtImage(48),
                )
            }
            LolTheme {
                App(
                    windowScope = this,
                    onMinimize = { windowState.isMinimized = true },
                    onClose = { isVisible = false },
                )
            }
        }
    }
}

@Composable
private fun App(
    windowScope: WindowScope,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
) {
    var currentPage by remember { mutableStateOf(Page.DASHBOARD) }
    var lcuStatus by remember { mutableStateOf(LcuStatus(connected = false)) }
    var autoStatus by remember { mutableStateOf("") }
    val autoLogs = remember { mutableStateListOf<LogLine>() }

    // ── 자동 업데이트 ──
    val updateScope = rememberCoroutineScope()
    val updateService = remember { UpdateService(APP_VERSION, updateScope) }
    var updateState by remember { mutableStateOf(UpdateState.IDLE) }
    var updateProgress by remember { mutableStateOf(0) }
    var updateVersion by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        updateService.onStateChanged = {
            updateState = updateService.state
            updateProgress = updateService.downloadProgress
            updateVersion = updateService.updateInfo?.version ?: ""
        }
        // 시작 시 업데이트 확인
        updateService.checkForUpdates()
    }

    // ── LCU 상태 폴링 ──
    LaunchedEffect(Unit) {
        while (isActive) {
            lcuStatus = try { LcuClient.getStatus() } catch (_: Exception) { LcuStatus(connected = false) }
            delay(5_000)
        }
    }

    // ── 게임 페이즈 모니터 ──
    val monitorScope = rememberCoroutineScope()
    var dodgeCount by remember { mutableStateOf(0) }
    val monitor = remember {
        GamePhaseMonitor(
            scope = monitorScope,
            onLog = { type, message -> autoLogs.add(LogLine(type, message)) },
            onAutoStatus = { autoStatus = it },
            onNotification = { _, body -> autoStatus = body },
        )
    }
    LaunchedEffect(Unit) {
        monitor.start()
    }
    // Sync dodge count from monitor
    LaunchedEffect(Unit) {
        while (isActive) {
            dodgeCount = monitor.dodgeCount
            delay(3_000)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 메인 UI
        Column(
            modifier = Modifier.fillMaxSize().background(LolColors.BgPrimary),
        ) {
            with(windowScope) {
                Titlebar(version = APP_VERSION, lcuStatus = lcuStatus, onMinimize = onMinimize, onClose = onClose)
            }
            HorizontalDivider(thickness = 1.dp, color = LolColors.Border)

            Grid16(Modifier.fillMaxSize(), gap = 0.dp) {
                Sidebar(currentPage = currentPage, onPageChange = { currentPage = it }, modifier = Modifier.colSpan(3))
                Box(Modifier.colSpan(13).fillMaxHeight().background(LolColors.BgPrimary)) {
                    when (currentPage) {
                        Page.DASHBOARD -> DashboardPage()
                        Page.MATCHES -> MatchHistoryPage()
                        Page.COLLECT -> CollectPage(
                            lcuStatus = lcuStatus,
                            autoStatus = autoStatus,
                            updateState = updateState,
                            updateVersion = updateVersion,
                            currentVersion = APP_VERSION,
                            downloadProgress = updateProgress,
                            updateErrorMessage = updateService.errorMessage,
                            onInstallUpdate = { updateService.installUpdate() },
                            onRetryUpdate = { updateService.retryDownload() },
                            dodgeCount = dodgeCount,
                        )
                        Page.CUSTOM -> CustomGamePage()
                        Page.SUMMONER -> SummonerPage()
                    }
                }
            }
        }

        // 업데이트 오버레이 (다운로드 중 / 설치 중 전체 화면)
        AnimatedVisibility(
            visible = updateState == UpdateState.CHECKING || updateState == UpdateState.INSTALLING,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            UpdateOverlay(updateState, updateProgress)
        }
    }
}

@Composable
private fun UpdateOverlay(state: UpdateState, progress: Int) {
    val message = when (state) {
        UpdateState.CHECKING -> "업데이트 확인 중..."
        UpdateState.INSTALLING -> "업데이트 설치 중... 잠시 후 재시작됩니다"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LolColors.BgPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("LoL 수집기", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LolColors.Primary, letterSpacing = 1.sp)
            Text(message, fontSize = 13.sp, color = LolColors.TextSecondary)
            if (state == UpdateState.CHECKING) {
                LinearProgressIndicator(
                    modifier = Modifier.width(240.dp).height(4.dp),
                    color = LolColors.Primary,
                    trackColor = LolColors.BgTertiary,
                )
            }
        }
    }
}
