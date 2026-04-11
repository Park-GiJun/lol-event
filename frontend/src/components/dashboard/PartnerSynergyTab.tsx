import { useDuoStats } from '@/hooks/useDuoStats';
import { useSortTable } from '@/hooks/useSortTable';
import { SortableTh } from '@/components/common/SortableTh';
import { PlayerLink } from '@/components/common/PlayerLink';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';
import { Button } from '@/components/common/Button';
import { useState } from 'react';
import type { DuoStat } from '@/lib/types/stats';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

type SortCol = 'winRate' | 'games' | 'kda' | 'avgKills';

function ColGroup() {
  return (
    <colgroup>
      <col style={{ width: 36 }} />
      <col />
      <col style={{ width: 90 }} />
      <col style={{ width: 56 }} />
      <col style={{ width: 64 }} />
      <col style={{ width: 80 }} />
    </colgroup>
  );
}

export function PartnerSynergyTab() {
  const [mode, setMode] = useState('normal');
  const { data, isLoading, error, refetch } = useDuoStats(mode, 1);
  const { sortKey, sortDir, handleSort, sorted } = useSortTable<SortCol>('winRate');

  const getValue = (key: SortCol, d: DuoStat): number => {
    if (key === 'winRate')  return d.winRate;
    if (key === 'games')    return d.games;
    if (key === 'kda')      return d.kda;
    if (key === 'avgKills') return d.avgKills;
    return 0;
  };

  const displayed = data ? sorted(data.duos, getValue) : [];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12, gap: 6 }}>
        {MODES.map(m => (
          <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'} size="sm" onClick={() => setMode(m.value)}>
            {m.label}
          </Button>
        ))}
      </div>

      {isLoading ? (
        <div className="table-wrapper">
          <table className="table member-stats-table">
            <ColGroup />
            <thead>
              <tr>
                <th>#</th><th>파트너 조합</th>
                <th className="table-number">승률</th>
                <th className="table-number">게임</th>
                <th className="table-number">KDA</th>
                <th className="table-number">평균 K/D/A</th>
              </tr>
            </thead>
            <tbody>
              {Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>
                  <td><Skeleton className="h-4 w-6" /></td>
                  <td><Skeleton className="h-4 w-40" /></td>
                  <td className="table-number"><Skeleton className="h-4 w-14 ml-auto" /></td>
                  <td className="table-number"><Skeleton className="h-4 w-8 ml-auto" /></td>
                  <td className="table-number"><Skeleton className="h-4 w-12 ml-auto" /></td>
                  <td className="table-number"><Skeleton className="h-4 w-20 ml-auto" /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : error ? (
        <InlineError message="파트너 데이터를 불러오지 못했습니다." onRetry={() => void refetch()} />
      ) : displayed.length === 0 ? (
        <p style={{ color: 'var(--color-text-secondary)', fontSize: 13 }}>듀오 데이터가 없습니다.</p>
      ) : (
        <div className="table-wrapper">
          <table className="table member-stats-table">
            <ColGroup />
            <thead>
              <tr>
                <th>#</th>
                <th>파트너 조합</th>
                <SortableTh label="승률"     col="winRate"  sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="게임"     col="games"    sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="KDA"      col="kda"      sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
                <SortableTh label="K/D/A"    col="avgKills" sortKey={sortKey} sortDir={sortDir} onSort={handleSort} right />
              </tr>
            </thead>
            <tbody>
              {displayed.map((d: DuoStat, idx) => {
                const wrColor = d.winRate >= 60 ? 'var(--color-win)' : d.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
                const kdaColor = d.kda >= 5 ? 'var(--color-win)' : d.kda >= 3 ? 'var(--color-primary)' : 'var(--color-text-primary)';
                return (
                  <tr key={`${d.player1}-${d.player2}`} className="member-stats-row">
                    <td style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>{idx + 1}</td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                        <PlayerLink riotId={d.player1} mode="all">
                          <span style={{ fontWeight: 700, fontSize: 13 }}>{d.player1.split('#')[0]}</span>
                        </PlayerLink>
                        <span style={{
                          color: 'var(--color-primary)', fontSize: 11, fontWeight: 700,
                          background: 'rgba(0,180,216,0.1)', borderRadius: 3,
                          padding: '1px 4px', border: '1px solid rgba(0,180,216,0.2)',
                        }}>+</span>
                        <PlayerLink riotId={d.player2} mode="all">
                          <span style={{ fontWeight: 700, fontSize: 13 }}>{d.player2.split('#')[0]}</span>
                        </PlayerLink>
                      </div>
                    </td>
                    <td className="table-number">
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'flex-end' }}>
                        <span style={{ fontWeight: 700, color: wrColor }}>{d.winRate.toFixed(1)}%</span>
                        <div style={{ height: 3, width: 50, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
                          <div style={{ width: `${d.winRate}%`, height: '100%', background: wrColor, borderRadius: 2 }} />
                        </div>
                      </div>
                    </td>
                    <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{d.games}</td>
                    <td className="table-number" style={{ fontWeight: 700, color: kdaColor }}>{d.kda.toFixed(2)}</td>
                    <td className="table-number" style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                      {d.avgKills.toFixed(1)} /
                      <span style={{ color: 'var(--color-error)' }}> {d.avgDeaths.toFixed(1)}</span>
                      {' '}/ {d.avgAssists.toFixed(1)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
