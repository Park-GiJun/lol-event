/** AI — 챗봇과 검색 문서 색인. LLM 장비가 꺼져 있으면 409 `RAG_DISABLED` 가 온다. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';

export type ChatRole = 'USER' | 'ASSISTANT';

export interface ChatMessage {
	role: ChatRole;
	content: string;
}

export interface ChatAnswer {
	reply: string;
}

/**
 * 대화 전체를 보내면 마지막 질문에 답한다. 서버는 대화를 기억하지 않는다.
 * 답 하나에 수십 초가 걸릴 수 있다.
 */
export function askChat(messages: ChatMessage[], config?: AxiosRequestConfig) {
	return api.post<ChatAnswer>('/api/rag/chat', { messages }, config);
}

export interface RagIndexStatus {
	/** RAG 가 켜져 있는지. false 면 색인도 질문도 안 된다. */
	enabled: boolean;
	running: boolean;
	/** 이번(또는 마지막) 전체 색인이 쓸 문서 수. */
	total: number;
	processed: number;
	/** 글이 바뀌어 실제로 임베딩한 수. */
	embedded: number;
	failed: number;
	startedAt: string | null;
	finishedAt: string | null;
	lastError: string | null;
	/** 지금 저장된 문서 수. 키는 문서 종류(PLAYER_PROFILE 등). */
	documentCounts: Record<string, number>;
}

export function getRagIndexStatus(config?: AxiosRequestConfig) {
	return api.get<RagIndexStatus>('/api/admin/rag/status', config);
}

/** 전체를 다시 색인한다. 뒤에서 돌고 바로 돌아온다. 이미 돌고 있으면 `started: false`. */
export function startRagReindex() {
	return api.post<{ started: boolean }>('/api/admin/rag/reindex');
}
