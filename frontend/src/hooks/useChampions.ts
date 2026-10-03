import { useQuery } from '@tanstack/react-query';
import { getChampionTier } from '@/api/stats/championStatsApi';
import type { GameMode } from '@/types';

/**
 * 챔피언 목록은 티어표를 그대로 쓴다.
 * 표본 미달 챔피언은 목록에 남되 서버 정렬에서 이미 뒤로 밀려 있다.
 */
export function useChampionTier(mode: GameMode = 'all', minGames = 5) {
	return useQuery({
		queryKey: ['champion-tier', mode, minGames],
		queryFn: ({ signal }) => getChampionTier(mode, minGames, { signal })
	});
}
