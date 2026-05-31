import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { PlaystyleDnaResult, PlaystyleDnaEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';

const DNA_LABELS: { key: keyof PlaystyleDnaEntry; label: string; color: string }[] = [
  { key: 'aggression',     label: '공격성',    color: '#e74c3c' },
  { key: 'durability',     label: '생존력',    color: '#3498db' },
  { key: 'teamPlay',       label: '팀 플레이', color: '#2ecc71' },
  { key: 'objectiveFocus', label: '오브젝트',  color: '#f39c12' },
  { key: 'economy',        label: '경제력',    color: '#9b59b6' },
  { key: 'visionControl',  label: '시야',      color: '#1abc9c' },
];

export default function DnaTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<PlaystyleDnaResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<PlaystyleDnaResult>(`/stats/playstyle-dna?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-sm)' }}>
        {data.players.map((p: PlaystyleDnaEntry) => (
          <div key={p.riotId} className="card" style={{ padding: 'var(--spacing-md) var(--spacing-lg)', cursor: 'pointer', transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast)' }}
            onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-md)' }}>
              <span style={{ fontWeight: 'var(--font-weight-extrabold)', fontSize: 'var(--font-size-sm)', color: 'var(--color-text-primary)' }}>{p.riotId.split('#')[0]}</span>
              <span className="badge badge-primary badge-sm">{p.styleTag}</span>
              <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)', marginLeft: 'auto' }}>
                {p.games}판
              </span>
            </div>
            <div className="grid-16">
              {DNA_LABELS.map(({ key, label, color }) => {
                const val = p[key] as number;
                return (
                  <div key={key} className="col-span-2">
                    <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginBottom: 3 }}>{label}</div>
                    <div style={{ height: 5, background: 'var(--color-bg-hover)', borderRadius: 'var(--radius-sm)', overflow: 'hidden', marginBottom: 3 }}>
                      <div style={{ width: `${Math.min(val * 100, 100)}%`, height: '100%', background: color, borderRadius: 'var(--radius-sm)', boxShadow: `0 0 4px ${color}66` }} />
                    </div>
                    <div style={{ fontSize: 'var(--font-size-xs)', color, fontWeight: 'var(--font-weight-bold)', fontVariantNumeric: 'tabular-nums' }}>{(val * 100).toFixed(0)}</div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
        {!data.players.length && (
          <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>데이터 없음</p>
        )}
      </div>
    </div>
  );
}
