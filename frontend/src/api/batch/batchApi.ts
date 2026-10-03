/** 통계 스냅샷 집계 — 운영자가 손으로 돌리는 진입점. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';

export interface BatchStatus {
	playerSnapshotCount: number;
	championSnapshotCount: number;
	championItemSnapshotCount: number;
	lastAggregatedAt: string | null;
	message: string;
}

export function getBatchStatus(config?: AxiosRequestConfig) {
	return api.get<BatchStatus>('/api/batch/status', config);
}

/** 전체 집계 잡을 시작한다. 끝나기를 기다리지 않는다. */
export function triggerBatch() {
	return api.post<string>('/api/batch/trigger');
}

/** 챔피언 아이템 통계만 그 자리에서 다시 집계한다. 끝날 때까지 돌아오지 않는다. */
export function triggerItemStats() {
	return api.post<string>('/api/batch/trigger-item-stats');
}
