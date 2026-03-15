-- 중복 데이터 정리: matches/participants/teams 전체 초기화 후 unique 제약 추가
-- 이후 lcu-service가 재수집하면 깨끗한 상태로 재삽입됨

TRUNCATE lol_event.match_participants, lol_event.match_teams, lol_event.matches RESTART IDENTITY;

-- 향후 중복 삽입 방지: 한 경기에 같은 플레이어(riot_id)는 한 번만
ALTER TABLE lol_event.match_participants
    ADD CONSTRAINT uq_match_participant UNIQUE (match_db_id, riot_id);
