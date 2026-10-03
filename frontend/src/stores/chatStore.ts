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
	setOpen: (open: boolean) => void;
	append: (message: ChatMessage) => void;
	clear: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
	open: false,
	messages: [],
	setOpen: (open) => set({ open }),
	append: (message) => set((s) => ({ messages: [...s.messages, message] })),
	clear: () => set({ messages: [] })
}));
