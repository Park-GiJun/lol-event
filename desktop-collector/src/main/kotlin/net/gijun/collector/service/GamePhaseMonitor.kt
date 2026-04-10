package net.gijun.collector.service

import kotlinx.coroutines.*
import net.gijun.collector.lcu.LcuClient

class GamePhaseMonitor(
    private val scope: CoroutineScope,
    private val onLog: (type: String, message: String) -> Unit,
    private val onAutoStatus: (message: String) -> Unit,
    private val onNotification: (title: String, body: String) -> Unit,
) {
    private var lastPhase = ""
    private var autoCollectScheduled = false

    fun start() {
        scope.launch {
            while (isActive) {
                checkAndAutoCollect()
                delay(30_000)
            }
        }
    }

    private suspend fun checkAndAutoCollect() {
        val lockfilePath = LcuClient.findLockfile() ?: run { lastPhase = ""; return }
        try {
            val phase = LcuClient.getGamePhase() ?: return

            if (phase == "Lobby") {
                try { LcuClient.cacheLobbyMembers() } catch (_: Exception) {}
            }

            if (lastPhase == "InProgress"
                && phase in listOf("EndOfGame", "WaitingForStats", "None", "Lobby")
                && !autoCollectScheduled
            ) {
                autoCollectScheduled = true
                onAutoStatus("게임 종료 감지 — 30초 후 자동 수집 시작")
                scope.launch {
                    delay(30_000)
                    autoCollectScheduled = false
                    try {
                        CollectService.runCollect { type, message ->
                            onLog(type, message)
                            if (type == "done") {
                                onNotification("LoL 수집기", message)
                                onAutoStatus("자동 수집 완료 — $message")
                            }
                            if (type == "error") {
                                onAutoStatus("자동 수집 실패 — $message")
                            }
                        }
                    } catch (e: Exception) {
                        onLog("error", e.message ?: "자동 수집 오류")
                        onAutoStatus("")
                    }
                }
            }
            lastPhase = phase
        } catch (_: Exception) {
            // LCU 미연결
        }
    }
}
