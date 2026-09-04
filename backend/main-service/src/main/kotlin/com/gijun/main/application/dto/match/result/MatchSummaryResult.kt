package com.gijun.main.application.dto.match.result

import com.gijun.main.domain.model.match.Match
import com.gijun.main.domain.model.match.MatchParticipant
import com.gijun.main.domain.model.match.MatchTeam

/**
 * 경기 목록 화면 전용 요약 DTO.
 *
 * 상세 DTO(MatchResult)는 참가자 1명당 114개 필드를 담는다. 목록에서 그걸 그대로 내리면
 * 154경기 기준 응답이 3.6MB가 되는데, 목록이 실제로 그리는 건 챔피언 아이콘 / KDA / CS /
 * 아이템 / 스펠뿐이다. 여기서는 그 화면이 쓰는 필드만 담는다.
 */
data class MatchPageResult(
    val matches: List<MatchSummaryResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

data class MatchSummaryResult(
    val matchId: String,
    val queueId: Int,
    val gameCreation: Long,
    val gameDuration: Int,
    val gameMode: String?,
    val participants: List<ParticipantSummaryResult>,
    val teams: List<TeamSummaryResult>,
) {
    companion object {
        fun from(domain: Match) = MatchSummaryResult(
            matchId = domain.matchId,
            queueId = domain.queueId,
            gameCreation = domain.gameCreation,
            gameDuration = domain.gameDuration,
            gameMode = domain.gameMode,
            participants = domain.participants.map { ParticipantSummaryResult.from(it) },
            teams = domain.teams.map { TeamSummaryResult.from(it) },
        )
    }
}

data class ParticipantSummaryResult(
    val puuid: String?,
    val riotId: String,
    val champion: String,
    val championId: Int,
    val team: String,
    val teamId: Int,
    val win: Boolean,
    val kills: Int,
    val deaths: Int,
    val assists: Int,
    val damage: Int,
    val cs: Int,
    val gold: Int,
    val visionScore: Int,
    val champLevel: Int,
    val spell1Id: Int,
    val spell2Id: Int,
    val perkPrimaryStyle: Int,
    val perkSubStyle: Int,
    val perk0: Int,
    val item0: Int, val item1: Int, val item2: Int,
    val item3: Int, val item4: Int, val item5: Int, val item6: Int,
    /** 목록의 포지션 표기는 이 값만 쓴다. 원본 lane/role 은 내려보내지 않는다. */
    val assignedPosition: String,
) {
    companion object {
        fun from(domain: MatchParticipant) = ParticipantSummaryResult(
            puuid = domain.puuid,
            riotId = domain.riotId,
            champion = domain.champion,
            championId = domain.championId,
            team = domain.team,
            teamId = domain.teamId,
            win = domain.win,
            kills = domain.kills,
            deaths = domain.deaths,
            assists = domain.assists,
            damage = domain.damage,
            cs = domain.cs,
            gold = domain.gold,
            visionScore = domain.visionScore,
            champLevel = domain.champLevel,
            spell1Id = domain.spell1Id,
            spell2Id = domain.spell2Id,
            perkPrimaryStyle = domain.perkPrimaryStyle,
            perkSubStyle = domain.perkSubStyle,
            perk0 = domain.perk0,
            item0 = domain.item0, item1 = domain.item1, item2 = domain.item2,
            item3 = domain.item3, item4 = domain.item4, item5 = domain.item5, item6 = domain.item6,
            assignedPosition = domain.assignedPosition,
        )
    }
}

data class TeamSummaryResult(
    val teamId: Int,
    val win: Boolean,
    val baronKills: Int,
    val dragonKills: Int,
    val towerKills: Int,
) {
    companion object {
        fun from(domain: MatchTeam) = TeamSummaryResult(
            teamId = domain.teamId,
            win = domain.win,
            baronKills = domain.baronKills,
            dragonKills = domain.dragonKills,
            towerKills = domain.towerKills,
        )
    }
}
