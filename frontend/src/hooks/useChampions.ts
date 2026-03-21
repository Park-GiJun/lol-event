import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { OverviewStats } from '@/lib/types/stats';

export function useChampions() {
  return useQuery({
    queryKey: ['champions'],
    queryFn: () => api.get<OverviewStats>('/stats/overview'),
  });
}
