import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { DamageAnalysisResult, DamagePlayerEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge } from './shared';

const PROFILE_COLORS: Record<string, string> = {
  AD: '#f97316',
  AP: '#60a5fa',
  Hybrid: '#a78bfa',
  Tank: '#6b7280',
  Unknown: 'var(--color-text-disabled)',
};

function DamageBar({ physical, magic, trueVal }: { physical: number; magic: number; trueVal: number }) {
  return (
    <div style={{ display: 'flex', height: 8, borderRadius: 4, overflow: 'hidden', minWidth: 80, gap: 1 }}>
      <div style={{ width: `${physical * 100}%`, background: '#f97316', borderRadius: '4px 0 0 4px', transition: 'width 0.3s' }} />
      <div style={{ width: `${magic * 100}%`, background: '#60a5fa', transition: 'width 0.3s' }} />
      <div style={{ width: `${trueVal * 100}%`, background: '#f1f5f9', borderRadius: '0 4px 4px 0', transition: 'width 0.3s' }} />
    </div>
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
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
        데미지 유형 분포 — 물리(주황) / 마법(파랑) / 트루(흰색)
      </p>
      <div style={{ display: 'flex', gap: 16, fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        <span><span style={{ color: '#f97316', fontWeight: 700 }}>■</span> 물리</span>
        <span><span style={{ color: '#60a5fa', fontWeight: 700 }}>■</span> 마법</span>
        <span><span style={{ color: '#f1f5f9', fontWeight: 700 }}>■</span> 트루</span>
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
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]} · {p.games}판</div>
                </td>
                <td className="table-number">
                  <span style={{ fontSize: 11, fontWeight: 700, color: PROFILE_COLORS[p.damageProfile] ?? 'inherit', padding: '2px 6px', borderRadius: 8, background: (PROFILE_COLORS[p.damageProfile] ?? '#888') + '22' }}>
                    {p.damageProfile}
                  </span>
                </td>
                <td className="table-number" style={{ fontWeight: 700, color: 'var(--color-primary)' }}>
                  {(p.avgTotalDamage / 1000).toFixed(1)}k
                </td>
                <td style={{ paddingTop: 8, paddingBottom: 8 }}>
                  <DamageBar physical={p.physicalRatio} magic={p.magicRatio} trueVal={p.trueRatio} />
                </td>
                <td className="table-number" style={{ color: '#f97316' }}>{(p.physicalRatio * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ color: '#60a5fa' }}>{(p.magicRatio * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ color: '#e2e8f0' }}>{(p.trueRatio * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgDamageTaken / 1000).toFixed(1)}k</td>
                <td className="table-number">{(p.avgTurretDamage / 1000).toFixed(1)}k</td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={10} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
