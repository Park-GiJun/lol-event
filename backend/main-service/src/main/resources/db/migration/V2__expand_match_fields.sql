-- matches 테이블 필드 추가
ALTER TABLE lol_event.matches
    ADD COLUMN IF NOT EXISTS game_mode     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS game_type     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS game_version  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS map_id        INTEGER,
    ADD COLUMN IF NOT EXISTS season_id     INTEGER,
    ADD COLUMN IF NOT EXISTS platform_id   VARCHAR(10);

-- match_participants 테이블 필드 추가
ALTER TABLE lol_event.match_participants
    -- 참가자 기본 정보
    ADD COLUMN IF NOT EXISTS champion_id                          INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS team_id                              INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS spell1_id                            INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS spell2_id                            INTEGER      NOT NULL DEFAULT 0,

    -- 멀티킬
    ADD COLUMN IF NOT EXISTS champ_level                          INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS double_kills                         INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS triple_kills                         INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quadra_kills                         INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS penta_kills                          INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS unreal_kills                         INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS killing_sprees                       INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS largest_killing_spree                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS largest_multi_kill                   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS largest_critical_strike              INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS longest_time_spent_living            INTEGER      NOT NULL DEFAULT 0,

    -- 퍼스트 블러드 / 타워 등
    ADD COLUMN IF NOT EXISTS first_blood_kill                     BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS first_blood_assist                   BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS first_tower_kill                     BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS first_tower_assist                   BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS first_inhibitor_kill                 BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS first_inhibitor_assist               BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS inhibitor_kills                      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS turret_kills                         INTEGER      NOT NULL DEFAULT 0,

    -- 와드
    ADD COLUMN IF NOT EXISTS wards_killed                         INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS wards_placed                         INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sight_wards_bought_in_game           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS vision_wards_bought_in_game          INTEGER      NOT NULL DEFAULT 0,

    -- 아이템
    ADD COLUMN IF NOT EXISTS item0                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS item1                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS item2                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS item3                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS item4                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS item5                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS item6                                INTEGER      NOT NULL DEFAULT 0,

    -- 룬
    ADD COLUMN IF NOT EXISTS perk0                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk0_var1                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk0_var2                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk0_var3                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk1                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk1_var1                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk1_var2                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk1_var3                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk2                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk2_var1                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk2_var2                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk2_var3                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk3                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk3_var1                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk3_var2                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk3_var3                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk4                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk4_var1                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk4_var2                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk4_var3                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk5                                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk5_var1                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk5_var2                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk5_var3                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk_primary_style                   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS perk_sub_style                       INTEGER      NOT NULL DEFAULT 0,

    -- 데미지
    ADD COLUMN IF NOT EXISTS magic_damage_dealt                   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS magic_damage_dealt_to_champions      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS magical_damage_taken                 INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS physical_damage_dealt                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS physical_damage_dealt_to_champions   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS physical_damage_taken                INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS true_damage_dealt                    INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS true_damage_dealt_to_champions       INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS true_damage_taken                    INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_damage_dealt                   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_damage_dealt_to_champions      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_damage_taken                   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS damage_dealt_to_objectives           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS damage_dealt_to_turrets              INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS damage_self_mitigated                INTEGER      NOT NULL DEFAULT 0,

    -- 힐 / CC
    ADD COLUMN IF NOT EXISTS total_heal                           INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_units_healed                   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS time_ccing_others                    INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_time_crowd_control_dealt       INTEGER      NOT NULL DEFAULT 0,

    -- 미니언
    ADD COLUMN IF NOT EXISTS neutral_minions_killed               INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS neutral_minions_killed_team_jungle   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS neutral_minions_killed_enemy_jungle  INTEGER      NOT NULL DEFAULT 0,

    -- 스코어
    ADD COLUMN IF NOT EXISTS combat_player_score                  INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS objective_player_score               INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_player_score                   INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_score_rank                     INTEGER      NOT NULL DEFAULT 0,

    -- 항복
    ADD COLUMN IF NOT EXISTS game_ended_in_surrender              BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS game_ended_in_early_surrender        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS caused_early_surrender               BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS early_surrender_accomplice           BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS team_early_surrendered               BOOLEAN      NOT NULL DEFAULT FALSE,

    -- 증강 (아레나/칼바람 특수 모드)
    ADD COLUMN IF NOT EXISTS player_augment1                      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS player_augment2                      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS player_augment3                      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS player_augment4                      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS player_augment5                      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS player_augment6                      INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS player_subteam_id                    INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS subteam_placement                    INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS role_bound_item                      INTEGER      NOT NULL DEFAULT 0,

    -- 포지션
    ADD COLUMN IF NOT EXISTS lane                                 VARCHAR(20),
    ADD COLUMN IF NOT EXISTS role                                 VARCHAR(20);

-- 팀 정보 테이블 생성
CREATE TABLE IF NOT EXISTS lol_event.match_teams (
    id                      BIGSERIAL PRIMARY KEY,
    match_db_id             BIGINT   NOT NULL REFERENCES lol_event.matches(id),
    team_id                 INTEGER  NOT NULL,
    win                     BOOLEAN  NOT NULL DEFAULT FALSE,
    baron_kills             INTEGER  NOT NULL DEFAULT 0,
    dragon_kills            INTEGER  NOT NULL DEFAULT 0,
    tower_kills             INTEGER  NOT NULL DEFAULT 0,
    inhibitor_kills         INTEGER  NOT NULL DEFAULT 0,
    rift_herald_kills       INTEGER  NOT NULL DEFAULT 0,
    horde_kills             INTEGER  NOT NULL DEFAULT 0,
    first_blood             BOOLEAN  NOT NULL DEFAULT FALSE,
    first_tower             BOOLEAN  NOT NULL DEFAULT FALSE,
    first_baron             BOOLEAN  NOT NULL DEFAULT FALSE,
    first_inhibitor         BOOLEAN  NOT NULL DEFAULT FALSE,
    first_dragon            BOOLEAN  NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_match_teams_match_db_id ON lol_event.match_teams(match_db_id);
