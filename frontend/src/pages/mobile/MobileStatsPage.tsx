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
  WeeklyAwardsResult,
  DefeatContributionResult,
  MultiKillHighlightsResult,
  ChaosMatchResult,
  SurvivalIndexResult,
  JungleDominanceResult,
  SupportImpactResult,
  RivalMatchupResult,
  TeamChemistryResult,
  PositionBadgeResult,
  ChampionCertificateResult,
  PlaystyleDnaResult,
  MetaShiftResult,
  PlayerComparisonResult,
  SessionReportResult,
  ChampionTierResult,
  GameLengthTendencyResult,
  EarlyGameDominanceResult,
  ComebackIndexResult,
  GoldEfficiencyResult,
  GrowthCurveResult,
} from '../../lib/types/stats';
import { useDragon } from '../../context/DragonContext';
import { LoadingCenter } from '../../components/common/Spinner';
import { MobilePlayerListPage } from './MobilePlayerListPage';

type MainTab = '개요' | '랭킹' | 'Elo' | 'MVP' | '시너지' | '듀오' | '라인' | '어워즈' | '팀분석' | '장인' | '개인분석';
const MAIN_TABS: MainTab[] = ['개요', '랭킹', 'Elo', 'MVP', '시너지', '듀오', '라인', '어워즈', '팀분석', '장인', '개인분석'];

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

const noModeTabGroups: MainTab[] = ['랭킹', 'Elo', '어워즈', '팀분석', '장인', '개인분석'];

export function MobileStatsPage() {
  const [tab, setTab] = useState<MainTab>('개요');
  const [mode, setMode] = useState('normal');

  return (
    <div>
      {/* Main tab bar */}
      <div className="m-tab-bar" style={{ overflowX: 'auto', scrollbarWidth: 'none', flexWrap: 'nowrap' }}>
        {MAIN_TABS.map(t => (
          <button key={t} className={`m-tab${tab === t ? ' active' : ''}`} onClick={() => setTab(t)}
            style={{ flexShrink: 0 }}>
            {t}
          </button>
        ))}
      </div>

      {/* Mode chips */}
      {!noModeTabGroups.includes(tab) && (
        <div className="m-sort-chips">
          {MODES.map(m => (
            <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
              {m.label}
            </button>
          ))}
        </div>
      )}

      {tab === '개요'    && <OverviewTab mode={mode} />}
      {tab === '랭킹'    && <MobilePlayerListPage />}
      {tab === 'Elo'     && <EloTab />}
      {tab === 'MVP'     && <MvpTab mode={mode} />}
      {tab === '시너지'  && <SynergyTab mode={mode} />}
      {tab === '듀오'    && <DuoTab mode={mode} />}
      {tab === '라인'    && <LaneTab mode={mode} />}
      {tab === '어워즈'  && <AwardsGroup />}
      {tab === '팀분석'  && <TeamAnalysisGroup />}
      {tab === '장인'    && <MasteryGroup />}
      {tab === '개인분석' && <PersonalAnalysisGroup />}
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

/* ══════════════════════════════════════════════════════════════════════ */
/* ── 어워즈 그룹 ─────────────────────────────────────────────────────── */
/* ══════════════════════════════════════════════════════════════════════ */
type AwardsSubTab = '주간어워즈' | '떡락지수' | '멀티킬' | '혼돈경기' | '초반지배' | '컴백지수';
const AWARDS_SUB_TABS: AwardsSubTab[] = ['주간어워즈', '떡락지수', '멀티킬', '혼돈경기', '초반지배', '컴백지수'];

function AwardsGroup() {
  const [sub, setSub] = useState<AwardsSubTab>('주간어워즈');
  const [mode, setMode] = useState('normal');

  return (
    <div>
      <div className="m-sort-chips" style={{ marginBottom: 0 }}>
        {MODES.map(m => (
          <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
            {m.label}
          </button>
        ))}
      </div>
      <div className="m-tab-bar" style={{ overflowX: 'auto', scrollbarWidth: 'none', flexWrap: 'nowrap' }}>
        {AWARDS_SUB_TABS.map(t => (
          <button key={t} className={`m-tab${sub === t ? ' active' : ''}`} onClick={() => setSub(t)} style={{ flexShrink: 0, fontSize: 12 }}>
            {t}
          </button>
        ))}
      </div>
      {sub === '주간어워즈' && <WeeklyAwardsTab mode={mode} />}
      {sub === '떡락지수'   && <DefeatContribTab mode={mode} />}
      {sub === '멀티킬'     && <MultiKillTab mode={mode} />}
      {sub === '혼돈경기'   && <ChaosMatchTab mode={mode} />}
      {sub === '초반지배'   && <EarlyGameTab mode={mode} />}
      {sub === '컴백지수'   && <ComebackTab mode={mode} />}
    </div>
  );
}

/* ── 주간 어워즈 ──────────────────────────────────────────────────────── */
function WeeklyAwardsTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-awards', mode],
    queryFn: () => api.get<WeeklyAwardsResult>(`/stats/awards?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const awards = [
    { emoji: '💀', label: '데스왕',      entry: data.mostDeaths },
    { emoji: '📉', label: '최저 KDA',    entry: data.worstKda },
    { emoji: '💰', label: '골드낭비',    entry: data.highGoldLowDamage },
    { emoji: '🏳️',  label: '항복왕',     entry: data.mostSurrenders },
    { emoji: '⚔️',  label: '펜타킬',    entry: data.pentaKillHero },
    { emoji: '🦸',  label: '혼자싸움',   entry: data.loneHero },
    { emoji: '🏆', label: '승률왕',      entry: data.highestWinRate },
    { emoji: '🎯', label: '원챔장인',    entry: data.mostGamesChampion },
  ];

  return (
    <div>
      <p className="m-section-title">이번 기간 어워즈</p>
      <div className="m-card">
        {awards.map(({ emoji, label, entry }) => entry && (
          <div key={label} className="m-leader-row"
            onClick={() => navigate(`/m/player/${encodeURIComponent(entry.riotId)}`)}>
            <span style={{ fontSize: 16, width: 28 }}>{emoji}</span>
            <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>{label}</span>
            <span style={{ fontWeight: 700, flex: 1 }}>{entry.riotId.split('#')[0]}</span>
            <span style={{ fontSize: 12, color: 'var(--color-primary)' }}>{entry.displayValue}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ── 떡락 지수 ────────────────────────────────────────────────────────── */
function DefeatContribTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-defeat', mode],
    queryFn: () => api.get<DefeatContributionResult>(`/stats/defeat-contribution?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">패배 기여도 순위 (높을수록 패배에 기여)</p>
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
              <span style={{ fontSize: 16, fontWeight: 800, color: '#FF6B6B' }}>{p.avgDefeatScore.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임 {p.losses}패</span>
              <span className="m-stat-chip">데스 {p.avgDeaths.toFixed(1)}</span>
              <span className="m-stat-chip">딜 {Math.round(p.avgDamage).toLocaleString()}</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

/* ── 멀티킬 하이라이트 ─────────────────────────────────────────────────── */
function MultiKillTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-multikill', mode],
    queryFn: () => api.get<MultiKillHighlightsResult>(`/stats/multikill-highlights?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const KILL_COLOR: Record<string, string> = {
    PENTA: '#FFD700', QUADRA: '#AA47BC', TRIPLE: '#4A9EFF', DOUBLE: '#4CAF50',
  };

  return (
    <div>
      {data.pentaKillEvents.length > 0 && (
        <>
          <p className="m-section-title">펜타킬 순간들</p>
          {data.pentaKillEvents.map((e, i) => {
            const c = champions.get(e.championId);
            const date = new Date(e.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
            return (
              <div key={i} className="m-synergy-card">
                {c?.imageUrl ? (
                  <img src={c.imageUrl} alt={c.nameKo} width={40} height={40} style={{ borderRadius: 8, flexShrink: 0 }} />
                ) : (
                  <div style={{ width: 40, height: 40, borderRadius: 8, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, flexShrink: 0 }}>
                    {e.champion.slice(0, 2)}
                  </div>
                )}
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <button onClick={() => navigate(`/m/player/${encodeURIComponent(e.riotId)}`)}
                      style={{ fontWeight: 700, fontSize: 13, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}>
                      {e.riotId.split('#')[0]}
                    </button>
                    <span style={{ fontSize: 11, fontWeight: 700, color: KILL_COLOR[e.multiKillType] ?? 'var(--color-primary)' }}>
                      {e.multiKillType === 'PENTA' ? '펜타킬!' : e.multiKillType}
                    </span>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                    {c?.nameKo ?? e.champion} · {date}
                  </div>
                </div>
              </div>
            );
          })}
        </>
      )}

      <p className="m-section-title" style={{ marginTop: 12 }}>플레이어별 멀티킬</p>
      {data.playerRankings.map((p, i) => {
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
            </div>
            <div className="m-stat-chips">
              {p.pentaKills > 0 && <span className="m-stat-chip" style={{ color: '#FFD700' }}>펜타 {p.pentaKills}</span>}
              {p.quadraKills > 0 && <span className="m-stat-chip" style={{ color: '#AA47BC' }}>쿼드라 {p.quadraKills}</span>}
              {p.tripleKills > 0 && <span className="m-stat-chip" style={{ color: '#4A9EFF' }}>트리플 {p.tripleKills}</span>}
              <span className="m-stat-chip">더블 {p.doubleKills}</span>
            </div>
          </div>
        );
      })}
      {data.playerRankings.length === 0 && <div className="m-empty">멀티킬 데이터가 없습니다</div>}
    </div>
  );
}

/* ── 혼돈 경기 ────────────────────────────────────────────────────────── */
function ChaosMatchTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-chaos', mode],
    queryFn: () => api.get<ChaosMatchResult>(`/stats/chaos-match?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const sections = [
    { label: '혼돈 TOP', matches: data.topChaosMatches, color: '#FF6B2B' },
    { label: '학살전 TOP', matches: data.topBloodBathMatches, color: '#FF4757' },
    { label: '전략전 TOP', matches: data.topStrategicMatches, color: '#2196F3' },
  ];

  return (
    <div>
      <div className="m-card" style={{ marginBottom: 12 }}>
        <div style={{ textAlign: 'center', padding: '4px 0' }}>
          <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>평균 혼돈지수 </span>
          <span style={{ fontSize: 18, fontWeight: 800, color: '#FF6B2B' }}>{data.avgChaosIndex.toFixed(1)}</span>
        </div>
      </div>
      {sections.map(({ label, matches, color }) => matches.length > 0 && (
        <div key={label}>
          <p className="m-section-title">{label}</p>
          {matches.slice(0, 3).map((m, i) => {
            const date = new Date(m.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
            return (
              <div key={i} className="m-synergy-card" style={{ cursor: 'pointer' }}
                onClick={() => navigate(`/m/match/${m.matchId}`)}>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                    <span style={{ fontSize: 13, fontWeight: 700, color }}>{m.chaosIndex.toFixed(1)}</span>
                    <span className="m-stat-chip" style={{ fontSize: 10 }}>{m.gameTypeTag}</span>
                    <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginLeft: 'auto' }}>{date} · {m.gameDurationMin.toFixed(0)}분</span>
                  </div>
                  <div className="m-stat-chips">
                    <span className="m-stat-chip">킬 {m.totalKills}</span>
                    <span className="m-stat-chip">밀도 {m.killDensity.toFixed(2)}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}

/* ── 초반 지배력 ──────────────────────────────────────────────────────── */
function EarlyGameTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-earlygame', mode],
    queryFn: () => api.get<EarlyGameDominanceResult>(`/stats/early-game?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {(data.firstBloodKing || data.towerDestroyer) && (
        <>
          <p className="m-section-title">타이틀 보유자</p>
          <div className="m-card">
            {data.firstBloodKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.firstBloodKing!)}`)}>
                <span style={{ fontSize: 16, width: 28 }}>⚔️</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 80 }}>퍼블왕</span>
                <span style={{ fontWeight: 700 }}>{data.firstBloodKing.split('#')[0]}</span>
              </div>
            )}
            {data.towerDestroyer && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.towerDestroyer!)}`)}>
                <span style={{ fontSize: 16, width: 28 }}>🏰</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 80 }}>포탑왕</span>
                <span style={{ fontWeight: 700 }}>{data.towerDestroyer.split('#')[0]}</span>
              </div>
            )}
          </div>
          <div className="m-stat-chips" style={{ marginBottom: 12 }}>
            <span className="m-stat-chip">퍼블 승률 {(data.overallFirstBloodWinRate * 100).toFixed(1)}%</span>
            <span className="m-stat-chip">선포탑 승률 {(data.overallFirstTowerWinRate * 100).toFixed(1)}%</span>
          </div>
        </>
      )}
      <p className="m-section-title">초반 지배력 순위</p>
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
              <span style={{ fontSize: 16, fontWeight: 800, color: 'var(--color-primary)' }}>{p.earlyGameScore.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">퍼블 {(p.firstBloodRate * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">선포탑 {(p.firstTowerRate * 100).toFixed(0)}%</span>
              {p.badges.map(b => <span key={b} className="m-stat-chip" style={{ color: '#FFD700' }}>{b}</span>)}
            </div>
          </div>
        );
      })}
    </div>
  );
}

/* ── 컴백 지수 ────────────────────────────────────────────────────────── */
function ComebackTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-comeback', mode],
    queryFn: () => api.get<ComebackIndexResult>(`/stats/comeback?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.comebackKing && (
        <>
          <p className="m-section-title">역전의 왕</p>
          <div className="m-card" style={{ marginBottom: 12 }}>
            <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.comebackKing!)}`)}>
              <span style={{ fontSize: 20, width: 32 }}>👑</span>
              <span style={{ fontWeight: 700, fontSize: 15 }}>{data.comebackKing.split('#')[0]}</span>
            </div>
          </div>
        </>
      )}
      <p className="m-section-title">컴백 지수 순위</p>
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
                {p.isKing && <span style={{ fontSize: 11, color: '#FFD700', marginLeft: 4 }}>역전왕</span>}
              </div>
              <span style={{ fontSize: 16, fontWeight: 800, color: 'var(--color-primary)' }}>{(p.comebackBonus * 100).toFixed(0)}%</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">전체 {p.totalGames}게임</span>
              <span className="m-stat-chip" style={{ color: p.contestWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>
                접전 {p.contestWinRate.toFixed(1)}%
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════ */
/* ── 팀분석 그룹 ─────────────────────────────────────────────────────── */
/* ══════════════════════════════════════════════════════════════════════ */
type TeamSubTab = '라이벌' | '팀케미' | '세션' | '비교';
const TEAM_SUB_TABS: TeamSubTab[] = ['라이벌', '팀케미', '세션', '비교'];

function TeamAnalysisGroup() {
  const [sub, setSub] = useState<TeamSubTab>('라이벌');
  const [mode, setMode] = useState('normal');

  return (
    <div>
      <div className="m-sort-chips" style={{ marginBottom: 0 }}>
        {MODES.map(m => (
          <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
            {m.label}
          </button>
        ))}
      </div>
      <div className="m-tab-bar" style={{ overflowX: 'auto', scrollbarWidth: 'none', flexWrap: 'nowrap' }}>
        {TEAM_SUB_TABS.map(t => (
          <button key={t} className={`m-tab${sub === t ? ' active' : ''}`} onClick={() => setSub(t)} style={{ flexShrink: 0, fontSize: 12 }}>
            {t}
          </button>
        ))}
      </div>
      {sub === '라이벌' && <RivalTab mode={mode} />}
      {sub === '팀케미'  && <TeamChemTab mode={mode} />}
      {sub === '세션'    && <SessionTab mode={mode} />}
      {sub === '비교'    && <CompareTab mode={mode} />}
    </div>
  );
}

/* ── 숙명의 라이벌 ────────────────────────────────────────────────────── */
function RivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-rival', mode],
    queryFn: () => api.get<RivalMatchupResult>(`/stats/rival-matchup?mode=${mode}&minGames=3`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.topRivalry && (
        <>
          <p className="m-section-title">최고의 라이벌</p>
          <div className="m-card" style={{ marginBottom: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0' }}>
              <button onClick={() => navigate(`/m/player/${encodeURIComponent(data.topRivalry!.player1)}`)}
                style={{ fontWeight: 700, fontSize: 14, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}>
                {data.topRivalry.player1.split('#')[0]}
              </button>
              <span style={{ color: 'var(--color-text-disabled)', fontWeight: 700 }}>VS</span>
              <button onClick={() => navigate(`/m/player/${encodeURIComponent(data.topRivalry!.player2)}`)}
                style={{ fontWeight: 700, fontSize: 14, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}>
                {data.topRivalry.player2.split('#')[0]}
              </button>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{data.topRivalry.games}게임</span>
              <span className="m-stat-chip">{data.topRivalry.player1Wins}승 {data.topRivalry.player2Wins}승</span>
            </div>
          </div>
        </>
      )}
      <p className="m-section-title">전체 라이벌 관계</p>
      {data.rivalries.map((r, i) => (
        <div key={i} className="m-synergy-card">
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
              <button onClick={() => navigate(`/m/player/${encodeURIComponent(r.player1)}`)}
                style={{ fontWeight: 700, fontSize: 13, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}>
                {r.player1.split('#')[0]}
              </button>
              <span style={{ fontSize: 11, color: 'var(--color-text-disabled)', fontWeight: 700 }}>VS</span>
              <button onClick={() => navigate(`/m/player/${encodeURIComponent(r.player2)}`)}
                style={{ fontWeight: 700, fontSize: 13, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}>
                {r.player2.split('#')[0]}
              </button>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{r.games}게임</span>
              <span className="m-stat-chip">{r.player1Wins}승 : {r.player2Wins}승</span>
              <span className="m-stat-chip" style={{ color: r.player1WinRate >= 60 ? 'var(--color-win)' : r.player1WinRate <= 40 ? 'var(--color-loss)' : 'inherit' }}>
                {r.player1WinRate.toFixed(1)}%
              </span>
            </div>
          </div>
        </div>
      ))}
      {data.rivalries.length === 0 && <div className="m-empty">라이벌 데이터가 없습니다</div>}
    </div>
  );
}

/* ── 팀 케미스트리 ────────────────────────────────────────────────────── */
function TeamChemTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-teamchem', mode],
    queryFn: () => api.get<TeamChemistryResult>(`/stats/team-chemistry?mode=${mode}&minGames=3`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  function ChemCard({ entries, title }: { entries: typeof data.bestDuos; title: string }) {
    if (entries.length === 0) return null;
    return (
      <>
        <p className="m-section-title">{title}</p>
        {entries.slice(0, 5).map((e, i) => (
          <div key={i} className="m-synergy-card">
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>
                {e.players.map(p => p.split('#')[0]).join(' + ')}
              </div>
              <div className="m-stat-chips">
                <span className="m-stat-chip">{e.games}게임</span>
                <span className="m-stat-chip" style={{ color: e.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>
                  {e.winRate.toFixed(1)}%
                </span>
                <span className="m-stat-chip">{e.wins}승 {e.games - e.wins}패</span>
              </div>
            </div>
          </div>
        ))}
      </>
    );
  }

  return (
    <div>
      <ChemCard entries={data.bestDuos} title="베스트 2인조" />
      <ChemCard entries={data.bestTrios} title="베스트 3인조" />
      <ChemCard entries={data.bestFullTeams} title="베스트 5인조" />
      {data.worstDuos.length > 0 && (
        <>
          <p className="m-section-title">워스트 조합 (주의)</p>
          {data.worstDuos.slice(0, 3).map((e, i) => (
            <div key={i} className="m-synergy-card">
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>
                  {e.players.map(p => p.split('#')[0]).join(' + ')}
                </div>
                <div className="m-stat-chips">
                  <span className="m-stat-chip">{e.games}게임</span>
                  <span className="m-stat-chip" style={{ color: 'var(--color-loss)' }}>{e.winRate.toFixed(1)}%</span>
                </div>
              </div>
            </div>
          ))}
        </>
      )}
    </div>
  );
}

/* ── 세션 분석 ────────────────────────────────────────────────────────── */
function SessionTab({ mode }: { mode: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-sessions', mode],
    queryFn: () => api.get<SessionReportResult>(`/stats/sessions?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <div className="m-card" style={{ marginBottom: 12 }}>
        <div style={{ textAlign: 'center', padding: '4px 0' }}>
          <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>총 내전 세션 </span>
          <span style={{ fontSize: 20, fontWeight: 800, color: 'var(--color-primary)' }}>{data.totalSessions}회</span>
        </div>
      </div>
      {data.sessions.map((s, i) => (
        <div key={i} className="m-player-card">
          <div className="m-player-card-header">
            <div style={{ flex: 1 }}>
              <span className="m-player-name">{s.date}</span>
            </div>
            <span className="m-player-games">{s.games}게임 · {s.totalDurationMin.toFixed(0)}분</span>
          </div>
          <div className="m-stat-chips">
            {s.sessionMvp && <span className="m-stat-chip">MVP: {s.sessionMvp.split('#')[0]} ({s.sessionMvpKda.toFixed(2)})</span>}
            <span className="m-stat-chip">팀100: {s.team100Wins}승</span>
            <span className="m-stat-chip">팀200: {s.team200Wins}승</span>
            {s.pentaKills > 0 && <span className="m-stat-chip" style={{ color: '#FFD700' }}>펜타킬 {s.pentaKills}회</span>}
          </div>
          {s.participants.length > 0 && (
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 4 }}>
              {s.participants.map(p => p.split('#')[0]).join(', ')}
            </div>
          )}
        </div>
      ))}
      {data.sessions.length === 0 && <div className="m-empty">세션 데이터가 없습니다</div>}
    </div>
  );
}

/* ── 플레이어 비교 ────────────────────────────────────────────────────── */
function CompareTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [p1, setP1] = useState('');
  const [p2, setP2] = useState('');
  const [query, setQuery] = useState<{ p1: string; p2: string } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['mobile-compare', query, mode],
    queryFn: () => query ? api.get<PlayerComparisonResult>(`/stats/compare?player1=${encodeURIComponent(query.p1)}&player2=${encodeURIComponent(query.p2)}&mode=${mode}`) : null,
    enabled: !!query,
  });

  function StatRow({ label, v1, v2, higherIsBetter = true }: { label: string; v1: number; v2: number; higherIsBetter?: boolean }) {
    const p1Better = higherIsBetter ? v1 > v2 : v1 < v2;
    const p2Better = higherIsBetter ? v2 > v1 : v2 < v1;
    return (
      <div style={{ display: 'flex', alignItems: 'center', padding: '6px 0', borderBottom: '1px solid var(--color-border)' }}>
        <span style={{ flex: 1, textAlign: 'right', fontWeight: p1Better ? 700 : 400, color: p1Better ? 'var(--color-win)' : 'inherit' }}>
          {typeof v1 === 'number' && v1 % 1 !== 0 ? v1.toFixed(2) : v1}
        </span>
        <span style={{ width: 80, textAlign: 'center', fontSize: 11, color: 'var(--color-text-secondary)' }}>{label}</span>
        <span style={{ flex: 1, textAlign: 'left', fontWeight: p2Better ? 700 : 400, color: p2Better ? 'var(--color-win)' : 'inherit' }}>
          {typeof v2 === 'number' && v2 % 1 !== 0 ? v2.toFixed(2) : v2}
        </span>
      </div>
    );
  }

  return (
    <div>
      <div className="m-card" style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
          <input value={p1} onChange={e => setP1(e.target.value)} placeholder="플레이어1 (닉네임#태그)"
            style={{ flex: 1, padding: '8px 10px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--color-bg-hover)', color: 'var(--color-text-primary)', fontSize: 12 }} />
          <input value={p2} onChange={e => setP2(e.target.value)} placeholder="플레이어2 (닉네임#태그)"
            style={{ flex: 1, padding: '8px 10px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--color-bg-hover)', color: 'var(--color-text-primary)', fontSize: 12 }} />
        </div>
        <button onClick={() => p1 && p2 && setQuery({ p1, p2 })}
          style={{ width: '100%', padding: '10px', borderRadius: 8, background: 'var(--color-primary)', color: '#fff', border: 'none', fontWeight: 700, cursor: 'pointer' }}>
          비교하기
        </button>
      </div>

      {isLoading && <LoadingCenter />}
      {data && (
        <>
          <div style={{ display: 'flex', gap: 8, marginBottom: 4, justifyContent: 'space-between', padding: '0 8px' }}>
            <button onClick={() => navigate(`/m/player/${encodeURIComponent(data.player1)}`)}
              style={{ fontWeight: 700, fontSize: 14, color: '#4A9EFF', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}>
              {data.player1.split('#')[0]}
            </button>
            <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>vs</span>
            <button onClick={() => navigate(`/m/player/${encodeURIComponent(data.player2)}`)}
              style={{ fontWeight: 700, fontSize: 14, color: '#FF6B6B', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}>
              {data.player2.split('#')[0]}
            </button>
          </div>
          <p className="m-section-title">전체 스탯</p>
          <div className="m-card">
            <StatRow label="게임수" v1={data.overallP1Stats.games} v2={data.overallP2Stats.games} />
            <StatRow label="승률%" v1={data.overallP1Stats.winRate} v2={data.overallP2Stats.winRate} />
            <StatRow label="KDA" v1={data.overallP1Stats.kda} v2={data.overallP2Stats.kda} />
            <StatRow label="평균딜" v1={data.overallP1Stats.avgDamage} v2={data.overallP2Stats.avgDamage} />
            <StatRow label="평균CS" v1={data.overallP1Stats.avgCs} v2={data.overallP2Stats.avgCs} />
            <StatRow label="시야" v1={data.overallP1Stats.avgVisionScore} v2={data.overallP2Stats.avgVisionScore} />
          </div>
          {data.togetherGames > 0 && (
            <>
              <p className="m-section-title">같은 팀 ({data.togetherGames}게임 · {data.togetherWinRate.toFixed(1)}%)</p>
              {data.p1TogetherStats && data.p2TogetherStats && (
                <div className="m-card">
                  <StatRow label="KDA" v1={data.p1TogetherStats.kda} v2={data.p2TogetherStats.kda} />
                  <StatRow label="평균딜" v1={data.p1TogetherStats.avgDamage} v2={data.p2TogetherStats.avgDamage} />
                </div>
              )}
            </>
          )}
          {data.versusGames > 0 && (
            <>
              <p className="m-section-title">상대전 ({data.versusGames}게임)</p>
              <div className="m-card">
                <div style={{ textAlign: 'center', padding: '6px 0', fontSize: 13 }}>
                  <span style={{ fontWeight: 700, color: '#4A9EFF' }}>{data.player1.split('#')[0]}</span>
                  <span style={{ margin: '0 8px', color: 'var(--color-text-secondary)' }}>{data.player1VsWinRate.toFixed(1)}%</span>
                  <span style={{ color: 'var(--color-text-secondary)' }}>승률</span>
                </div>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════ */
/* ── 장인 그룹 ───────────────────────────────────────────────────────── */
/* ══════════════════════════════════════════════════════════════════════ */
type MasterySubTab = '포지션장인' | '챔피언장인' | '챔피언티어' | '메타변화';
const MASTERY_SUB_TABS: MasterySubTab[] = ['포지션장인', '챔피언장인', '챔피언티어', '메타변화'];

function MasteryGroup() {
  const [sub, setSub] = useState<MasterySubTab>('포지션장인');
  const [mode, setMode] = useState('normal');

  return (
    <div>
      <div className="m-sort-chips" style={{ marginBottom: 0 }}>
        {MODES.map(m => (
          <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
            {m.label}
          </button>
        ))}
      </div>
      <div className="m-tab-bar" style={{ overflowX: 'auto', scrollbarWidth: 'none', flexWrap: 'nowrap' }}>
        {MASTERY_SUB_TABS.map(t => (
          <button key={t} className={`m-tab${sub === t ? ' active' : ''}`} onClick={() => setSub(t)} style={{ flexShrink: 0, fontSize: 12 }}>
            {t}
          </button>
        ))}
      </div>
      {sub === '포지션장인'  && <PositionBadgeTab mode={mode} />}
      {sub === '챔피언장인'  && <ChampCertTab mode={mode} />}
      {sub === '챔피언티어'  && <ChampTierTab mode={mode} />}
      {sub === '메타변화'    && <MetaShiftTab mode={mode} />}
    </div>
  );
}

/* ── 포지션 장인 배지 ──────────────────────────────────────────────────── */
function PositionBadgeTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-position-badge', mode],
    queryFn: () => api.get<PositionBadgeResult>(`/stats/position-badge?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const POS_LABEL: Record<string, string> = { TOP: '탑', JUNGLE: '정글', MID: '미드', BOTTOM: '원딜', SUPPORT: '서폿' };

  return (
    <div>
      <p className="m-section-title">포지션별 최고 플레이어</p>
      <div className="m-card">
        {data.topPositions.map(p => (
          <div key={p.position} className="m-leader-row"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 50 }}>{POS_LABEL[p.position] ?? p.position}</span>
            <span style={{ fontWeight: 700, flex: 1 }}>{p.riotId.split('#')[0]}</span>
            <span style={{ fontSize: 12 }}>{p.games}게임 {p.winRate.toFixed(0)}%</span>
          </div>
        ))}
      </div>
      {Object.entries(data.allPositionRankings).map(([pos, players]) => (
        <div key={pos}>
          <p className="m-section-title">{POS_LABEL[pos] ?? pos} 랭킹</p>
          {players.slice(0, 5).map((p, i) => {
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
                  <span className="m-stat-chip" style={{ color: p.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>
                    {p.winRate.toFixed(1)}%
                  </span>
                  <span className="m-stat-chip">KDA {p.kda.toFixed(2)}</span>
                  <span className="m-stat-chip">점수 {p.positionScore.toFixed(1)}</span>
                </div>
              </div>
            );
          })}
        </div>
      ))}
    </div>
  );
}

/* ── 챔피언 장인 인증서 ────────────────────────────────────────────────── */
function ChampCertTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-champ-cert', mode],
    queryFn: () => api.get<ChampionCertificateResult>(`/stats/champion-certificate?mode=${mode}&minGames=5`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">인증된 챔피언 장인</p>
      {data.certifiedMasters.map((e, i) => {
        const c = champions.get(e.championId);
        const [name, tag] = e.riotId.split('#');
        return (
          <div key={i} className="m-synergy-card">
            {c?.imageUrl ? (
              <img src={c.imageUrl} alt={c.nameKo} width={40} height={40} style={{ borderRadius: 8, flexShrink: 0 }} />
            ) : (
              <div style={{ width: 40, height: 40, borderRadius: 8, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, flexShrink: 0 }}>
                {e.champion.slice(0, 2)}
              </div>
            )}
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                <button onClick={() => navigate(`/m/player/${encodeURIComponent(e.riotId)}`)}
                  style={{ fontWeight: 700, fontSize: 13, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}>
                  {name}
                </button>
                {tag && <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>#{tag}</span>}
                <span style={{ fontSize: 11, color: '#FFD700', marginLeft: 'auto' }}>★ {c?.nameKo ?? e.champion}</span>
              </div>
              <div className="m-stat-chips">
                <span className="m-stat-chip">{e.games}게임</span>
                <span className="m-stat-chip" style={{ color: e.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>{e.winRate.toFixed(1)}%</span>
                <span className="m-stat-chip">KDA {e.kda.toFixed(2)}</span>
              </div>
            </div>
          </div>
        );
      })}
      {data.certifiedMasters.length === 0 && <div className="m-empty">인증된 장인이 없습니다 (5게임 이상 필요)</div>}
    </div>
  );
}

/* ── 챔피언 티어리스트 ────────────────────────────────────────────────── */
function ChampTierTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-champ-tier', mode],
    queryFn: () => api.get<ChampionTierResult>(`/stats/champion-tier?mode=${mode}&minGames=3`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const TIER_COLOR: Record<string, string> = {
    'S+': '#FF4757', S: '#FF6B2B', A: '#FFD700', B: '#4CAF50', C: '#4A9EFF', D: '#9E9E9E',
  };
  const tierOrder = ['S+', 'S', 'A', 'B', 'C', 'D'];

  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
        총 {data.totalMatches}게임 분석 (3게임 이상 챔피언)
      </div>
      {tierOrder.map(tier => {
        const entries = data.byTier[tier] ?? [];
        if (entries.length === 0) return null;
        return (
          <div key={tier}>
            <p className="m-section-title" style={{ color: TIER_COLOR[tier] }}>Tier {tier}</p>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
              {entries.map(e => {
                const c = champions.get(e.championId);
                return (
                  <div key={e.champion} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, width: 52 }}>
                    {c?.imageUrl ? (
                      <img src={c.imageUrl} alt={c.nameKo} width={44} height={44} style={{ borderRadius: 8, border: `2px solid ${TIER_COLOR[tier]}` }} />
                    ) : (
                      <div style={{ width: 44, height: 44, borderRadius: 8, background: 'var(--color-bg-hover)', border: `2px solid ${TIER_COLOR[tier]}`, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 9 }}>
                        {e.champion.slice(0, 2)}
                      </div>
                    )}
                    <span style={{ fontSize: 9, color: 'var(--color-text-secondary)', textAlign: 'center', lineHeight: 1.2 }}>
                      {c?.nameKo ?? e.champion}
                    </span>
                    <span style={{ fontSize: 9, color: e.winRate >= 60 ? 'var(--color-win)' : e.winRate <= 40 ? 'var(--color-loss)' : 'inherit' }}>
                      {e.winRate.toFixed(0)}%
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}

/* ── 메타 변화 추적기 ──────────────────────────────────────────────────── */
function MetaShiftTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-meta', mode],
    queryFn: () => api.get<MetaShiftResult>(`/stats/meta-shift?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  function ChampRow({ e, arrow, color }: { e: typeof data.risingChampions[0]; arrow: string; color: string }) {
    const c = champions.get(e.championId);
    return (
      <div className="m-synergy-card">
        {c?.imageUrl ? (
          <img src={c.imageUrl} alt={c.nameKo} width={36} height={36} style={{ borderRadius: 6, flexShrink: 0 }} />
        ) : (
          <div style={{ width: 36, height: 36, borderRadius: 6, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 9, flexShrink: 0 }}>
            {e.champion.slice(0, 2)}
          </div>
        )}
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 2 }}>{c?.nameKo ?? e.champion}</div>
          <div className="m-stat-chips">
            <span className="m-stat-chip" style={{ color }}>{arrow} {(e.trend * 100).toFixed(1)}%p</span>
            <span className="m-stat-chip">{e.totalGames}게임</span>
            <span className="m-stat-chip">승률 {e.winRate.toFixed(1)}%</span>
            <span className="m-stat-chip" style={{ fontSize: 10 }}>{e.metaTag}</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
        {data.totalMatchesAnalyzed}게임 분석
      </div>
      {data.risingChampions.length > 0 && (
        <>
          <p className="m-section-title" style={{ color: '#4CAF50' }}>상승세</p>
          {data.risingChampions.map((e, i) => <ChampRow key={i} e={e} arrow="↑" color="#4CAF50" />)}
        </>
      )}
      {data.fallingChampions.length > 0 && (
        <>
          <p className="m-section-title" style={{ color: '#FF4757' }}>하락세</p>
          {data.fallingChampions.map((e, i) => <ChampRow key={i} e={e} arrow="↓" color="#FF4757" />)}
        </>
      )}
      {data.stableTopChampions.length > 0 && (
        <>
          <p className="m-section-title">꾸준히 강세</p>
          {data.stableTopChampions.map((e, i) => <ChampRow key={i} e={e} arrow="→" color="var(--color-text-secondary)" />)}
        </>
      )}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════════════ */
/* ── 개인분석 그룹 ────────────────────────────────────────────────────── */
/* ══════════════════════════════════════════════════════════════════════ */
type PersonalSubTab = '생존지수' | '정글점령' | '서폿기여' | 'DNA' | '게임성향' | '골드효율' | '성장곡선';
const PERSONAL_SUB_TABS: PersonalSubTab[] = ['생존지수', '정글점령', '서폿기여', 'DNA', '게임성향', '골드효율', '성장곡선'];

function PersonalAnalysisGroup() {
  const [sub, setSub] = useState<PersonalSubTab>('생존지수');
  const [mode, setMode] = useState('normal');

  return (
    <div>
      <div className="m-sort-chips" style={{ marginBottom: 0 }}>
        {MODES.map(m => (
          <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
            {m.label}
          </button>
        ))}
      </div>
      <div className="m-tab-bar" style={{ overflowX: 'auto', scrollbarWidth: 'none', flexWrap: 'nowrap' }}>
        {PERSONAL_SUB_TABS.map(t => (
          <button key={t} className={`m-tab${sub === t ? ' active' : ''}`} onClick={() => setSub(t)} style={{ flexShrink: 0, fontSize: 12 }}>
            {t}
          </button>
        ))}
      </div>
      {sub === '생존지수'  && <SurvivalTab mode={mode} />}
      {sub === '정글점령'  && <JungleTab mode={mode} />}
      {sub === '서폿기여'  && <SupportTab mode={mode} />}
      {sub === 'DNA'       && <DnaTab mode={mode} />}
      {sub === '게임성향'  && <GameLengthTab mode={mode} />}
      {sub === '골드효율'  && <GoldEffTab mode={mode} />}
      {sub === '성장곡선'  && <GrowthTab mode={mode} />}
    </div>
  );
}

/* ── 생존 지수 ────────────────────────────────────────────────────────── */
function SurvivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-survival', mode],
    queryFn: () => api.get<SurvivalIndexResult>(`/stats/survival-index?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">탱킹/생존 지수 순위</p>
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
              <span style={{ fontSize: 16, fontWeight: 800, color: '#2196F3' }}>{p.survivalIndex.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip">받은딜 {Math.round(p.avgDamageTaken).toLocaleString()}</span>
              <span className="m-stat-chip">경감률 {(p.avgMitigationRatio * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">생존율 {(p.avgSurvivalRatio * 100).toFixed(0)}%</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

/* ── 정글 점령 ────────────────────────────────────────────────────────── */
function JungleTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-jungle', mode],
    queryFn: () => api.get<JungleDominanceResult>(`/stats/jungle-dominance?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">정글 점령 지수 순위</p>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        const c = p.topChampionId ? champions.get(p.topChampionId) : null;
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1 }}>
                {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={28} height={28} style={{ borderRadius: 4 }} />}
                <div>
                  <span className="m-player-name">{name}</span>
                  {tag && <span className="m-player-tag"> #{tag}</span>}
                  <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{p.playStyleTag}</div>
                </div>
              </div>
              <span style={{ fontSize: 16, fontWeight: 800, color: '#4CAF50' }}>{p.avgJungleDominance.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip">침입 {(p.avgInvadeRatio * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">오브젝트 {(p.avgObjShare * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">KP {(p.avgKp * 100).toFixed(0)}%</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

/* ── 서폿 기여 ────────────────────────────────────────────────────────── */
function SupportTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-support', mode],
    queryFn: () => api.get<SupportImpactResult>(`/stats/support-impact?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">서폿/힐러 기여도 순위</p>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        const c = p.topChampionId ? champions.get(p.topChampionId) : null;
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1 }}>
                {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={28} height={28} style={{ borderRadius: 4 }} />}
                <div>
                  <span className="m-player-name">{name}</span>
                  {tag && <span className="m-player-tag"> #{tag}</span>}
                  <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{p.roleTag}</div>
                </div>
              </div>
              <span style={{ fontSize: 16, fontWeight: 800, color: '#E91E8A' }}>{p.supportImpact.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip">힐 {(p.avgHealShare * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">CC {(p.avgCcShare * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">시야 {(p.avgVisionShare * 100).toFixed(0)}%</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

/* ── 플레이스타일 DNA ──────────────────────────────────────────────────── */
function DnaTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-dna', mode],
    queryFn: () => api.get<PlaystyleDnaResult>(`/stats/playstyle-dna?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  function DnaBar({ label, value, color }: { label: string; value: number; color: string }) {
    return (
      <div style={{ marginBottom: 4 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, marginBottom: 2 }}>
          <span style={{ color: 'var(--color-text-secondary)' }}>{label}</span>
          <span style={{ color }}>{value.toFixed(0)}</span>
        </div>
        <div style={{ height: 4, borderRadius: 2, background: 'var(--color-bg-hover)' }}>
          <div style={{ height: '100%', borderRadius: 2, background: color, width: `${Math.min(value, 100)}%` }} />
        </div>
      </div>
    );
  }

  return (
    <div>
      {data.players.map(p => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
                <span className="m-stat-chip" style={{ marginLeft: 8 }}>{p.styleTag}</span>
              </div>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{p.games}게임</span>
            </div>
            <div style={{ marginTop: 8, padding: '0 4px' }}>
              <DnaBar label="공격성" value={p.aggression} color="#FF6B2B" />
              <DnaBar label="생존력" value={p.durability} color="#2196F3" />
              <DnaBar label="팀플레이" value={p.teamPlay} color="#4CAF50" />
              <DnaBar label="오브젝트" value={p.objectiveFocus} color="#FFD700" />
              <DnaBar label="골드효율" value={p.economy} color="#E91E8A" />
              <DnaBar label="시야장악" value={p.visionControl} color="#9C27B0" />
            </div>
          </div>
        );
      })}
      {data.players.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

/* ── 게임 길이별 성향 ──────────────────────────────────────────────────── */
function GameLengthTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-gamelength', mode],
    queryFn: () => api.get<GameLengthTendencyResult>(`/stats/game-length-tendency?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const LENGTH_LABEL: Record<string, string> = {
    SHORT_GAME: '단기전 강자', MID_GAME: '중반전 강자', LONG_GAME: '장기전 강자', BALANCED: '밸런스형',
  };

  return (
    <div>
      <p className="m-section-title">플레이어별 게임 길이 성향</p>
      {data.players.map(p => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
                <div style={{ fontSize: 11, color: 'var(--color-primary)', marginTop: 1 }}>
                  {LENGTH_LABEL[p.tendency] ?? p.tendency}
                </div>
              </div>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{p.totalGames}게임</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">단기 {p.shortGame.winRate.toFixed(0)}% ({p.shortGame.games})</span>
              <span className="m-stat-chip">중반 {p.midGame.winRate.toFixed(0)}% ({p.midGame.games})</span>
              <span className="m-stat-chip">장기 {p.longGame.winRate.toFixed(0)}% ({p.longGame.games})</span>
            </div>
          </div>
        );
      })}
      {data.players.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

/* ── 골드 효율 ────────────────────────────────────────────────────────── */
function GoldEffTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-goldeff', mode],
    queryFn: () => api.get<GoldEfficiencyResult>(`/stats/gold-efficiency?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {(data.dmgEfficiencyKing || data.visionEfficiencyKing || data.csEfficiencyKing) && (
        <>
          <p className="m-section-title">효율 왕</p>
          <div className="m-card" style={{ marginBottom: 12 }}>
            {data.dmgEfficiencyKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.dmgEfficiencyKing!)}`)}>
                <span style={{ fontSize: 14, width: 28 }}>⚔️</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>딜효율왕</span>
                <span style={{ fontWeight: 700 }}>{data.dmgEfficiencyKing.split('#')[0]}</span>
              </div>
            )}
            {data.visionEfficiencyKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.visionEfficiencyKing!)}`)}>
                <span style={{ fontSize: 14, width: 28 }}>👁️</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>시야효율왕</span>
                <span style={{ fontWeight: 700 }}>{data.visionEfficiencyKing.split('#')[0]}</span>
              </div>
            )}
            {data.csEfficiencyKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.csEfficiencyKing!)}`)}>
                <span style={{ fontSize: 14, width: 28 }}>🌾</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>CS효율왕</span>
                <span style={{ fontWeight: 700 }}>{data.csEfficiencyKing.split('#')[0]}</span>
              </div>
            )}
          </div>
        </>
      )}
      <p className="m-section-title">골드 효율 순위</p>
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
              <span style={{ fontSize: 16, fontWeight: 800, color: '#FFD700' }}>{p.goldEfficiencyScore.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip">딜/골드 {p.avgDmgPerGold.toFixed(2)}</span>
              <span className="m-stat-chip">CS/골드 {p.avgCsPerGold.toFixed(2)}</span>
              {p.tags.map(t => <span key={t} className="m-stat-chip" style={{ color: '#FFD700', fontSize: 10 }}>{t}</span>)}
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

/* ── 성장 곡선 ────────────────────────────────────────────────────────── */
function GrowthTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [riotId, setRiotId] = useState('');
  const [query, setQuery] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['mobile-growth', query, mode],
    queryFn: () => query ? api.get<GrowthCurveResult>(`/stats/player/${encodeURIComponent(query)}/growth-curve?mode=${mode}`) : null,
    enabled: !!query,
  });

  const TREND_COLOR = { IMPROVING: '#4CAF50', DECLINING: '#FF4757', STABLE: '#9E9E9E' };
  const TREND_LABEL = { IMPROVING: '상승 중', DECLINING: '하락 중', STABLE: '안정적' };

  return (
    <div>
      <div className="m-card" style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <input value={riotId} onChange={e => setRiotId(e.target.value)} placeholder="닉네임#태그"
            style={{ flex: 1, padding: '8px 10px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--color-bg-hover)', color: 'var(--color-text-primary)', fontSize: 12 }} />
          <button onClick={() => riotId && setQuery(riotId)}
            style={{ padding: '8px 16px', borderRadius: 8, background: 'var(--color-primary)', color: '#fff', border: 'none', fontWeight: 700, cursor: 'pointer' }}>
            조회
          </button>
        </div>
      </div>

      {isLoading && <LoadingCenter />}
      {data && (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <button onClick={() => navigate(`/m/player/${encodeURIComponent(data.riotId)}`)}
              style={{ fontWeight: 700, fontSize: 15, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}>
              {data.riotId.split('#')[0]}
            </button>
            <span style={{ fontSize: 13, fontWeight: 700, color: TREND_COLOR[data.trend] }}>
              {TREND_LABEL[data.trend]}
            </span>
          </div>
          <div className="m-card" style={{ marginBottom: 12 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, textAlign: 'center' }}>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700 }}>{data.totalGames}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>총 게임</div>
              </div>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--color-primary)' }}>{data.recentAvgKda.toFixed(2)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>최근 KDA</div>
              </div>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700 }}>{data.overallAvgKda.toFixed(2)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>전체 KDA</div>
              </div>
            </div>
          </div>
          <p className="m-section-title">최근 경기 기록</p>
          {data.entries.slice(-15).reverse().map((e, i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 0', borderBottom: '1px solid var(--color-border)' }}>
              <div style={{ width: 6, height: 6, borderRadius: '50%', background: e.win ? 'var(--color-win)' : 'var(--color-loss)', flexShrink: 0 }} />
              <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 40 }}>
                {new Date(e.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' })}
              </span>
              <span style={{ fontSize: 12, flex: 1 }}>{e.champion}</span>
              <span className="m-stat-chip" style={{ color: e.rollingKda >= data.overallAvgKda ? 'var(--color-win)' : 'inherit' }}>
                KDA {e.kda.toFixed(2)}
              </span>
              <span className="m-stat-chip">추세 {e.rollingKda.toFixed(2)}</span>
            </div>
          ))}
        </div>
      )}
      {!data && !isLoading && query && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}
