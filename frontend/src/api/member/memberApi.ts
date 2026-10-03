/** 내전 멤버. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';

export function getMembers(config?: AxiosRequestConfig) {
	return api.get<Member[]>('/api/members', config);
}

export function registerMember(riotId: string) {
	return api.post<Member>('/api/members/register', { riotId });
}

/** 한 건이 실패해도 나머지는 계속하고, 건별 결과를 돌려준다. */
export function registerMembers(riotIds: string[]) {
	return api.post<BulkRegisterResponse>('/api/members/register-bulk', { riotIds });
}

export function deleteMember(puuid: string) {
	return api.delete<void>(`/api/members/${encodeURIComponent(puuid)}`);
}

export interface Member {
	riotId: string;
	puuid: string;
	registeredAt: string;
}

export interface BulkRegisterResult {
	riotId: string;
	status: 'ok' | 'skip' | 'error';
	reason?: string;
}

export interface BulkRegisterResponse {
	results: BulkRegisterResult[];
	total: number;
}
