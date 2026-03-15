import type { ApiResponse } from '../types/api';

const BACKEND_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9832/api';
const LCU_BASE = import.meta.env.VITE_LCU_BASE_URL || 'http://localhost:9832/api';

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

export const lcuApi = {
  status: () => fetch(`${LCU_BASE}/lcu/status`).then(r => r.json()),
  fetchSSE: (onMessage: (data: Record<string, unknown>) => void, onDone: () => void) => {
    const es = new EventSource(`${LCU_BASE}/lcu/collect`);
    es.onmessage = (e) => {
      const data = JSON.parse(e.data);
      onMessage(data);
      if (data.type === 'done' || data.type === 'error') { es.close(); onDone(); }
    };
    es.onerror = () => { es.close(); onDone(); };
    return es;
  },
  getMatches: () => fetch(`${LCU_BASE}/matches`).then(r => r.json()),
};
