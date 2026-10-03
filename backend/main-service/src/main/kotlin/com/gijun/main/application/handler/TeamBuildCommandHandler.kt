package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.BuildTeamsCommand
import com.gijun.main.application.dto.result.TeamBuildMemberResult
import com.gijun.main.application.dto.result.TeamBuildResult
import com.gijun.main.application.dto.result.TeamBuildTeamResult
import com.gijun.main.application.dto.result.TeamCandidateResult
import com.gijun.main.application.port.`in`.BuildTeamsUseCase
import com.gijun.main.application.port.`in`.GetTeamCandidatesUseCase
import com.gijun.main.application.port.out.external.LlmCompletionPort
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import com.gijun.main.domain.match.enums.Position
import com.gijun.main.domain.rag.enums.RagDocumentType
import com.gijun.main.domain.rating.service.RatingMath
import com.gijun.main.domain.team.model.TeamBuildModel
import com.gijun.main.domain.team.model.TeamCandidateModel
import com.gijun.main.domain.team.service.TeamBalancer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

/**
 * 팀 편성.
 *
 * **편성은 코드가 하고 LLM 은 해설만 한다.** 누가 어느 팀 어느 자리에 가는지와 Elo·기대 승률은
 * [TeamBalancer] 와 [RatingMath] 가 낸다. LLM 에게는 확정된 편성과 각자의 프로필 문서를 주고
 * "왜 이렇게 됐는지" 를 말로 풀게 한다. 편성을 모델에게 맡기면 5 명씩 나누는 것부터 틀린다.
 *
 * 해설이 실패해도 편성은 돌려준다 — LLM 장비가 꺼져 있어도 팀은 짤 수 있어야 한다.
 */
@Service
class TeamBuildCommandHandler(
    private val getTeamCandidatesUseCase: GetTeamCandidatesUseCase,
    private val ragDocumentQueryPersistencePort: RagDocumentQueryPersistencePort,
    private val llmCompletionPort: LlmCompletionPort,
) : BuildTeamsUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun buildTeams(command: BuildTeamsCommand): TeamBuildResult {
        val known = getTeamCandidatesUseCase.getTeamCandidates().associateBy { it.riotId }
        val candidates = command.players.map { player -> candidateOf(player.riotId.trim(), player.positions, known) }
        val groups = command.togetherGroups.map { group -> group.map { it.trim() }.filter { it.isNotEmpty() } }.filter { it.size > 1 }

        val built = TeamBalancer.build(candidates, groups, command.seed ?: 0)
        val teams = teamResults(built)

        val (commentary, commentaryError) = if (command.commentary) commentaryFor(teams, built, groups, candidates) else (null to null)
        return TeamBuildResult(teams, built.eloSpread, built.positionConflict, commentary, commentaryError)
    }

    /** 기록이 없는 손님도 받는다. 시작 점수에, 포지션을 안 줬으면 다섯 자리 전부. */
    private fun candidateOf(
        riotId: String,
        requested: List<Position>?,
        known: Map<String, TeamCandidateResult>,
    ): TeamCandidateModel {
        val candidate = known[riotId]
        val positions =
            requested?.toSet()?.takeIf { it.isNotEmpty() }
                ?: candidate?.defaultPositions?.map { Position.valueOf(it) }?.toSet()
                ?: TeamBalancer.LANES.toSet()
        return TeamCandidateModel(
            riotId = riotId,
            elo = candidate?.elo ?: RatingMath.START,
            positions = positions,
            positionGames = candidate?.positions.orEmpty().associate { Position.valueOf(it.position) to it.games },
        )
    }

    private fun teamResults(built: TeamBuildModel): List<TeamBuildTeamResult> =
        built.teams.mapIndexed { index, team ->
            val others =
                built.teams
                    .filterIndexed { other, _ -> other != index }
                    .map { it.averageElo }
                    .average()
            TeamBuildTeamResult(
                name = "${index + 1}팀",
                averageElo = team.averageElo,
                winProbability = RatingMath.expected(team.averageElo, others),
                members = team.slots.map { TeamBuildMemberResult(it.riotId, it.position.name, it.elo, it.offRole) },
            )
        }

    private fun commentaryFor(
        teams: List<TeamBuildTeamResult>,
        built: TeamBuildModel,
        groups: List<List<String>>,
        candidates: List<TeamCandidateModel>,
    ): Pair<String?, String?> =
        try {
            llmCompletionPort.complete(COMMENTARY_SYSTEM, commentaryFacts(teams, built, groups, candidates)).trim() to null
        } catch (e: Exception) {
            log.warn("팀 편성 해설을 만들지 못했다: {}", e.message)
            null to (e.message ?: e.javaClass.simpleName)
        }

    private fun commentaryFacts(
        teams: List<TeamBuildTeamResult>,
        built: TeamBuildModel,
        groups: List<List<String>>,
        candidates: List<TeamCandidateModel>,
    ): String =
        buildString {
            appendLine("## 확정된 편성")
            teams.forEach { team ->
                appendLine("${team.name} — 평균 Elo ${team.averageElo.roundToInt()}, 기대 승률 ${(team.winProbability * PERCENT).roundToInt()}%")
                team.members.forEach { member ->
                    val off = if (member.offRole) " (가능 포지션이 아님)" else ""
                    appendLine(
                        "- ${RagDocumentWriter.positionLabel(member.position)}: ${member.riotId} (Elo ${member.elo.roundToInt()})$off",
                    )
                }
            }
            appendLine()
            appendLine("## 편성 조건")
            appendLine("- 팀 평균 Elo 차: ${built.eloSpread.roundToInt()}")
            if (groups.isEmpty()) {
                appendLine("- 같은 팀으로 묶은 사람: 없음")
            } else {
                groups.forEach { appendLine("- 같은 팀으로 묶음: ${it.joinToString(", ")}") }
            }
            candidates.filter { it.positions.size < TeamBalancer.LANES.size }.forEach { candidate ->
                appendLine(
                    "- ${candidate.riotId} 가능 포지션: ${candidate.positions.joinToString(", ") { RagDocumentWriter.positionLabel(it.name) }}",
                )
            }
            if (built.positionConflict) appendLine("- 가능 포지션만으로는 자리를 다 채울 수 없어서, 일부는 가능 포지션이 아닌 자리에 앉았다.")
            appendLine()
            appendLine("## 플레이어 프로필")
            candidates.forEach { candidate ->
                val profile = ragDocumentQueryPersistencePort.findContent(RagDocumentType.PLAYER_PROFILE, candidate.riotId)
                appendLine(profile?.take(PROFILE_EXCERPT) ?: "[플레이어] ${candidate.riotId} — 기록 없음(시작 점수로 계산).")
                appendLine()
            }
        }

    private companion object {
        const val PERCENT = 100

        /** 30 명이면 프로필만으로 컨텍스트를 꽤 쓴다. 앞부분(전적·포지션·주 챔피언)만 준다. */
        const val PROFILE_EXCERPT = 450

        val COMMENTARY_SYSTEM =
            """
            너는 리그 오브 레전드 내전의 팀 편성을 해설하는 도우미다.

            아래 편성은 코드가 이미 계산해 확정한 것이다. 규칙:
            - 편성을 바꾸거나 다른 안을 제안하지 않는다.
            - 숫자는 주어진 것만 쓴다. 새 숫자를 만들거나 어림하지 않는다.
            - 프로필에 없는 사실(성격, 실력 평가 등)을 지어내지 않는다.

            쓸 내용:
            1. 팀별로 한두 문장 — 누가 중심이고 어느 라인이 강한지, 프로필의 포지션·주 챔피언을 근거로.
            2. 눈여겨볼 라인 맞대결 한두 개.
            3. 묶음이나 포지션 제한 때문에 생긴 특징이 있으면 한 문장.

            한국어로, 군더더기 없이 쓴다. 제목이나 표는 쓰지 않는다.
            """.trimIndent()
    }
}
