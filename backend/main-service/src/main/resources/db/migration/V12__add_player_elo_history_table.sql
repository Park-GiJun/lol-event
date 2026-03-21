CREATE TABLE lol_event.player_elo_history
(
    id             BIGSERIAL     NOT NULL,
    riot_id        VARCHAR(100)  NOT NULL,
    match_id       VARCHAR(100)  NOT NULL,
    elo_before     NUMERIC(8, 2) NOT NULL,
    elo_after      NUMERIC(8, 2) NOT NULL,
    delta          NUMERIC(6, 2) NOT NULL,
    win            BOOLEAN       NOT NULL,
    game_creation  BIGINT        NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_player_elo_history PRIMARY KEY (id)
);

CREATE INDEX idx_elo_history_riot_id ON lol_event.player_elo_history (riot_id, game_creation DESC);
