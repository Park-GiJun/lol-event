import { useQuery } from '@tanstack/react-query';
import { getSessionDetail, getSessionReport } from '@/api/stats/sessionApi';
import type { GameMode } from '@/types';

/*
 * 세션 훅. 세션은 오전 6시에 시작하는 하루다 — 새벽 경기는 전날 세션에 들어간다.
 */

export function useSessions(mode: GameMode = 'all') {
	return useQuery({
		queryKey: ['sessions', mode],
		queryFn: ({ signal }) => getSessionReport(mode, { signal })
	});
}

/** 세션 상세. 없는 날짜는 백엔드가 404 를 준다 — 화면이 "없는 세션" 을 직접 그리므로 알림은 끈다. */
export function useSessionDetail(date: string, mode: GameMode = 'all') {
	return useQuery({
		queryKey: ['sessions', date, mode],
		queryFn: ({ signal }) => getSessionDetail(date, mode, { signal }),
		enabled: !!date
	});
}
