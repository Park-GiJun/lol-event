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
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {data.players.map((p: PlaystyleDnaEntry) => (
          <div key={p.riotId} className="card" style={{ padding: '12px 16px', cursor: 'pointer' }}
            onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 10 }}>
              <span style={{ fontWeight: 700, fontSize: 14 }}>{p.riotId.split('#')[0]}</span>
              <span style={{ fontSize: 11, fontWeight: 700, background: 'var(--color-primary)22',
                color: 'var(--color-primary)', borderRadius: 4, padding: '2px 8px',
                border: '1px solid var(--color-primary)44' }}>
                {p.styleTag}
              </span>
              <span style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginLeft: 'auto' }}>
                {p.games}판
              </span>
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {DNA_LABELS.map(({ key, label, color }) => {
                const val = p[key] as number;
                return (
                  <div key={key} style={{ minWidth: 80 }}>
                    <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginBottom: 2 }}>{label}</div>
                    <div style={{ height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden', marginBottom: 2 }}>
                      <div style={{ width: `${Math.min(val * 100, 100)}%`, height: '100%', background: color, borderRadius: 2 }} />
                    </div>
                    <div style={{ fontSize: 10, color, fontWeight: 600 }}>{(val * 100).toFixed(0)}</div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
        {!data.players.length && (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>데이터 없음</p>
        )}
      </div>
    </div>
  );
}
