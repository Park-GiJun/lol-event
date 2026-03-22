ALTER TABLE lol_event.player_stats_snapshot
    ADD COLUMN IF NOT EXISTS avg_gold         INT            NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS avg_vision_score NUMERIC(6, 2)  NOT NULL DEFAULT 0;
