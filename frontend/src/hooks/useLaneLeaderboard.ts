import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { LaneLeaderboardResult } from '@/lib/types/stats';

export function useLaneLeaderboard(lane: string) {
  return useQuery({
    queryKey: ['lane-leaderboard', lane],
    queryFn: () => api.get<LaneLeaderboardResult>(`/stats/lane?lane=${lane}`),
    enabled: lane !== 'ALL',
  });
}
