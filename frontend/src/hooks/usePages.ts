import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { ChampionPageResult, HomeResult, SummonerProfileResult } from '@/lib/types/page';

/**
 * 화면 단위 집계 훅.
 *
 * 예전에는 홈이 6번, 소환사 화면이 5번 왕복했고 그중 듀오·라이벌은 전체 조합(각 57KB)을
 * 받아 화면에서 한 명 것만 골라 썼다. 이제 서버가 걸러서 한 번에 준다.
 */

export function useHome(mode: string = 'all') {
  return useQuery({
    queryKey: ['home', mode],
    queryFn: () => api.get<HomeResult>(`/home?mode=${mode}`),
  });
}

export function useSummoner(riotId: string, mode: string = 'all') {
  return useQuery({
    queryKey: ['summoner', riotId, mode],
    queryFn: () =>
      api.get<SummonerProfileResult>(`/summoner/${encodeURIComponent(riotId)}?mode=${mode}`),
    enabled: !!riotId,
  });
}

export function useChampionPage(champion: string, mode: string = 'all') {
  return useQuery({
    queryKey: ['champion-page', champion, mode],
    queryFn: () =>
      api.get<ChampionPageResult>(`/champions/${encodeURIComponent(champion)}?mode=${mode}`),
    enabled: !!champion,
  });
}
