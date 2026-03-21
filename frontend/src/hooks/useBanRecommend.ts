import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { OverviewStats } from '@/lib/types/stats';

export function useBanRecommend() {
  return useQuery({
    queryKey: ['bans', 'recommend'],
    queryFn: () => api.get<OverviewStats>('/stats/overview'),
  });
}
