import { useQuery } from '@tanstack/react-query';
import { getPlayerStats } from '@/api/stats/playerStatsApi';

export function usePlayerDetail(riotId: string) {
	return useQuery({
		queryKey: ['players', riotId, 'detail'],
		queryFn: ({ signal }) => getPlayerStats(riotId, 'all', { signal }),
		enabled: !!riotId
	});
}
