package com.gijun.lolml

import com.gijun.lolml.data.Match
import com.gijun.lolml.data.MatchCleaner
import com.gijun.lolml.data.PlayerRank
import com.gijun.lolml.data.RankFormat
import com.gijun.lolml.data.RankReader
import com.gijun.lolml.data.RankWriter
import com.gijun.lolml.data.SnapshotFormat
import com.gijun.lolml.data.SnapshotReader
import com.gijun.lolml.data.SnapshotWriter
import com.gijun.lolml.eval.printReport
import com.gijun.lolml.extract.DbConfig
import com.gijun.lolml.extract.MatchExtractor
import com.gijun.lolml.extract.PlayerExtractor
import com.gijun.lolml.extract.RiotApi
import com.gijun.lolml.feature.FeatureBuilder
import com.gijun.lolml.feature.TierPrior
import java.nio.file.Files
import kotlin.system.exitProcess

/** 5v5 내전(소환사의 협곡). 서비스의 `GameMode.NORMAL` 과 같은 범위다. */
private val CUSTOM_QUEUE_IDS = setOf(0, 3130)

/** 서비스의 레이팅 검증과 같은 기준이다. 이보다 짧으면 다시하기·초반 AFK 로 본다. */
private const val MIN_DURATION_SEC = 600

private const val SOLO_QUEUE = "RANKED_SOLO_5x5"
private const val FLEX_QUEUE = "RANKED_FLEX_SR"

/**
 * 맨 앞 경기들은 전원이 Elo 1500 이라 피처가 전부 0 에 가깝다. 상태는 움직이게 두되
 * 학습·평가 표본에서는 뺀다.
 */
private const val WARMUP_MATCHES = 30

/** walk-forward 의 첫 학습에 쓰는 경기 수. 이 뒤의 경기가 전부 평가 대상이다. */
private const val MIN_TRAIN_MATCHES = 60

/**
 * 추출과 학습은 따로 돈다. 학습은 DB 를 모르고 스냅샷 파일만 읽는다.
 *
 *   ./gradlew run --args="extract"   운영 DB → data/ 스냅샷
 *   ./gradlew run --args="ranks"     운영 DB 의 Riot ID → Riot API → data/ 의 티어 스냅샷
 *   ./gradlew run --args="train"     스냅샷 → 피처 → walk-forward 학습·평가
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "extract" -> {
            runExtract()
        }

        "ranks" -> {
            runRanks()
        }

        "train" -> {
            runTrain()
        }

        else -> {
            System.err.println("usage: extract | ranks | train")
            exitProcess(1)
        }
    }
}

private fun runExtract() {
    val matches = ArrayList<Match>()
    MatchExtractor(DbConfig.fromEnv()).extract { matches.add(it) }
    SnapshotWriter(SnapshotFormat.DEFAULT_PATH).write(matches.asSequence())
    println("${matches.size}경기 → ${SnapshotFormat.DEFAULT_PATH}")
}

private fun runRanks() {
    val players = PlayerExtractor(DbConfig.fromEnv()).extract()
    val api = RiotApi.fromEnv()
    val fetchedAt = System.currentTimeMillis()
    val ranks =
        players.map { (playerId, riotId) ->
            val queues = api.puuid(riotId)?.let { api.ranks(it) }
            PlayerRank(playerId, riotId, queues != null, queues?.get(SOLO_QUEUE), queues?.get(FLEX_QUEUE), fetchedAt)
        }
    RankWriter(RankFormat.DEFAULT_PATH).write(ranks)
    println(
        "${ranks.size}명 → ${RankFormat.DEFAULT_PATH} " +
            "(계정 못 찾음 ${ranks.count { !it.found }}, 솔로랭크 ${ranks.count { it.solo != null }}, " +
            "자유랭크만 ${ranks.count { it.solo == null && it.flex != null }}, " +
            "언랭 ${ranks.count { it.found && it.solo == null && it.flex == null }})",
    )
}

private fun runTrain() {
    val raw = SnapshotReader(SnapshotFormat.DEFAULT_PATH).read()
    val cleaned = MatchCleaner(MIN_DURATION_SEC, CUSTOM_QUEUE_IDS).clean(raw)
    println("스냅샷 ${raw.size}경기 → 정제 후 ${cleaned.size}경기 → 예열 $WARMUP_MATCHES 경기 제외")

    // 티어 스냅샷이 없으면 티어 피처는 전부 0 이고, 티어 라인 Elo 는 라인 Elo 와 같아진다.
    val hasRanks = Files.exists(RankFormat.DEFAULT_PATH)
    if (!hasRanks) println("${RankFormat.DEFAULT_PATH} 가 없다 — 티어 없이 돈다 (`ranks` 로 받는다)")
    val tiers = if (hasRanks) TierPrior(RankReader(RankFormat.DEFAULT_PATH).read()) else TierPrior()

    val examples = FeatureBuilder(tiers).build(cleaned).drop(WARMUP_MATCHES)
    require(examples.size > MIN_TRAIN_MATCHES) { "학습할 경기가 너무 적다: ${examples.size}" }
    printReport(examples, MIN_TRAIN_MATCHES)
}
