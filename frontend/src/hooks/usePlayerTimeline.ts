import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { PlayerTimelineResult, TimelineStatsResult } from '@/lib/types/stats';

export function usePlayerTimeline(riotId: string) {
  return useQuery({
    queryKey: ['players', riotId, 'timeline'],
    queryFn: () => api.get<PlayerTimelineResult>(`/stats/player/${encodeURIComponent(riotId)}/timeline`),
    enabled: !!riotId,
  });
}

export function useTimelineStats(mode: string) {
  return useQuery({
    queryKey: ['timeline-stats', mode],
    queryFn: () => api.get<TimelineStatsResult>(`/stats/timeline?mode=${mode}`),
  });
}
