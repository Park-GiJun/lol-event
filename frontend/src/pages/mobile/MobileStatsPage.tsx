import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../lib/api/api';
import type {
  OverviewStats,
  EloLeaderboardResult,
  MvpStatsResult,
  ChampionSynergyResult,
  DuoStatsResult,
  LaneLeaderboardResult,
} from '../../lib/types/stats';
import { useDragon } from '../../context/DragonContext';
import { LoadingCenter } from '../../components/common/Spinner';
import { MobilePlayerListPage } from './MobilePlayerListPage';

type MainTab = '개요' | '랭킹' | 'Elo' | 'MVP' | '시너지' | '듀오' | '라인';
const MAIN_TABS: MainTab[] = ['개요', '랭킹', 'Elo', 'MVP', '시너지', '듀오', '라인'];

const MODES = [
  { value: 'normal', label: '5v5' },
  { value: 'aram', label: '칼바람' },
  { value: 'all', label: '전체' },
];

const LANES = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'] as const;
const LANE_LABELS: Record<string, string> = {
  TOP: 'TOP', JUNGLE: 'JGL', MID: 'MID', BOTTOM: 'BOT', SUPPORT: 'SUP',
};

function eloTier(elo: number) {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return              { label: 'Bronze',     color: '#CD7F32' };
}

export function MobileStatsPage() {
  const [tab, setTab] = useState<MainTab>('개요');
  const [mode, setMode] = useState('normal');

  return (
    <div>
      {/* Main tab bar */}
      <div className="m-tab-bar">
        {MAIN_TABS.map(t => (
          <button key={t} className={`m-tab${tab === t ? ' active' : ''}`} onClick={() => setTab(t)}>
            {t}
          </button>
        ))}
      </div>

      {/* Mode chips (not for ranking tab — that has its own) */}
      {tab !== '랭킹' && (
        <div className="m-sort-chips">
          {MODES.map(m => (
            <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
              {m.label}
            </button>
          ))}
        </div>
      )}

      {tab === '개요'  && <OverviewTab mode={mode} />}
      {tab === '랭킹'  && <MobilePlayerListPage />}
      {tab === 'Elo'   && <EloTab />}
      {tab === 'MVP'   && <MvpTab mode={mode} />}
      {tab === '시너지' && <SynergyTab mode={mode} />}
      {tab === '듀오'  && <DuoTab mode={mode} />}
      {tab === '라인'  && <LaneTab mode={mode} />}
    </div>
  );
}

/* ── Overview ─────────────────────────────────────────────────────────── */
function OverviewTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-overview', mode],
    queryFn: () => api.get<OverviewStats>(`/stats/overview?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const leaders = [
    { label: '승률왕', stat: data.winRateLeader },
    { label: 'KDA왕', stat: data.kdaLeader },
    { label: '킬왕', stat: data.killsLeader },
    { label: '딜왕', stat: data.damageLeader },
  ].filter(l => l.stat);

  return (
    <div>
      {/* Summary */}
      <div className="m-overview-grid" style={{ marginBottom: 12 }}>
        <div className="m-overview-stat">
          <div className="m-overview-stat-value">{data.matchCount}</div>
          <div className="m-overview-stat-label">총 경기</div>
        </div>
        <div className="m-overview-stat">
          <div className="m-overview-stat-value">{data.avgGameMinutes.toFixed(1)}<span style={{ fontSize: 12 }}>분</span></div>
          <div className="m-overview-stat-label">평균 시간</div>
        </div>
      </div>

      {/* Top picks */}
      {data.topPickedChampions.length > 0 && (
        <>
          <p className="m-section-title">인기 챔피언</p>
          <div style={{ display: 'flex', gap: 10, overflowX: 'auto', paddingBottom: 8, marginBottom: 12, scrollbarWidth: 'none' }}>
            {data.topPickedChampions.slice(0, 10).map(cp => {
              const c = Array.from(champions.values()).find(ch => ch.championKey === cp.champion || ch.nameKo === cp.champion);
              return (
                <div key={cp.champion}
                  style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, cursor: 'pointer', flexShrink: 0 }}
                  onClick={() => navigate(`/m/champion/${encodeURIComponent(c?.championKey ?? cp.champion)}`)}
                >
                  {c?.imageUrl ? (
                    <img src={c.imageUrl} alt={c.nameKo} width={48} height={48} style={{ borderRadius: 8, objectFit: 'cover' }} />
                  ) : (
                    <div style={{ width: 48, height: 48, borderRadius: 8, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11 }}>
                      {cp.champion.slice(0, 2)}
                    </div>
                  )}
                  <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{cp.picks}픽</span>
                </div>
              );
            })}
          </div>
        </>
      )}

      {/* Leaders */}
      {leaders.length > 0 && (
        <>
          <p className="m-section-title">명예의 전당</p>
          <div className="m-card">
            {leaders.map(({ label, stat }) => stat && (
              <div key={label} className="m-leader-row"
                onClick={() => navigate(`/m/player/${encodeURIComponent(stat.riotId)}`)}>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 60 }}>{label}</span>
                <span style={{ fontWeight: 700, flex: 1 }}>{stat.riotId.split('#')[0]}</span>
                <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-primary)' }}>{stat.displayValue}</span>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Objectives */}
      <p className="m-section-title" style={{ marginTop: 12 }}>오브젝트</p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
        {[
          { label: '바론', value: data.totalBaronKills },
          { label: '드래곤', value: data.totalDragonKills },
          { label: '포탑', value: data.totalTowerKills },
          { label: '전령', value: data.totalRiftHeraldKills },
          { label: '억제기', value: data.totalInhibitorKills },
          { label: '퍼블', value: data.totalFirstBloods },
        ].map(({ label, value }) => (
          <div key={label} className="m-overview-stat">
            <div className="m-overview-stat-value" style={{ fontSize: 18 }}>{value.toLocaleString()}</div>
            <div className="m-overview-stat-label">{label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ── Elo ──────────────────────────────────────────────────────────────── */
function EloTab() {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-elo'],
    queryFn: () => api.get<EloLeaderboardResult>('/stats/elo'),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.players.map((p, i) => {
        const tier = eloTier(p.elo);
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{p.rank}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
                <div style={{ fontSize: 11, color: tier.color, marginTop: 1 }}>{tier.label}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 18, fontWeight: 800, color: tier.color }}>{p.elo.toFixed(1)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                  {p.games > 0 ? `${p.winRate.toFixed(1)}% · ${p.wins}승 ${p.losses}패` : `${p.games}게임`}
                </div>
                {(p.winStreak >= 3 || p.lossStreak >= 3) && (
                  <div style={{ fontSize: 10, marginTop: 2 }}>
                    {p.winStreak >= 3
                      ? <span style={{ color: '#FF6B2B' }}>🔥 {p.winStreak}연승</span>
                      : <span style={{ color: '#6BAAFF' }}>🧊 {p.lossStreak}연패</span>
                    }
                  </div>
                )}
              </div>
            </div>
          </div>
        );
      })}
      {data.players.length === 0 && <div className="m-empty">Elo 데이터가 없습니다</div>}
    </div>
  );
}

/* ── MVP ──────────────────────────────────────────────────────────────── */
function MvpTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-mvp', mode],
    queryFn: () => api.get<MvpStatsResult>(`/stats/mvp?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
              </div>
              <span className="m-player-games">{p.games}게임</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">MVP {p.mvpCount}회</span>
              <span className="m-stat-chip">ACE {p.aceCount}회</span>
              <span className="m-stat-chip">평균점수 {p.avgMvpScore.toFixed(1)}</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">MVP 데이터가 없습니다</div>}
    </div>
  );
}

/* ── Synergy ──────────────────────────────────────────────────────────── */
function SynergyTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-synergy', mode],
    queryFn: () => api.get<ChampionSynergyResult>(`/stats/synergy?mode=${mode}&minGames=3`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.synergies.map((s, i) => {
        const c1 = champions.get(s.champion1Id);
        const c2 = champions.get(s.champion2Id);
        return (
          <div key={i} className="m-synergy-card">
            <div style={{ display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0 }}>
              {c1?.imageUrl ? (
                <img src={c1.imageUrl} alt={c1.nameKo} width={36} height={36} style={{ borderRadius: 6 }} />
              ) : (
                <div style={{ width: 36, height: 36, borderRadius: 6, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10 }}>
                  {s.champion1.slice(0, 2)}
                </div>
              )}
              <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>+</span>
              {c2?.imageUrl ? (
                <img src={c2.imageUrl} alt={c2.nameKo} width={36} height={36} style={{ borderRadius: 6 }} />
              ) : (
                <div style={{ width: 36, height: 36, borderRadius: 6, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10 }}>
                  {s.champion2.slice(0, 2)}
                </div>
              )}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 12, fontWeight: 600 }}>
                {c1?.nameKo ?? s.champion1} + {c2?.nameKo ?? s.champion2}
              </div>
              <div className="m-stat-chips" style={{ marginTop: 4 }}>
                <span className="m-stat-chip">{s.games}게임</span>
                <span className="m-stat-chip" style={{ color: s.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>
                  {s.winRate.toFixed(1)}%
                </span>
              </div>
            </div>
          </div>
        );
      })}
      {data.synergies.length === 0 && <div className="m-empty">시너지 데이터가 없습니다</div>}
    </div>
  );
}

/* ── Duo ──────────────────────────────────────────────────────────────── */
function DuoTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-duo', mode],
    queryFn: () => api.get<DuoStatsResult>(`/stats/duo?mode=${mode}&minGames=2`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.duos.map((d, i) => {
        const [name1] = d.player1.split('#');
        const [name2] = d.player2.split('#');
        return (
          <div key={i} className="m-synergy-card">
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginBottom: 4 }}>
                <button
                  onClick={() => navigate(`/m/player/${encodeURIComponent(d.player1)}`)}
                  style={{ fontWeight: 700, fontSize: 13, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}
                >
                  {name1}
                </button>
                <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>+</span>
                <button
                  onClick={() => navigate(`/m/player/${encodeURIComponent(d.player2)}`)}
                  style={{ fontWeight: 700, fontSize: 13, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}
                >
                  {name2}
                </button>
              </div>
              <div className="m-stat-chips">
                <span className="m-stat-chip">{d.games}게임</span>
                <span className="m-stat-chip" style={{ color: d.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>
                  {d.winRate.toFixed(1)}%
                </span>
                <span className="m-stat-chip">KDA {d.kda.toFixed(2)}</span>
              </div>
            </div>
          </div>
        );
      })}
      {data.duos.length === 0 && <div className="m-empty">듀오 데이터가 없습니다</div>}
    </div>
  );
}

/* ── Lane ─────────────────────────────────────────────────────────────── */
function LaneTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [lane, setLane] = useState('TOP');
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-lane', lane, mode],
    queryFn: () => api.get<LaneLeaderboardResult>(`/stats/lane?lane=${lane}&mode=${mode}`),
  });

  return (
    <div>
      {/* Lane selector */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 12, flexWrap: 'wrap' }}>
        {LANES.map(l => (
          <button key={l} className={`m-lane-tab${lane === l ? ' active' : ''}`} onClick={() => setLane(l)}>
            {LANE_LABELS[l]}
          </button>
        ))}
      </div>

      {isLoading ? <LoadingCenter /> : (
        <>
          {(!data || data.players.length === 0) && <div className="m-empty">데이터가 없습니다</div>}
          {data?.players.map((p, i) => {
            const [name, tag] = p.riotId.split('#');
            return (
              <div key={p.riotId} className="m-player-card"
                onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
                <div className="m-player-card-header">
                  <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
                  <div style={{ flex: 1 }}>
                    <span className="m-player-name">{name}</span>
                    {tag && <span className="m-player-tag"> #{tag}</span>}
                  </div>
                  <span className="m-player-games">{p.games}판</span>
                </div>
                <div className="m-win-bar-wrap">
                  <div className="m-win-bar-label">
                    <span>{p.winRate.toFixed(1)}%</span>
                    <span>{p.wins}승 {p.games - p.wins}패</span>
                  </div>
                  <div className="m-win-bar">
                    <div className="m-win-bar-fill" style={{ width: `${p.winRate}%` }} />
                  </div>
                </div>
                <div className="m-stat-chips">
                  <span className="m-stat-chip">KDA {p.kda.toFixed(2)}</span>
                  <span className="m-stat-chip">딜 {Math.round(p.avgDamage).toLocaleString()}</span>
                  <span className="m-stat-chip">CS {p.avgCs.toFixed(1)}</span>
                </div>
              </div>
            );
          })}
        </>
      )}
    </div>
  );
}
