import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/api';
import type { SessionDetailResult, SessionReportResult } from '@/lib/types/stats';

/*
 * 세션 훅. 세션은 오전 6시에 시작하는 하루다 — 새벽 경기는 전날 세션에 들어간다.
 */

export function useSessions(mode: string = 'all') {
	return useQuery({
		queryKey: ['sessions', mode],
		queryFn: () => api.get<SessionReportResult>(`/stats/sessions?mode=${mode}`)
	});
}

/** 세션 상세. 없는 날짜는 백엔드가 404 를 준다 — 그건 실제 오류라 재시도하지 않는다. */
export function useSessionDetail(date: string, mode: string = 'all') {
	return useQuery({
		queryKey: ['sessions', date, mode],
		queryFn: () =>
			api.get<SessionDetailResult>(`/stats/sessions/${encodeURIComponent(date)}?mode=${mode}`),
		enabled: !!date,
		retry: false
	});
}
