/**
 * TanStack Query 설정.
 *
 * **화면은 실패 토스트를 적지 않는다** — 여기 QueryCache/MutationCache 의 onError 가 한곳에서 낸다.
 * 화면이 또 적으면 한 번의 실패에 알림이 두 개 뜬다.
 *
 * | 하고 싶은 것 | 방법 |
 * |---|---|
 * | 문구를 바꾼다 | `meta: { errorMessage }` |
 * | 레이어를 침묵시키고 화면이 직접 낸다 | `meta: { silent: true }` |
 * | 저장 성공을 알린다 | `meta: { successMessage }` (mutation) |
 */

import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { errorMessageOf } from '@/lib/constants/errorMessages';
import { showToast } from '@/stores/toastStore';
import { ApiError } from './client';

/** Query/Mutation `meta` 로 넘길 수 있는 항목. */
export interface AppQueryMeta extends Record<string, unknown> {
	/** true 면 레이어가 토스트를 내지 않는다(화면이 직접 낸다). */
	silent?: boolean;
	/** 서버 문구 대신 띄울 문구. */
	errorMessage?: string;
	/** mutation 성공 시 띄울 문구. 없으면 조용히 넘어간다. */
	successMessage?: string;
}

function toastError(error: unknown, meta?: AppQueryMeta): void {
	// 취소는 실패가 아니다.
	if (axios.isCancel(error)) return;
	if (meta?.silent) return;
	const message =
		meta?.errorMessage ??
		(error instanceof ApiError
			? errorMessageOf(error.errorCode, error.message)
			: '요청을 처리하지 못했습니다.');
	if (message) showToast(message, 'error');
}

/**
 * 부가 정보용 `meta` — 실패해도 알리지 않는다.
 * 호버 미리보기처럼 "안 떠도 화면이 성립하는" 조회에 쓴다. 그런 것까지 토스트를 띄우면
 * 마우스를 올릴 때마다 오류 알림이 쌓인다.
 */
export const SILENT_META = { silent: true } as const satisfies AppQueryMeta;

export function createQueryClient(): QueryClient {
	return new QueryClient({
		queryCache: new QueryCache({
			onError: (error, query) => toastError(error, query.meta as AppQueryMeta | undefined)
		}),
		mutationCache: new MutationCache({
			onError: (error, _vars, _ctx, mutation) =>
				toastError(error, mutation.meta as AppQueryMeta | undefined),
			onSuccess: (_data, _vars, _ctx, mutation) => {
				const meta = mutation.meta as AppQueryMeta | undefined;
				if (meta?.successMessage) showToast(meta.successMessage, 'success');
			}
		}),
		defaultOptions: {
			queries: {
				// 통계는 경기가 올라올 때만 바뀐다. 화면을 옮겨 다닐 때마다 다시 받을 이유가 없다.
				staleTime: 5 * 60 * 1000,
				gcTime: 30 * 60 * 1000,
				// 4xx 는 다시 해도 같다. 재시도는 그게 의미 있는 조회가 직접 켠다.
				retry: false
			},
			mutations: {
				retry: false
			}
		}
	});
}
