import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getRagIndexStatus, startRagReindex } from '@/api/rag/ragApi';
import { RefreshIcon } from '@/components/icons/LolIcons';

const STATUS_KEY = ['rag-index-status'] as const;

const DOC_LABEL: Record<string, string> = {
	PLAYER_PROFILE: '플레이어',
	CHAMPION_PROFILE: '챔피언',
	MATCH_REVIEW: '경기'
};

/**
 * AI 검색 문서 색인. 경기가 올라오면 알아서 갱신되고, 여기서는 전체를 손으로 다시 돌린다.
 * 도는 동안에는 2초마다 진행 상황을 다시 읽는다.
 */
export function RagIndexCard() {
	const queryClient = useQueryClient();
	const { data: status } = useQuery({
		queryKey: STATUS_KEY,
		queryFn: ({ signal }) => getRagIndexStatus({ signal }),
		refetchInterval: (query) => (query.state.data?.running ? 2000 : false)
	});

	const reindex = useMutation({
		mutationFn: startRagReindex,
		onSuccess: () => queryClient.invalidateQueries({ queryKey: STATUS_KEY }),
		meta: { successMessage: '전체 색인을 시작했습니다.' }
	});

	const counts = Object.entries(status?.documentCounts ?? {});

	return (
		<section className="stats-section card" style={{ marginBottom: 'var(--spacing-lg)' }}>
			<div className="section-head">
				<RefreshIcon size={16} />
				<span className="section-head-title">AI 검색 문서</span>
			</div>

			<p className="team-ai-hint">
				저장된 문서:{' '}
				{counts.length === 0
					? '없음'
					: counts.map(([type, count]) => `${DOC_LABEL[type] ?? type} ${count}건`).join(' · ')}
			</p>

			{status?.running && (
				<p className="team-ai-hint" role="status">
					색인 중 — {status.processed}/{status.total} (임베딩 {status.embedded}건, 실패{' '}
					{status.failed}
					건)
				</p>
			)}
			{status && !status.running && status.finishedAt && (
				<p className="team-ai-hint">
					마지막 색인: {new Date(status.finishedAt).toLocaleString('ko-KR')} — {status.total}건 중
					임베딩 {status.embedded}건, 실패 {status.failed}건
				</p>
			)}
			{status?.lastError && <p className="team-ai-problem">마지막 오류: {status.lastError}</p>}

			<button
				className="btn btn-secondary"
				onClick={() => reindex.mutate()}
				disabled={!status || status.running || reindex.isPending}
			>
				<RefreshIcon size={14} /> {status?.running ? '색인 중…' : '전체 다시 색인'}
			</button>
		</section>
	);
}
