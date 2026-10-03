import { useQuery } from '@tanstack/react-query';
import { getStats } from '@/api/stats/playerStatsApi';
import type { GameMode } from '@/types';

export function usePlayers(mode: GameMode = 'normal') {
	return useQuery({
		queryKey: ['players', mode],
		queryFn: ({ signal }) => getStats(mode, { signal })
	});
}
