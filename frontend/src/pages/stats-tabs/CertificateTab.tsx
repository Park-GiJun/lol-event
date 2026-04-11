import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { ChampionCertificateResult, ChampionCertEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { ChampImg } from './shared';

export default function CertificateTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData]       = useState<ChampionCertificateResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<ChampionCertificateResult>(`/stats/champion-certificate?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        일정 판수 이상 + 높은 KDA + 승률 조건 충족 시 장인 인증
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 12 }}>
        {data.certifiedMasters.map((cert: ChampionCertEntry) => {
          const nameKo = champions.get(cert.championId)?.nameKo ?? cert.champion;
          return (
            <div key={`${cert.riotId}-${cert.champion}`} className="card"
              style={{ padding: '16px', borderLeft: '3px solid #FFD700', cursor: 'pointer', boxShadow: '0 0 16px rgba(255,215,0,0.06)', transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast)' }}
              onClick={() => navigate(`/player-stats/${encodeURIComponent(cert.riotId)}`)}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
                <ChampImg championId={cert.championId} champion={cert.champion} size={40} style={{ borderRadius: 8, boxShadow: '0 0 10px rgba(255,215,0,0.15)' }} />
                <div>
                  <div style={{ fontWeight: 800, fontSize: 14 }}>{cert.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 1 }}>{nameKo}</div>
                </div>
                <span style={{ marginLeft: 'auto', fontSize: 18, filter: 'drop-shadow(0 0 4px rgba(255,215,0,0.4))' }}>🎖️</span>
              </div>
              <div style={{ display: 'flex', gap: 12, fontSize: 12, fontVariantNumeric: 'tabular-nums' }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>{cert.games}판</span>
                <span style={{ fontWeight: 700, color: cert.winRate >= 60 ? 'var(--color-win)' : 'var(--color-primary)' }}>
                  {cert.winRate}%
                </span>
                <span style={{ color: 'var(--color-text-secondary)' }}>KDA <span style={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>{cert.kda.toFixed(2)}</span></span>
              </div>
            </div>
          );
        })}
        {!data.certifiedMasters.length && (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', gridColumn: '1/-1' }}>
            인증된 장인이 없습니다
          </p>
        )}
      </div>
    </div>
  );
}
