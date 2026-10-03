import { useEffect, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { ApiError } from '@/api/client';
import { SILENT_META } from '@/api/queryClient';
import { askChat, type ChatMessage } from '@/api/rag/ragApi';
import { RichText } from '@/components/common/RichText';
import { ChatIcon, CloseIcon, SendIcon, TrashIcon } from '@/components/icons/LolIcons';
import { errorMessageOf } from '@/lib/constants/errorMessages';
import { useChatStore } from '@/stores/chatStore';

const EXAMPLES = [
	'세라핀 카운터가 뭐야?',
	'우리 원딜이 징크스인데 서폿 뭐 하면 좋아?',
	'라인전 제일 강한 탑은 누구야?'
];

/**
 * 전역 챗봇. 셸에 한 번 마운트되어 모든 화면에 떠 있다.
 *
 * 실패는 토스트가 아니라 **대화 안에** 적는다(`SILENT_META`). 질문한 자리에서 답이 안 온 이유를
 * 봐야 하고, 토스트는 3초 뒤 사라져 무슨 일이 있었는지 남지 않는다.
 */
export function ChatWidget() {
	const { open, messages, setOpen, append, clear } = useChatStore();
	const [draft, setDraft] = useState('');
	const [failure, setFailure] = useState<string | null>(null);
	const scrollRef = useRef<HTMLDivElement>(null);
	const inputRef = useRef<HTMLTextAreaElement>(null);

	const ask = useMutation({
		mutationFn: (history: ChatMessage[]) => askChat(history),
		onSuccess: (answer) => append({ role: 'ASSISTANT', content: answer.reply }),
		onError: (error) =>
			setFailure(
				error instanceof ApiError
					? errorMessageOf(error.errorCode, error.message)
					: '답을 받지 못했습니다.'
			),
		meta: SILENT_META
	});

	// 새 말이 붙거나 창을 열면 맨 아래로 내린다. DOM 을 맞추는 일이라 effect 가 맞는 자리다.
	useEffect(() => {
		if (!open) return;
		scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
	}, [open, messages.length, ask.isPending, failure]);

	useEffect(() => {
		if (open) inputRef.current?.focus();
	}, [open]);

	function send(text: string) {
		const question = text.trim();
		if (!question || ask.isPending) return;
		const next: ChatMessage[] = [...messages, { role: 'USER', content: question }];
		append({ role: 'USER', content: question });
		setDraft('');
		setFailure(null);
		ask.mutate(next);
	}

	/** 실패한 질문은 대화에 남아 있다. 그대로 다시 보낸다. */
	function retry() {
		if (ask.isPending || messages.at(-1)?.role !== 'USER') return;
		setFailure(null);
		ask.mutate(messages);
	}

	function reset() {
		clear();
		setFailure(null);
	}

	if (!open) {
		return (
			<button className="chat-fab" onClick={() => setOpen(true)} aria-label="AI 챗봇 열기">
				<ChatIcon size={22} />
			</button>
		);
	}

	return (
		<section className="chat-panel" aria-label="AI 챗봇">
			<header className="chat-head">
				<ChatIcon size={17} />
				<span className="chat-title">내전 AI</span>
				<button
					className="t-iconbtn"
					onClick={reset}
					disabled={messages.length === 0 || ask.isPending}
					aria-label="대화 지우기"
				>
					<TrashIcon size={16} />
				</button>
				<button className="t-iconbtn" onClick={() => setOpen(false)} aria-label="챗봇 닫기">
					<CloseIcon size={18} />
				</button>
			</header>

			<div className="chat-body" ref={scrollRef}>
				{messages.length === 0 && (
					<div className="chat-empty">
						<p>내전 기록을 물어보세요. 전적·승률·Elo 는 실제 기록에서 가져옵니다.</p>
						<div className="chat-examples">
							{EXAMPLES.map((example) => (
								<button key={example} className="chat-example" onClick={() => send(example)}>
									{example}
								</button>
							))}
						</div>
					</div>
				)}

				{messages.map((message, i) => (
					<div key={i} className={`chat-msg chat-msg-${message.role === 'USER' ? 'user' : 'bot'}`}>
						{message.role === 'USER' ? message.content : <RichText text={message.content} />}
					</div>
				))}

				{ask.isPending && (
					<div className="chat-msg chat-msg-bot chat-msg-pending" role="status">
						기록을 찾아보는 중…
					</div>
				)}

				{failure && (
					<div className="chat-msg chat-msg-error" role="alert">
						{failure}
						<button className="chat-retry" onClick={retry}>
							다시 시도
						</button>
					</div>
				)}
			</div>

			<form
				className="chat-input"
				onSubmit={(e) => {
					e.preventDefault();
					send(draft);
				}}
			>
				<textarea
					ref={inputRef}
					value={draft}
					onChange={(e) => setDraft(e.target.value)}
					onKeyDown={(e) => {
						// 한글 조합 중의 Enter 는 글자를 확정하는 키다. 그때 보내면 마지막 글자가 두 번 들어간다.
						if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
							e.preventDefault();
							send(draft);
						}
					}}
					placeholder="질문을 입력하세요"
					rows={1}
					maxLength={1000}
					aria-label="질문"
				/>
				<button type="submit" disabled={!draft.trim() || ask.isPending} aria-label="보내기">
					<SendIcon size={18} />
				</button>
			</form>
		</section>
	);
}
