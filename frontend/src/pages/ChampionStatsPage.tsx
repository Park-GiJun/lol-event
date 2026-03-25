import { useEffect, useState, useCallback, useMemo, lazy, Suspense, Component } from 'react';
import type { ReactNode } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../lib/api/api';
import type { ChampionDetailStats, ChampionPlayerStat, ChampionLaneStat } from '../lib/types/stats';
import { LoadingCenter } from '../components/common/Spinner';
import { Skeleton } from '../components/common/Skeleton';
import { ChartSkeleton } from '../components/common/ChartSkeleton';
import { useChampions } from '../hooks/useChampions';

class ChartErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
  constructor(props: { children: ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError() { return { hasError: true }; }
  render() {
    if (this.state.hasError) return null;
    return this.props.children;
  }
}

const ChampionChartsSection = lazy(
  () => import('../components/dashboard/ChampionChartsSection')
    .then(m => ({ default: m.ChampionChartsSection }))
);
import { Button } from '../components/common/Button';
import { BreadcrumbNav } from '../components/common/BreadcrumbNav';
import { useDragon } from '../context/DragonContext';
import { PlayerLink } from '../components/common/PlayerLink';
import '../styles/pages/stats.css';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

const CDN = 'https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons';

interface MatchupStat { opponent: string; opponentId: number; games: number; wins: number; winRate: number; }
interface ChampionMatchupResult { champion: string; championId: number; matchups: MatchupStat[]; }

function ChampIcon({ id, size = 28 }: { id: number; size?: number }) {
  if (!id) return <div style={{ width: size, height: size, borderRadius: 4, background: 'var(--color-bg-hover)' }} />;
  return (
    <img
      src={`${CDN}/${id}.png`}
      alt=""
      width={size} height={size}
      style={{ borderRadius: 4, objectFit: 'cover', flexShrink: 0 }}
      onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
    />
  );
}

function ChampionMatchupSection({ champion, mode }: { champion: string; mode: string }) {
  const [data, setData] = useState<ChampionMatchupResult | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api.get<ChampionMatchupResult>(
      `/stats/matchup?champion=${encodeURIComponent(champion)}&mode=${mode}&samePosition=true`
    )
      .then(res => setData(res))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [champion, mode]);

  if (loading) return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', marginBottom: 12 }}>같은 라인 상대 챔피언 승률</div>
      <div style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>불러오는 중...</div>
    </div>
  );

  const matchups = data?.matchups ?? [];

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', marginBottom: 12 }}>
        같은 라인 상대 챔피언 승률
        <span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-secondary)', marginLeft: 8 }}>
          동일 포지션 기준 · 2판 이상
        </span>
      </div>
      {matchups.length === 0 ? (
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>데이터 없음</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 8 }}>
          {matchups.map(m => {
            const color = m.winRate >= 60 ? 'var(--color-win)' : m.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
            return (
              <div key={m.opponent} style={{
                display: 'flex', alignItems: 'center', gap: 8,
                padding: '8px 10px', borderRadius: 'var(--radius-sm)',
                background: 'var(--color-bg-hover)', border: '1px solid var(--color-border)',
              }}>
                <ChampIcon id={m.opponentId} size={32} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 600, fontSize: 12, marginBottom: 3 }}>{m.opponent}</div>
                  <div style={{ height: 4, background: 'var(--color-bg-card)', borderRadius: 2, overflow: 'hidden', marginBottom: 3 }}>
                    <div style={{ width: `${m.winRate}%`, height: '100%', background: color, borderRadius: 2 }} />
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
                    <span style={{ fontWeight: 700, color }}>{m.winRate}%</span>
                    <span style={{ marginLeft: 4 }}>{m.wins}승 {m.games - m.wins}패 ({m.games}판)</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

const SORT_COLS: { key: keyof ChampionPlayerStat; label: string }[] = [
  { key: 'games',          label: '판수' },
  { key: 'winRate',        label: '승률' },
  { key: 'kda',            label: 'KDA' },
  { key: 'avgDamage',      label: '딜량' },
  { key: 'avgVisionScore', label: '시야' },
  { key: 'avgCs',          label: 'CS' },
];

const LANE_META: Record<string, { label: string; emoji: string }> = {
  TOP:     { label: '탑',   emoji: '🛡️' },
  JUNGLE:  { label: '정글', emoji: '🌲' },
  MID:     { label: '미드', emoji: '⚡' },
  BOTTOM:  { label: '원딜', emoji: '🏹' },
  SUPPORT: { label: '서폿', emoji: '💫' },
};

function ChampionLaneStats({ laneStats }: { laneStats: ChampionLaneStat[] }) {
  const [selected, setSelected] = useState<string>(laneStats[0]?.position ?? '');
  if (laneStats.length === 0) return null;
  const stat = laneStats.find(s => s.position === selected);
  const wrColor = (wr: number) => wr >= 60 ? 'var(--color-win)' : wr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', marginBottom: 12 }}>포지션별 통계</div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 14, flexWrap: 'wrap' }}>
        {laneStats.map(s => {
          const m = LANE_META[s.position] ?? { label: s.position, emoji: '' };
          const wr = s.winRate;
          const c = wr >= 60 ? 'var(--color-win)' : wr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
          return (
            <button key={s.position}
              className={`lane-tab ${selected === s.position ? 'active' : ''}`}
              onClick={() => setSelected(s.position)}
              style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, padding: '8px 14px', minWidth: 72 }}>
              <span style={{ fontSize: 16 }}>{m.emoji}</span>
              <span className="lane-tab-label">{m.label}</span>
              <span style={{ fontSize: 10, fontWeight: 700, color: c }}>{s.winRate}%</span>
              <span className="lane-tab-games">{s.games}판</span>
            </button>
          );
        })}
      </div>
      {stat && (
        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', alignItems: 'flex-start' }}>
          {/* 승률 */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, minWidth: 80 }}>
            <div style={{ fontSize: 28, fontWeight: 800, color: wrColor(stat.winRate) }}>{stat.winRate}%</div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{stat.wins}승 {stat.games - stat.wins}패</div>
            <div style={{ height: 5, width: 70, background: 'var(--color-bg-hover)', borderRadius: 3, overflow: 'hidden' }}>
              <div style={{ width: `${stat.winRate}%`, height: '100%', background: wrColor(stat.winRate), borderRadius: 3 }} />
            </div>
          </div>
          {/* KDA */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <div style={{ fontSize: 22, fontWeight: 800 }}>{stat.kda.toFixed(2)} KDA</div>
            <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
              {stat.avgKills.toFixed(1)} /&nbsp;
              <span style={{ color: 'var(--color-error)' }}>{stat.avgDeaths.toFixed(1)}</span>
              &nbsp;/ {stat.avgAssists.toFixed(1)}
            </div>
          </div>
          {/* 기타 스탯 */}
          {[
            { label: '평균 딜량', value: stat.avgDamage.toLocaleString() },
            { label: '평균 CS',   value: stat.avgCs.toFixed(1) },
            { label: '평균 골드', value: stat.avgGold.toLocaleString() },
          ].map(({ label, value }) => (
            <div key={label} style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <div style={{ fontSize: 18, fontWeight: 700 }}>{value}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{label}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const RANK_COLORS: Record<number, string> = { 1: '#FFD700', 2: '#C0C0C0', 3: '#CD7F32' };

function RankBadge({ rank }: { rank: number }) {
  const color = RANK_COLORS[rank];
  if (color) return (
    <div style={{
      width: 24, height: 24, borderRadius: '50%',
      background: color, display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: 11, fontWeight: 800, color: '#111', flexShrink: 0,
    }}>{rank}</div>
  );
  return <span style={{ color: 'var(--color-text-disabled)', fontSize: 12, width: 24, textAlign: 'center' }}>{rank}</span>;
}

function WinRatePill({ winRate, wins, losses }: { winRate: number; wins: number; losses: number }) {
  const color = winRate >= 60 ? 'var(--color-win)' : winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 90 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ fontWeight: 700, fontSize: 13, color, minWidth: 36 }}>{winRate}%</span>
        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
          <span style={{ color: 'var(--color-win)' }}>{wins}W</span>
          {' '}<span style={{ color: 'var(--color-loss)' }}>{losses}L</span>
        </span>
      </div>
      <div style={{ height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
        <div style={{ width: `${winRate}%`, height: '100%', background: color, borderRadius: 2, transition: 'width 0.3s' }} />
      </div>
    </div>
  );
}

function KdaDisplay({ kda, kills, deaths, assists }: { kda: number; kills: number; deaths: number; assists: number }) {
  const color = kda >= 5 ? 'var(--color-win)' : kda >= 3 ? 'var(--color-primary)' : 'var(--color-text-primary)';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
      <span style={{ fontWeight: 700, fontSize: 13, color }}>{kda.toFixed(2)}</span>
      <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
        {kills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{deaths.toFixed(1)}</span> / {assists.toFixed(1)}
      </span>
    </div>
  );
}

export function ChampionStatsPage() {
  const { champion } = useParams<{ champion: string }>();
  const navigate = useNavigate();
  const { champions, items } = useDragon();

  const [data, setData]       = useState<ChampionDetailStats | null>(null);
  const [mode, setMode]       = useState('normal');
  const [loading, setLoading] = useState(true);
  const [sort, setSort]       = useState<keyof ChampionPlayerStat>('games');

  const { data: overviewData } = useChampions();

  const overallKda = useMemo(() => {
    if (!data || data.laneStats.length === 0) return null;
    const totalGamesLane = data.laneStats.reduce((s, l) => s + l.games, 0);
    if (totalGamesLane === 0) return null;
    return data.laneStats.reduce((s, l) => s + l.kda * l.games, 0) / totalGamesLane;
  }, [data]);

  const pickRate = useMemo(() => {
    if (!overviewData || !champion) return null;
    const champStat = overviewData.topPickedChampions.find(c => c.champion === champion);
    if (!champStat || overviewData.matchCount === 0) return null;
    return (champStat.picks / overviewData.matchCount * 100).toFixed(1);
  }, [overviewData, champion]);

  const load = useCallback(async () => {
    if (!champion) return;
    setLoading(true);
    try {
      setData(await api.get<ChampionDetailStats>(
        `/stats/champion/${encodeURIComponent(champion)}?mode=${mode}`
      ));
    } finally { setLoading(false); }
  }, [champion, mode]);

  useEffect(() => { load(); }, [load]);

  const sorted = data?.players
    ? [...data.players].sort((a, b) => (b[sort] as number) - (a[sort] as number))
    : [];

  const champImg = data ? champions.get(data.championId)?.imageUrl : null;

  return (
    <div>
      {/* Breadcrumb */}
      <BreadcrumbNav items={[
        { label: '홈', path: '/' },
        { label: '챔피언', path: '/champions' },
        { label: (data?.championId && champions.get(data.championId)?.nameKo) || (champion ?? '') },
      ]} />
      {/* 헤더 */}
      <div className="page-header flex items-center justify-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {champImg && (
            <img
              src={champImg}
              alt={data?.champion}
              style={{ width: 52, height: 52, borderRadius: 8, border: '2px solid var(--color-border)', objectFit: 'cover' }}
              onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
            />
          )}
          <div>
            <h1 className="page-title">
              {(data?.championId && champions.get(data.championId)?.nameKo) || data?.champion || champion} 장인 랭킹
            </h1>
            <p className="page-subtitle">
              총 {data?.totalGames ?? 0}경기 · 승률 {data?.winRate ?? 0}% · {sorted.length}명
            </p>
          </div>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {/* 수치 카드 */}
      {loading ? (
        <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
              <Skeleton className="h-7 w-16 mb-1" />
              <Skeleton className="h-3 w-10" />
            </div>
          ))}
        </div>
      ) : data ? (
        <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
          {/* 승률 */}
          <div className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
            <div className="font-mono" style={{
              fontSize: 22, fontWeight: 800,
              color: data.winRate >= 60 ? 'var(--color-win)' : data.winRate < 50 ? 'var(--color-loss)' : 'var(--color-primary)',
            }}>
              {data.winRate.toFixed(1)}%
            </div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>승률</div>
          </div>
          {/* 총 판수 */}
          <div className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
            <div className="font-mono" style={{ fontSize: 22, fontWeight: 800 }}>
              {data.totalGames}
            </div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>총 판수</div>
          </div>
          {/* 전체 KDA */}
          <div className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
            <div className="font-mono" style={{ fontSize: 22, fontWeight: 800 }}>
              {overallKda !== null ? overallKda.toFixed(2) : '—'}
            </div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>전체 KDA</div>
          </div>
          {/* 픽률 */}
          <div className="card" style={{ minWidth: 110, flex: '1 1 110px', padding: '12px 16px' }}>
            <div className="font-mono" style={{ fontSize: 22, fontWeight: 800 }}>
              {pickRate !== null ? `${pickRate}%` : '—'}
            </div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>픽률</div>
          </div>
        </div>
      ) : null}

      {loading ? <LoadingCenter /> : (
        <>
        {/* 인기 아이템 */}
        {data && data.itemStats.length > 0 && (
          <div className="card" style={{ marginBottom: 16 }}>
            <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)', marginBottom: 12 }}>
              인기 아이템 <span style={{ fontSize: 11, fontWeight: 400, color: 'var(--color-text-secondary)' }}>픽률 기준</span>
            </div>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
              {data.itemStats.map((item, idx) => {
                const itemData = items.get(item.itemId);
                const wrColor  = item.winRate >= 60 ? 'var(--color-win)' : item.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
                return (
                  <div key={item.itemId} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                    <div style={{ position: 'relative' }}>
                      {idx === 0 && (
                        <div style={{
                          position: 'absolute', top: -8, left: '50%', transform: 'translateX(-50%)',
                          fontSize: 9, fontWeight: 700, color: '#FFD700', whiteSpace: 'nowrap',
                        }}>1위</div>
                      )}
                      {itemData?.imageUrl
                        ? <img src={itemData.imageUrl} alt={itemData.nameKo}
                            style={{ width: 44, height: 44, borderRadius: 6, border: `2px solid ${idx === 0 ? '#FFD700' : 'var(--color-border)'}`, objectFit: 'cover' }}
                            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                        : <div style={{ width: 44, height: 44, borderRadius: 6, background: 'var(--color-bg-hover)', border: '2px solid var(--color-border)' }} />
                      }
                    </div>
                    <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', textAlign: 'center', maxWidth: 52, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {itemData?.nameKo ?? `#${item.itemId}`}
                    </div>
                    <div style={{ fontSize: 11, fontWeight: 700, color: wrColor }}>{item.winRate}%</div>
                    <div style={{ fontSize: 9, color: 'var(--color-text-disabled)' }}>{item.picks}픽</div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 라인별 통계 */}
        {data && data.laneStats?.length > 0 && (
          <ChampionLaneStats laneStats={data.laneStats} />
        )}

        {/* 같은 라인 상대 챔피언 승률 */}
        {champion && <ChampionMatchupSection champion={champion} mode={mode} />}

        {/* 포지션별 차트 (Story 3.2: React.lazy — Chart.js 동적 로딩) */}
        {data && data.laneStats?.length > 0 && (
          <ChartErrorBoundary>
            <Suspense fallback={<ChartSkeleton />}>
              <ChampionChartsSection
                key={`${champion}-${mode}`}
                laneStats={data.laneStats}
              />
            </Suspense>
          </ChartErrorBoundary>
        )}

        <div className="card">
          {/* 정렬 탭 */}
          <div className="member-sort-tabs">
            {SORT_COLS.map(c => (
              <button key={c.key}
                className={`member-sort-tab ${sort === c.key ? 'active' : ''}`}
                onClick={() => setSort(c.key)}>
                {c.label}
              </button>
            ))}
          </div>

          <div className="table-wrapper">
            <table className="table member-stats-table">
              <thead>
                <tr>
                  <th style={{ width: 36 }}>#</th>
                  <th>플레이어</th>
                  <th>판수</th>
                  <th style={{ minWidth: 120 }}>승률</th>
                  <th>KDA</th>
                  <th className="table-number">평균 딜</th>
                  <th className="table-number">시야</th>
                  <th className="table-number">CS</th>
                  <th className="table-number">골드</th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((p, i) => (
                  <tr key={p.riotId}
                    className="member-stats-row"
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                    <td><RankBadge rank={i + 1} /></td>
                    <td>
                      <PlayerLink riotId={p.riotId} mode={mode}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                          <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</span>
                        </div>
                      </PlayerLink>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, fontSize: 14 }}>{p.games}</span>
                      <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>판</span>
                    </td>
                    <td><WinRatePill winRate={p.winRate} wins={p.wins} losses={p.games - p.wins} /></td>
                    <td><KdaDisplay kda={p.kda} kills={p.avgKills} deaths={p.avgDeaths} assists={p.avgAssists} /></td>
                    <td className="table-number" style={{ fontSize: 12 }}>{p.avgDamage.toLocaleString()}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{p.avgVisionScore.toFixed(1)}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{p.avgCs.toFixed(1)}</td>
                    <td className="table-number" style={{ fontSize: 12 }}>{p.avgGold.toLocaleString()}</td>
                  </tr>
                ))}
                {!sorted.length && (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '48px 0', color: 'var(--color-text-secondary)' }}>
                      데이터 없음
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
        </>
      )}
    </div>
  );
}
