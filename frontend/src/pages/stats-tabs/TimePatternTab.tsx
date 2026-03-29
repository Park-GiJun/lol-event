import { useQuery } from '@tanstack/react-query';
import { api } from '../../lib/api/api';
import type { TimePatternResult, DayPatternEntry, HourPatternEntry } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';

export default function TimePatternTab({ mode }: { mode: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['time-pattern', mode],
    queryFn: () => api.get<TimePatternResult>(`/stats/time-pattern?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data) return <div style={{ padding: 24, color: 'var(--color-text-secondary)' }}>데이터 없음</div>;

  const maxDayGames = Math.max(...data.byDay.map((d: DayPatternEntry) => d.games), 1);
  const maxHourGames = Math.max(...data.byHour.map((h: HourPatternEntry) => h.games), 1);

  return (
    <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
      <div style={{ flex: 1, minWidth: 200 }}>
        <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 14 }}>요일별 내전 횟수</div>
        {data.busiestDay && <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>가장 활발한 요일: <strong>{data.busiestDay}요일</strong></div>}
        {data.byDay.map((d: DayPatternEntry) => (
          <div key={d.dayOfWeek} style={{ marginBottom: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 3 }}>
              <span>{d.dayName}요일</span>
              <span style={{ color: 'var(--color-text-secondary)' }}>{d.games}게임 ({d.sessions}세션)</span>
            </div>
            <div style={{ height: 8, borderRadius: 4, background: 'var(--color-bg-hover)', overflow: 'hidden' }}>
              <div style={{ height: '100%', borderRadius: 4, background: 'var(--color-primary)', width: `${d.games / maxDayGames * 100}%`, transition: 'width 0.3s' }} />
            </div>
          </div>
        ))}
      </div>
      <div style={{ flex: 1, minWidth: 200 }}>
        <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 14 }}>시간대별 내전 횟수</div>
        {data.busiestHour !== null && <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>가장 활발한 시간: <strong>{data.busiestHour}시</strong></div>}
        {data.byHour.map((h: HourPatternEntry) => (
          <div key={h.hour} style={{ marginBottom: 6 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 2 }}>
              <span>{h.hour}시</span>
              <span style={{ color: 'var(--color-text-secondary)' }}>{h.games}게임</span>
            </div>
            <div style={{ height: 6, borderRadius: 3, background: 'var(--color-bg-hover)', overflow: 'hidden' }}>
              <div style={{ height: '100%', borderRadius: 3, background: 'var(--color-primary)', width: `${h.games / maxHourGames * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
