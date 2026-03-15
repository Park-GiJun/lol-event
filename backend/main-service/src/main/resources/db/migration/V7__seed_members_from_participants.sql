-- 기존 match_participants 데이터에서 고유 참가자를 members 테이블에 등록
-- puuid 기준으로 중복 제거, ON CONFLICT로 이미 등록된 멤버는 스킵
INSERT INTO lol_event.members (riot_id, puuid, registered_at)
SELECT DISTINCT ON (puuid)
    riot_id,
    puuid,
    NOW()
FROM lol_event.match_participants
WHERE puuid IS NOT NULL
  AND puuid != ''
ON CONFLICT (puuid) DO NOTHING;
