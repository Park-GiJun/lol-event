-- 통계 조회 경로에 비어 있던 인덱스를 채운다.
--
-- 지금까지 match_participants 에는 match_db_id 와 puuid 인덱스만 있었다.
-- 그런데 통계는 거의 전부 riot_id 로 사람을 묶고 champion 으로 챔피언을 묶는다.
-- puuid 는 수집 경로에서만 쓰이고 조회 쪽에서는 riot_id 가 키다.

-- 플레이어 단위 조회·집계. 선수 상세, 라이벌, 듀오, Elo 백필이 전부 이 컬럼으로 묶는다.
CREATE INDEX IF NOT EXISTS idx_match_participants_riot_id
    ON lol_event.match_participants (riot_id);

-- 챔피언 단위 조회·집계. 장인 랭킹, 티어표, 상성, 아이템·룬 스냅샷 집계가 쓴다.
CREATE INDEX IF NOT EXISTS idx_match_participants_champion
    ON lol_event.match_participants (champion);

-- 경기 목록·페이징은 항상 "queue_id IN (...) ORDER BY game_creation DESC" 형태다.
-- queue_id 단독 인덱스로는 정렬을 못 받쳐서 매번 sort 가 붙었다.
CREATE INDEX IF NOT EXISTS idx_matches_queue_id_game_creation
    ON lol_event.matches (queue_id, game_creation DESC);

-- 위 복합 인덱스가 queue_id 를 선두 컬럼으로 갖고 있어 단독 인덱스는 완전히 중복이다.
-- 남겨두면 쓰이지도 않으면서 INSERT 마다 갱신 비용만 든다.
DROP INDEX IF EXISTS lol_event.idx_matches_queue_id;
