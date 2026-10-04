import { useEffect, useRef } from 'react';
import type { ChatMessage } from '@/api/rag/ragApi';
import { RichText } from '@/components/common/RichText';

interface ChatThreadProps {
	messages: ChatMessage[];
	pending: boolean;
	failure: string | null;
	/** 대화가 비었을 때 보여 줄 질문. 누르면 그대로 보낸다. */
	examples: string[];
	onExample: (example: string) => void;
	onRetry: () => void;
}

/** 대화가 흐르는 칸. 창과 화면이 같이 쓴다. */
export function ChatThread({
	messages,
	pending,
	failure,
	examples,
	onExample,
	onRetry
}: ChatThreadProps) {
	const scrollRef = useRef<HTMLDivElement>(null);

	// 새 말이 붙으면 맨 아래로 내린다. DOM 을 맞추는 일이라 effect 가 맞는 자리다.
	useEffect(() => {
		scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
	}, [messages.length, pending, failure]);

	return (
		<div className="chat-body" ref={scrollRef}>
			{messages.length === 0 && (
				<div className="chat-empty">
					<p>내전 기록을 물어보세요. 전적·승률·Elo 는 실제 기록에서 가져옵니다.</p>
					<div className="chat-examples">
						{examples.map((example) => (
							<button key={example} className="chat-example" onClick={() => onExample(example)}>
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

			{pending && (
				<div className="chat-msg chat-msg-bot chat-msg-pending" role="status">
					기록을 찾아보는 중…
				</div>
			)}

			{failure && (
				<div className="chat-msg chat-msg-error" role="alert">
					{failure}
					<button className="chat-retry" onClick={onRetry}>
						다시 시도
					</button>
				</div>
			)}
		</div>
	);
}
