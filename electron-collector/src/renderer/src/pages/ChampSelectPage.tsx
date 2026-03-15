import { useEffect, useState, useCallback } from 'react';
import { RefreshCw, Shield } from 'lucide-react';

interface Action {
  actorCellId: number;
  championId: number;
  completed: boolean;
  type: 'ban' | 'pick' | string;
}

interface MyTeamSlot {
  cellId: number;
  summonerName?: string;
  championId?: number;
  assignedPosition?: string;
  isPlaceholder?: boolean;
}

interface BannedChamp {
  championId: number;
  team: 'blue' | 'red';
}

interface ChampSelectState {
  myTeam: MyTeamSlot[];
  theirTeam: MyTeamSlot[];
  bans: BannedChamp[];
  timer: number;
  phase: string;
}

export function ChampSelectPage() {
  const [state, setState] = useState<ChampSelectState | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await window.lol.getChampSelect();
      if (!data) { setState(null); return; }

      const myTeam: MyTeamSlot[] = ((data['myTeam'] as unknown[]) ?? []).map((s) => {
        const slot = s as Record<string, unknown>;
        return {
          cellId: slot['cellId'] as number,
          summonerName: slot['summonerName'] as string | undefined,
          championId: slot['championId'] as number | undefined,
          assignedPosition: slot['assignedPosition'] as string | undefined,
        };
      });

      const theirTeam: MyTeamSlot[] = ((data['theirTeam'] as unknown[]) ?? []).map((s) => {
        const slot = s as Record<string, unknown>;
        return {
          cellId: slot['cellId'] as number,
          summonerName: slot['summonerName'] as string | undefined,
          championId: slot['championId'] as number | undefined,
          assignedPosition: slot['assignedPosition'] as string | undefined,
        };
      });

      const banActions: Action[] = [];
      for (const actionGroup of ((data['actions'] as unknown[][]) ?? [])) {
        for (const a of actionGroup) {
          const action = a as Action;
          if (action.type === 'ban' && action.completed && action.championId) {
            banActions.push(action);
          }
        }
      }

      const bans: BannedChamp[] = banActions.map((a, i) => ({
        championId: a.championId,
        team: i < 5 ? 'blue' : 'red',
      }));

      const timer = data['timer'] as Record<string, unknown> | undefined;
      const phase = (timer?.['phase'] ?? (data['phase'] ?? '')) as string;
      const remaining = (timer?.['adjustedTimeLeftInPhase'] ?? 0) as number;

      setState({ myTeam, theirTeam, bans, timer: Math.ceil(remaining / 1000), phase });
    } catch {
      setError('챔피언 선택 중이 아니거나 LCU 연결 실패');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const positionLabel: Record<string, string> = {
    top: '탑', jungle: '정글', middle: '미드', bottom: '원딜', utility: '서포터', '': '-',
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">챔피언 선택</h1>
        <p className="page-subtitle">밴픽 화면의 실시간 정보를 표시합니다</p>
      </div>

      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <span className="card-title"><Shield size={14} style={{ marginRight: 6 }} />밴픽 상태</span>
          <button className="btn btn-secondary btn-sm" onClick={load} disabled={loading}>
            <RefreshCw size={12} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
            {loading ? '로딩 중...' : '새로고침'}
          </button>
        </div>
        {error && <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>{error}</p>}
        {!state && !error && !loading && (
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            챔피언 선택 화면이 아닙니다
          </p>
        )}
        {state && (
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            {state.phase && <span style={{ color: 'var(--color-primary)', fontSize: 'var(--font-size-xs)' }}>Phase: {state.phase}</span>}
            {state.timer > 0 && <span style={{ color: 'var(--color-warning)', fontSize: 'var(--font-size-xs)' }}>⏱ {state.timer}초</span>}
          </div>
        )}
      </div>

      {state && (
        <>
          {state.bans.length > 0 && (
            <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
              <div className="card-header">
                <span className="card-title">밴 목록</span>
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {state.bans.map((ban, i) => (
                  <div key={i} style={{
                    padding: '4px 10px', borderRadius: 'var(--radius-sm)',
                    background: ban.team === 'blue' ? 'rgba(59,158,255,0.15)' : 'rgba(232,64,64,0.15)',
                    border: `1px solid ${ban.team === 'blue' ? 'rgba(59,158,255,0.3)' : 'rgba(232,64,64,0.3)'}`,
                    fontSize: 'var(--font-size-xs)', color: ban.team === 'blue' ? 'var(--color-info)' : 'var(--color-error)',
                  }}>
                    #{ban.championId}
                  </div>
                ))}
              </div>
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--spacing-md)' }}>
            <div className="card">
              <div className="card-header">
                <span className="card-title" style={{ color: 'var(--color-info)' }}>블루팀</span>
              </div>
              {state.myTeam.map((slot, i) => (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  padding: '6px 0', borderBottom: i < state.myTeam.length - 1 ? '1px solid var(--color-border)' : 'none',
                }}>
                  <span style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)', width: 30 }}>
                    {positionLabel[slot.assignedPosition ?? ''] ?? '-'}
                  </span>
                  <span style={{ flex: 1, fontSize: 'var(--font-size-sm)' }}>
                    {slot.summonerName || `Player ${i + 1}`}
                  </span>
                  {slot.championId ? (
                    <span style={{ color: 'var(--color-primary)', fontSize: 'var(--font-size-xs)' }}>#{slot.championId}</span>
                  ) : (
                    <span style={{ color: 'var(--color-text-disabled)', fontSize: 'var(--font-size-xs)' }}>선택 중</span>
                  )}
                </div>
              ))}
            </div>

            <div className="card">
              <div className="card-header">
                <span className="card-title" style={{ color: 'var(--color-error)' }}>레드팀</span>
              </div>
              {state.theirTeam.map((slot, i) => (
                <div key={i} style={{
                  display: 'flex', alignItems: 'center', gap: 8,
                  padding: '6px 0', borderBottom: i < state.theirTeam.length - 1 ? '1px solid var(--color-border)' : 'none',
                }}>
                  <span style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-xs)', width: 30 }}>
                    {positionLabel[slot.assignedPosition ?? ''] ?? '-'}
                  </span>
                  <span style={{ flex: 1, fontSize: 'var(--font-size-sm)' }}>
                    {slot.summonerName || `Player ${i + 1}`}
                  </span>
                  {slot.championId ? (
                    <span style={{ color: 'var(--color-primary)', fontSize: 'var(--font-size-xs)' }}>#{slot.championId}</span>
                  ) : (
                    <span style={{ color: 'var(--color-text-disabled)', fontSize: 'var(--font-size-xs)' }}>선택 중</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        </>
      )}

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
