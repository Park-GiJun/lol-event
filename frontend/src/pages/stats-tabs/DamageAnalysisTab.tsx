import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { DamageAnalysisResult, DamagePlayerEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge } from './shared';

// 데미지 유형/프로필 구분색 — 의미상 고정된 분류 색이라 유지한다.
const DMG_PHYSICAL = '#f97316';
const DMG_MAGIC = '#60a5fa';
const DMG_TRUE = 'var(--color-text-primary)';

const PROFILE_COLORS: Record<string, string> = {
  AD: DMG_PHYSICAL,
  AP: DMG_MAGIC,
  Hybrid: '#a78bfa',
  Tank: 'var(--color-text-disabled)',
  Unknown: 'var(--color-text-disabled)',
};

function DamageBar({ physical, magic, trueVal }: { physical: number; magic: number; trueVal: number }) {
  return (
    <div style={{ display: 'flex', height: 8, borderRadius: 'var(--radius-xs)', overflow: 'hidden', minWidth: 80, gap: 1 }}>
      <div style={{ width: `${physical * 100}%`, background: DMG_PHYSICAL, borderRadius: 'var(--radius-xs) 0 0 var(--radius-xs)', transition: 'width 0.3s' }} />
      <div style={{ width: `${magic * 100}%`, background: DMG_MAGIC, transition: 'width 0.3s' }} />
      <div style={{ width: `${trueVal * 100}%`, background: DMG_TRUE, borderRadius: '0 var(--radius-xs) var(--radius-xs) 0', transition: 'width 0.3s' }} />
    </div>
  );
}

function LegendDot({ color, label }: { color: string; label: string }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}>
      <span style={{ width: 9, height: 9, borderRadius: 'var(--radius-xs)', background: color, flexShrink: 0 }} />
      {label}
    </span>
  );
}

export default function DamageAnalysisTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<DamageAnalysisResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<DamageAnalysisResult>(`/stats/damage-analysis?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div className="section-head">
        <span className="icon-chip">⚔️</span>
        <span className="section-head-title">데미지 유형 분포</span>
        <div className="section-head-action" style={{ display: 'flex', gap: 'var(--spacing-md)', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
          <LegendDot color={DMG_PHYSICAL} label="물리" />
          <LegendDot color={DMG_MAGIC} label="마법" />
          <LegendDot color={DMG_TRUE} label="트루" />
        </div>
      </div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">프로필</th>
              <th className="table-number">총딜</th>
              <th style={{ minWidth: 100 }}>딜 비율</th>
              <th className="table-number">물리%</th>
              <th className="table-number">마법%</th>
              <th className="table-number">트루%</th>
              <th className="table-number">탱킹</th>
              <th className="table-number">포탑딜</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: DamagePlayerEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)' }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 'var(--font-size-2xs)', color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]} · {p.games}판</div>
                </td>
                <td className="table-number">
                  <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: 'var(--font-weight-bold)', color: PROFILE_COLORS[p.damageProfile] ?? 'var(--color-text-secondary)', padding: '2px 6px', borderRadius: 'var(--radius-md)', background: 'var(--color-bg-tertiary)', border: '1px solid var(--color-border)' }}>
                    {p.damageProfile}
                  </span>
                </td>
                <td className="table-number" style={{ fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>
                  {(p.avgTotalDamage / 1000).toFixed(1)}k
                </td>
                <td style={{ paddingTop: 'var(--spacing-sm)', paddingBottom: 'var(--spacing-sm)' }}>
                  <DamageBar physical={p.physicalRatio} magic={p.magicRatio} trueVal={p.trueRatio} />
                </td>
                <td className="table-number" style={{ color: DMG_PHYSICAL }}>{(p.physicalRatio * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ color: DMG_MAGIC }}>{(p.magicRatio * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ color: DMG_TRUE }}>{(p.trueRatio * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgDamageTaken / 1000).toFixed(1)}k</td>
                <td className="table-number">{(p.avgTurretDamage / 1000).toFixed(1)}k</td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={10} style={{ textAlign: 'center', padding: 'var(--spacing-2xl) 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
