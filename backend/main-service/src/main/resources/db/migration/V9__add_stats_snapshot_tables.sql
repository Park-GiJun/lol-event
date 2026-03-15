-- 플레이어별 통계 스냅샷 (배치 집계 결과 저장)
CREATE TABLE IF NOT EXISTS lol_event.player_stats_snapshot
(
    id             BIGSERIAL PRIMARY KEY,
    riot_id        VARCHAR(100) NOT NULL,
    mode           VARCHAR(20)  NOT NULL, -- normal / aram / all
    games          INT          NOT NULL DEFAULT 0,
    wins           INT          NOT NULL DEFAULT 0,
    losses         INT          NOT NULL DEFAULT 0,
    win_rate       INT          NOT NULL DEFAULT 0,
    avg_kills      NUMERIC(6,2) NOT NULL DEFAULT 0,
    avg_deaths     NUMERIC(6,2) NOT NULL DEFAULT 0,
    avg_assists    NUMERIC(6,2) NOT NULL DEFAULT 0,
    kda            NUMERIC(6,2) NOT NULL DEFAULT 0,
    avg_damage     INT          NOT NULL DEFAULT 0,
    avg_cs         NUMERIC(6,2) NOT NULL DEFAULT 0,
    top_champion   VARCHAR(100),
    aggregated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uq_player_stats_snapshot UNIQUE (riot_id, mode)
);

-- 챔피언별 통계 스냅샷 (배치 집계 결과 저장)
CREATE TABLE IF NOT EXISTS lol_event.champion_stats_snapshot
(
    id             BIGSERIAL PRIMARY KEY,
    champion       VARCHAR(100) NOT NULL,
    champion_id    INT          NOT NULL DEFAULT 0,
    mode           VARCHAR(20)  NOT NULL,
    games          INT          NOT NULL DEFAULT 0,
    wins           INT          NOT NULL DEFAULT 0,
    win_rate       INT          NOT NULL DEFAULT 0,
    total_bans     INT          NOT NULL DEFAULT 0,
    aggregated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uq_champion_stats_snapshot UNIQUE (champion, mode)
);
