import { POSITIONS as POS } from '@/lib/position';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../lib/api/api';
import type { PositionChampionPoolResult, PlayerPositionEntry, PositionChampEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { PlayerLink } from '../../components/common/PlayerLink';

export default function PositionPoolTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const [selectedPos, setSelectedPos] = useState('TOP');
  const [selectedPlayer, setSelectedPlayer] = useState<string | null>(null);
  const { data, isLoading } = useQuery({
    queryKey: ['pos-pool', mode],
    queryFn: () => api.get<PositionChampionPoolResult>(`/stats/position-champion-pool?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data) return <div style={{ padding: 24, color: 'var(--color-text-secondary)' }}>데이터 없음</div>;

  const POSITIONS = POS;
  const POS_LABEL: Record<string, string> = { TOP: '탑', JUNGLE: '정글', MID: '미드', BOTTOM: '원딜', SUPPORT: '서폿' };

  const posPlayers = data.allPlayers.filter((p: PlayerPositionEntry) => p.position === selectedPos).sort((a: PlayerPositionEntry, b: PlayerPositionEntry) => b.games - a.games);
  const allRiotIds = [...new Set(data.allPlayers.map((p: PlayerPositionEntry) => p.riotId))].sort();

  const playerEntries = selectedPlayer
    ? data.allPlayers.filter((p: PlayerPositionEntry) => p.riotId === selectedPlayer).sort((a: PlayerPositionEntry, b: PlayerPositionEntry) => b.games - a.games)
    : [];

  return (
    <div>
      <div className="grid-16" style={{ marginBottom: 'var(--spacing-md)' }}>
        {POSITIONS.map(pos => (
          <button key={pos} className={`member-sort-tab col-span-3 ${selectedPos === pos && !selectedPlayer ? 'active' : ''}`}
            onClick={() => { setSelectedPos(pos); setSelectedPlayer(null); }}>
            {POS_LABEL[pos]}
          </button>
        ))}
        <select onChange={e => setSelectedPlayer(e.target.value || null)} value={selectedPlayer ?? ''}
          style={{ padding: '6px 10px', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', background: 'var(--color-bg-secondary)', color: 'var(--color-text-primary)', cursor: 'pointer', fontSize: 'var(--font-size-xs)' }}>
          <option value="">플레이어 선택</option>
          {(allRiotIds as string[]).map(id => <option key={id} value={id}>{id.split('#')[0]}</option>)}
        </select>
      </div>

      {!selectedPlayer ? (
        <div className="table-wrapper">
          <table className="table member-stats-table" style={{ fontSize: 'var(--font-size-sm)' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left' }}>플레이어</th>
              <th style={{ textAlign: 'center' }}>주챔피언</th>
              <th className="table-number">게임</th>
              <th className="table-number">승률</th>
            </tr>
          </thead>
          <tbody>
            {posPlayers.map((p: PlayerPositionEntry) => {
              const c = p.topChampionId ? champions.get(p.topChampionId) : null;
              return (
                <tr key={p.riotId} className="member-stats-row"
                  onClick={() => setSelectedPlayer(p.riotId)}>
                  <td><PlayerLink riotId={p.riotId}><span style={{ fontWeight: 'var(--font-weight-semibold)' }}>{p.riotId.split('#')[0]}</span></PlayerLink></td>
                  <td style={{ textAlign: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 'var(--spacing-xs)' }}>
                      {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={24} height={24} style={{ borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border)' }} />}
                      <span>{c?.nameKo ?? p.topChampion ?? '-'}</span>
                    </div>
                  </td>
                  <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{p.games}</td>
                  <td className="table-number" style={{ fontWeight: 'var(--font-weight-bold)', color: p.winRate >= 60 ? 'var(--color-win)' : p.winRate < 45 ? 'var(--color-loss)' : 'var(--color-primary)' }}>{p.winRate.toFixed(1)}%</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        </div>
      ) : (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-md)' }}>
            <button className="btn btn-ghost btn-sm" onClick={() => setSelectedPlayer(null)}>← 뒤로</button>
            <span style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
              <strong style={{ color: 'var(--color-text-primary)' }}><PlayerLink riotId={selectedPlayer}>{selectedPlayer.split('#')[0]}</PlayerLink></strong> 포지션별 챔피언 풀
            </span>
          </div>
          {playerEntries.map((pe: PlayerPositionEntry) => (
            <div key={pe.position} style={{ marginBottom: 'var(--spacing-lg)' }}>
              <div className="section-head">
                <span className="icon-chip icon-chip-sm">{POS_LABEL[pe.position]?.[0] ?? pe.position[0]}</span>
                <span className="section-head-title">{POS_LABEL[pe.position] ?? pe.position}</span>
                <span className="section-head-action" style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)', fontVariantNumeric: 'tabular-nums' }}>{pe.games}게임 · {pe.winRate.toFixed(1)}%</span>
              </div>
              <div className="grid-16">
                {pe.champions.slice(0, 8).map((ce: PositionChampEntry) => {
                  const c = champions.get(ce.championId);
                  return (
                    <div key={ce.champion} className="col-span-2" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
                      {c?.imageUrl ? (
                        <img src={c.imageUrl} alt={c.nameKo} width={44} height={44} style={{ borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)' }} />
                      ) : (
                        <div style={{ width: 44, height: 44, borderRadius: 'var(--radius-md)', background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, color: 'var(--color-text-secondary)' }}>{ce.champion.slice(0, 2)}</div>
                      )}
                      <span style={{ fontSize: 9, color: 'var(--color-text-secondary)', textAlign: 'center' }}>{c?.nameKo ?? ce.champion}</span>
                      <span style={{ fontSize: 9, color: ce.winRate >= 60 ? 'var(--color-win)' : 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>{ce.winRate.toFixed(0)}% ({ce.games})</span>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
