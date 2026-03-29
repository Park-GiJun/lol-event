import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type {
  WeeklyAwardsResult,
  DefeatContributionResult,
  MultiKillHighlightsResult,
  ChaosMatchResult,
  EarlyGameDominanceResult,
  ComebackIndexResult,
  BanAnalysisResult,
  TimePatternResult,
} from '../../../lib/types/stats';
import { useDragonChampions } from '../../../context/DragonContext';
import { LoadingCenter } from '../../../components/common/Spinner';

const MODES = [
  { value: 'normal', label: '5v5' },
  { value: 'aram', label: '칼바람' },
  { value: 'all', label: '전체' },
];

type AwardsSubTab = '주간어워즈' | '떡락지수' | '멀티킬' | '혼돈경기' | '초반지배' | '컴백지수' | '밴분석' | '시간패턴';
const AWARDS_SUB_TABS: AwardsSubTab[] = ['주간어워즈', '떡락지수', '멀티킬', '혼돈경기', '초반지배', '컴백지수', '밴분석', '시간패턴'];

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

function MultiKillTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const champions = useDragonChampions();
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

function MobileBanTab({ mode }: { mode: string }) {
  const champions = useDragonChampions();
  const { data, isLoading } = useQuery({
    queryKey: ['m-ban', mode],
    queryFn: () => api.get<BanAnalysisResult>(`/stats/ban-analysis?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data || data.topBanned.length === 0) return <div className="m-empty">밴 데이터가 없습니다</div>;

  return (
    <div>
      <p className="m-section-title">총 {data.totalGamesAnalyzed}게임 밴 분석</p>
      {data.topBanned.map((e, i) => {
        const c = champions.get(e.championId);
        return (
          <div key={e.champion} className="m-synergy-card">
            {c?.imageUrl ? (
              <img src={c.imageUrl} alt={c.nameKo} width={40} height={40} style={{ borderRadius: 8, flexShrink: 0 }} />
            ) : (
              <div style={{ width: 40, height: 40, borderRadius: 8, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, flexShrink: 0 }}>
                {e.champion.slice(0, 2)}
              </div>
            )}
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 3 }}>
                <span style={{ fontWeight: 600, fontSize: 13 }}>{c?.nameKo ?? e.champion}</span>
                <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>#{i + 1}</span>
              </div>
              <div className="m-stat-chips">
                <span className="m-stat-chip">{e.banCount}회 밴</span>
                <span className="m-stat-chip" style={{ color: e.banRate >= 50 ? '#FF4757' : e.banRate >= 30 ? '#FF6B2B' : 'inherit' }}>
                  밴율 {e.banRate.toFixed(1)}%
                </span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function MobileTimeTab({ mode }: { mode: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['m-time', mode],
    queryFn: () => api.get<TimePatternResult>(`/stats/time-pattern?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const maxDay = Math.max(...data.byDay.map(d => d.games), 1);
  const maxHour = Math.max(...data.byHour.map(h => h.games), 1);

  return (
    <div>
      {data.busiestDay && (
        <div className="m-card" style={{ marginBottom: 12 }}>
          <div style={{ textAlign: 'center', fontSize: 13 }}>
            가장 활발한 요일 <strong style={{ color: 'var(--color-primary)' }}>{data.busiestDay}요일</strong>
            {data.busiestHour !== null && <> · <strong style={{ color: 'var(--color-primary)' }}>{data.busiestHour}시</strong></>}
          </div>
        </div>
      )}
      <p className="m-section-title">요일별</p>
      {data.byDay.map(d => (
        <div key={d.dayOfWeek} style={{ marginBottom: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 3 }}>
            <span style={{ fontWeight: 600 }}>{d.dayName}요일</span>
            <span style={{ color: 'var(--color-text-secondary)' }}>{d.games}게임 · {d.sessions}세션</span>
          </div>
          <div style={{ height: 8, borderRadius: 4, background: 'var(--color-bg-hover)' }}>
            <div style={{ height: '100%', borderRadius: 4, background: 'var(--color-primary)', width: `${d.games / maxDay * 100}%` }} />
          </div>
        </div>
      ))}
      <p className="m-section-title" style={{ marginTop: 12 }}>시간대별</p>
      {data.byHour.map(h => (
        <div key={h.hour} style={{ marginBottom: 6, display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', width: 28, flexShrink: 0 }}>{h.hour}시</span>
          <div style={{ flex: 1, height: 6, borderRadius: 3, background: 'var(--color-bg-hover)' }}>
            <div style={{ height: '100%', borderRadius: 3, background: 'var(--color-primary)', width: `${h.games / maxHour * 100}%` }} />
          </div>
          <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', width: 30, textAlign: 'right', flexShrink: 0 }}>{h.games}</span>
        </div>
      ))}
    </div>
  );
}

export default function MobileAwardsGroup() {
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
      {sub === '밴분석'     && <MobileBanTab mode={mode} />}
      {sub === '시간패턴'   && <MobileTimeTab mode={mode} />}
    </div>
  );
}
