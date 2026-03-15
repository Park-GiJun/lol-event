CREATE TABLE IF NOT EXISTS lol_event.match_team_bans (
    id            BIGSERIAL PRIMARY KEY,
    team_db_id    BIGINT      NOT NULL REFERENCES lol_event.match_teams(id),
    champion_id   INTEGER     NOT NULL,
    champion_name VARCHAR(50) NOT NULL,
    pick_turn     INTEGER     NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_match_team_bans_team_db_id ON lol_event.match_team_bans(team_db_id);
