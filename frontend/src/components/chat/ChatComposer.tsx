import { useEffect, useRef, useState } from 'react';
import { SendIcon } from '@/components/icons/LolIcons';

interface ChatComposerProps {
	pending: boolean;
	/** @returns 보냈으면 true. 그때만 입력 칸을 비운다. */
	onSend: (text: string) => boolean;
}

/** 질문 입력 칸. 나타나면 바로 칠 수 있게 초점을 잡는다. */
export function ChatComposer({ pending, onSend }: ChatComposerProps) {
	const [draft, setDraft] = useState('');
	const inputRef = useRef<HTMLTextAreaElement>(null);

	useEffect(() => {
		inputRef.current?.focus();
	}, []);

	function submit() {
		if (onSend(draft)) setDraft('');
	}

	return (
		<form
			className="chat-input"
			onSubmit={(e) => {
				e.preventDefault();
				submit();
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
						submit();
					}
				}}
				placeholder="질문을 입력하세요"
				rows={1}
				maxLength={1000}
				aria-label="질문"
			/>
			<button type="submit" disabled={!draft.trim() || pending} aria-label="보내기">
				<SendIcon size={18} />
			</button>
		</form>
	);
}
