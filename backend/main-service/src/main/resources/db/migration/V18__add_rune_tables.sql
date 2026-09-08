-- 룬 통계.
--
-- match_participants 는 perk0~perk5(실제 룬)와 perk_primary_style/perk_sub_style(계열)을
-- 처음부터 담고 있었는데 통계 쪽에서 한 번도 읽지 않았다. 아이템 통계는 스냅샷 배치까지
-- 돌리면서 룬은 통째로 비어 있었다.
--
-- 화면에 룬 이름·아이콘을 띄우려면 DataDragon 의 runesReforged 도 같이 받아둬야 한다.
-- 챔피언·아이템·스펠과 같은 방식으로 정적 테이블 하나를 더 둔다.

CREATE TABLE IF NOT EXISTS lol_event.ddragon_runes
(
    id             BIGSERIAL PRIMARY KEY,
    rune_id        INTEGER      NOT NULL UNIQUE,
    rune_key       VARCHAR(60)  NOT NULL,
    name_ko        VARCHAR(100) NOT NULL,
    description    TEXT,
    icon_path      VARCHAR(200),
    image_url      VARCHAR(300),
    -- 소속 계열(정밀 8000, 지배 8100, 마법 8200, 결의 8400, 영감 8300).
    -- 계열 자체도 같은 테이블에 넣는다. 그 행은 rune_id = style_id 이고 slot = -1 이다.
    style_id       INTEGER      NOT NULL DEFAULT 0,
    style_name_ko  VARCHAR(100),
    -- 계열 안에서의 줄 번호. 0번 줄이 핵심 룬(키스톤)이다. 계열 행은 -1.
    slot           INTEGER      NOT NULL DEFAULT 0,
    version        VARCHAR(20),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ddragon_runes_style_id
    ON lol_event.ddragon_runes (style_id);

-- 챔피언별 룬 통계 스냅샷 (배치 집계 결과 저장).
-- 키스톤만이 아니라 (핵심 룬, 주 계열, 보조 계열) 조합 단위로 센다.
-- 같은 키스톤이라도 보조 계열이 다르면 다른 빌드라 승률이 갈린다.
CREATE TABLE IF NOT EXISTS lol_event.champion_rune_stats_snapshot
(
    id            BIGSERIAL PRIMARY KEY,
    champion      VARCHAR(100) NOT NULL,
    mode          VARCHAR(20)  NOT NULL,
    keystone      INTEGER      NOT NULL,
    primary_style INTEGER      NOT NULL,
    sub_style     INTEGER      NOT NULL,
    picks         INT          NOT NULL DEFAULT 0,
    wins          INT          NOT NULL DEFAULT 0,
    win_rate      INT          NOT NULL DEFAULT 0,
    aggregated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_champion_rune_stats_snapshot
        UNIQUE (champion, mode, keystone, primary_style, sub_style)
);

CREATE INDEX IF NOT EXISTS idx_champion_rune_stats_champion_mode
    ON lol_event.champion_rune_stats_snapshot (champion, mode);
