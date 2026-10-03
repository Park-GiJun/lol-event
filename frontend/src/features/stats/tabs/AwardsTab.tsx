import { useNavigate } from 'react-router-dom';
import { type WeeklyAwardsResult, getWeeklyAwards } from '@/api/stats/overviewStatsApi';
import { LoadingCenter } from '@/components/common/Spinner';
import { useQuery } from '@tanstack/react-query';
import type { GameMode } from '@/types';

const AWARD_LABELS: Record<string, string> = {
	mostDeaths: '단일 경기 최다 사망',
	worstKda: '평균 KDA 최하위',
	highGoldLowDamage: '먹튀 골드왕',
	mostSurrenders: '항복 유발자',
	pentaKillHero: '펜타킬 영웅',
	loneHero: '그래도 난 했다',
	highestWinRate: '승률 1위',
	mostGamesChampion: '챔피언 장인'
};

export default function AwardsTab({ mode }: { mode: GameMode }) {
	const navigate = useNavigate();
	const { data, isLoading: loading } = useQuery({
		queryKey: ['awards', mode],
		queryFn: ({ signal }) => getWeeklyAwards(mode, { signal })
	});

	if (loading) return <LoadingCenter />;
	if (!data) return null;

	const entries = Object.entries(AWARD_LABELS) as [keyof WeeklyAwardsResult, string][];

	return (
		<div>
			<div className="hero-banner" style={{ marginBottom: 'var(--spacing-lg)' }}>
				<div className="hero-eyebrow">Weekly Awards</div>
				<h2 className="hero-title">내전 어워즈</h2>
				<p className="hero-subtitle">명예(?)의 전당</p>
			</div>
			<div
				style={{
					display: 'grid',
					gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
					gap: 'var(--spacing-md)'
				}}
			>
				{entries.map(([key, label]) => {
					const entry = data[key];
					return (
						<div
							key={key}
							className={entry ? 'card clickable' : 'card'}
							onClick={
								entry
									? () => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)
									: undefined
							}
						>
							<div className="stat-card-label" style={{ marginBottom: 'var(--spacing-sm)' }}>
								{label}
							</div>
							{entry ? (
								<>
									<div
										style={{
											fontWeight: 'var(--font-weight-bold)',
											fontSize: 'var(--font-size-md)',
											color: 'var(--color-text-primary)'
										}}
									>
										{entry.riotId.split('#')[0]}
									</div>
									<div
										className="stat-card-value"
										style={{ fontSize: 'var(--font-size-xl)', marginTop: 'var(--spacing-xs)' }}
									>
										{entry.displayValue}
									</div>
									<div
										style={{
											fontSize: 'var(--font-size-xs)',
											color: 'var(--color-text-disabled)',
											marginTop: '2px'
										}}
									>
										{entry.games}판
									</div>
								</>
							) : (
								<div
									style={{
										color: 'var(--color-text-disabled)',
										fontSize: 'var(--font-size-sm)',
										marginTop: 'var(--spacing-xs)'
									}}
								>
									데이터 없음
								</div>
							)}
						</div>
					);
				})}
			</div>
		</div>
	);
}
