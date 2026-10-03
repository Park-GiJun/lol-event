/**
 * 공통 axios 인스턴스 + 백엔드 응답 봉투(CommonApiResponse) 해제.
 *
 * 도메인 API 함수는 **데이터를 그대로 반환하고 실패 시 `ApiError` 를 던진다.**
 * TanStack Query 는 queryFn 이 throw 할 때만 실패로 인식한다 — 그래야 전역 onError · retry ·
 * isError 가 동작한다.
 *
 * - 실패 알림: `@/api/queryClient` 의 QueryCache/MutationCache onError 가 한곳에서 토스트로 낸다.
 *   화면은 실패 문구를 적지 않는다.
 * - 취소: queryKey 가 바뀌면 Query 가 이전 요청을 버리고 `signal` 로 실제 요청까지 끊는다.
 *   그래서 API 함수는 `config` 를 받아 `signal` 을 넘길 수 있게 열어 둔다.
 */

import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios';

/** 백엔드 공통 응답 봉투. */
interface CommonApiResponse<T> {
	success: boolean;
	data: T;
	message: string | null;
	errorCode?: string | null;
}

/** API 실패. `errorCode` 는 화면 분기용 계약값이고 `message` 는 서버가 준 문구다. */
export class ApiError extends Error {
	readonly errorCode?: string;
	readonly status?: number;

	constructor(message: string, errorCode?: string, status?: number) {
		super(message);
		this.name = 'ApiError';
		this.errorCode = errorCode;
		this.status = status;
	}
}

const FALLBACK_MESSAGE = '요청을 처리하지 못했습니다.';

/**
 * `baseURL` 은 **비워 둔다**(same-origin 상대경로). 호출부는 전부 `/api/...` 로 부른다.
 * dev 는 vite proxy 가, 운영은 nginx 가 `/api/*` 를 백엔드로 보낸다.
 *
 * 예전에는 `VITE_API_BASE_URL` 로 백엔드 절대 주소를 박았다. 그러면 브라우저가 교차 출처로
 * 호출해 CORS 설정에 기대게 되고, dev 와 운영의 네트워크 모델이 달라진다.
 */
export const client: AxiosInstance = axios.create({ baseURL: '' });

async function request<T>(
	method: string,
	url: string,
	data?: unknown,
	config?: AxiosRequestConfig
): Promise<T> {
	try {
		const response = await client({ method, url, data, ...config });
		const body = response.data as CommonApiResponse<T>;
		if (body.success) return body.data;
		throw new ApiError(
			body.message || FALLBACK_MESSAGE,
			body.errorCode ?? undefined,
			response.status
		);
	} catch (error: unknown) {
		if (error instanceof ApiError) throw error;
		// 취소는 실패가 아니다 — Query 가 알아보게 그대로 올려보낸다(토스트도 뜨지 않는다).
		if (axios.isCancel(error)) throw error;

		const axiosError = error as {
			response?: { status?: number; data?: { message?: string | null; errorCode?: string | null } };
		};
		throw new ApiError(
			axiosError.response?.data?.message || FALLBACK_MESSAGE,
			axiosError.response?.data?.errorCode ?? undefined,
			axiosError.response?.status
		);
	}
}

export const api = {
	get: <T>(url: string, config?: AxiosRequestConfig) => request<T>('get', url, undefined, config),
	post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
		request<T>('post', url, data, config),
	delete: <T>(url: string, config?: AxiosRequestConfig) =>
		request<T>('delete', url, undefined, config)
};
