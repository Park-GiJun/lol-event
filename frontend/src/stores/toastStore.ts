/**
 * 토스트 — 전역 싱글턴.
 *
 * `showToast` 는 컴포넌트 밖(Query onError)에서도 호출되므로 store 액션을 훅이 아닌 모듈 함수로
 * 노출한다(Zustand 는 `getState()` 로 밖에서 쓸 수 있다).
 */

import { create } from 'zustand';

export type ToastType = 'info' | 'success' | 'error' | 'warning';

export interface Toast {
	id: number;
	message: string;
	type: ToastType;
	duration: number;
}

interface ToastState {
	toasts: Toast[];
	push: (toast: Toast) => void;
	dismiss: (id: number) => void;
}

export const useToastStore = create<ToastState>((set) => ({
	toasts: [],
	push: (toast) => set((s) => ({ toasts: [...s.toasts, toast] })),
	dismiss: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }))
}));

/** 같은 문구가 연달아 뜨는 것을 막는 창. 화면 하나가 조회를 여러 개 던지면 같은 실패가 겹친다. */
const DUPLICATE_WINDOW_MS = 400;
let lastToast: { message: string; type: ToastType; at: number } | null = null;
let seq = 0;

export function showToast(message: string, type: ToastType = 'info', duration = 3000): void {
	const now = Date.now();
	if (
		lastToast &&
		lastToast.message === message &&
		lastToast.type === type &&
		now - lastToast.at < DUPLICATE_WINDOW_MS
	) {
		return;
	}
	lastToast = { message, type, at: now };

	const id = ++seq;
	const { push, dismiss } = useToastStore.getState();
	push({ id, message, type, duration });
	setTimeout(() => dismiss(id), duration);
}
