import { Link, useParams } from 'react-router-dom';
import { useSessionDetail } from '@/hooks/useSessions';
import { Sparkline } from '@/components/ds/Chart';
import { PersonLink } from '@/components/ds/Champion';
import { Stat } from '@/components/ds/Stat';
import { InlineError } from '@/components/common/InlineError';
import { LoadingCenter } from '@/components/common/Spinner';
import { diffColor, signed } from '@/lib/timeline';
import type { SessionDetailResult, SessionMatchEntry } from '@/lib/types/stats';

/**
 * 하루치 내전.
 *
 * 세션은 오전 6시에 시작하는 하루다 — 새벽 경기는 전날 세션에 들어간다.
 * 타임라인 수치의 모집단은 전체 경기가 아니라 [timelineGames] 다.
 */
export function SessionDetailPage() {
	const { date = '' } = useParams();
	const { data, isPending, error, refetch } = useSessionDetail(date);

	if (isPending) return <LoadingCenter />;
	if (error || !data) {
		return (
			<div className="t-page">
				<InlineError message={`${date} 세션을 불러오지 못했습니다.`} onRetry={() => refetch()} />
				<p className="t-empty">
					<Link to="/sessions">세션 목록으로</Link>
				</p>
			</div>
		);
	}

	return (
		<div className="t-page">
			<Summary data={data} />
			<Matches data={data} />
			<Players data={data} />
		</div>
	);
}

function Summary({ data }: { data: SessionDetailResult }) {
	const start = new Date(data.firstGameAt);
	const end = new Date(data.lastGameAt);
	const time = (d: Date) => d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

	return (
		<section className="t-card">
			<div className="t-card-head">
				<h2 className="t-card-title">{data.date}</h2>
				<span className="t-card-more">
					{time(start)} ~ {time(end)}
				</span>
			</div>

			<div style={{ display: 'flex', flexWrap: 'wrap', gap: 28, alignItems: 'flex-end' }}>
				<Stat label="경기" value={data.games} hero sample={`약 ${data.totalDurationMin}분`} />
				<Stat
					label="진영 승수"
					value={
						<>
							<span style={{ color: 'var(--color-win)' }}>{data.team100Wins}</span>
							<span className="t-stat-sample"> : </span>
							<span style={{ color: 'var(--color-loss)' }}>{data.team200Wins}</span>
						</>
					}
					sample="블루 : 레드"
				/>
				<Stat
					label="총 킬"
					value={data.totalKills}
					sample={data.pentaKills > 0 ? `펜타킬 ${data.pentaKills}` : undefined}
				/>
				<Stat
					label="한타"
					value={`${data.teamFights}회`}
					sample={
						data.timelineGames > 0
							? `블루 ${data.team100FightWins} : ${data.team200FightWins} 레드 · ${data.timelineGames}경기 기준`
							: '타임라인 없음'
					}
				/>
				{data.longestWinStreak && (
					<Stat
						label="최다 연승"
						value={`${data.longestWinStreak.length}연승`}
						sample={data.longestWinStreak.riotId.split('#')[0]}
					/>
				)}
				{data.longestLossStreak && (
					<Stat
						label="최다 연패"
						value={`${data.longestLossStreak.length}연패`}
						sample={data.longestLossStreak.riotId.split('#')[0]}
					/>
				)}
			</div>

			{data.timelineGames < data.games && (
				<p className="t-stat-sample" style={{ display: 'block', marginTop: 12 }}>
					{data.games}경기 중 {data.timelineGames}경기에만 타임라인이 있습니다. 한타·골드 곡선은 그
					경기들만 셉니다.
				</p>
			)}
		</section>
	);
}

function Matches({ data }: { data: SessionDetailResult }) {
	return (
		<section className="t-card">
			<div className="t-card-head">
				<h2 className="t-card-title">경기</h2>
				{data.biggestComeback && (
					<span className="t-card-more">
						최대 역전 {signed(data.biggestComeback.goldDiffAt15)} 에서 뒤집음
					</span>
				)}
			</div>

			<div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
				{data.matches.map((m, i) => (
					<MatchRow
						key={m.matchId}
						match={m}
						index={i + 1}
						comeback={m.matchId === data.biggestComeback?.matchId}
						longest={m.matchId === data.longestGame?.matchId}
						shortest={m.matchId === data.shortestGame?.matchId}
					/>
				))}
			</div>
		</section>
	);
}

function MatchRow({
	match: m,
	index,
	comeback,
	longest,
	shortest
}: {
	match: SessionMatchEntry;
	index: number;
	comeback: boolean;
	longest: boolean;
	shortest: boolean;
}) {
	const at = new Date(m.gameCreation);
	return (
		<Link
			to={`/matches/${encodeURIComponent(m.matchId)}`}
			className="t-result"
			style={{
				display: 'flex',
				alignItems: 'center',
				gap: 12,
				flexWrap: 'wrap',
				padding: '8px 12px',
				borderRadius: 10,
				background: 'var(--gray-50)',
				textDecoration: 'none',
				color: 'inherit'
			}}
		>
			<span className="t-rank">{index}</span>
			<span className="t-stat-sample" style={{ minWidth: 52 }}>
				{at.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
			</span>

			<span
				className={`t-chip ${m.winnerTeamId === 100 ? 't-chip-win' : 't-chip-loss'}`}
				style={{ minWidth: 44, justifyContent: 'center' }}
			>
				{m.winnerTeamId === 100 ? '블루' : m.winnerTeamId === 200 ? '레드' : '?'}
			</span>

			<span className="t-stat-sample" style={{ minWidth: 60 }}>
				{Math.round(m.durationSec / 60)}분
			</span>
			<span className="t-stat-sample" style={{ minWidth: 60 }}>
				{m.totalKills}킬
			</span>

			{comeback && <span className="t-mark t-mark-strong">역전</span>}
			{longest && <span className="t-mark">최장</span>}
			{shortest && <span className="t-mark">최단</span>}

			<span style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 10 }}>
				{m.goldDiffAt15 != null && (
					<span className="t-stat-sample" title="15분 팀 골드 격차 (블루 − 레드)">
						15분 <b style={{ color: diffColor(m.goldDiffAt15, 500) }}>{signed(m.goldDiffAt15)}</b>
					</span>
				)}
				{m.hasTimeline ? (
					<Sparkline values={m.teamGoldDiffByMinute} />
				) : (
					<span className="t-stat-sample">타임라인 없음</span>
				)}
			</span>
		</Link>
	);
}

function Players({ data }: { data: SessionDetailResult }) {
	return (
		<section className="t-card">
			<div className="t-card-head">
				<h2 className="t-card-title">참가자</h2>
				<span className="t-card-more">{data.players.length}명</span>
			</div>

			<div className="t-tablewrap">
				<table className="t-table">
					<thead>
						<tr>
							<th>플레이어</th>
							<th className="t-num">전적</th>
							<th className="t-num">K / D / A</th>
							<th className="t-num">KDA</th>
							<th className="t-num" title="15분 골드 격차. 타임라인이 있는 경기만">
								골드차@15
							</th>
							<th className="t-num" title="15분 전 킬 / 데스">
								초반 K / D
							</th>
							<th className="t-num">솔로킬</th>
						</tr>
					</thead>
					<tbody>
						{data.players.map((p) => (
							<tr key={p.riotId}>
								<td>
									<PersonLink riotId={p.riotId} />
								</td>
								<td className="t-num">
									<span style={{ color: 'var(--color-win)' }}>{p.wins}</span>
									{' - '}
									<span style={{ color: 'var(--color-loss)' }}>{p.games - p.wins}</span>
								</td>
								<td className="t-num">
									{p.kills} / {p.deaths} / {p.assists}
								</td>
								<td className="t-num">{p.kda.toFixed(2)}</td>
								<td className="t-num">
									{p.avgGoldDiff15 == null ? (
										<span className="t-stat-sample">-</span>
									) : (
										<>
											<span style={{ color: diffColor(p.avgGoldDiff15, 100) }}>
												{signed(p.avgGoldDiff15)}
											</span>
											<br />
											<span className="t-stat-sample">{p.laneGames}경기</span>
										</>
									)}
								</td>
								<td className="t-num">
									{p.earlyKills} / {p.earlyDeaths}
								</td>
								<td className="t-num">{p.soloKills}</td>
							</tr>
						))}
					</tbody>
				</table>
			</div>
		</section>
	);
}
