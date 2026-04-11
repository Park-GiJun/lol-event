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
    // ë¹Œë“œ ???¬ìš©??ICO ?Œì¼ ?ë™ ?ì„± (?†ìœ¼ë©?
    val icoFile = File("src/main/resources/icon.ico")
    if (!icoFile.exists()) {
        icoFile.parentFile.mkdirs()
        try { AppIcon.writeIcoFile(icoFile) } catch (_: Exception) {}
    }

    // ?±ê? ?¸ìŠ¤?´ìŠ¤ ??
    try {
        ServerSocket(SINGLE_INSTANCE_PORT, 0, InetAddress.getByName("127.0.0.1"))
    } catch (_: Exception) {
        JOptionPane.showMessageDialog(null, "?´ë? ?¤í–‰ ì¤‘ìž…?ˆë‹¤.", "LoL ?˜ì§‘ê¸?, JOptionPane.ERROR_MESSAGE)
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

        // ?œìŠ¤???¸ë ˆ??
        Tray(
            icon = trayIcon,
            tooltip = "LoL ?˜ì§‘ê¸?v$APP_VERSION",
            onAction = { isVisible = true },
            menu = {
                Item("ì°??´ê¸°") { isVisible = true }
                Item("?¹ì‚¬?´íŠ¸") {
                    try { Desktop.getDesktop().browse(URI("https://gijun.net")) } catch (_: Exception) {}
                }
                if (StartupService.isPackagedApp()) {
                    Separator()
                    Item(
                        if (startupRegistered) "?œìž‘ ?„ë¡œê·¸ëž¨ ?´ì œ" else "?œìž‘ ?„ë¡œê·¸ëž¨ ?±ë¡",
                    ) {
                        startupRegistered = StartupService.toggle()
                    }
                }
                Separator()
                Item("ì¢…ë£Œ") { exitApplication() }
            },
        )

        Window(
            onCloseRequest = { isVisible = false },
            visible = isVisible,
            state = windowState,
            title = "LoL ?˜ì§‘ê¸?,
            icon = windowIcon,
            undecorated = true,
            resizable = true,
        ) {
            window.minimumSize = java.awt.Dimension(760, 680)
            // AWT ?ˆë„???„ì´ì½?(?œìŠ¤?¬ë°”)
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

    // ?€?€ ?ë™ ?…ë°?´íŠ¸ ?€?€
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
        // ?œìž‘ ???…ë°?´íŠ¸ ?•ì¸
        updateService.checkForUpdates()
    }

    // ?€?€ LCU ?íƒœ ?´ë§ ?€?€
    LaunchedEffect(Unit) {
        while (isActive) {
            lcuStatus = try { LcuClient.getStatus() } catch (_: Exception) { LcuStatus(connected = false) }
            delay(5_000)
        }
    }

    // ?€?€ ê²Œìž„ ?˜ì´ì¦?ëª¨ë‹ˆ???€?€
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
        // ë©”ì¸ UI
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

        // ?…ë°?´íŠ¸ ?¤ë²„?ˆì´ (?¤ìš´ë¡œë“œ ì¤?/ ?¤ì¹˜ ì¤??„ì²´ ?”ë©´)
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
        UpdateState.CHECKING -> "?…ë°?´íŠ¸ ?•ì¸ ì¤?.."
        UpdateState.INSTALLING -> "?…ë°?´íŠ¸ ?¤ì¹˜ ì¤?.. ? ì‹œ ???¬ì‹œ?‘ë©?ˆë‹¤"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LolColors.BgPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("LoL ?˜ì§‘ê¸?, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LolColors.Primary, letterSpacing = 1.sp)
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
