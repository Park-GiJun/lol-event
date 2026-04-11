import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type {
  GoldEfficiencyResult,
  SurvivalIndexResult,
  BanAnalysisResult,
  DamageAnalysisResult,
} from '../../../lib/types/stats';
import { LoadingCenter } from '../../../components/common/Spinner';
import { MobileSubTabShell } from './MobileSubTabShell';
import { ChampImg } from '../../stats-tabs/shared';

type EfficiencySubTab = '골드효율' | '생존력' | '밴분석' | '데미지';
const EFFICIENCY_SUB_TABS: EfficiencySubTab[] = ['골드효율', '생존력', '밴분석', '데미지'];

function GoldEffTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-goldeff-e', mode],
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
                <span style={{ fontSize: 14, width: 28 }}>딜</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>딜효율왕</span>
                <span style={{ fontWeight: 700 }}>{data.dmgEfficiencyKing.split('#')[0]}</span>
              </div>
            )}
            {data.visionEfficiencyKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.visionEfficiencyKing!)}`)}>
                <span style={{ fontSize: 14, width: 28 }}>시야</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>시야효율왕</span>
                <span style={{ fontWeight: 700 }}>{data.visionEfficiencyKing.split('#')[0]}</span>
              </div>
            )}
            {data.csEfficiencyKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.csEfficiencyKing!)}`)}>
                <span style={{ fontSize: 14, width: 28 }}>CS</span>
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
              <span style={{ fontSize: 16, fontWeight: 800, color: '#FFD700', fontVariantNumeric: 'tabular-nums' }}>{p.goldEfficiencyScore.toFixed(1)}</span>
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

function SurvivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-survival-e', mode],
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
              <span style={{ fontSize: 16, fontWeight: 800, color: '#2196F3', fontVariantNumeric: 'tabular-nums' }}>{p.survivalIndex.toFixed(1)}</span>
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

function BanTab({ mode }: { mode: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-ban-e', mode],
    queryFn: () => api.get<BanAnalysisResult>(`/stats/ban-analysis?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data || data.topBanned.length === 0) return <div className="m-empty">밴 데이터가 없습니다</div>;

  return (
    <div>
      <p className="m-section-title">총 {data.totalGamesAnalyzed}게임 밴 분석</p>
      {data.topBanned.map((e, i) => (
        <div key={e.champion} className="m-synergy-card">
          <ChampImg championId={e.championId} champion={e.champion} size={40} style={{ borderRadius: 8, border: 'none', flexShrink: 0 }} />
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 3 }}>
              <span style={{ fontWeight: 600, fontSize: 13 }}>{e.champion}</span>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>#{i + 1}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{e.banCount}회 밴</span>
              <span className="m-stat-chip" style={{ color: e.banRate >= 50 ? '#FF4757' : e.banRate >= 30 ? '#FF6B2B' : 'inherit', fontVariantNumeric: 'tabular-nums' }}>
                밴율 {e.banRate.toFixed(1)}%
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

const PROFILE_COLOR: Record<string, string> = {
  AD: '#FF6B2B', AP: '#9C27B0', Hybrid: '#4CAF50', Tank: '#2196F3', Unknown: '#9E9E9E',
};

function DamageTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-damage-e', mode],
    queryFn: () => api.get<DamageAnalysisResult>(`/stats/damage-analysis?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">데미지 분석 순위</p>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        const totalRatio = p.physicalRatio + p.magicRatio + p.trueRatio;
        const physW = totalRatio > 0 ? (p.physicalRatio / totalRatio * 100) : 0;
        const magW = totalRatio > 0 ? (p.magicRatio / totalRatio * 100) : 0;
        const trueW = totalRatio > 0 ? (p.trueRatio / totalRatio * 100) : 0;
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
              </div>
              <span style={{ fontSize: 12, fontWeight: 700, color: PROFILE_COLOR[p.damageProfile] ?? 'inherit' }}>{p.damageProfile}</span>
            </div>
            <div className="m-stat-chips" style={{ marginBottom: 6 }}>
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip" style={{ fontVariantNumeric: 'tabular-nums' }}>평균 {Math.round(p.avgTotalDamage).toLocaleString()}</span>
              <span className="m-stat-chip" style={{ fontVariantNumeric: 'tabular-nums' }}>포탑 {Math.round(p.avgTurretDamage).toLocaleString()}</span>
            </div>
            <div style={{ display: 'flex', height: 6, borderRadius: 3, overflow: 'hidden', gap: 1 }}>
              <div style={{ width: `${physW}%`, background: '#FF6B2B', borderRadius: '3px 0 0 3px' }} title={`물리 ${physW.toFixed(0)}%`} />
              <div style={{ width: `${magW}%`, background: '#9C27B0' }} title={`마법 ${magW.toFixed(0)}%`} />
              <div style={{ width: `${trueW}%`, background: '#9E9E9E', borderRadius: '0 3px 3px 0' }} title={`트루 ${trueW.toFixed(0)}%`} />
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 3, fontSize: 10, color: 'var(--color-text-secondary)' }}>
              <span style={{ color: '#FF6B2B' }}>물리 {(p.physicalRatio * 100).toFixed(0)}%</span>
              <span style={{ color: '#9C27B0' }}>마법 {(p.magicRatio * 100).toFixed(0)}%</span>
              <span>트루 {(p.trueRatio * 100).toFixed(0)}%</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

const RENDER_MAP: Record<EfficiencySubTab, (mode: string) => React.ReactNode> = {
  '골드효율': mode => <GoldEffTab mode={mode} />,
  '생존력':   mode => <SurvivalTab mode={mode} />,
  '밴분석':   mode => <BanTab mode={mode} />,
  '데미지':   mode => <DamageTab mode={mode} />,
};

export default function MobileEfficiencyGroup() {
  return (
    <MobileSubTabShell
      tabs={EFFICIENCY_SUB_TABS}
      defaultTab="골드효율"
      renderTab={(sub, mode) => RENDER_MAP[sub](mode)}
    />
  );
}
