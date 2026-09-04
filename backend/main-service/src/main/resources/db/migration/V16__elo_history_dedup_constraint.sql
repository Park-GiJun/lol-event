-- Elo 정합성 보정.
--   1) riot_id 가 빈 문자열인 유령 레코드 제거. 참가자 riotId 를 거르지 않아 쌓인 것들이다.
--   2) 같은 (riot_id, match_id) 중복 제거 후 유니크 제약. Kafka 중복 배달로 한 경기가
--      두 번 반영되는 것을 DB 차원에서 막는다.
-- 제약을 건 뒤 POST /api/admin/elo/reset 으로 전체 재집계해야 점수 자체가 새 규칙으로 맞춰진다.

DELETE FROM lol_event.player_elo_history WHERE btrim(riot_id) = '';
DELETE FROM lol_event.player_elo         WHERE btrim(riot_id) = '';

DELETE FROM lol_event.player_elo_history a
    USING lol_event.player_elo_history b
    WHERE a.riot_id = b.riot_id
      AND a.match_id = b.match_id
      AND a.id > b.id;

ALTER TABLE lol_event.player_elo_history
    DROP CONSTRAINT IF EXISTS uq_elo_history_match_player;

ALTER TABLE lol_event.player_elo_history
    ADD CONSTRAINT uq_elo_history_match_player UNIQUE (riot_id, match_id);

CREATE INDEX IF NOT EXISTS idx_elo_history_match_id
    ON lol_event.player_elo_history (match_id);

-- 같은 승리인데 왜 변동폭이 다른지 설명할 근거. 같은 포지션 상대와 비교한 라인전 점수(0~1, 0.5=호각).
ALTER TABLE lol_event.player_elo_history
    ADD COLUMN IF NOT EXISTS lane_performance NUMERIC(4, 3) NOT NULL DEFAULT 0.5;
