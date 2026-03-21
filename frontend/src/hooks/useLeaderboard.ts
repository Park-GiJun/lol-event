import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { EloLeaderboardResult } from '@/lib/types/stats';

export function useLeaderboard() {
  return useQuery({
    queryKey: ['leaderboard'],
    queryFn: () => api.get<EloLeaderboardResult>('/stats/elo'),
  });
}
