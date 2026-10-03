package com.gijun.main.infrastructure.adapter.out.persistence.rag

import com.gijun.main.application.port.out.persistence.RagDocumentCommandPersistencePort
import com.gijun.main.application.port.out.persistence.RagDocumentQueryPersistencePort
import com.gijun.main.domain.rag.enums.RagDocumentType
import com.gijun.main.domain.rag.model.RagDocumentModel
import com.gijun.main.domain.rag.model.RagSearchHitModel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

/*
 * 이 테이블만 JPA 가 아니라 JdbcTemplate 으로 다룬다. Hibernate 가 `vector` 타입을 모르고, 여기서
 * 하는 일은 upsert 와 거리순 조회 두 가지라 엔티티가 주는 것이 없다.
 *
 * 타입과 연산자에 `public.` 을 붙여 적는다. 확장은 public 스키마에 있고(V21), 접속의 search_path 에
 * public 이 없으면 `vector` 도 `<=>` 도 못 찾는다.
 */

@Component
class RagDocumentCommandPersistenceAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : RagDocumentCommandPersistencePort {
    override fun upsert(
        document: RagDocumentModel,
        embedding: List<Float>,
    ) {
        jdbcTemplate.update(
            UPSERT,
            document.docType.name,
            document.sourceKey,
            document.content,
            PgVector.literal(embedding),
        )
    }

    override fun delete(
        docType: RagDocumentType,
        sourceKey: String,
    ): Int = jdbcTemplate.update(DELETE, docType.name, sourceKey)

    private companion object {
        const val UPSERT = """
            INSERT INTO lol_event.rag_documents (doc_type, source_key, content, embedding, updated_at)
            VALUES (?, ?, ?, ?::public.vector, now())
            ON CONFLICT (doc_type, source_key) DO UPDATE
               SET content = EXCLUDED.content,
                   embedding = EXCLUDED.embedding,
                   updated_at = now()
        """

        const val DELETE = "DELETE FROM lol_event.rag_documents WHERE doc_type = ? AND source_key = ?"
    }
}

@Component
class RagDocumentQueryPersistenceAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : RagDocumentQueryPersistencePort {
    override fun findNearest(
        embedding: List<Float>,
        limit: Int,
        docType: RagDocumentType?,
    ): List<RagSearchHitModel> {
        val vector = PgVector.literal(embedding)
        return if (docType == null) {
            jdbcTemplate.query(NEAREST, ::toHit, vector, limit)
        } else {
            jdbcTemplate.query(NEAREST_IN_TYPE, ::toHit, vector, docType.name, limit)
        }
    }

    override fun findContent(
        docType: RagDocumentType,
        sourceKey: String,
    ): String? =
        jdbcTemplate
            .query(CONTENT, { rs, _ -> rs.getString("content") }, docType.name, sourceKey)
            .firstOrNull()

    private fun toHit(
        rs: ResultSet,
        @Suppress("UNUSED_PARAMETER") rowNum: Int,
    ): RagSearchHitModel =
        RagSearchHitModel(
            document =
                RagDocumentModel(
                    docType = RagDocumentType.valueOf(rs.getString("doc_type")),
                    sourceKey = rs.getString("source_key"),
                    content = rs.getString("content"),
                ),
            distance = rs.getDouble("distance"),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
        )

    private companion object {
        // 거리는 별칭으로 정렬한다. 식을 두 번 쓰면 벡터 파라미터도 두 번 넘겨야 한다.
        const val NEAREST = """
            SELECT doc_type, source_key, content, updated_at,
                   embedding OPERATOR(public.<=>) ?::public.vector AS distance
              FROM lol_event.rag_documents
             ORDER BY distance
             LIMIT ?
        """

        const val NEAREST_IN_TYPE = """
            SELECT doc_type, source_key, content, updated_at,
                   embedding OPERATOR(public.<=>) ?::public.vector AS distance
              FROM lol_event.rag_documents
             WHERE doc_type = ?
             ORDER BY distance
             LIMIT ?
        """

        const val CONTENT = "SELECT content FROM lol_event.rag_documents WHERE doc_type = ? AND source_key = ?"
    }
}
