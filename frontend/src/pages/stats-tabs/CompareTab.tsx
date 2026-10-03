import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getPlayerComparison, type PlayerStatSnapshot } from '@/api/stats/playerStatsApi';
import { LoadingCenter } from '../../components/common/Spinner';
import { Button } from '../../components/common/Button';
import type { GameMode } from '@/types';

export default function CompareTab({ mode }: { mode: GameMode }) {
	const [p1Input, setP1Input] = useState('');
	const [p2Input, setP2Input] = useState('');
	// 입력하는 동안에는 조회하지 않는다. '비교' 를 누른 조합만 키가 된다.
	const [pair, setPair] = useState<{ p1: string; p2: string } | null>(null);
	const { data, isFetching: loading } = useQuery({
		queryKey: ['compare', pair?.p1, pair?.p2, mode],
		queryFn: ({ signal }) => getPlayerComparison(pair!.p1, pair!.p2, mode, { signal }),
		enabled: !!pair
	});

	const load = () => {
		const p1 = p1Input.trim();
		const p2 = p2Input.trim();
		if (p1 && p2) setPair({ p1, p2 });
	};

	const StatRow = ({
		label,
		v1,
		v2,
		higher
	}: {
		label: string;
		v1: string | number;
		v2: string | number;
		higher?: 'p1' | 'p2' | 'none';
	}) => (
		<tr>
			<td
				className="table-number"
				style={{
					fontWeight: higher === 'p1' ? 'var(--font-weight-bold)' : 'var(--font-weight-normal)',
					color: higher === 'p1' ? 'var(--color-win)' : 'var(--color-text-primary)'
				}}
			>
				{v1}
			</td>
			<td
				style={{
					textAlign: 'center',
					fontSize: 'var(--font-size-xs)',
					color: 'var(--color-text-secondary)',
					whiteSpace: 'nowrap'
				}}
			>
				{label}
			</td>
			<td
				className="table-number"
				style={{
					fontWeight: higher === 'p2' ? 'var(--font-weight-bold)' : 'var(--font-weight-normal)',
					color: higher === 'p2' ? 'var(--color-win)' : 'var(--color-text-primary)'
				}}
			>
				{v2}
			</td>
		</tr>
	);

	const compareSnap = (
		s1: PlayerStatSnapshot | null,
		s2: PlayerStatSnapshot | null,
		title: string,
		icon: string
	) => {
		if (!s1 && !s2) return null;
		const safe = (v: number | undefined) => v?.toFixed(2) ?? '-';
		const hi = (a: number | undefined, b: number | undefined): 'p1' | 'p2' | 'none' =>
			a == null || b == null ? 'none' : a > b ? 'p1' : a < b ? 'p2' : 'none';
		return (
			<div>
				<div className="section-head">
					<span className="icon-chip-sm icon-chip">{icon}</span>
					<span className="section-head-title">{title}</span>
				</div>
				<div className="table-wrapper">
					<table>
						<thead>
							<tr>
								<th style={{ textAlign: 'center', color: 'var(--color-primary)' }}>
									{data?.player1.split('#')[0]}
								</th>
								<th style={{ textAlign: 'center', color: 'var(--color-text-disabled)' }}>지표</th>
								<th style={{ textAlign: 'center', color: 'var(--color-primary)' }}>
									{data?.player2.split('#')[0]}
								</th>
							</tr>
						</thead>
						<tbody>
							<StatRow
								label="경기수"
								v1={s1?.games ?? '-'}
								v2={s2?.games ?? '-'}
								higher={hi(s1?.games, s2?.games)}
							/>
							<StatRow
								label="승률%"
								v1={s1 ? s1.winRate.toFixed(1) : '-'}
								v2={s2 ? s2.winRate.toFixed(1) : '-'}
								higher={hi(s1?.winRate, s2?.winRate)}
							/>
							<StatRow
								label="KDA"
								v1={s1 ? safe(s1.kda) : '-'}
								v2={s2 ? safe(s2.kda) : '-'}
								higher={hi(s1?.kda, s2?.kda)}
							/>
							<StatRow
								label="평균킬"
								v1={s1 ? safe(s1.avgKills) : '-'}
								v2={s2 ? safe(s2.avgKills) : '-'}
								higher={hi(s1?.avgKills, s2?.avgKills)}
							/>
							<StatRow
								label="평균데스"
								v1={s1 ? safe(s1.avgDeaths) : '-'}
								v2={s2 ? safe(s2.avgDeaths) : '-'}
								higher={hi(s2?.avgDeaths, s1?.avgDeaths)}
							/>
							<StatRow
								label="평균딜"
								v1={s1 ? Math.round(s1.avgDamage).toLocaleString() : '-'}
								v2={s2 ? Math.round(s2.avgDamage).toLocaleString() : '-'}
								higher={hi(s1?.avgDamage, s2?.avgDamage)}
							/>
							<StatRow
								label="평균CS"
								v1={s1 ? safe(s1.avgCs) : '-'}
								v2={s2 ? safe(s2.avgCs) : '-'}
								higher={hi(s1?.avgCs, s2?.avgCs)}
							/>
							<StatRow
								label="시야"
								v1={s1 ? safe(s1.avgVisionScore) : '-'}
								v2={s2 ? safe(s2.avgVisionScore) : '-'}
								higher={hi(s1?.avgVisionScore, s2?.avgVisionScore)}
							/>
						</tbody>
					</table>
				</div>
			</div>
		);
	};

	return (
		<div>
			<div className="grid-16" style={{ marginBottom: 'var(--spacing-md)' }}>
				<input
					value={p1Input}
					onChange={(e) => setP1Input(e.target.value)}
					placeholder="플레이어1 (RiotID#태그)"
					className="col-span-6"
					style={{
						minWidth: 140,
						padding: '7px 10px',
						borderRadius: 'var(--radius-sm)',
						border: '1px solid var(--color-border)',
						background: 'var(--color-bg-secondary)',
						color: 'var(--color-text-primary)',
						fontSize: 'var(--font-size-sm)'
					}}
				/>
				<input
					value={p2Input}
					onChange={(e) => setP2Input(e.target.value)}
					placeholder="플레이어2 (RiotID#태그)"
					className="col-span-6"
					style={{
						minWidth: 140,
						padding: '7px 10px',
						borderRadius: 'var(--radius-sm)',
						border: '1px solid var(--color-border)',
						background: 'var(--color-bg-secondary)',
						color: 'var(--color-text-primary)',
						fontSize: 'var(--font-size-sm)'
					}}
				/>
				<Button
					className="col-span-4"
					variant="primary"
					size="sm"
					onClick={load}
					disabled={loading}
				>
					{loading ? '로딩...' : '비교'}
				</Button>
			</div>

			{loading && <LoadingCenter />}

			{data && !loading && (
				<div>
					<div className="grid-16" style={{ marginBottom: 'var(--spacing-md)' }}>
						<div className="card col-span-8 card-body" style={{ textAlign: 'center' }}>
							<div
								style={{
									fontSize: 'var(--font-size-sm)',
									fontWeight: 'var(--font-weight-bold)',
									color: 'var(--color-primary)'
								}}
							>
								{data.player1.split('#')[0]}
							</div>
							<div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>
								{data.player1}
							</div>
						</div>
						<div className="card col-span-8 card-body" style={{ textAlign: 'center' }}>
							<div
								style={{
									fontSize: 'var(--font-size-sm)',
									fontWeight: 'var(--font-weight-bold)',
									color: 'var(--color-primary)'
								}}
							>
								{data.player2.split('#')[0]}
							</div>
							<div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>
								{data.player2}
							</div>
						</div>
					</div>

					{data.togetherGames > 0 && (
						<div className="card card-body" style={{ marginBottom: 'var(--spacing-sm)' }}>
							<div
								style={{
									fontSize: 'var(--font-size-xs)',
									color: 'var(--color-text-secondary)',
									marginBottom: 'var(--spacing-sm)'
								}}
							>
								함께한 경기:{' '}
								<strong style={{ color: 'var(--color-text-primary)' }}>
									{data.togetherGames}판
								</strong>{' '}
								| 승률:{' '}
								<strong
									style={{
										color: data.togetherWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)'
									}}
								>
									{data.togetherWinRate.toFixed(1)}%
								</strong>
							</div>
							{compareSnap(data.p1TogetherStats, data.p2TogetherStats, '함께 플레이', '')}
						</div>
					)}

					{data.versusGames > 0 && (
						<div className="card card-body" style={{ marginBottom: 'var(--spacing-sm)' }}>
							<div
								style={{
									fontSize: 'var(--font-size-xs)',
									color: 'var(--color-text-secondary)',
									marginBottom: 'var(--spacing-sm)'
								}}
							>
								맞대결:{' '}
								<strong style={{ color: 'var(--color-text-primary)' }}>{data.versusGames}판</strong>{' '}
								| {data.player1.split('#')[0]} 승률:{' '}
								<strong
									style={{
										color: data.player1VsWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)'
									}}
								>
									{data.player1VsWinRate.toFixed(1)}%
								</strong>
							</div>
							{compareSnap(data.p1VersusStats, data.p2VersusStats, '맞대결', '')}
						</div>
					)}

					<div className="card card-body">
						{compareSnap(data.overallP1Stats, data.overallP2Stats, '전체 통계', '')}
					</div>
				</div>
			)}
		</div>
	);
}
