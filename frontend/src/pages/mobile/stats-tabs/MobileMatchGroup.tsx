import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type {
  MultiKillHighlightsResult,
  ChaosMatchResult,
  ComebackIndexResult,
} from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';
import { MobileSubTabShell } from './MobileSubTabShell';
import { ChampImg } from '../../stats-tabs/shared';

type MatchSubTab = '멀티킬' | '혼돈경기' | '컴백';
const MATCH_SUB_TABS: MatchSubTab[] = ['멀티킬', '혼돈경기', '컴백'];

const KILL_COLOR: Record<string, string> = {
  PENTA: '#FFD700', QUADRA: '#AA47BC', TRIPLE: '#4A9EFF', DOUBLE: '#4CAF50',
};

function MultiKillTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-multikill-m', mode],
    queryFn: () => api.get<MultiKillHighlightsResult>(`/stats/multikill-highlights?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.pentaKillEvents.length > 0 && (
        <>
          <p className="m-section-title">펜타킬 순간들</p>
          {data.pentaKillEvents.map((e, i) => {
            const date = new Date(e.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
            return (
              <div key={i} className="m-synergy-card">
                <ChampImg championId={e.championId} champion={e.champion} size={40} style={{ borderRadius: 8, border: 'none', flexShrink: 0 }} />
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
                    {e.champion} · {date}
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
    queryKey: ['mobile-chaos-m', mode],
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

function ComebackTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-comeback-m', mode],
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
              <span style={{ fontSize: 16, width: 28 }}>왕</span>
              <span style={{ fontWeight: 700, fontSize: 15 }}>{data.comebackKing.split('#')[0]}</span>
            </div>
          </div>
        </>
      )}
      {data.topComebackMatches.length > 0 && (
        <>
          <p className="m-section-title">역전 경기들</p>
          {data.topComebackMatches.map((m, i) => {
            const date = new Date(m.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
            return (
              <div key={i} className="m-synergy-card" style={{ cursor: 'pointer' }}
                onClick={() => navigate(`/m/match/${m.matchId}`)}>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 3 }}>
                    <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{date} · {m.gameDurationMin.toFixed(0)}분</span>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                    승팀: {m.winnerParticipants.map(p => p.split('#')[0]).join(', ')}
                  </div>
                </div>
              </div>
            );
          })}
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
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

const RENDER_MAP: Record<MatchSubTab, (mode: string) => React.ReactNode> = {
  '멀티킬':   mode => <MultiKillTab mode={mode} />,
  '혼돈경기': mode => <ChaosMatchTab mode={mode} />,
  '컴백':     mode => <ComebackTab mode={mode} />,
};

export default function MobileMatchGroup() {
  return (
    <MobileSubTabShell
      tabs={MATCH_SUB_TABS}
      defaultTab="멀티킬"
      renderTab={(sub, mode) => RENDER_MAP[sub](mode)}
    />
  );
}
