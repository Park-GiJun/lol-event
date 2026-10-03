package com.gijun.main.application.handler

import com.gijun.main.application.dto.command.DeleteRagDocumentCommand
import com.gijun.main.application.dto.command.IndexRagDocumentCommand
import com.gijun.main.application.dto.query.SearchRagDocumentsQuery
import com.gijun.main.application.port.out.external.TextEmbeddingPort
import com.gijun.main.application.port.out.persistence.RagDocumentCommandPersistencePort
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import com.gijun.main.domain.rag.enums.RagDocumentType
import com.gijun.main.domain.rag.exception.EmptyRagTextException
import com.gijun.main.domain.rag.model.RagDocumentModel
import com.gijun.main.domain.rag.model.RagSearchHitModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RagDocumentHandlerTest {
    private val embedding = FakeEmbedding()
    private val store = FakeStore()
    private val commandHandler = RagDocumentCommandHandler(embedding, store, store)
    private val queryHandler = RagDocumentQueryHandler(embedding, store)

    @Test
    fun `글을 임베딩해 저장한다`() {
        val result = commandHandler.indexRagDocument(profile("Faker#KR1", "  라인전이 강하다  "))

        assertTrue(result.embedded)
        // 앞뒤 공백은 뜻이 없다. 그대로 두면 공백만 다른 글을 다른 글로 보고 다시 임베딩한다.
        assertEquals("라인전이 강하다", store.contentOf("Faker#KR1"))
        assertEquals(listOf("라인전이 강하다"), embedding.asked)
    }

    @Test
    fun `저장된 글과 같으면 임베딩을 건너뛴다`() {
        commandHandler.indexRagDocument(profile("Faker#KR1", "라인전이 강하다"))

        val again = commandHandler.indexRagDocument(profile("Faker#KR1", "라인전이 강하다"))

        assertFalse(again.embedded)
        assertEquals(1, embedding.asked.size)
    }

    @Test
    fun `글이 바뀌면 다시 임베딩해 덮어쓴다`() {
        commandHandler.indexRagDocument(profile("Faker#KR1", "라인전이 강하다"))

        val changed = commandHandler.indexRagDocument(profile("Faker#KR1", "한타가 강하다"))

        assertTrue(changed.embedded)
        assertEquals("한타가 강하다", store.contentOf("Faker#KR1"))
        assertEquals(1, store.size)
    }

    @Test
    fun `빈 글은 임베딩 서버에 보내지 않고 거절한다`() {
        assertThrows(EmptyRagTextException::class.java) { commandHandler.indexRagDocument(profile("Faker#KR1", "   ")) }
        assertThrows(EmptyRagTextException::class.java) { queryHandler.searchRagDocuments(SearchRagDocumentsQuery(" ")) }
        assertTrue(embedding.asked.isEmpty())
    }

    @Test
    fun `지우면 검색에서 빠진다`() {
        commandHandler.indexRagDocument(profile("Faker#KR1", "라인전이 강하다"))

        commandHandler.deleteRagDocument(DeleteRagDocumentCommand(RagDocumentType.PLAYER_PROFILE, "Faker#KR1"))

        assertEquals(0, store.size)
    }

    @Test
    fun `검색은 질문을 임베딩해 종류와 건수를 그대로 넘긴다`() {
        commandHandler.indexRagDocument(profile("Faker#KR1", "라인전이 강하다"))

        val hits = queryHandler.searchRagDocuments(SearchRagDocumentsQuery("라인전 잘하는 사람", 3, RagDocumentType.PLAYER_PROFILE))

        assertEquals(listOf("Faker#KR1"), hits.map { it.sourceKey })
        assertEquals(3, store.lastLimit)
        assertEquals(RagDocumentType.PLAYER_PROFILE, store.lastDocType)
    }

    @Test
    fun `건수는 범위 안으로 당긴다`() {
        // 모델이 tool 인자로 0 이나 1000 을 넣는 일이 실제로 있다.
        queryHandler.searchRagDocuments(SearchRagDocumentsQuery("x", limit = 1_000))
        assertEquals(SearchRagDocumentsQuery.MAX_LIMIT, store.lastLimit)

        queryHandler.searchRagDocuments(SearchRagDocumentsQuery("x", limit = 0))
        assertEquals(1, store.lastLimit)
    }

    private fun profile(
        riotId: String,
        content: String,
    ) = IndexRagDocumentCommand(RagDocumentType.PLAYER_PROFILE, riotId, content)

    private class FakeEmbedding : TextEmbeddingPort {
        val asked = mutableListOf<String>()

        override fun embed(text: String): List<Float> {
            asked += text
            return listOf(text.length.toFloat())
        }
    }

    private class FakeStore :
        RagDocumentCommandPersistencePort,
        RagDocumentQueryPersistencePort {
        private val rows = LinkedHashMap<Pair<RagDocumentType, String>, RagDocumentModel>()
        var lastLimit: Int? = null
        var lastDocType: RagDocumentType? = null

        val size get() = rows.size

        fun contentOf(sourceKey: String) = rows.values.single { it.sourceKey == sourceKey }.content

        override fun upsert(
            document: RagDocumentModel,
            embedding: List<Float>,
        ) {
            rows[document.docType to document.sourceKey] = document
        }

        override fun delete(
            docType: RagDocumentType,
            sourceKey: String,
        ): Int = if (rows.remove(docType to sourceKey) != null) 1 else 0

        override fun findNearest(
            embedding: List<Float>,
            limit: Int,
            docType: RagDocumentType?,
        ): List<RagSearchHitModel> {
            lastLimit = limit
            lastDocType = docType
            return rows.values.take(limit).map { RagSearchHitModel(it, 0.1, Instant.EPOCH) }
        }

        override fun findSourceKeys(docType: RagDocumentType): List<String> = rows.keys.filter { it.first == docType }.map { it.second }

        override fun countByType(): Map<RagDocumentType, Int> = rows.keys.groupingBy { it.first }.eachCount()

        override fun findContent(
            docType: RagDocumentType,
            sourceKey: String,
        ): String? = rows[docType to sourceKey]?.content
    }
}
