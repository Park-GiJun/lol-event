-- 타임라인 파생 지표 스냅샷.
--
-- 타임라인 원본(match_timelines.raw)은 경기당 60KB 라 사람별·챔피언별 지표를 매 요청에
-- 계산할 수 없다. 기존 *_stats_snapshot 네 테이블과 같은 방식으로 배치가 미리 채워 둔다.
-- 읽기는 "스냅샷 우선, 없으면 원본 계산 fallback" 관용구를 그대로 따른다.
--
-- 좌표 지표(라인 점유·이탈·상대 진영 체류)는 프레임이 분당 1점이라는 한계를 안고 있다.
-- 그래서 비율과 함께 frames_sampled 를 반드시 같이 저장한다 — 분모 없는 비율은 화면에서
-- 오해를 부른다. 자세한 이유는 domain/service/PositionMetrics.kt 머리 주석에 있다.
--
-- 세션 스냅샷은 만들지 않는다. 세션 하나는 경기 수가 유계(실측 최대 열몇 판)라 요청 시
-- 파싱해도 원본이 1MB 수준이고, 만들면 "세션이 언제 바뀌나"를 관리하는 비용만 생긴다.

-- ──────────────────────────────────────────────────────────────────────────
-- 1. 사람별
-- ──────────────────────────────────────────────────────────────────────────

-- player_stats_snapshot 을 거울처럼 따른다. 키도 같은 (riot_id, mode) 다.
--
-- 비율 컬럼을 NUMERIC(5,1) 로 둔 것은 기존 win_rate INT 에서 의도적으로 벗어난 것이다.
-- 승률은 정수 퍼센트로 충분했지만 lane_share_rate 는 표본이 작을 때 분수 단위로 움직이고,
-- 응답 DTO 규약도 "비율 0~100, 소수 첫째 자리"다.
CREATE TABLE IF NOT EXISTS lol_event.player_timeline_stats_snapshot
(
    id                   BIGSERIAL PRIMARY KEY,
    riot_id              VARCHAR(100) NOT NULL,
    mode                 VARCHAR(20)  NOT NULL,
    -- 타임라인이 있는 경기 수. 아래 모든 수치의 모집단이다(전체 경기가 아니다).
    games                INT          NOT NULL DEFAULT 0,
    -- 그중 같은 자리 라인 상대가 있었던 경기 수. 격차 지표의 분모다.
    lane_games           INT          NOT NULL DEFAULT 0,
    -- 좌표가 있는 프레임 수(0분 제외). 좌표 비율 지표의 분모다.
    frames_sampled       INT          NOT NULL DEFAULT 0,
    avg_gold_diff15      NUMERIC(8, 1),
    avg_cs_diff15        NUMERIC(6, 1),
    avg_xp_diff15        NUMERIC(8, 1),
    avg_cs_at10          NUMERIC(6, 1),
    avg_solo_kills       NUMERIC(5, 1) NOT NULL DEFAULT 0,
    first_blood_rate     NUMERIC(5, 1) NOT NULL DEFAULT 0,
    lane_lead_rate       NUMERIC(5, 1),
    -- 좌표 파생. 정글·포지션 미상은 라인 지표가 NULL 이다 (0 이 아니다 —
    -- 0 을 넣으면 "라인을 안 선다"로 읽힌다).
    lane_share_rate      NUMERIC(5, 1),
    roam_rate            NUMERIC(5, 1),
    enemy_half_rate      NUMERIC(5, 1) NOT NULL DEFAULT 0,
    counter_jungle_rate  NUMERIC(5, 1) NOT NULL DEFAULT 0,
    -- 한타 파생. 킬 3개 이상인 교전만 센다.
    teamfights           INT           NOT NULL DEFAULT 0,
    teamfight_kills      INT           NOT NULL DEFAULT 0,
    teamfight_deaths     INT           NOT NULL DEFAULT 0,
    aggregated_at        TIMESTAMP     NOT NULL,
    CONSTRAINT uq_player_timeline_stats_snapshot UNIQUE (riot_id, mode)
);

-- 리더보드는 mode 하나로 전원을 읽는다: WHERE mode = ?
CREATE INDEX IF NOT EXISTS idx_player_timeline_stats_mode
    ON lol_event.player_timeline_stats_snapshot (mode);

-- ──────────────────────────────────────────────────────────────────────────
-- 2. 챔피언별
-- ──────────────────────────────────────────────────────────────────────────

-- champion_stats_snapshot 이 (champion, mode) 인 것과 달리 position 을 키에 넣는다.
-- 응답 DTO(TimelineChampionEntry)가 이미 포지션별로 쪼개 내려주고 있고, lane_share_rate 는
-- 라인을 모르면 의미가 없다.
--
-- **여기서 기존 패턴을 의도적으로 깬다: avg_* 가 아니라 sum_* + count_* 로 저장한다.**
-- 포지션별 행을 챔피언 총합으로 롤업할 때 평균은 가중치 없이 더할 수 없다. 합계와 개수로
-- 두면 읽기 계층에서 나눠 쓰면 되고, 센티널 position='' 총합 행을 만드는 짓을 피한다.
CREATE TABLE IF NOT EXISTS lol_event.champion_timeline_stats_snapshot
(
    id                  BIGSERIAL PRIMARY KEY,
    champion            VARCHAR(100) NOT NULL,
    mode                VARCHAR(20)  NOT NULL,
    -- Position enum 이름. UNKNOWN 은 포지션 추정이 실패한 참가자다.
    position            VARCHAR(20)  NOT NULL,
    games               INT          NOT NULL DEFAULT 0,
    wins                INT          NOT NULL DEFAULT 0,
    lane_games          INT          NOT NULL DEFAULT 0,
    frames_sampled      INT          NOT NULL DEFAULT 0,
    sum_gold_diff15     BIGINT       NOT NULL DEFAULT 0,
    sum_cs_diff15       BIGINT       NOT NULL DEFAULT 0,
    sum_xp_diff15       BIGINT       NOT NULL DEFAULT 0,
    sum_cs_at10         BIGINT       NOT NULL DEFAULT 0,
    count_cs_at10       INT          NOT NULL DEFAULT 0,
    sum_solo_kills      BIGINT       NOT NULL DEFAULT 0,
    lane_lead_games     INT          NOT NULL DEFAULT 0,
    -- 좌표 파생도 합계로. 분모는 lane_frames / frames_sampled 를 쓴다.
    sum_lane_frames     BIGINT       NOT NULL DEFAULT 0,
    lane_phase_frames   INT          NOT NULL DEFAULT 0,
    sum_enemy_half      BIGINT       NOT NULL DEFAULT 0,
    aggregated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT uq_champion_timeline_stats_snapshot UNIQUE (champion, mode, position)
);

-- 챔피언 화면은 (champion, mode) 로 포지션 행 몇 개만 집는다.
CREATE INDEX IF NOT EXISTS idx_champion_timeline_stats_champion_mode
    ON lol_event.champion_timeline_stats_snapshot (champion, mode);

-- ──────────────────────────────────────────────────────────────────────────
-- 3. 위치 히트맵
-- ──────────────────────────────────────────────────────────────────────────

-- 좌표를 그대로 쌓지 않고 **32×32 격자로 접어서** 담는다.
-- 500경기면 킬이 32,000건, 프레임 좌표는 200,000점이다. 원본 좌표를 행으로 쌓으면
-- 테이블이 그대로 커지고 화면은 어차피 격자로 뭉쳐 그린다.
-- 격자로 접으면 행 수가 (scope, kind, phase) 조합당 최대 1,024 로 상한이 잡힌다.
--
-- 셀 크기는 약 465 맵단위다(협곡 한 변 14,870 / 32). 표본이 17판인 지금 64 격자로 쪼개면
-- 거의 모든 셀이 1 카운트가 되어 의미가 없다. **200판을 넘기면 64 를 재검토할 것.**
--
-- phase='ALL' 은 저장하지 않는다. 세 구간을 더하면 나온다.
--
-- kind='PRESENCE'(프레임 체류)는 scope_type 이 POSITION / GLOBAL 일 때만 채운다.
-- 사람별·챔피언별 체류 히트맵은 분당 1점 한계상 이 계획에서 가장 신뢰도가 낮은데 저장
-- 비용은 가장 크다. 표본이 늘면 넓히면 된다 — 키 구조가 이미 허용한다.
CREATE TABLE IF NOT EXISTS lol_event.position_heatmap_snapshot
(
    id            BIGSERIAL PRIMARY KEY,
    mode          VARCHAR(20)  NOT NULL,
    -- PLAYER / CHAMPION / POSITION / GLOBAL
    scope_type    VARCHAR(20)  NOT NULL,
    -- riotId / 챔피언명 / Position 이름 / ''(GLOBAL)
    scope_key     VARCHAR(100) NOT NULL,
    -- DEATH / KILL / PRESENCE / OBJECTIVE
    kind          VARCHAR(20)  NOT NULL,
    -- EARLY(0~15분) / MID(15~25) / LATE(25+)
    phase         VARCHAR(10)  NOT NULL,
    grid_x        INT          NOT NULL,
    grid_y        INT          NOT NULL,
    count         INT          NOT NULL DEFAULT 0,
    aggregated_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_position_heatmap_snapshot
        UNIQUE (mode, scope_type, scope_key, kind, phase, grid_x, grid_y)
);

-- 히트맵 한 장이 이 다섯 값으로 정확히 집힌다:
--   WHERE mode = ? AND scope_type = ? AND scope_key = ? AND kind = ? AND phase = ?
-- grid_x / grid_y 는 선택도가 낮아 선두에 두지 않는다.
CREATE INDEX IF NOT EXISTS idx_position_heatmap_scope
    ON lol_event.position_heatmap_snapshot (mode, scope_type, scope_key, kind, phase);

COMMENT ON TABLE lol_event.position_heatmap_snapshot IS
    '좌표를 32x32 격자로 접은 히트맵. 표본이 200판을 넘으면 격자 해상도를 재검토한다.';
