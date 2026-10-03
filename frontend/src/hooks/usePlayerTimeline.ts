import { useQuery } from '@tanstack/react-query';
import { SILENT_META } from '@/api/queryClient';
import {
	getPlayerTimeline,
	getTimelineChampions,
	getTimelineLane,
	getTimelineStats
} from '@/api/stats/timelineStatsApi';
import type { GameMode } from '@/types';

/*
 * 타임라인(15분) 지표 훅. 전부 부가 정보라 실패해도 화면을 막지 않고 알리지도 않는다
 * — 못 받으면 그 열만 비운다.
 */

export function usePlayerTimeline(riotId: string) {
	return useQuery({
		queryKey: ['players', riotId, 'timeline'],
		queryFn: ({ signal }) => getPlayerTimeline(riotId, { signal }),
		enabled: !!riotId,
		retry: 1,
		meta: SILENT_META
	});
}

export function useTimelineStats(mode: GameMode) {
	return useQuery({
		queryKey: ['timeline-stats', mode],
		queryFn: ({ signal }) => getTimelineStats(mode, { signal }),
		retry: 1,
		meta: SILENT_META
	});
}

export function useTimelineLane(lane: string, mode: GameMode) {
	return useQuery({
		queryKey: ['timeline-lane', lane, mode],
		queryFn: ({ signal }) => getTimelineLane(lane, mode, { signal }),
		enabled: !!lane,
		retry: 1,
		meta: SILENT_META
	});
}

/** champion 을 주면 그 챔피언만 받는다. */
export function useTimelineChampions(mode: GameMode, champion?: string) {
	return useQuery({
		queryKey: ['timeline-champions', mode, champion ?? null],
		queryFn: ({ signal }) => getTimelineChampions(mode, champion, { signal }),
		retry: 1,
		meta: SILENT_META
	});
}
