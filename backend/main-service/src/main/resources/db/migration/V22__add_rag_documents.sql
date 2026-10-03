-- RAG 검색 대상 글과 그 임베딩.
--
-- (doc_type, source_key) 가 신원이다. 배치가 같은 글을 다시 만들면 덮어쓴다(upsert).
-- embedding 의 차원은 임베딩 모델(bge-m3)의 1024 다. 모델을 바꾸면 이 컬럼의 차원도 바꾸고
-- 전부 다시 임베딩해야 한다 — 다른 모델의 벡터끼리는 거리가 뜻이 없다.
CREATE TABLE lol_event.rag_documents (
    id         BIGSERIAL PRIMARY KEY,
    doc_type   VARCHAR(32)         NOT NULL,
    source_key VARCHAR(128)        NOT NULL,
    content    TEXT                NOT NULL,
    embedding  public.vector(1024) NOT NULL,
    updated_at TIMESTAMPTZ         NOT NULL DEFAULT now(),
    CONSTRAINT uq_rag_documents_identity UNIQUE (doc_type, source_key)
);

-- 벡터 인덱스(HNSW)는 만들지 않는다. 글이 멤버 수 + 경기 수 규모(수백 건)라 전수 비교가 더 빠르고
-- 정확하다. 수만 건을 넘기면 그때 추가한다:
--   CREATE INDEX ON lol_event.rag_documents USING hnsw (embedding public.vector_cosine_ops);
