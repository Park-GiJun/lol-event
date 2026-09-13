import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type {
  PlayerTimelineResult,
  TimelineChampionsResult,
  TimelineLaneResult,
  TimelineStatsResult,
} from '@/lib/types/stats';

/*
 * 타임라인(15분) 지표 훅. 전부 부가 정보라 실패해도 화면을 막지 않게 retry 를 한 번만 한다.
 */

export function usePlayerTimeline(riotId: string) {
  return useQuery({
    queryKey: ['players', riotId, 'timeline'],
    queryFn: () => api.get<PlayerTimelineResult>(`/stats/player/${encodeURIComponent(riotId)}/timeline`),
    enabled: !!riotId,
    retry: 1,
  });
}

export function useTimelineStats(mode: string) {
  return useQuery({
    queryKey: ['timeline-stats', mode],
    queryFn: () => api.get<TimelineStatsResult>(`/stats/timeline?mode=${mode}`),
    retry: 1,
  });
}

export function useTimelineLane(lane: string, mode: string) {
  return useQuery({
    queryKey: ['timeline-lane', lane, mode],
    queryFn: () => api.get<TimelineLaneResult>(`/stats/timeline/lane?lane=${lane}&mode=${mode}`),
    enabled: !!lane,
    retry: 1,
  });
}

/** champion 을 주면 그 챔피언만 받는다. */
export function useTimelineChampions(mode: string, champion?: string) {
  const query = champion ? `&champion=${encodeURIComponent(champion)}` : '';
  return useQuery({
    queryKey: ['timeline-champions', mode, champion ?? null],
    queryFn: () => api.get<TimelineChampionsResult>(`/stats/timeline/champions?mode=${mode}${query}`),
    retry: 1,
  });
}
