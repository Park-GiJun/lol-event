-- 챔피언별 아이템 통계 스냅샷 (배치 집계 결과 저장)
CREATE TABLE IF NOT EXISTS lol_event.champion_item_stats_snapshot
(
    id            BIGSERIAL PRIMARY KEY,
    champion      VARCHAR(100) NOT NULL,
    mode          VARCHAR(20)  NOT NULL,
    item_id       INT          NOT NULL,
    picks         INT          NOT NULL DEFAULT 0,
    wins          INT          NOT NULL DEFAULT 0,
    win_rate      INT          NOT NULL DEFAULT 0,
    aggregated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_champion_item_stats_snapshot UNIQUE (champion, mode, item_id)
);
