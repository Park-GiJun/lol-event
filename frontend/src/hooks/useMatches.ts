import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { SILENT_META } from '@/api/queryClient';
import { getMatch, getMatchPage, getMatchTimeline } from '@/api/match/matchApi';
import type { GameMode } from '@/types';

const PAGE_SIZE = 20;

/**
 * 경기 목록. 한 페이지씩 요약 필드만 받는다.
 *
 * 예전에는 /matches 로 전체 경기를 상세 필드까지 받았는데, 154경기에 3.6MB였다.
 * 목록이 실제로 그리는 건 챔피언·KDA·아이템뿐이라 /matches/page 로 옮겼다.
 */
export function useMatches(mode: GameMode = 'normal') {
	return useInfiniteQuery({
		queryKey: ['matches', mode],
		initialPageParam: 0,
		queryFn: ({ pageParam, signal }) => getMatchPage(mode, pageParam, PAGE_SIZE, { signal }),
		getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined)
	});
}

/** 경기 상세. 참가자 전체 필드가 필요한 상세 화면에서만 쓴다. */
export function useMatch(matchId: string) {
	return useQuery({
		queryKey: ['match', matchId],
		queryFn: ({ signal }) => getMatch(matchId, { signal }),
		enabled: !!matchId
	});
}

/**
 * 경기 타임라인. 골드 곡선·킬 좌표·오브젝트·교전 구간이 한 번에 온다.
 *
 * 부가 정보라 실패해도 화면을 막지 않고 알리지도 않는다. 타임라인이 없는 경기도
 * 404 가 아니라 `hasTimeline: false` 로 정상 응답이 오므로, 그건 에러가 아니라 빈 상태다.
 */
export function useMatchTimeline(matchId: string) {
	return useQuery({
		queryKey: ['match', matchId, 'timeline'],
		queryFn: ({ signal }) => getMatchTimeline(matchId, { signal }),
		enabled: !!matchId,
		retry: 1,
		meta: SILENT_META
	});
}
