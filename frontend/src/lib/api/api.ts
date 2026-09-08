import type { ApiResponse } from '../types/api';

// 기본값은 로컬에서 main-service 를 직접 띄웠을 때의 주소다.
// 예전에는 9832(API Gateway)를 가리켰는데 게이트웨이를 걷어냈다.
// 배포 환경에서는 .env 의 VITE_API_BASE_URL 이 덮는다.
const BACKEND_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';

// lcuApi 는 지웠다. 서버의 lcu-service 는 윈도우 롤 lockfile 을 읽는 코드라
// 리눅스 컨테이너에서 동작한 적이 없고, 이 클라이언트를 쓰는 화면도 없었다.
// LCU 연동은 데스크탑 수집기가 로컬에서 직접 한다.

type ErrorHandler = (title: string, message: string) => void;
let globalErrorHandler: ErrorHandler | null = null;
export function setErrorHandler(handler: ErrorHandler) { globalErrorHandler = handler; }

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const config: RequestInit = {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  };
  const res = await fetch(url, config);
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    globalErrorHandler?.(`HTTP ${res.status}`, err.message || '요청 실패');
    throw new Error(err.message || `HTTP ${res.status}`);
  }
  const result: ApiResponse<T> = await res.json();
  if (!result.success) {
    globalErrorHandler?.('오류', result.message || '알 수 없는 오류');
    throw new Error(result.message || 'API Error');
  }
  return result.data as T;
}

export const api = {
  get: <T>(endpoint: string) => request<T>(`${BACKEND_BASE}${endpoint}`),
  post: <T>(endpoint: string, data: unknown) =>
    request<T>(`${BACKEND_BASE}${endpoint}`, { method: 'POST', body: JSON.stringify(data) }),
  delete: <T>(endpoint: string) => request<T>(`${BACKEND_BASE}${endpoint}`, { method: 'DELETE' }),
};

