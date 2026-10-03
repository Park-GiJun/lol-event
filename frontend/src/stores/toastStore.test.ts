import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { showToast, useToastStore } from '@/stores/toastStore';

describe('showToast', () => {
	beforeEach(() => {
		vi.useFakeTimers();
		useToastStore.setState({ toasts: [] });
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it('띄우고 시간이 지나면 스스로 사라진다', () => {
		showToast('저장했습니다', 'success', 1000);
		expect(useToastStore.getState().toasts.map((t) => t.message)).toEqual(['저장했습니다']);

		vi.advanceTimersByTime(1000);
		expect(useToastStore.getState().toasts).toEqual([]);
	});

	it('같은 문구가 연달아 오면 하나만 띄운다', () => {
		// 화면 하나가 조회 넷을 던지고 서버가 죽어 있으면 같은 실패가 넷 온다.
		vi.setSystemTime(10_000);
		showToast('서버 오류', 'error');
		vi.setSystemTime(10_100);
		showToast('서버 오류', 'error');

		expect(useToastStore.getState().toasts).toHaveLength(1);
	});

	it('창이 지나면 같은 문구도 다시 띄운다', () => {
		vi.setSystemTime(20_000);
		showToast('서버 오류', 'error');
		vi.setSystemTime(21_000);
		showToast('서버 오류', 'error');

		expect(useToastStore.getState().toasts).toHaveLength(2);
	});
});
