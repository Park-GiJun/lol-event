import { useQuery } from '@tanstack/react-query';
import { getLaneLeaderboard } from '@/api/stats/rankingStatsApi';
import type { GameMode } from '@/types';

export function useLaneLeaderboard(lane: string, mode?: GameMode) {
	return useQuery({
		queryKey: ['lane-leaderboard', lane, mode],
		queryFn: ({ signal }) => getLaneLeaderboard(lane, mode, { signal }),
		enabled: lane !== 'ALL'
	});
}
