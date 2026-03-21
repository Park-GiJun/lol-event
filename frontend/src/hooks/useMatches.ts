import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { Match } from '@/lib/types/match';

export function useMatches() {
  return useQuery({
    queryKey: ['matches'],
    queryFn: () => api.get<Match[]>('/matches'),
  });
}
