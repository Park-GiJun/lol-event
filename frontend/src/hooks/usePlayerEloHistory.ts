import { useQuery } from '@tanstack/react-query';
import { getEloHistory } from '@/api/rating/ratingApi';

const HISTORY_LIMIT = 20;

export function usePlayerEloHistory(riotId: string) {
	return useQuery({
		queryKey: ['players', riotId, 'elo-history'],
		queryFn: ({ signal }) => getEloHistory(riotId, HISTORY_LIMIT, { signal }),
		enabled: !!riotId
	});
}
