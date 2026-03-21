CREATE TABLE lol_event.player_elo
(
    id         BIGSERIAL    NOT NULL,
    riot_id    VARCHAR(100) NOT NULL,
    elo        NUMERIC(8, 2) NOT NULL DEFAULT 1500.00,
    games      INTEGER      NOT NULL DEFAULT 0,
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_player_elo PRIMARY KEY (id),
    CONSTRAINT uq_player_elo_riot_id UNIQUE (riot_id)
);
