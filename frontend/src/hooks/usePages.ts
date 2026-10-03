import { useQuery } from '@tanstack/react-query';
import { getChampionPage, getHome, getSummonerProfile } from '@/api/page/pageApi';
import type { GameMode } from '@/types';

/** 화면 단위 집계 훅. 한 화면이 한 번만 왕복한다 — 배경은 `@/api/page/pageApi` 참고. */

export function useHome(mode: GameMode = 'all') {
	return useQuery({
		queryKey: ['home', mode],
		queryFn: ({ signal }) => getHome(mode, { signal })
	});
}

export function useSummoner(riotId: string, mode: GameMode = 'all') {
	return useQuery({
		queryKey: ['summoner', riotId, mode],
		queryFn: ({ signal }) => getSummonerProfile(riotId, mode, { signal }),
		enabled: !!riotId
	});
}

export function useChampionPage(champion: string, mode: GameMode = 'all') {
	return useQuery({
		queryKey: ['champion-page', champion, mode],
		queryFn: ({ signal }) => getChampionPage(champion, mode, { signal }),
		enabled: !!champion
	});
}
