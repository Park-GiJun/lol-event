-- RAG 문서 임베딩을 담을 pgvector 확장.
--
-- Postgres 이미지가 pgvector/pgvector 여야 한다 (운영은 pg16 + pgvector 0.8.7).
-- 확장이 없는 이미지에서는 이 마이그레이션이 실패하고 서비스가 뜨지 않는다 — 로컬 DB 도 같은 이미지로 맞춘다.
--
-- 스키마를 public 으로 못박는다. Flyway 의 기본 스키마는 lol_event 라, 적지 않으면 타입이 거기에 생기고
-- search_path 에 lol_event 가 없는 접속(psql 등)에서 `vector` 를 못 찾는다.
-- 쓰는 쪽은 `public.vector(1024)` 로 적는다 — 차원은 bge-m3 의 1024 다(`rag.embedding.dimensions`).
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;
