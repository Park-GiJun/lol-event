package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.DeleteRagDocumentCommand
import com.gijun.main.application.dto.command.IndexRagDocumentCommand
import com.gijun.main.application.dto.query.GetChampionPageQuery
import com.gijun.main.application.dto.query.GetSummonerProfileQuery
import com.gijun.main.application.dto.result.RagIndexSummaryResult
import com.gijun.main.application.dto.result.StartRagReindexResult
import com.gijun.main.application.port.`in`.DeleteRagDocumentUseCase
import com.gijun.main.application.port.`in`.GetChampionPageUseCase
import com.gijun.main.application.port.`in`.GetChampionSynergyUseCase
import com.gijun.main.application.port.`in`.GetDragonChampionsUseCase
import com.gijun.main.application.port.`in`.GetSummonerProfileUseCase
import com.gijun.main.application.port.`in`.IndexMatchRagDocumentsUseCase
import com.gijun.main.application.port.`in`.IndexRagDocumentUseCase
import com.gijun.main.application.port.`in`.StartRagReindexUseCase
import com.gijun.main.application.port.out.persistence.MatchQueryPersistencePort
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.domain.match.model.MatchModel
import com.gijun.main.domain.rag.enums.RagDocumentType
import com.gijun.main.shared.domain.vo.MatchId
import com.gijun.main.shared.domain.vo.RiotId
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

/**
 * 검색 문서를 다시 쓴다 — 경기 한 판이 들어왔을 때와, 손으로 전체를 돌릴 때.
 *
 * 문서는 세 종류다. 경기 리뷰, 플레이어 요약, 챔피언 요약. 글은 [RagDocumentWriter] 가 통계 유즈케이스의
 * 결과로 쓰고, 저장은 [IndexRagDocumentUseCase] 가 한다(글이 그대로면 임베딩을 건너뛴다).
 * 그래서 전체를 다시 돌려도 임베딩 서버에는 바뀐 문서만 간다.
 *
 * 칼바람 경기는 넣지 않는다. 통계 집계가 칼바람을 빼므로 문서도 같은 범위를 본다.
 */
@Service
class RagIndexCommandHandler(
    private val matchQueryPersistencePort: MatchQueryPersistencePort,
    private val ragDocumentQueryPersistencePort: RagDocumentQueryPersistencePort,
    private val getSummonerProfileUseCase: GetSummonerProfileUseCase,
    private val getChampionPageUseCase: GetChampionPageUseCase,
    private val getChampionSynergyUseCase: GetChampionSynergyUseCase,
    private val getDragonChampionsUseCase: GetDragonChampionsUseCase,
    private val indexRagDocumentUseCase: IndexRagDocumentUseCase,
    private val deleteRagDocumentUseCase: DeleteRagDocumentUseCase,
    private val progress: RagIndexProgress,
) : IndexMatchRagDocumentsUseCase,
    StartRagReindexUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    // 전체 색인은 몇 분 걸린다. 요청 스레드를 잡고 있지 않게 뒤에서 돈다. 한 번에 하나만.
    private val executor =
        Executors.newSingleThreadExecutor { task -> Thread(task, "rag-reindex").apply { isDaemon = true } }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    override fun indexMatchRagDocuments(matchId: MatchId): RagIndexSummaryResult {
        val match = matchQueryPersistencePort.findByMatchId(matchId) ?: return RagIndexSummaryResult(0, 0, 0)
        if (match.queueId !in SCOPE.queueIds) return RagIndexSummaryResult(0, 0, 0)

        val counter = Counter()
        writeAll(tasksFor(listOf(match), championNames()), counter::record, counter::fail)
        log.info("RAG 색인 — matchId={} 문서 {} 건 중 {} 건 임베딩, {} 건 실패", matchId.value, counter.total, counter.embedded, counter.failed)
        return RagIndexSummaryResult(counter.total, counter.embedded, counter.failed)
    }

    override fun startRagReindex(): StartRagReindexResult {
        if (!progress.tryStart()) return StartRagReindexResult(started = false)
        try {
            executor.execute(::reindexAll)
        } catch (e: RuntimeException) {
            // 맡기지 못했는데 "도는 중" 으로 남으면, 다시 뜰 때까지 색인을 시작할 수 없다.
            progress.finish(error = e.message ?: e.javaClass.simpleName)
            throw e
        }
        return StartRagReindexResult(started = true)
    }

    private fun reindexAll() {
        try {
            val matches = matchQueryPersistencePort.findAllWithParticipants(SCOPE.queueIds)
            val tasks = tasksFor(matches, championNames())
            progress.total(tasks.size)
            writeAll(tasks, { progress.recorded(it) }, { progress.failed(it) })
            removeOrphans(tasks)
            progress.finish()
            log.info("RAG 전체 색인 끝 — {}", progress.snapshot())
        } catch (e: Throwable) {
            // Error(메모리 부족 등)까지 잡는다. 어떻게 끝나든 "도는 중" 을 풀어야 다음 색인을 시작할 수 있다.
            log.error("RAG 전체 색인이 중단됐다", e)
            progress.finish(error = e.message ?: e.javaClass.simpleName)
            if (e is Error) throw e
        }
    }

    /** 문서마다 따로 실패한다. 한 사람의 통계가 깨져 있어도 나머지는 계속 쓴다. */
    private fun writeAll(
        tasks: List<Task>,
        onDone: (embedded: Boolean) -> Unit,
        onFailed: (reason: String) -> Unit,
    ) {
        var failuresInARow = 0
        tasks.forEach { task ->
            try {
                val result = indexRagDocumentUseCase.indexRagDocument(IndexRagDocumentCommand(task.docType, task.sourceKey, task.write()))
                onDone(result.embedded)
                failuresInARow = 0
            } catch (e: Exception) {
                log.warn("RAG 문서를 쓰지 못했다 — {} {}: {}", task.docType, task.sourceKey, e.message)
                onFailed("${task.docType} ${task.sourceKey}: ${e.message ?: e.javaClass.simpleName}")
                // 연달아 실패하면 임베딩 서버가 죽은 것이다. 남은 문서마다 타임아웃을 기다리면 Kafka 컨슈머가
                // 몇 분씩 묶여 파티션을 빼앗긴다. 그만두고, 놓친 문서는 다음 색인이 메운다.
                if (++failuresInARow >= MAX_FAILURES_IN_A_ROW) {
                    error("연달아 $MAX_FAILURES_IN_A_ROW 건 실패해 색인을 멈췄다. 마지막 오류: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    /** 이 경기들이 건드리는 문서 전부. 글은 쓸 차례가 왔을 때 만든다 — 통계 조회가 여기서 일어난다. */
    private fun tasksFor(
        matches: List<MatchModel>,
        names: ChampionNames,
    ): List<Task> {
        val reviews =
            matches.map { match -> Task(RagDocumentType.MATCH_REVIEW, match.matchId) { RagDocumentWriter.matchReview(match, names) } }

        val players =
            matches.flatMap { it.participants }.map { it.riotId }.filter { it.isNotBlank() }.distinct().map { riotId ->
                Task(RagDocumentType.PLAYER_PROFILE, riotId) {
                    RagDocumentWriter.playerProfile(
                        getSummonerProfileUseCase.getSummonerProfile(GetSummonerProfileQuery(RiotId(riotId), SCOPE)),
                        names,
                    )
                }
            }

        val champions =
            matches.flatMap { it.participants }.map { it.champion }.filter { it.isNotBlank() }.distinct().map { champion ->
                Task(RagDocumentType.CHAMPION_PROFILE, champion) {
                    RagDocumentWriter.championProfile(
                        getChampionPageUseCase.getChampionPage(GetChampionPageQuery(champion, SCOPE)),
                        getChampionSynergyUseCase.getChampionSynergy(champion),
                        names,
                    )
                }
            }
        return reviews + players + champions
    }

    /** 원본이 사라진 문서(지운 경기 등)를 뺀다. 전체를 돌 때만 알 수 있다. */
    private fun removeOrphans(tasks: List<Task>) {
        val alive = tasks.groupBy({ it.docType }, { it.sourceKey }).mapValues { it.value.toSet() }
        RagDocumentType.entries.forEach { docType ->
            val keep = alive[docType].orEmpty()
            ragDocumentQueryPersistencePort
                .findSourceKeys(docType)
                .filterNot { it in keep }
                .forEach { deleteRagDocumentUseCase.deleteRagDocument(DeleteRagDocumentCommand(docType, it)) }
        }
    }

    private fun championNames() = ChampionNames(getDragonChampionsUseCase.getDragonChampions().associate { it.championKey to it.nameKo })

    private class Task(
        val docType: RagDocumentType,
        val sourceKey: String,
        val write: () -> String,
    )

    private class Counter {
        var total = 0
        var embedded = 0
        var failed = 0

        fun record(wasEmbedded: Boolean) {
            total++
            if (wasEmbedded) embedded++
        }

        @Suppress("UNUSED_PARAMETER")
        fun fail(reason: String) {
            total++
            failed++
        }
    }

    private companion object {
        val SCOPE = GameMode.ALL
        const val MAX_FAILURES_IN_A_ROW = 3
    }
}
