import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type {
  PositionBadgeResult,
  ChampionCertificateResult,
  ChampionTierResult,
  MetaShiftResult,
  MetaShiftChampion,
} from '../../../lib/types/stats';
import { useDragon } from '../../../context/DragonContext';
import { LoadingCenter } from '../../../components/common/Spinner';
import { MobileSubTabShell } from './MobileSubTabShell';
import { ChampImg } from '../../stats-tabs/shared';

type MasterySubTab = '포지션장인' | '챔피언장인' | '챔피언티어' | '메타변화';
const MASTERY_SUB_TABS: MasterySubTab[] = ['포지션장인', '챔피언장인', '챔피언티어', '메타변화'];

const POS_LABEL: Record<string, string> = { TOP: '탑', JUNGLE: '정글', MID: '미드', BOTTOM: '원딜', SUPPORT: '서폿' };

function PositionBadgeTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-position-badge', mode],
    queryFn: () => api.get<PositionBadgeResult>(`/stats/position-badge?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

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
            <ChampImg championId={e.championId} champion={e.champion} size={40} style={{ borderRadius: 8, border: 'none', flexShrink: 0 }} />
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
                    <ChampImg championId={e.championId} champion={e.champion} size={44} style={{ borderRadius: 8, border: `2px solid ${TIER_COLOR[tier]}` }} />
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

function MetaChampRow({ e, arrow, color, champMap }: {
  e: MetaShiftChampion;
  arrow: string;
  color: string;
  champMap: Map<number, { imageUrl: string | null; nameKo: string; championKey?: string; championId: number }>;
}) {
  const c = champMap.get(e.championId);
  return (
    <div className="m-synergy-card">
      <ChampImg championId={e.championId} champion={e.champion} size={36} style={{ borderRadius: 6, border: 'none', flexShrink: 0 }} />
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

function MetaShiftTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-meta', mode],
    queryFn: () => api.get<MetaShiftResult>(`/stats/meta-shift?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
        {data.totalMatchesAnalyzed}게임 분석
      </div>
      {data.risingChampions.length > 0 && (
        <>
          <p className="m-section-title" style={{ color: '#4CAF50' }}>상승세</p>
          {data.risingChampions.map((e, i) => <MetaChampRow key={i} e={e} arrow="↑" color="#4CAF50" champMap={champions} />)}
        </>
      )}
      {data.fallingChampions.length > 0 && (
        <>
          <p className="m-section-title" style={{ color: '#FF4757' }}>하락세</p>
          {data.fallingChampions.map((e, i) => <MetaChampRow key={i} e={e} arrow="↓" color="#FF4757" champMap={champions} />)}
        </>
      )}
      {data.stableTopChampions.length > 0 && (
        <>
          <p className="m-section-title">꾸준히 강세</p>
          {data.stableTopChampions.map((e, i) => <MetaChampRow key={i} e={e} arrow="→" color="var(--color-text-secondary)" champMap={champions} />)}
        </>
      )}
    </div>
  );
}

const RENDER_MAP: Record<MasterySubTab, (mode: string) => React.ReactNode> = {
  '포지션장인': mode => <PositionBadgeTab mode={mode} />,
  '챔피언장인': mode => <ChampCertTab mode={mode} />,
  '챔피언티어': mode => <ChampTierTab mode={mode} />,
  '메타변화':   mode => <MetaShiftTab mode={mode} />,
};

export default function MobileMasteryGroup() {
  return (
    <MobileSubTabShell
      tabs={MASTERY_SUB_TABS}
      defaultTab="포지션장인"
      renderTab={(sub, mode) => RENDER_MAP[sub](mode)}
    />
  );
}
