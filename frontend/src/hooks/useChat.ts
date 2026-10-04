import { useMutation } from '@tanstack/react-query';
import { ApiError } from '@/api/client';
import { SILENT_META } from '@/api/queryClient';
import { askChat, type ChatMessage } from '@/api/rag/ragApi';
import { errorMessageOf } from '@/lib/constants/errorMessages';
import { useChatStore } from '@/stores/chatStore';

/**
 * 챗봇 대화 한 벌. 떠 있는 창(`ChatWidget`)과 화면(`ChatPage`)이 같이 쓴다.
 *
 * 실패는 토스트가 아니라 **대화 안에** 적는다(`SILENT_META`). 질문한 자리에서 답이 안 온 이유를
 * 봐야 하고, 토스트는 3초 뒤 사라져 무슨 일이 있었는지 남지 않는다.
 *
 * 기다리는 중·실패까지 store 에 둔다. 창에서 묻고 화면으로 옮겨 가도 "찾아보는 중" 이 이어져야 한다.
 */
export function useChat() {
	const { messages, pending, failure, append, clear, setPending, setFailure } = useChatStore();

	const ask = useMutation({
		mutationFn: (history: ChatMessage[]) => askChat(history),
		onMutate: () => {
			setPending(true);
			setFailure(null);
		},
		onSuccess: (answer) => append({ role: 'ASSISTANT', content: answer.reply }),
		onError: (error) =>
			setFailure(
				error instanceof ApiError
					? errorMessageOf(error.errorCode, error.message)
					: '답을 받지 못했습니다.'
			),
		onSettled: () => setPending(false),
		meta: SILENT_META
	});

	/** @returns 보냈으면 true. 비었거나 답을 기다리는 중이면 보내지 않는다. */
	function send(text: string): boolean {
		const question = text.trim();
		if (!question || pending) return false;
		const next: ChatMessage[] = [...messages, { role: 'USER', content: question }];
		append({ role: 'USER', content: question });
		ask.mutate(next);
		return true;
	}

	/** 실패한 질문은 대화에 남아 있다. 그대로 다시 보낸다. */
	function retry() {
		if (pending || messages.at(-1)?.role !== 'USER') return;
		ask.mutate(messages);
	}

	function reset() {
		clear();
		setFailure(null);
	}

	return { messages, pending, failure, send, retry, reset };
}
