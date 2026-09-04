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
  if (!data) return <div style={{ padding: 'var(--spacing-lg)', color: 'var(--color-text-secondary)' }}>데이터 없음</div>;

  const maxDayGames = Math.max(...data.byDay.map((d: DayPatternEntry) => d.games), 1);
  const maxHourGames = Math.max(...data.byHour.map((h: HourPatternEntry) => h.games), 1);

  return (
    <div className="grid-16" style={{ gap: 'var(--spacing-lg)' }}>
      <div className="col-span-8">
        <div className="section-head" style={{ paddingBottom: 'var(--spacing-sm)', borderBottom: '1px solid var(--color-border)' }}>
          <span className="icon-chip" aria-hidden></span>
          <span className="section-head-title">요일별 내전 횟수</span>
        </div>
        {data.busiestDay && <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-sm)' }}>가장 활발한 요일: <strong style={{ color: 'var(--color-primary)' }}>{data.busiestDay}요일</strong></div>}
        {data.byDay.map((d: DayPatternEntry) => {
          const ratio = d.games / maxDayGames;
          return (
            <div key={d.dayOfWeek} style={{ marginBottom: 'var(--spacing-sm)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-xs)', marginBottom: 4 }}>
                <span style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{d.dayName}요일</span>
                <span style={{ color: 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>{d.games}게임 <span style={{ color: 'var(--color-text-disabled)' }}>({d.sessions}세션)</span></span>
              </div>
              <div style={{ height: 8, borderRadius: 'var(--radius-sm)', background: 'var(--color-bg-hover)', overflow: 'hidden' }}>
                <div style={{ height: '100%', borderRadius: 'var(--radius-sm)', background: 'linear-gradient(90deg, var(--color-primary), color-mix(in srgb, var(--color-primary) 55%, transparent))', width: `${ratio * 100}%`, transition: 'width 0.4s ease', boxShadow: ratio > 0.8 ? '0 0 6px color-mix(in srgb, var(--color-primary) 40%, transparent)' : undefined }} />
              </div>
            </div>
          );
        })}
      </div>
      <div className="col-span-8">
        <div className="section-head" style={{ paddingBottom: 'var(--spacing-sm)', borderBottom: '1px solid var(--color-border)' }}>
          <span className="icon-chip" aria-hidden></span>
          <span className="section-head-title">시간대별 내전 횟수</span>
        </div>
        {data.busiestHour !== null && <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', marginBottom: 'var(--spacing-sm)' }}>가장 활발한 시간: <strong style={{ color: 'var(--color-primary)' }}>{data.busiestHour}시</strong></div>}
        {data.byHour.map((h: HourPatternEntry) => {
          const ratio = h.games / maxHourGames;
          return (
            <div key={h.hour} style={{ marginBottom: 7 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 'var(--font-size-xs)', marginBottom: 3 }}>
                <span style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>{h.hour}시</span>
                <span style={{ color: 'var(--color-text-secondary)', fontVariantNumeric: 'tabular-nums' }}>{h.games}게임</span>
              </div>
              <div style={{ height: 6, borderRadius: 3, background: 'var(--color-bg-hover)', overflow: 'hidden' }}>
                <div style={{ height: '100%', borderRadius: 3, background: 'linear-gradient(90deg, var(--color-primary), color-mix(in srgb, var(--color-primary) 45%, transparent))', width: `${ratio * 100}%`, transition: 'width 0.4s ease' }} />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
