-- 실력 레이팅(laneElo)과 전적 레이팅(teamElo)을 분리한다.
--
-- 왜 나누나: 이 내전은 매 세션 한 사람이 포지션까지 보고 팀을 짠다. 편성자가 균형을 맞추는 만큼
-- 팀 승패에 남는 건 실력차가 아니라 편성자의 추정 오차다. 158경기 워크포워드 검증에서
-- 팀 승패로 갱신한 Elo 는 라인 결과 예측에서 무정보 기준선보다 나빴고(0.6941 vs 0.6931),
-- 라인 맞대결로 갱신한 Elo 가 팀 승패까지도 더 잘 맞혔다(세션 첫 경기 0.6560 vs 0.6841).
-- 그래서 두 값을 별도 컬럼으로 두고 하나로 합치지 않는다.
--
-- 적용 후 반드시 POST /api/admin/elo/reset 으로 전체 재집계해야 한다. 새 테이블은 비어 있다.

-- ────────────────────────────────────────────────────────────
-- 1. 레이팅 본체
-- ────────────────────────────────────────────────────────────

CREATE TABLE lol_event.player_rating
(
    id         BIGSERIAL     NOT NULL,
    riot_id    VARCHAR(100)  NOT NULL,
    -- 실력 레이팅. 같은 포지션 맞대결 결과로만 움직인다.
    lane_elo   NUMERIC(8, 2) NOT NULL DEFAULT 1500.00,
    lane_duels INTEGER       NOT NULL DEFAULT 0,
    lane_wins  INTEGER       NOT NULL DEFAULT 0,
    -- 전적 레이팅. 팀 승패로만 움직인다.
    team_elo   NUMERIC(8, 2) NOT NULL DEFAULT 1500.00,
    team_games INTEGER       NOT NULL DEFAULT 0,
    team_wins  INTEGER       NOT NULL DEFAULT 0,
    -- 연승/연패는 화면 표시 전용이다. 어떤 산식에도 들어가지 않는다.
    team_win_streak  INTEGER NOT NULL DEFAULT 0,
    team_loss_streak INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_player_rating PRIMARY KEY (id),
    CONSTRAINT uq_player_rating_riot_id UNIQUE (riot_id)
);

COMMENT ON COLUMN lol_event.player_rating.lane_elo IS
    '원값. 표시용 수축(1500 + (elo-1500) * n/(n+10))은 API 응답에서만 적용한다.';

-- ────────────────────────────────────────────────────────────
-- 2. 변동 내역
-- ────────────────────────────────────────────────────────────

CREATE TABLE lol_event.rating_history
(
    id            BIGSERIAL     NOT NULL,
    riot_id       VARCHAR(100)  NOT NULL,
    match_id      VARCHAR(100)  NOT NULL,
    lane_before   NUMERIC(8, 2) NOT NULL,
    lane_after    NUMERIC(8, 2) NOT NULL,
    -- WIN / LOSS / NONE. NONE 은 라인 맞대결이 성립하지 않은 경기다
    -- (칼바람이거나, 한 포지션에 두 명이 잡혀 상대를 특정할 수 없는 경기).
    lane_result   VARCHAR(8)    NOT NULL DEFAULT 'NONE',
    lane_opponent VARCHAR(100),
    team_before   NUMERIC(8, 2) NOT NULL,
    team_after    NUMERIC(8, 2) NOT NULL,
    win           BOOLEAN       NOT NULL,
    game_creation BIGINT        NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_rating_history PRIMARY KEY (id),
    -- Kafka 는 최소 한 번 배달이라 애플리케이션 체크만으로는 경쟁 조건이 남는다.
    CONSTRAINT uq_rating_history_match_player UNIQUE (riot_id, match_id)
);

CREATE INDEX idx_rating_history_riot_id ON lol_event.rating_history (riot_id, game_creation DESC);
CREATE INDEX idx_rating_history_match_id ON lol_event.rating_history (match_id);

-- ────────────────────────────────────────────────────────────
-- 3. 매치 쪽 추가분
-- ────────────────────────────────────────────────────────────

-- 그 경기의 라인 승자를 무엇으로 판정했는지. 검증용 메타데이터라 계산에는 쓰지 않는다.
-- 값: TIMELINE_15 | LEGACY_FINAL
ALTER TABLE lol_event.matches
    ADD COLUMN IF NOT EXISTS lane_method VARCHAR(16);

-- 타임라인 participantFrames 의 키(1~10). 이게 없으면 프레임과 사람을 이을 수 없다.
-- 타임라인 수집 이전에 저장된 경기는 0 으로 남고, 그 경기는 자동으로 LEGACY_FINAL 로 내려간다.
ALTER TABLE lol_event.match_participants
    ADD COLUMN IF NOT EXISTS participant_id INTEGER NOT NULL DEFAULT 0;

-- game-timelines 원본. 가공하지 않고 통째로 보관한다.
--
-- matches 에 컬럼으로 붙이지 않은 이유: 경기당 60KB 급이라 목록·통계 쿼리가 매번 끌고 오면
-- 그것만으로 응답이 무거워진다. Hibernate 의 지연 로딩 basic 은 바이트코드 인핸스먼트가
-- 켜져 있어야 실제로 지연되는데 기본값이 꺼져 있어서, 컬럼으로 두면 사실상 항상 즉시 로딩된다.
CREATE TABLE lol_event.match_timelines
(
    match_id   VARCHAR(50) NOT NULL,
    raw        JSONB       NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_match_timelines PRIMARY KEY (match_id),
    CONSTRAINT fk_match_timelines_match FOREIGN KEY (match_id)
        REFERENCES lol_event.matches (match_id) ON DELETE CASCADE
);

-- ────────────────────────────────────────────────────────────
-- 4. 옛 테이블
-- ────────────────────────────────────────────────────────────
-- player_elo / player_elo_history 는 더 이상 어떤 엔티티도 매핑하지 않는다.
-- 지우지 않고 남겨 둔다 — 옛 값은 없어진 산식으로만 나오는 숫자라 한 번 지우면 되살릴 수 없다.
-- 새 레이팅이 자리를 잡은 뒤 직접 DROP 하면 된다.

COMMENT ON TABLE lol_event.player_elo IS
    '폐기됨(V19). player_rating 이 대신한다. 되살릴 수 없는 옛 산식의 결과라 보존만 해 둔다.';
COMMENT ON TABLE lol_event.player_elo_history IS
    '폐기됨(V19). rating_history 가 대신한다.';
