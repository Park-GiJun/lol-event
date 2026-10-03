import { useQuery } from '@tanstack/react-query';
import { getOverviewStats } from '@/api/stats/overviewStatsApi';

export function useBanRecommend() {
	return useQuery({
		queryKey: ['overview', undefined],
		queryFn: ({ signal }) => getOverviewStats(undefined, { signal })
	});
}
