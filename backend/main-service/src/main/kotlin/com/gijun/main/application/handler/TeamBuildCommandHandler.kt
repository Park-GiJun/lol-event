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
            seatElo = candidate?.seatElo.orEmpty().mapKeys { Position.valueOf(it.key) },
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

    /**
     * 모델에게 줄 사실. **비교와 판단을 여기서 끝낸다** — 라인별 Elo 차, 팀마다 강한 라인과 약한 라인,
     * 익숙하지 않은 자리에 앉은 사람. 모델은 이걸 문장으로 옮기고, 스스로 셈하지 않는다.
     */
    private fun commentaryFacts(
        teams: List<TeamBuildTeamResult>,
        built: TeamBuildModel,
        groups: List<List<String>>,
        candidates: List<TeamCandidateModel>,
    ): String =
        buildString {
            val byId = candidates.associateBy { it.riotId }
            val lanes = TeamBalancer.LANES.map { it.name }

            appendLine("## 확정된 편성 (팀 평균과 맞대결은 전부 \"이 자리 Elo\" — 그 포지션에서의 라인전 실적을 반영한 값 — 로 계산했다)")
            teams.forEach { team ->
                appendLine("${team.name} — 평균 Elo ${team.averageElo.roundToInt()}, 기대 승률 ${(team.winProbability * PERCENT).roundToInt()}%")
                team.members.forEach { member ->
                    val games = byId[member.riotId]?.positionGames.orEmpty()
                    val here = games[Position.valueOf(member.position)] ?: 0
                    val total = games.values.sum()
                    val overall = byId[member.riotId]?.elo?.roundToInt()
                    val familiarity =
                        when {
                            member.offRole -> "가능 포지션이 아닌 자리"
                            total == 0 -> "기록 없음"
                            else -> "이 자리 ${here}판 / 전체 ${total}판"
                        }
                    appendLine(
                        "- ${RagDocumentWriter.positionLabel(
                            member.position,
                        )}: ${member.riotId} (이 자리 Elo ${member.elo.roundToInt()}, 전체 Elo $overall, $familiarity)",
                    )
                }
            }

            appendLine()
            appendLine("## 라인별 맞대결 (이 자리 Elo)")
            lanes.forEach { lane ->
                val seats =
                    teams
                        .map { team ->
                            team.name to team.members.first { it.position == lane }
                        }.sortedByDescending { it.second.elo }
                val gap = (seats.first().second.elo - seats.last().second.elo).roundToInt()
                appendLine(
                    "- ${RagDocumentWriter.positionLabel(lane)}: " +
                        seats.joinToString(" > ") { (team, member) -> "$team ${member.riotId} ${member.elo.roundToInt()}" } +
                        " (차이 $gap)",
                )
            }

            appendLine()
            appendLine("## 팀별 강한 라인과 약한 라인 (그 라인의 전체 팀 평균과의 차)")
            val laneAverage = lanes.associateWith { lane -> teams.map { team -> team.members.first { it.position == lane }.elo }.average() }
            teams.forEach { team ->
                val edges = team.members.map { it to (it.elo - laneAverage.getValue(it.position)).roundToInt() }
                val best = edges.maxBy { it.second }
                val worst = edges.minBy { it.second }
                // 그 자리를 거의 안 가 본 사람. "갈 수는 있다" 와 "익숙하다" 는 다르다.
                val unfamiliar =
                    team.members.mapNotNull { member ->
                        val games = byId[member.riotId]?.positionGames.orEmpty()
                        val here = games[Position.valueOf(member.position)] ?: 0
                        val total = games.values.sum()
                        when {
                            member.offRole -> "${member.riotId}(${RagDocumentWriter.positionLabel(member.position)}, 가능 포지션 아님)"
                            total > 0 && here * UNFAMILIAR_RATIO < total ->
                                "${member.riotId}(${RagDocumentWriter.positionLabel(member.position)} ${here}판 / 전체 ${total}판)"
                            else -> null
                        }
                    }
                appendLine("- ${team.name}")
                appendLine(
                    "  - 가장 강한 라인: ${RagDocumentWriter.positionLabel(best.first.position)} ${best.first.riotId} " +
                        "(Elo ${best.first.elo.roundToInt()}, 라인 평균 대비 ${signed(best.second)})",
                )
                appendLine(
                    "  - 가장 약한 라인: ${RagDocumentWriter.positionLabel(worst.first.position)} ${worst.first.riotId} " +
                        "(Elo ${worst.first.elo.roundToInt()}, 라인 평균 대비 ${signed(worst.second)})",
                )
                appendLine("  - 익숙하지 않은 자리에 앉은 사람: ${unfamiliar.ifEmpty { listOf("없음") }.joinToString(", ")}")
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
            appendLine("## 플레이어별 기록 (강점·약점은 같은 포지션 사람들과 비교한 순위다)")
            candidates.forEach { candidate ->
                appendLine("### ${candidate.riotId}")
                val profile = ragDocumentQueryPersistencePort.findContent(RagDocumentType.PLAYER_PROFILE, candidate.riotId)
                val facts = profile?.lines()?.filter { line -> PROFILE_LINES.any { line.startsWith(it) } }.orEmpty()
                if (facts.isEmpty()) appendLine("기록 없음(시작 점수로 계산).") else facts.forEach(::appendLine)
            }
        }

    private fun signed(value: Int) = if (value >= 0) "+$value" else value.toString()

    private companion object {
        const val PERCENT = 100

        /** 그 자리 판수가 전체의 5 분의 1 에 못 미치면 익숙하지 않은 자리로 본다. */
        const val UNFAMILIAR_RATIO = 5

        /** 프로필 문서에서 해설에 쓸 줄. 전부 주면 30 명일 때 컨텍스트를 다 쓴다. */
        val PROFILE_LINES = listOf("전적:", "포지션:", "강점:", "약점:", "자주 하는 챔피언:")

        val COMMENTARY_SYSTEM =
            """
            너는 리그 오브 레전드 내전의 팀 편성을 해설하는 도우미다.

            아래 편성은 코드가 이미 계산해 확정한 것이다. 규칙:
            - 편성을 바꾸거나 다른 안을 제안하지 않는다.
            - 숫자는 주어진 것만 쓴다. 새 숫자를 만들거나 어림하지 않는다.
            - 프로필에 없는 사실(성격, 실력 평가 등)을 지어내지 않는다.

            아래 형식 그대로 쓴다. 모든 항목에 근거가 된 수치(Elo, 순위, 판수, 승률)를 괄호로 붙인다.
            수치를 댈 수 없는 말은 쓰지 않는다.

            **1팀** (평균 Elo, 기대 승률)
            - 장점: 그 팀의 "가장 강한 라인" 과, 그 팀 선수의 "강점" 줄에 있는 것만. 두세 가지.
            - 단점: 그 팀의 "가장 약한 라인", "익숙하지 않은 자리에 앉은 사람", 그 팀 선수의 "약점" 줄에 있는 것만. 두세 가지.
              강한 라인을 단점에 쓰거나 약한 라인을 장점에 쓰지 않는다. 익숙하지 않은 사람이 "없음" 이면 그 얘기는 하지 않는다.
            (팀마다 반복)

            **핵심 맞대결**
            - Elo 차가 가장 큰 라인 한두 개와, 차가 가장 작은 접전 라인 하나. 누가 유리한지와 차이.

            **총평**
            - 어느 팀이 얼마나 유리한지 한 문장. 기대 승률을 그대로 적고, 차이가 5%p 안쪽이면 "거의 비슷하다" 고 쓴다. 과장하지 않는다. 묶음이나 포지션 제한 때문에 생긴 불균형이 있으면 한 문장.

            한국어로, 군더더기 없이 쓴다. 표는 쓰지 않는다.
            """.trimIndent()
    }
}
