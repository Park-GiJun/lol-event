import { ChatComposer } from '@/components/chat/ChatComposer';
import { ChatThread } from '@/components/chat/ChatThread';
import { TrashIcon } from '@/components/icons/LolIcons';
import { useChat } from '@/hooks/useChat';

const EXAMPLES = [
	'미드에서 승률 높은 챔피언 알려줘',
	'아군이 탑 사이온, 정글 자르반, 원딜 징크스, 서폿 룰루일 때 미드 뭐 하면 좋아?',
	'세라핀 카운터가 뭐야?',
	'피즈 잘하는 사람이 누구야?',
	'라인전 제일 강한 탑은 누구야?',
	'Elo 순위 10위까지 보여줘'
];

/**
 * 챗봇을 화면 하나로 크게 본다. 떠 있는 창(`ChatWidget`)과 **같은 대화**다 — 창에서 묻다가
 * 여기로 와도 이어지고, 다른 화면으로 가면 창에서 이어진다.
 */
export function ChatPage() {
	const { messages, pending, failure, send, retry, reset } = useChat();

	return (
		<div className="t-page chat-page">
			<header className="t-page-head chat-page-head">
				<div>
					<h1 className="t-page-title">AI 챗봇</h1>
					<p className="t-page-sub">
						전적·승률·Elo 는 실제 내전 기록에서 가져옵니다. 답 하나에 수십 초가 걸릴 수 있습니다.
					</p>
				</div>
				<button
					className="btn btn-secondary btn-sm"
					onClick={reset}
					disabled={messages.length === 0 || pending}
				>
					<TrashIcon size={15} />
					대화 지우기
				</button>
			</header>

			<section className="chat-page-panel" aria-label="AI 챗봇 대화">
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
		</div>
	);
}
