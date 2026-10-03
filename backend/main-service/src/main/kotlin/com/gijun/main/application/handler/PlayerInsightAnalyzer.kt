package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.LaneLeaderboardResult
import com.gijun.main.application.dto.result.PlayerLaneStat
import com.gijun.main.application.dto.result.SummonerProfileResult
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/** 한 사람의 강점과 약점. 문장마다 근거가 된 수치와 비교 대상이 들어 있다. */
internal data class PlayerInsights(
    val strengths: List<String>,
    val weaknesses: List<String>,
)

/**
 * 강점·약점을 **수치로** 뽑는다. LLM 이 "잘한다/못한다" 를 지어내지 않게, 판단까지 여기서 끝낸다.
 *
 * 기준은 **같은 포지션을 가는 사람들 사이의 순위**다. 전체 평균과 견주면 서포터는 딜량이, 정글은 CS 가
 * 늘 "약점" 으로 나온다. 주 포지션의 지표를 그 포지션을 충분히 해 본 사람들과만 비교한다.
 *
 * 위 3 분의 1 이면 강점, 아래 3 분의 1 이면 약점으로 본다. 가운데는 말하지 않는다 — 평범한 것까지
 * 다 적으면 정작 눈에 띄는 것이 묻힌다.
 */
internal object PlayerInsightAnalyzer {
    /** 이 판수는 해야 비교에 낀다. 한두 판의 평균은 그 사람의 실력이 아니다. */
    const val MIN_GAMES = 5

    /** 비교할 사람이 이보다 적으면 순위가 뜻이 없다. */
    private const val MIN_PEERS = 4
    private const val MAX_EACH = 4

    private const val CHAMPION_MIN_GAMES = 5
    private const val POSITION_MIN_GAMES = 8
    private const val GOOD_WIN_RATE = 55
    private const val BAD_WIN_RATE = 42
    private const val PERCENT = 100

    private class Metric(
        val label: String,
        val value: (PlayerLaneStat) -> Double,
        val format: (Double) -> String,
        val higherIsBetter: Boolean = true,
        /** 이 지표가 뜻이 있는 포지션. null 이면 전부. */
        val lanes: Set<String>? = null,
    )

    private val METRICS =
        listOf(
            Metric("승률", { it.winRate.toDouble() }, { "${it.roundToInt()}%" }),
            Metric("KDA", { it.kda }, { decimal(it, 2) }),
            Metric("평균 데스", { it.avgDeaths }, { decimal(it, 1) }, higherIsBetter = false),
            Metric("평균 딜량", { it.avgDamage.toDouble() }, { thousands(it) }, lanes = setOf("TOP", "JUNGLE", "MID", "ADC")),
            Metric("평균 CS", { it.avgCs }, { decimal(it, 1) }, lanes = setOf("TOP", "MID", "ADC")),
            Metric("평균 골드", { it.avgGold.toDouble() }, { thousands(it) }, lanes = setOf("TOP", "JUNGLE", "MID", "ADC")),
            Metric("시야 점수", { it.avgVisionScore }, { decimal(it, 1) }),
            Metric("오브젝트 딜", { it.avgObjectiveDamage.toDouble() }, { thousands(it) }, lanes = setOf("JUNGLE")),
            Metric("와드 설치", { it.avgWardsPlaced }, { decimal(it, 1) }, lanes = setOf("SUPPORT")),
            Metric("CC 시간", { it.avgCcTime }, { decimal(it, 1) + "초" }, lanes = setOf("SUPPORT", "JUNGLE", "TOP")),
        )

    /**
     * @param laneBoard 그 사람 주 포지션의 포지션별 통계. 주 포지션이 없으면 null.
     */
    fun analyze(
        profile: SummonerProfileResult,
        laneBoard: LaneLeaderboardResult?,
        names: ChampionNames,
    ): PlayerInsights {
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()

        elo(profile, strengths, weaknesses)
        laneBoard?.let { lane(profile.profile.riotId, it, strengths, weaknesses) }
        positions(profile, strengths, weaknesses)
        champions(profile, names, strengths, weaknesses)

        return PlayerInsights(strengths.take(MAX_EACH + 2), weaknesses.take(MAX_EACH + 2))
    }

    /** 라인 Elo 순위. 이 저장소에서 승패를 가장 잘 맞힌 지표라 맨 앞에 둔다. */
    private fun elo(
        profile: SummonerProfileResult,
        strengths: MutableList<String>,
        weaknesses: MutableList<String>,
    ) {
        val rating = profile.profile.rating ?: return
        val rank = profile.profile.eloRank ?: return
        val total = profile.profile.eloRankedTotal
        if (total < MIN_PEERS) return

        val text =
            "라인 Elo ${rating.laneElo.roundToInt()} — ${total}명 중 ${rank}위" +
                " (라인전 ${rating.laneDuels}번 중 ${rating.laneWins}승, 승률 ${(rating.laneWinRate * PERCENT).roundToInt()}%)"
        when (tierOf(rank, total)) {
            Tier.TOP -> strengths += text
            Tier.BOTTOM -> weaknesses += text
            Tier.MIDDLE -> Unit
        }
    }

    private fun lane(
        riotId: String,
        board: LaneLeaderboardResult,
        strengths: MutableList<String>,
        weaknesses: MutableList<String>,
    ) {
        val peers = board.players.filter { it.games >= MIN_GAMES }
        val self = peers.firstOrNull { it.riotId == riotId } ?: return
        if (peers.size < MIN_PEERS) return
        val lane = RagDocumentWriter.positionLabel(board.lane)

        val ranked =
            METRICS
                .filter { it.lanes == null || board.lane in it.lanes }
                .map { metric ->
                    val mine = metric.value(self)
                    val better = peers.count { if (metric.higherIsBetter) metric.value(it) > mine else metric.value(it) < mine }
                    Triple(metric, mine, better + 1)
                }

        fun line(item: Triple<Metric, Double, Int>) =
            "${item.first.label} ${item.first.format(item.second)} — $lane ${peers.size}명 중 ${item.third}위"

        strengths +=
            ranked
                .filter { tierOf(it.third, peers.size) == Tier.TOP }
                .sortedBy { it.third }
                .take(MAX_EACH)
                .map(::line)
        weaknesses +=
            ranked
                .filter { tierOf(it.third, peers.size) == Tier.BOTTOM }
                .sortedByDescending { it.third }
                .take(MAX_EACH)
                .map(::line)
    }

    /** 주 포지션이 아닌 자리의 성적. 팀을 짤 때 "그 자리에 앉혀도 되는가" 의 근거다. */
    private fun positions(
        profile: SummonerProfileResult,
        strengths: MutableList<String>,
        weaknesses: MutableList<String>,
    ) {
        profile.positionStats.filter { it.games >= POSITION_MIN_GAMES }.forEach { stat ->
            val text = "${RagDocumentWriter.positionLabel(stat.position)}에서 ${stat.games}판 승률 ${stat.winRate}%"
            when {
                stat.winRate >= GOOD_WIN_RATE -> strengths += text
                stat.winRate <= BAD_WIN_RATE -> weaknesses += text
            }
        }
    }

    private fun champions(
        profile: SummonerProfileResult,
        names: ChampionNames,
        strengths: MutableList<String>,
        weaknesses: MutableList<String>,
    ) {
        val played = profile.championStats.filter { it.games >= CHAMPION_MIN_GAMES }
        played.filter { it.winRate >= GOOD_WIN_RATE }.maxByOrNull { it.winRate }?.let {
            strengths += "${names.label(it.champion)} ${it.games}판 승률 ${it.winRate}% (KDA ${it.kda})"
        }
        played.filter { it.winRate <= BAD_WIN_RATE }.minByOrNull { it.winRate }?.let {
            weaknesses += "${names.label(it.champion)} ${it.games}판 승률 ${it.winRate}% (KDA ${it.kda})"
        }
    }

    private enum class Tier { TOP, MIDDLE, BOTTOM }

    private fun tierOf(
        rank: Int,
        total: Int,
    ): Tier {
        val third = ceil(total / 3.0).toInt()
        return when {
            rank <= third -> Tier.TOP
            rank > total - third -> Tier.BOTTOM
            else -> Tier.MIDDLE
        }
    }

    private fun decimal(
        value: Double,
        digits: Int,
    ) = String.format(Locale.ROOT, "%.${digits}f", value)

    private fun thousands(value: Double) = String.format(Locale.ROOT, "%,d", value.roundToInt())
}
