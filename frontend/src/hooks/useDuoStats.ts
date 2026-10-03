import { useQuery } from '@tanstack/react-query';
import { getDuoStats } from '@/api/stats/teamStatsApi';
import type { GameMode } from '@/types';

export function useDuoStats(mode: GameMode = 'normal', minGames = 1) {
	return useQuery({
		queryKey: ['duo', mode, minGames],
		queryFn: ({ signal }) => getDuoStats(mode, minGames, { signal })
	});
}
