CREATE SCHEMA IF NOT EXISTS lol_event;

CREATE TABLE lol_event.members (
    id         BIGSERIAL PRIMARY KEY,
    riot_id    VARCHAR(100) NOT NULL,
    puuid      VARCHAR(100) NOT NULL UNIQUE,
    registered_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE lol_event.matches (
    id            BIGSERIAL PRIMARY KEY,
    match_id      VARCHAR(50)  NOT NULL UNIQUE,
    queue_id      INTEGER      NOT NULL,
    game_creation BIGINT       NOT NULL,
    game_duration INTEGER      NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE lol_event.match_participants (
    id           BIGSERIAL PRIMARY KEY,
    match_db_id  BIGINT       NOT NULL REFERENCES lol_event.matches(id),
    puuid        VARCHAR(100),
    riot_id      VARCHAR(100) NOT NULL,
    champion     VARCHAR(50)  NOT NULL,
    team         VARCHAR(10)  NOT NULL,
    win          BOOLEAN      NOT NULL,
    kills        INTEGER      NOT NULL DEFAULT 0,
    deaths       INTEGER      NOT NULL DEFAULT 0,
    assists      INTEGER      NOT NULL DEFAULT 0,
    damage       INTEGER      NOT NULL DEFAULT 0,
    cs           INTEGER      NOT NULL DEFAULT 0,
    gold         INTEGER      NOT NULL DEFAULT 0,
    vision_score INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_match_participants_match_db_id ON lol_event.match_participants(match_db_id);
CREATE INDEX idx_match_participants_puuid ON lol_event.match_participants(puuid);
CREATE INDEX idx_matches_queue_id ON lol_event.matches(queue_id);
