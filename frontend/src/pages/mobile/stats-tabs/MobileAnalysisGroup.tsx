import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type {
  RivalMatchupResult,
  PlayerComparisonResult,
  ChampionTierResult,
} from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';
import { MobileSubTabShell } from './MobileSubTabShell';
import { ChampImg } from '../../stats-tabs/shared';

type AnalysisSubTab = '라이벌' | '비교' | '티어';
const ANALYSIS_SUB_TABS: AnalysisSubTab[] = ['라이벌', '비교', '티어'];

function RivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-rival-a', mode],
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

function StatRow({ label, v1, v2, higherIsBetter = true }: { label: string; v1: number; v2: number; higherIsBetter?: boolean }) {
  const p1Better = higherIsBetter ? v1 > v2 : v1 < v2;
  const p2Better = higherIsBetter ? v2 > v1 : v2 < v1;
  return (
    <div style={{ display: 'flex', alignItems: 'center', padding: '6px 0', borderBottom: '1px solid var(--color-border)' }}>
      <span style={{ flex: 1, textAlign: 'right', fontWeight: p1Better ? 700 : 400, color: p1Better ? 'var(--color-win)' : 'inherit' }}>
        {v1 % 1 !== 0 ? v1.toFixed(2) : v1}
      </span>
      <span style={{ width: 80, textAlign: 'center', fontSize: 11, color: 'var(--color-text-secondary)' }}>{label}</span>
      <span style={{ flex: 1, textAlign: 'left', fontWeight: p2Better ? 700 : 400, color: p2Better ? 'var(--color-win)' : 'inherit' }}>
        {v2 % 1 !== 0 ? v2.toFixed(2) : v2}
      </span>
    </div>
  );
}

function CompareTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [p1, setP1] = useState('');
  const [p2, setP2] = useState('');
  const [query, setQuery] = useState<{ p1: string; p2: string } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['mobile-compare-a', query, mode],
    queryFn: () => query ? api.get<PlayerComparisonResult>(`/stats/compare?player1=${encodeURIComponent(query.p1)}&player2=${encodeURIComponent(query.p2)}&mode=${mode}`) : null,
    enabled: !!query,
  });

  return (
    <div>
      <div className="m-card" style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
          <input value={p1} onChange={e => setP1(e.target.value)} placeholder="플레이어1 (닉네임#태그)"
            style={{ flex: 1, padding: '8px 10px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--bg-surface)', color: 'var(--color-text-primary)', fontSize: 12 }} />
          <input value={p2} onChange={e => setP2(e.target.value)} placeholder="플레이어2 (닉네임#태그)"
            style={{ flex: 1, padding: '8px 10px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--bg-surface)', color: 'var(--color-text-primary)', fontSize: 12 }} />
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

const TIER_COLOR: Record<string, string> = {
  S: '#FFD700', A: '#4CAF50', B: '#4A9EFF', C: '#9E9E9E',
};

function TierTab({ mode }: { mode: string }) {
  const [selectedTier, setSelectedTier] = useState<string>('전체');
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-tier-a', mode],
    queryFn: () => api.get<ChampionTierResult>(`/stats/champion-tier?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const TIERS = ['전체', 'S', 'A', 'B', 'C'];
  const list = selectedTier === '전체' ? data.tierList : (data.byTier[selectedTier] ?? []);

  return (
    <div>
      <div className="m-stat-chips" style={{ marginBottom: 8 }}>
        {TIERS.map(t => (
          <button key={t} className={`m-sort-chip${selectedTier === t ? ' active' : ''}`} onClick={() => setSelectedTier(t)}>
            {t}
          </button>
        ))}
      </div>
      <p className="m-section-title">총 {data.totalMatches}게임 기준</p>
      {list.map((entry, i) => (
        <div key={entry.champion} className="m-synergy-card">
          <ChampImg championId={entry.championId} champion={entry.champion} size={40} style={{ borderRadius: 8, border: 'none', flexShrink: 0 }} />
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 3 }}>
              <span style={{ fontWeight: 600, fontSize: 13 }}>{entry.champion}</span>
              <span style={{ fontSize: 12, fontWeight: 700, color: TIER_COLOR[entry.tier] ?? 'var(--color-text-secondary)' }}>{entry.tier}</span>
              <span style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginLeft: 'auto' }}>#{i + 1}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{entry.games}게임</span>
              <span className="m-stat-chip" style={{ color: entry.winRate >= 60 ? 'var(--color-win)' : entry.winRate <= 40 ? 'var(--color-loss)' : 'inherit' }}>
                {entry.winRate.toFixed(1)}%
              </span>
              <span className="m-stat-chip">KDA {entry.kda.toFixed(2)}</span>
              <span className="m-stat-chip">픽률 {entry.pickRate.toFixed(1)}%</span>
            </div>
          </div>
        </div>
      ))}
      {list.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

const RENDER_MAP: Record<AnalysisSubTab, (mode: string) => React.ReactNode> = {
  '라이벌': mode => <RivalTab mode={mode} />,
  '비교':   mode => <CompareTab mode={mode} />,
  '티어':   mode => <TierTab mode={mode} />,
};

export default function MobileAnalysisGroup() {
  return (
    <MobileSubTabShell
      tabs={ANALYSIS_SUB_TABS}
      defaultTab="라이벌"
      renderTab={(sub, mode) => RENDER_MAP[sub](mode)}
    />
  );
}
