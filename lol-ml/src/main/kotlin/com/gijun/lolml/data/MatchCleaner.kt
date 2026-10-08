package com.gijun.lolml.data

/**
 * 정제 규칙. 원본 스냅샷은 건드리지 않고 여기서 걸러낸 결과만 학습에 쓴다.
 * 순서(시간순)는 유지한다.
 */
class MatchCleaner(
    /** 이보다 짧으면 다시하기·초반 AFK 로 본다. 값은 `game_duration` 분포를 보고 정한다. */
    private val minDurationSec: Int,
    /** 내전 큐만 남긴다. */
    private val queueIds: Set<Int>,
) {
    fun clean(matches: List<Match>): List<Match> {
        val seenIds = HashSet<String>()
        // 같은 경기가 다른 match_id 로 두 번 들어온 경우. 시작 시각과 열 명이 같으면 같은 경기다.
        val seenGames = HashSet<Pair<Long, Set<String>>>()
        return matches.filter { match ->
            match.queueId in queueIds &&
                match.gameDurationSec >= minDurationSec &&
                isFiveVsFive(match) &&
                seenIds.add(match.matchId) &&
                seenGames.add(match.gameCreation to match.participants.map { it.playerId }.toSet())
        }
    }

    /** 팀마다 다섯, 열 명이 전부 다른 사람, 승패가 팀 안에서는 같고 팀끼리는 반대. */
    private fun isFiveVsFive(match: Match): Boolean {
        val blue = match.participants.filter { it.team == Team.BLUE }
        val red = match.participants.filter { it.team == Team.RED }
        if (blue.size != TEAM_SIZE || red.size != TEAM_SIZE) return false
        if (match.participants
                .map { it.playerId }
                .toSet()
                .size != TEAM_SIZE * 2
        ) {
            return false
        }
        val blueWin = blue.first().win
        return blue.all { it.win == blueWin } && red.all { it.win != blueWin }
    }

    private companion object {
        const val TEAM_SIZE = 5
    }
}
