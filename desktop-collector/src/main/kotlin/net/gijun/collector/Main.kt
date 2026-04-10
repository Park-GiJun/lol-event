package net.gijun.collector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import net.gijun.collector.lcu.LcuClient
import net.gijun.collector.lcu.LcuStatus
import net.gijun.collector.service.GamePhaseMonitor
import net.gijun.collector.ui.components.Page
import net.gijun.collector.ui.components.Sidebar
import net.gijun.collector.ui.components.Titlebar
import net.gijun.collector.ui.pages.*
import net.gijun.collector.ui.theme.LolColors
import net.gijun.collector.ui.theme.LolTheme
import java.awt.Desktop
import java.awt.image.BufferedImage
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import javax.swing.JOptionPane

private const val APP_VERSION = "1.0.0"
private const val SINGLE_INSTANCE_PORT = 47632

private fun createTrayIcon(): BitmapPainter {
    val img = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.color = java.awt.Color(0xC8, 0x9B, 0x3C) // Gold
    g.fillOval(2, 2, 28, 28)
    g.color = java.awt.Color(0x0A, 0x14, 0x28) // Dark
    g.fillOval(6, 6, 20, 20)
    g.color = java.awt.Color(0xC8, 0x9B, 0x3C)
    g.fillOval(10, 10, 12, 12)
    g.dispose()
    return BitmapPainter(org.jetbrains.skia.Image.makeFromBitmap(
        org.jetbrains.skia.Bitmap().also { bmp ->
            bmp.allocPixels(org.jetbrains.skia.ImageInfo.makeN32Premul(32, 32))
            for (y in 0 until 32) for (x in 0 until 32) {
                bmp.erase(img.getRGB(x, y), org.jetbrains.skia.IRect.makeXYWH(x, y, 1, 1))
            }
        }
    ).toComposeImageBitmap())
}

fun main() {
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
        val trayIcon = remember { createTrayIcon() }

        // 시스템 트레이
        Tray(
            icon = trayIcon,
            tooltip = "LoL 수집기",
            menu = {
                Item("창 열기") { isVisible = true }
                Item("웹사이트") {
                    try { Desktop.getDesktop().browse(URI("https://gijun.net")) } catch (_: Exception) {}
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
            undecorated = true,
            resizable = true,
        ) {
            window.minimumSize = java.awt.Dimension(760, 680)
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
    var currentPage by remember { mutableStateOf(Page.COLLECT) }
    var lcuStatus by remember { mutableStateOf(LcuStatus(connected = false)) }
    var autoStatus by remember { mutableStateOf("") }

    val autoLogs = remember { mutableStateListOf<LogLine>() }

    LaunchedEffect(Unit) {
        while (isActive) {
            lcuStatus = try { LcuClient.getStatus() } catch (_: Exception) { LcuStatus(connected = false) }
            delay(5_000)
        }
    }

    val monitorScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val monitor = GamePhaseMonitor(
            scope = monitorScope,
            onLog = { type, message -> autoLogs.add(LogLine(type, message)) },
            onAutoStatus = { autoStatus = it },
            onNotification = { _, body -> autoStatus = body },
        )
        monitor.start()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(LolColors.BgPrimary),
    ) {
        with(windowScope) {
            Titlebar(version = APP_VERSION, lcuStatus = lcuStatus, onMinimize = onMinimize, onClose = onClose)
        }
        HorizontalDivider(thickness = 1.dp, color = LolColors.Border)

        Row(Modifier.fillMaxSize()) {
            Sidebar(currentPage = currentPage, onPageChange = { currentPage = it })
            Box(Modifier.fillMaxSize().background(LolColors.BgPrimary)) {
                when (currentPage) {
                    Page.COLLECT -> CollectPage(lcuStatus, autoStatus)
                    Page.CUSTOM -> CustomGamePage()
                    Page.SUMMONER -> SummonerPage()
                }
            }
        }
    }
}
