package com.gijun.lolml

import com.gijun.lolml.data.Match
import com.gijun.lolml.data.Participant
import com.gijun.lolml.data.Position
import com.gijun.lolml.data.Team

fun participant(
    playerId: String,
    team: Team,
    win: Boolean,
    kills: Int = 3,
    deaths: Int = 3,
    assists: Int = 3,
    position: Position = Position.UNKNOWN,
    gold: Int = 10_000,
) = Participant(playerId, team, position, "Garen", win, kills, deaths, assists, gold, damageSelfMitigated = 0)

private val SEATS = listOf(Position.TOP, Position.JUNGLE, Position.MID, Position.ADC, Position.SUPPORT)

/** 블루는 [blue], 레드는 [red] 다섯 명. 적힌 순서대로 탑·정글·미드·원딜·서폿에 앉는다. */
fun match(
    id: String,
    blue: List<String>,
    red: List<String>,
    blueWin: Boolean,
    gameCreation: Long = 0L,
    queueId: Int = 0,
    durationSec: Int = 1800,
    blueKills: Int = 3,
    /** 레드는 10,000 이다. 더 많으면 블루가 다섯 라인을 다 이긴 것이고, 같으면 맞대결이 성립하지 않는다. */
    blueGold: Int = 10_000,
) = Match(
    matchId = id,
    queueId = queueId,
    gameCreation = gameCreation,
    gameDurationSec = durationSec,
    participants =
        blue.mapIndexed {
            i,
            id,
            ->
            participant(id, Team.BLUE, blueWin, kills = blueKills, position = SEATS[i % SEATS.size], gold = blueGold)
        } +
            red.mapIndexed { i, id -> participant(id, Team.RED, !blueWin, position = SEATS[i % SEATS.size]) },
)

val TEAM_A = listOf("a1", "a2", "a3", "a4", "a5")
val TEAM_B = listOf("b1", "b2", "b3", "b4", "b5")
