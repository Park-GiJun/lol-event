package net.gijun.collector.service

import kotlinx.coroutines.*
import net.gijun.collector.lcu.LcuClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DodgeEvent(
    val timestamp: String,
    val teamComposition: List<String>, // riotId or champion info
)

class GamePhaseMonitor(
    private val scope: CoroutineScope,
    private val onLog: (type: String, message: String) -> Unit,
    private val onAutoStatus: (message: String) -> Unit,
    private val onNotification: (title: String, body: String) -> Unit,
) {
    private var lastPhase = ""
    private var autoCollectScheduled = false
    private var lastChampSelectTeam: List<String> = emptyList()

    // Publicly accessible dodge tracking state
    var dodgeCount: Int = 0
        private set
    val dodgeHistory: MutableList<DodgeEvent> = mutableListOf()

    fun start() {
        scope.launch {
            while (isActive) {
                checkAndAutoCollect()
                delay(30_000)
            }
        }
        // Secondary coroutine for faster dodge detection (5s polling)
        scope.launch {
            while (isActive) {
                checkForDodge()
                delay(5_000)
            }
        }
    }

    private suspend fun checkForDodge() {
        try {
            val phase = LcuClient.getGamePhase() ?: return

            // Track team composition during champ select for dodge logging
            if (phase == "ChampSelect") {
                try {
                    val champSelect = LcuClient.getChampSelectFull()
                    if (champSelect != null) {
                        lastChampSelectTeam = (champSelect.myTeam + champSelect.theirTeam)
                            .filter { it.riotId.isNotEmpty() }
                            .map { slot ->
                                val champInfo = if (slot.championId > 0) "(champ:${slot.championId})" else ""
                                "${slot.riotId}$champInfo"
                            }
                    }
                } catch (_: Exception) {}
            }

            // Dodge detection: ChampSelect -> None or ChampSelect -> Lobby
            if (lastPhase == "ChampSelect" && phase in listOf("None", "Lobby")) {
                dodgeCount++
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val event = DodgeEvent(timestamp, lastChampSelectTeam.toList())
                dodgeHistory.add(event)
                onLog("warn", "닷지 감지 #$dodgeCount ($timestamp) — ${lastChampSelectTeam.size}명")
                onAutoStatus("닷지 감지 — 총 ${dodgeCount}회")
                onNotification("LoL 수집기", "닷지가 감지되었습니다 (#$dodgeCount)")
                lastChampSelectTeam = emptyList()
            }

            lastPhase = phase
        } catch (_: Exception) {}
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
