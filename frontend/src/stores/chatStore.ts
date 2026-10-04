/**
 * 챗봇 대화 — 전역.
 *
 * 창은 모든 화면에 떠 있고, 화면을 옮겨도 대화가 이어져야 한다. 그래서 컴포넌트 상태가 아니라
 * store 에 둔다. 새로고침하면 사라진다 — 서버도 대화를 기억하지 않는다.
 */

import { create } from 'zustand';
import type { ChatMessage } from '@/api/rag/ragApi';

interface ChatState {
	open: boolean;
	messages: ChatMessage[];
	/** 답을 기다리는 중. */
	pending: boolean;
	/** 마지막 질문이 실패한 이유. 다시 묻거나 대화를 지우면 사라진다. */
	failure: string | null;
	setOpen: (open: boolean) => void;
	append: (message: ChatMessage) => void;
	clear: () => void;
	setPending: (pending: boolean) => void;
	setFailure: (failure: string | null) => void;
}

export const useChatStore = create<ChatState>((set) => ({
	open: false,
	messages: [],
	pending: false,
	failure: null,
	setOpen: (open) => set({ open }),
	append: (message) => set((s) => ({ messages: [...s.messages, message] })),
	clear: () => set({ messages: [] }),
	setPending: (pending) => set({ pending }),
	setFailure: (failure) => set({ failure })
}));
