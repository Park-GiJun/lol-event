import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { WeeklyAwardsResult } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';

const AWARD_LABELS: Record<string, string> = {
  mostDeaths:        '💀 단일 경기 최다 사망',
  worstKda:          '😢 평균 KDA 최하위',
  highGoldLowDamage: '💰 먹튀 골드왕',
  mostSurrenders:    '🏳️ 항복 유발자',
  pentaKillHero:     '⚔️ 펜타킬 영웅',
  loneHero:          '🦸 그래도 난 했다',
  highestWinRate:    '🏆 승률 1위',
  mostGamesChampion: '🎮 챔피언 장인',
};

export default function AwardsTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<WeeklyAwardsResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<WeeklyAwardsResult>(`/stats/awards?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const entries = Object.entries(AWARD_LABELS) as [keyof WeeklyAwardsResult, string][];

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        내전 어워즈 — 명예(?)의 전당
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: 12 }}>
        {entries.map(([key, label]) => {
          const entry = data[key];
          return (
            <div key={key} className="card" style={{ padding: '14px 16px', transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast)', cursor: entry ? 'pointer' : 'default' }}
              onClick={entry ? () => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`) : undefined}>
              <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--color-text-secondary)', letterSpacing: 'var(--tracking-wide)', textTransform: 'uppercase', marginBottom: 8 }}>{label}</div>
              {entry ? (
                <>
                  <div style={{ fontWeight: 700, fontSize: 15 }}>{entry.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--color-primary)', marginTop: 4, fontVariantNumeric: 'tabular-nums' }}>{entry.displayValue}</div>
                  <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginTop: 2 }}>{entry.games}판</div>
                </>
              ) : (
                <div style={{ color: 'var(--color-text-disabled)', fontSize: 12, marginTop: 4 }}>데이터 없음</div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
