import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { Match, MatchPage } from '@/lib/types/match';

const PAGE_SIZE = 20;

/**
 * 경기 목록. 한 페이지씩 요약 필드만 받는다.
 *
 * 예전에는 /matches 로 전체 경기를 상세 필드까지 받았는데, 154경기에 3.6MB였다.
 * 목록이 실제로 그리는 건 챔피언·KDA·아이템뿐이라 /matches/page 로 옮겼다.
 */
export function useMatches(mode: string = 'normal') {
  return useInfiniteQuery({
    queryKey: ['matches', mode],
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      api.get<MatchPage>(`/matches/page?mode=${mode}&page=${pageParam}&size=${PAGE_SIZE}`),
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
  });
}

/** 경기 상세. 참가자 전체 필드가 필요한 상세 화면에서만 쓴다. */
export function useMatch(matchId: string) {
  return useQuery({
    queryKey: ['match', matchId],
    queryFn: () => api.get<Match>(`/matches/${encodeURIComponent(matchId)}`),
    enabled: !!matchId,
  });
}
