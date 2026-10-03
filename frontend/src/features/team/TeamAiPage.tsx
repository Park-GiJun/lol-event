import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ApiError } from '@/api/client';
import { SILENT_META } from '@/api/queryClient';
import { buildTeams, getTeamCandidates, type TeamBuildResult } from '@/api/team/teamBuildApi';
import { Button } from '@/components/common/Button';
import { RichText } from '@/components/common/RichText';
import { LoadingCenter } from '@/components/common/Spinner';
import { CloseIcon, ShuffleIcon } from '@/components/icons/LolIcons';
import { POSITION_ICON } from '@/components/icons/positionIcon';
import { POSITIONS, positionLabel, type Position } from '@/lib/position';
import {
	GROUP_LABELS,
	SIZES,
	entryOf,
	nameOf,
	problemOf,
	requestOf,
	togglePosition,
	type Entry
} from './teamAiState';

/**
 * AI 팀 짜기.
 *
 * 사람을 고르고, 각자 이번 판에 갈 수 있는 포지션과 "같은 팀이어야 하는 묶음" 을 정해 보낸다.
 * 편성은 서버 코드가 계산하고(팀 평균 Elo 를 맞춘다), AI 는 그 편성의 해설을 쓴다.
 */
export function TeamAiPage() {
	const [size, setSize] = useState<number>(10);
	const [entries, setEntries] = useState<Entry[]>([]);
	const [search, setSearch] = useState('');
	const [commentary, setCommentary] = useState(true);
	const [seed, setSeed] = useState(0);

	const { data: candidates, isLoading } = useQuery({
		queryKey: ['team-candidates'],
		queryFn: ({ signal }) => getTeamCandidates({ signal })
	});

	// 조건이 틀렸을 때 서버가 "무엇이" 틀렸는지 말해 준다. 그 문구를 결과 자리에 그대로 보여 준다.
	const build = useMutation({ mutationFn: buildTeams, meta: SILENT_META });

	const picked = useMemo(() => new Set(entries.map((e) => e.riotId)), [entries]);
	const available = useMemo(() => {
		const wanted = search.replace(/\s/g, '').toLowerCase();
		return (candidates ?? []).filter(
			(c) => !picked.has(c.riotId) && c.riotId.replace(/\s/g, '').toLowerCase().includes(wanted)
		);
	}, [candidates, picked, search]);

	const problem = problemOf(entries, size);
	const full = entries.length >= size;

	function update(riotId: string, change: (entry: Entry) => Entry) {
		setEntries((prev) => prev.map((e) => (e.riotId === riotId ? change(e) : e)));
	}

	function run(nextSeed: number) {
		setSeed(nextSeed);
		build.mutate(requestOf(entries, nextSeed, commentary));
	}

	if (isLoading) return <LoadingCenter />;

	return (
		<div className="t-page">
			<div className="t-page-head">
				<h1 className="t-page-title">AI 팀 짜기</h1>
				<p className="t-page-sub">
					팀 평균 라인 Elo 가 비슷해지게 5명씩 나눕니다. 편성은 계산으로 정하고, AI 는 해설을
					씁니다.
				</p>
			</div>

			<section className="t-card team-ai-section">
				<div className="t-card-head">
					<h2 className="t-card-title">인원</h2>
					<div className="t-seg">
						{SIZES.map((n) => (
							<button key={n} className={size === n ? 'active' : ''} onClick={() => setSize(n)}>
								{n}명
							</button>
						))}
					</div>
				</div>
				<p className="team-ai-hint">
					{size / 5}팀 · 고른 사람 {entries.length}/{size}
				</p>

				<input
					className="team-ai-search"
					value={search}
					onChange={(e) => setSearch(e.target.value)}
					placeholder="이름으로 찾기"
					aria-label="플레이어 찾기"
				/>
				<div className="team-ai-pool">
					{available.map((candidate) => (
						<button
							key={candidate.riotId}
							className="team-ai-candidate"
							disabled={full}
							onClick={() => setEntries((prev) => [...prev, entryOf(candidate)])}
							title={`${candidate.riotId} · Elo ${Math.round(candidate.elo)} · ${candidate.games}판`}
						>
							{nameOf(candidate.riotId)}
							<span className="team-ai-candidate-elo">{Math.round(candidate.elo)}</span>
						</button>
					))}
					{available.length === 0 && <span className="team-ai-hint">더 고를 사람이 없습니다.</span>}
				</div>
			</section>

			{entries.length > 0 && (
				<section className="t-card team-ai-section">
					<div className="t-card-head">
						<h2 className="t-card-title">이번 판 조건</h2>
						<button className="t-card-more" onClick={() => setEntries([])}>
							모두 빼기
						</button>
					</div>
					<p className="team-ai-hint">
						갈 수 있는 포지션을 켜 두세요. 처음 값은 지금까지 3판 이상 가 본 자리입니다. 같은 묶음을
						고른 사람끼리는 같은 팀이 됩니다.
					</p>

					<div className="team-ai-entries">
						{entries.map((entry) => (
							<div key={entry.riotId} className="team-ai-entry">
								<div className="team-ai-entry-name">
									{nameOf(entry.riotId)}
									<span className="team-ai-candidate-elo">{Math.round(entry.elo)}</span>
								</div>

								<div className="team-ai-positions" role="group" aria-label="가능 포지션">
									{POSITIONS.map((position) => (
										<PositionToggle
											key={position}
											position={position}
											on={entry.positions.includes(position)}
											games={entry.games[position] ?? 0}
											onToggle={() => update(entry.riotId, (e) => togglePosition(e, position))}
										/>
									))}
								</div>

								<select
									className="team-ai-group"
									value={entry.group ?? ''}
									onChange={(e) =>
										update(entry.riotId, (prev) => ({ ...prev, group: e.target.value || null }))
									}
									aria-label="같은 팀 묶음"
								>
									<option value="">묶음 없음</option>
									{GROUP_LABELS.map((label) => (
										<option key={label} value={label}>
											묶음 {label}
										</option>
									))}
								</select>

								<button
									className="t-iconbtn"
									onClick={() =>
										setEntries((prev) => prev.filter((e) => e.riotId !== entry.riotId))
									}
									aria-label={`${nameOf(entry.riotId)} 빼기`}
								>
									<CloseIcon size={16} />
								</button>
							</div>
						))}
					</div>

					<div className="team-ai-actions">
						<label className="team-ai-check">
							<input
								type="checkbox"
								checked={commentary}
								onChange={(e) => setCommentary(e.target.checked)}
							/>
							AI 해설 받기 (수십 초 걸립니다)
						</label>
						<Button onClick={() => run(0)} disabled={problem !== null} loading={build.isPending}>
							<ShuffleIcon size={15} /> 팀 짜기
						</Button>
						{build.data && (
							<Button
								variant="secondary"
								onClick={() => run(seed + 1)}
								disabled={problem !== null || build.isPending}
							>
								다른 편성 보기
							</Button>
						)}
					</div>
					{problem && <p className="team-ai-problem">{problem}</p>}
				</section>
			)}

			{build.isPending && (
				<section className="t-card team-ai-section" role="status">
					<p className="team-ai-hint">
						{commentary ? '편성을 계산하고 AI 해설을 쓰는 중입니다…' : '편성을 계산하는 중입니다…'}
					</p>
				</section>
			)}

			{build.isError && !build.isPending && (
				<section className="t-card team-ai-section" role="alert">
					<p className="team-ai-problem">
						{build.error instanceof ApiError ? build.error.message : '팀을 짜지 못했습니다.'}
					</p>
				</section>
			)}

			{build.data && !build.isPending && <TeamBuildView result={build.data} />}
		</div>
	);
}

function PositionToggle({
	position,
	on,
	games,
	onToggle
}: {
	position: Position;
	on: boolean;
	games: number;
	onToggle: () => void;
}) {
	const Icon = POSITION_ICON[position];
	return (
		<button
			className={`team-ai-position${on ? ' on' : ''}`}
			onClick={onToggle}
			aria-pressed={on}
			title={`${positionLabel(position)} · ${games}판`}
		>
			<Icon size={16} />
			<span>{games}</span>
		</button>
	);
}

function TeamBuildView({ result }: { result: TeamBuildResult }) {
	return (
		<>
			<section className="t-card team-ai-section">
				<div className="t-card-head">
					<h2 className="t-card-title">편성</h2>
					<span className="t-chip">팀 평균 Elo 차 {Math.round(result.eloSpread)}</span>
				</div>
				{result.positionConflict && (
					<p className="team-ai-problem">
						가능 포지션만으로는 자리를 다 채울 수 없어서, 표시된 사람은 가능 포지션이 아닌 자리에
						앉았습니다.
					</p>
				)}

				<div className="team-ai-teams">
					{result.teams.map((team) => (
						<div key={team.name} className="team-ai-team">
							<div className="team-ai-team-head">
								<strong>{team.name}</strong>
								<span>평균 {Math.round(team.averageElo)}</span>
								<span className="t-chip t-chip-blue">
									기대 승률 {Math.round(team.winProbability * 100)}%
								</span>
							</div>
							{team.members.map((member) => {
								const Icon = POSITION_ICON[member.position];
								return (
									<div key={member.riotId} className="team-ai-member">
										<Icon size={16} />
										<span className="team-ai-member-lane">{positionLabel(member.position)}</span>
										<span className="team-ai-member-name">{nameOf(member.riotId)}</span>
										{member.offRole && <span className="t-chip t-chip-low">가능 포지션 아님</span>}
										<span className="team-ai-candidate-elo">{Math.round(member.elo)}</span>
									</div>
								);
							})}
						</div>
					))}
				</div>
			</section>

			{(result.commentary || result.commentaryError) && (
				<section className="t-card team-ai-section">
					<div className="t-card-head">
						<h2 className="t-card-title">AI 해설</h2>
					</div>
					{result.commentary ? (
						<RichText text={result.commentary} />
					) : (
						<p className="team-ai-hint">
							해설은 만들지 못했습니다 ({result.commentaryError}). 편성은 그대로 쓸 수 있습니다.
						</p>
					)}
				</section>
			)}
		</>
	);
}
