import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type {
  RivalMatchupResult,
  TeamChemistryResult,
  TeamChemistryEntry,
  SessionReportResult,
  PlayerComparisonResult,
} from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';

const MODES = [
  { value: 'normal', label: '5v5' },
  { value: 'aram', label: '칼바람' },
  { value: 'all', label: '전체' },
];

type TeamSubTab = '라이벌' | '팀케미' | '세션' | '비교';
const TEAM_SUB_TABS: TeamSubTab[] = ['라이벌', '팀케미', '세션', '비교'];

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

function ChemCard({ entries, title }: { entries: TeamChemistryEntry[]; title: string }) {
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

function TeamChemTab({ mode }: { mode: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-teamchem', mode],
    queryFn: () => api.get<TeamChemistryResult>(`/stats/team-chemistry?mode=${mode}&minGames=3`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

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
    queryKey: ['mobile-compare', query, mode],
    queryFn: () => query ? api.get<PlayerComparisonResult>(`/stats/compare?player1=${encodeURIComponent(query.p1)}&player2=${encodeURIComponent(query.p2)}&mode=${mode}`) : null,
    enabled: !!query,
  });

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

export default function MobileTeamAnalysisGroup() {
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
