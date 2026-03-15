-- DataDragon 정적 데이터 테이블

CREATE TABLE lol_event.ddragon_champions (
    id           BIGSERIAL PRIMARY KEY,
    champion_id  INTEGER      NOT NULL UNIQUE,
    champion_key VARCHAR(60)  NOT NULL UNIQUE,
    name_ko      VARCHAR(100) NOT NULL,
    title_ko     VARCHAR(100),
    image_full   VARCHAR(100),
    image_url    VARCHAR(300),
    version      VARCHAR(20),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE lol_event.ddragon_items (
    id           BIGSERIAL PRIMARY KEY,
    item_id      INTEGER      NOT NULL UNIQUE,
    name_ko      VARCHAR(150) NOT NULL,
    description  TEXT,
    image_full   VARCHAR(100),
    image_url    VARCHAR(300),
    gold_total   INTEGER      NOT NULL DEFAULT 0,
    version      VARCHAR(20),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE lol_event.ddragon_summoner_spells (
    id          BIGSERIAL PRIMARY KEY,
    spell_id    INTEGER      NOT NULL UNIQUE,
    spell_key   VARCHAR(60)  NOT NULL UNIQUE,
    name_ko     VARCHAR(100) NOT NULL,
    description TEXT,
    image_full  VARCHAR(100),
    image_url   VARCHAR(300),
    version     VARCHAR(20),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
