import { CrownIcon } from '@/components/icons/LolIcons';
import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { LateGameResult, LateGamePlayerEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { RankBadge } from './shared';

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${String(s).padStart(2, '0')}`;
}

export default function LateGameTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<LateGameResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<LateGameResult>(`/stats/late-game?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  // 백엔드는 players 만 내려준다. 대표 선수는 목록 맨 앞에서 뽑는다.
  const lateGameKing = data.players[0]?.riotId ?? null;

  return (
    <div>
      {lateGameKing && (
        <div className="section-head" style={{ marginBottom: 'var(--spacing-md)' }}>
          <CrownIcon size={16} />
          <div>
            <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', fontWeight: 'var(--font-weight-bold)', letterSpacing: 'var(--tracking-wide)', textTransform: 'uppercase' }}>후반 지배왕</div>
            <div style={{ fontWeight: 'var(--font-weight-extrabold)', fontSize: 'var(--font-size-sm)', color: 'var(--color-primary)' }}>{lateGameKing.split('#')[0]}</div>
          </div>
        </div>
      )}
      <p style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-md)' }}>
        후반 영향력 — 억제기 파괴, 생존 시간, 킬링 스프리 종합 지수
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">억제기 파괴</th>
              <th className="table-number">평균 생존</th>
              <th className="table-number">킬링 스프리</th>
              <th className="table-number">최장 스프리</th>
              <th className="table-number">후반 지수</th>
            </tr>
          </thead>
          <tbody>
            {data.players.map((p: LateGamePlayerEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)' }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>
                    #{p.riotId.split('#')[1]}
                    {p.topChampion && <span> · {p.topChampion}</span>}
                  </div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{p.avgInhibitorKills.toFixed(2)}</td>
                <td className="table-number">{formatTime(p.avgSurvivalSeconds)}</td>
                <td className="table-number">{p.avgKillingSpree.toFixed(2)}</td>
                <td className="table-number">{p.longestKillingSpree}</td>
                <td className="table-number" style={{ fontWeight: 'var(--font-weight-bold)', color: 'var(--color-primary)' }}>
                  {p.lateGameScore.toFixed(1)}
                </td>
              </tr>
            ))}
            {!data.players.length && (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: 'var(--spacing-xl) 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
