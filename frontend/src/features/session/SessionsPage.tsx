import { Link } from 'react-router-dom';
import { useSessions } from '@/hooks/useSessions';
import { InlineError } from '@/components/common/InlineError';
import { LoadingCenter } from '@/components/common/Spinner';
import type { SessionEntry } from '@/api/stats/sessionApi';

/**
 * 세션 목록 — 하루치 내전 하나가 한 줄이다.
 *
 * 세션은 **오전 6시에 시작하는 하루**다. 내전은 밤에 하는데 자정으로 자르면 23:40 에
 * 시작한 판과 00:20 에 끝난 판이 다른 날로 갈린다.
 */
export function SessionsPage() {
	const { data, isPending, error, refetch } = useSessions();

	if (isPending) return <LoadingCenter />;
	if (error || !data) {
		return (
			<div className="t-page">
				<InlineError message="세션을 불러오지 못했습니다." onRetry={() => refetch()} />
			</div>
		);
	}

	if (data.sessions.length === 0) {
		return (
			<div className="t-page">
				<p className="t-empty">아직 기록된 세션이 없습니다.</p>
			</div>
		);
	}

	return (
		<div className="t-page">
			<section className="t-card">
				<div className="t-card-head">
					<h2 className="t-card-title">세션</h2>
					<span className="t-card-more">{data.totalSessions}일</span>
				</div>
				<p className="t-empty" style={{ padding: '0 0 10px', textAlign: 'left' }}>
					하루 저녁에 모아 한 판씩입니다. 새벽까지 이어진 경기도 그날 세션으로 묶습니다.
				</p>

				<div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
					{data.sessions.map((s) => (
						<SessionRow key={s.date} session={s} />
					))}
				</div>
			</section>
		</div>
	);
}

function SessionRow({ session: s }: { session: SessionEntry }) {
	return (
		<Link
			to={`/sessions/${s.date}`}
			className="t-card t-card-sunken"
			style={{
				display: 'block',
				padding: 'var(--spacing-md)',
				textDecoration: 'none',
				color: 'inherit'
			}}
		>
			<div style={{ display: 'flex', flexWrap: 'wrap', gap: 20, alignItems: 'baseline' }}>
				<span
					style={{ fontWeight: 'var(--font-weight-extrabold)', fontSize: 'var(--font-size-md)' }}
				>
					{s.date}
				</span>
				<span className="t-stat-sample">
					{s.games}경기 · 약 {s.totalDurationMin}분
				</span>

				<span
					style={{
						marginLeft: 'auto',
						display: 'flex',
						alignItems: 'center',
						gap: 10,
						fontVariantNumeric: 'tabular-nums'
					}}
				>
					<span className="t-stat-sample">블루</span>
					<b style={{ color: 'var(--color-win)' }}>{s.team100Wins}</b>
					<span className="t-stat-sample">:</span>
					<b style={{ color: 'var(--color-loss)' }}>{s.team200Wins}</b>
					<span className="t-stat-sample">레드</span>
				</span>
			</div>

			<div style={{ display: 'flex', flexWrap: 'wrap', gap: 18, marginTop: 8 }}>
				{s.sessionMvp && (
					<span className="t-stat-sample">
						MVP <b style={{ color: 'var(--color-primary)' }}>{s.sessionMvp.split('#')[0]}</b>
						{` (KDA ${s.sessionMvpKda.toFixed(2)})`}
					</span>
				)}
				<span className="t-stat-sample">총킬 {s.totalKills}</span>
				{s.pentaKills > 0 && <span className="t-mark t-mark-strong">펜타킬 {s.pentaKills}</span>}
				<span className="t-stat-sample" style={{ marginLeft: 'auto' }}>
					{s.participants.length}명
				</span>
			</div>
		</Link>
	);
}
