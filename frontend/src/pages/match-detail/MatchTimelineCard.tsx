import { useMatchTimeline } from '@/hooks/useMatches';
import { DivergingArea, EventStrip, MapScatter, type StripEvent } from '@/components/ds/Chart';
import { Stat } from '@/components/ds/Stat';
import { SkullIcon, SwordsIcon } from '@/components/icons/LolIcons';
import { OBJECTIVE_ICON, objectiveLabel } from '@/components/icons/objectiveIcon';
import { formatClock } from '@/lib/chart';
import type { MatchTimeline } from '@/api/match/matchApi';

/**
 * 경기 한 판의 흐름.
 *
 * 최종 스코어보드로는 "누가 잘했나"만 보이고 "어떻게 그렇게 됐나"는 안 보인다. 골드 곡선과
 * 이벤트 띠, 교전 구간이 그 서사를 채운다.
 *
 * 타임라인이 없는 경기는 카드 자체를 그리지 않는다 — 새 수집기 이전 경기는 영구히 없고,
 * 그건 오류가 아니라 정상이다.
 */
export function MatchTimelineCard({ matchId }: { matchId: string }) {
	const { data, isPending, error } = useMatchTimeline(matchId);

	if (isPending || error || !data?.hasTimeline) return null;

	return (
		<section className="t-card">
			<div className="t-card-head">
				<h2 className="t-card-title">경기 흐름</h2>
				<span className="t-card-more">{data.lastMinute}분까지 기록</span>
			</div>

			<DivergingArea
				label="팀 골드 격차 (블루 − 레드)"
				unit="분"
				points={data.teamGoldDiffByMinute.map((v, minute) => ({ at: minute, value: v }))}
			/>

			<EventStrip label="킬과 오브젝트" durationMs={data.durationMs} events={stripEvents(data)} />

			<div
				style={{
					display: 'flex',
					flexWrap: 'wrap',
					gap: 28,
					marginTop: 24,
					alignItems: 'flex-start'
				}}
			>
				<div>
					<div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 6 }}>
						킬이 난 자리
					</div>
					<MapScatter
						label="킬 위치"
						size={300}
						marks={data.kills
							.filter((k) => k.at)
							.map((k) => ({
								key: `${k.timestampMs}-${k.victimParticipantId}`,
								x: k.at!.x,
								y: k.at!.y,
								teamId: k.killingTeamId,
								title: `${formatClock(k.timestampMs)} · ${nameOf(data, k.killerParticipantId)} → ${nameOf(data, k.victimParticipantId)}`
							}))}
					/>
				</div>

				<TeamFights data={data} />
			</div>
		</section>
	);
}

/** 참가자 번호 -> 사람 이름. 0 은 포탑이나 미니언이 막타를 친 경우다. */
function nameOf(data: MatchTimeline, participantId: number): string {
	if (participantId === 0) return '포탑/미니언';
	const p = data.participants.find((x) => x.participantId === participantId);
	return p ? p.riotId.split('#')[0] : '?';
}

/**
 * 띠에 올릴 사건.
 *
 * 킬은 교전으로 묶이지 않은 것만 올린다 — 난전의 킬을 전부 찍으면 띠가 아이콘으로 덮여
 * 오브젝트가 안 보인다. 묶인 킬은 아래 교전 표가 대신 보여 준다.
 */
function stripEvents(data: MatchTimeline): StripEvent[] {
	const fightSpans = data.teamFights
		.filter((f) => f.isTeamFight)
		.map((f) => [f.startMs, f.endMs] as const);
	const inFight = (ms: number) => fightSpans.some(([from, until]) => ms >= from && ms <= until);

	const objectives: StripEvent[] = data.objectives.map((o) => {
		const Icon = OBJECTIVE_ICON[o.kind];
		return {
			key: `obj-${o.timestampMs}-${o.kind}-${o.lane}`,
			timestampMs: o.timestampMs,
			teamId: o.killingTeamId,
			icon: Icon ? <Icon size={16} /> : null,
			title: `${formatClock(o.timestampMs)} · ${objectiveLabel(o.kind, o.subType, o.lane, o.towerType)}`
		};
	});

	const fights: StripEvent[] = data.teamFights
		.filter((f) => f.isTeamFight)
		.map((f) => ({
			key: `fight-${f.startMs}`,
			timestampMs: f.startMs,
			teamId: f.winnerTeamId,
			icon: <SwordsIcon size={16} />,
			title: `${formatClock(f.startMs)} · 한타 ${f.team100Kills}–${f.team200Kills}`
		}));

	const soloKills: StripEvent[] = data.kills
		.filter((k) => !inFight(k.timestampMs))
		.map((k) => ({
			key: `kill-${k.timestampMs}-${k.victimParticipantId}`,
			timestampMs: k.timestampMs,
			teamId: k.killingTeamId,
			icon: <SkullIcon size={13} />,
			title: `${formatClock(k.timestampMs)} · ${nameOf(data, k.killerParticipantId)} → ${nameOf(data, k.victimParticipantId)}`
		}));

	return [...objectives, ...fights, ...soloKills];
}

function TeamFights({ data }: { data: MatchTimeline }) {
	const fights = data.teamFights.filter((f) => f.isTeamFight);
	if (fights.length === 0) {
		return (
			<div style={{ flex: '1 1 260px' }}>
				<div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 6 }}>
					교전
				</div>
				<p className="t-empty" style={{ textAlign: 'left' }}>
					킬 3개 이상이 몰린 구간이 없었다.
				</p>
			</div>
		);
	}

	const blueWins = fights.filter((f) => f.winnerTeamId === 100).length;
	const redWins = fights.filter((f) => f.winnerTeamId === 200).length;

	return (
		<div style={{ flex: '1 1 300px', minWidth: 0 }}>
			<div style={{ display: 'flex', flexWrap: 'wrap', gap: 24, marginBottom: 12 }}>
				<Stat label="한타" value={`${fights.length}회`} sample="킬 3개 이상이 몰린 구간" />
				<Stat
					label="한타 승"
					value={
						<>
							<span style={{ color: 'var(--color-win)' }}>{blueWins}</span>
							<span className="t-stat-sample"> : </span>
							<span style={{ color: 'var(--color-loss)' }}>{redWins}</span>
						</>
					}
					sample={
						fights.length - blueWins - redWins > 0
							? `${fights.length - blueWins - redWins}회는 교환`
							: undefined
					}
				/>
			</div>

			<div className="t-tablewrap">
				<table className="t-table">
					<thead>
						<tr>
							<th>시각</th>
							<th className="t-num">득실</th>
							<th>결과</th>
							<th>전리품</th>
						</tr>
					</thead>
					<tbody>
						{fights.map((f) => (
							<tr key={f.startMs}>
								<td className="t-stat-sample">{formatClock(f.startMs)}</td>
								<td className="t-num">
									<span style={{ color: 'var(--color-win)' }}>{f.team100Kills}</span>
									{' – '}
									<span style={{ color: 'var(--color-loss)' }}>{f.team200Kills}</span>
								</td>
								<td>
									{f.winnerTeamId == null ? (
										<span className="t-stat-sample">교환</span>
									) : (
										<span
											className={`t-chip ${f.winnerTeamId === 100 ? 't-chip-win' : 't-chip-loss'}`}
										>
											{f.winnerTeamId === 100 ? '블루' : '레드'}
										</span>
									)}
								</td>
								<td className="t-stat-sample">
									{f.objectiveKinds.length === 0
										? '-'
										: f.objectiveKinds.map((k) => objectiveLabel(k, '', '', '')).join(', ')}
								</td>
							</tr>
						))}
					</tbody>
				</table>
			</div>
		</div>
	);
}
