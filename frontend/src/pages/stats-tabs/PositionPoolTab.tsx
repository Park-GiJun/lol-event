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

  const POSITIONS = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'];
  const POS_LABEL: Record<string, string> = { TOP: '탑', JUNGLE: '정글', MID: '미드', BOTTOM: '원딜', SUPPORT: '서폿' };

  const posPlayers = data.allPlayers.filter((p: PlayerPositionEntry) => p.position === selectedPos).sort((a: PlayerPositionEntry, b: PlayerPositionEntry) => b.games - a.games);
  const allRiotIds = [...new Set(data.allPlayers.map((p: PlayerPositionEntry) => p.riotId))].sort();

  const playerEntries = selectedPlayer
    ? data.allPlayers.filter((p: PlayerPositionEntry) => p.riotId === selectedPlayer).sort((a: PlayerPositionEntry, b: PlayerPositionEntry) => b.games - a.games)
    : [];

  return (
    <div>
      <div className="grid-16" style={{ marginBottom: 16 }}>
        {POSITIONS.map(pos => (
          <button key={pos} className="col-span-3" onClick={() => { setSelectedPos(pos); setSelectedPlayer(null); }}
            style={{ padding: '6px 14px', borderRadius: 20, border: '1px solid var(--color-border)', background: selectedPos === pos && !selectedPlayer ? 'var(--color-primary)' : 'var(--color-bg-hover)', color: selectedPos === pos && !selectedPlayer ? '#fff' : 'var(--color-text-primary)', cursor: 'pointer', fontWeight: selectedPos === pos ? 600 : 400, fontSize: 12 }}>
            {POS_LABEL[pos]}
          </button>
        ))}
        <select onChange={e => setSelectedPlayer(e.target.value || null)} value={selectedPlayer ?? ''}
          style={{ padding: '6px 10px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--color-bg-hover)', color: 'var(--color-text-primary)', cursor: 'pointer', fontSize: 12 }}>
          <option value="">플레이어 선택</option>
          {(allRiotIds as string[]).map(id => <option key={id} value={id}>{id.split('#')[0]}</option>)}
        </select>
      </div>

      {!selectedPlayer ? (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 11 }}>
              <th style={{ textAlign: 'left', padding: '6px 8px' }}>플레이어</th>
              <th style={{ textAlign: 'center', padding: '6px 8px' }}>주챔피언</th>
              <th style={{ textAlign: 'right', padding: '6px 8px' }}>게임</th>
              <th style={{ textAlign: 'right', padding: '6px 8px' }}>승률</th>
            </tr>
          </thead>
          <tbody>
            {posPlayers.map((p: PlayerPositionEntry) => {
              const c = p.topChampionId ? champions.get(p.topChampionId) : null;
              return (
                <tr key={p.riotId} style={{ borderBottom: '1px solid var(--color-border)', cursor: 'pointer' }}
                  onClick={() => setSelectedPlayer(p.riotId)}>
                  <td style={{ padding: '8px' }}><PlayerLink riotId={p.riotId}>{p.riotId.split('#')[0]}</PlayerLink></td>
                  <td style={{ padding: '8px', textAlign: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4 }}>
                      {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={24} height={24} style={{ borderRadius: 4 }} />}
                      <span>{c?.nameKo ?? p.topChampion ?? '-'}</span>
                    </div>
                  </td>
                  <td style={{ padding: '8px', textAlign: 'right' }}>{p.games}</td>
                  <td style={{ padding: '8px', textAlign: 'right', color: p.winRate >= 60 ? 'var(--color-win)' : p.winRate < 45 ? 'var(--color-loss)' : 'inherit' }}>{p.winRate.toFixed(1)}%</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      ) : (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <button onClick={() => setSelectedPlayer(null)} style={{ padding: '4px 10px', borderRadius: 6, border: '1px solid var(--color-border)', background: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', fontSize: 12 }}>← 뒤로</button>
            <strong><PlayerLink riotId={selectedPlayer}>{selectedPlayer.split('#')[0]}</PlayerLink></strong> 포지션별 챔피언 풀
          </div>
          {playerEntries.map((pe: PlayerPositionEntry) => (
            <div key={pe.position} style={{ marginBottom: 20 }}>
              <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8 }}>{POS_LABEL[pe.position] ?? pe.position} ({pe.games}게임 · {pe.winRate.toFixed(1)}%)</div>
              <div className="grid-16">
                {pe.champions.slice(0, 8).map((ce: PositionChampEntry) => {
                  const c = champions.get(ce.championId);
                  return (
                    <div key={ce.champion} className="col-span-2" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
                      {c?.imageUrl ? (
                        <img src={c.imageUrl} alt={c.nameKo} width={44} height={44} style={{ borderRadius: 8, border: '1px solid var(--color-border)' }} />
                      ) : (
                        <div style={{ width: 44, height: 44, borderRadius: 8, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10 }}>{ce.champion.slice(0, 2)}</div>
                      )}
                      <span style={{ fontSize: 9, color: 'var(--color-text-secondary)', textAlign: 'center' }}>{c?.nameKo ?? ce.champion}</span>
                      <span style={{ fontSize: 9, color: ce.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>{ce.winRate.toFixed(0)}% ({ce.games})</span>
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
