import { Link, useLocation } from 'react-router-dom';
import { ChatComposer } from '@/components/chat/ChatComposer';
import { ChatThread } from '@/components/chat/ChatThread';
import { ChatIcon, CloseIcon, ExternalLinkIcon, TrashIcon } from '@/components/icons/LolIcons';
import { useChat } from '@/hooks/useChat';
import { CHAT_PAGE_PATH } from '@/lib/constants/chat';
import { useChatStore } from '@/stores/chatStore';

const EXAMPLES = [
	'세라핀 카운터가 뭐야?',
	'우리 원딜이 징크스인데 서폿 뭐 하면 좋아?',
	'라인전 제일 강한 탑은 누구야?'
];

/**
 * 전역 챗봇. 셸에 한 번 마운트되어 모든 화면에 떠 있다.
 *
 * 대화는 `useChat` 이 쥐고 있다 — 화면(`ChatPage`)과 같은 대화를 본다.
 */
export function ChatWidget() {
	const { open, setOpen } = useChatStore();
	const { messages, pending, failure, send, retry, reset } = useChat();
	const location = useLocation();

	if (location.pathname.startsWith(CHAT_PAGE_PATH)) return null;

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
				<Link
					className="t-iconbtn"
					to={CHAT_PAGE_PATH}
					onClick={() => setOpen(false)}
					aria-label="큰 화면으로 보기"
					title="큰 화면으로 보기"
				>
					<ExternalLinkIcon size={16} />
				</Link>
				<button
					className="t-iconbtn"
					onClick={reset}
					disabled={messages.length === 0 || pending}
					aria-label="대화 지우기"
				>
					<TrashIcon size={16} />
				</button>
				<button className="t-iconbtn" onClick={() => setOpen(false)} aria-label="챗봇 닫기">
					<CloseIcon size={18} />
				</button>
			</header>

			<ChatThread
				messages={messages}
				pending={pending}
				failure={failure}
				examples={EXAMPLES}
				onExample={send}
				onRetry={retry}
			/>
			<ChatComposer pending={pending} onSend={send} />
		</section>
	);
}
