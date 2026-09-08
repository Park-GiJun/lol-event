import { usePlayerEloHistory } from '@/hooks/usePlayerEloHistory';
import type { EloHistoryEntry } from '@/lib/types/stats';

/**
 * 라인전 폼 시계열.
 *
 * player_elo_history.lane_performance 는 경기마다 이미 쌓이고 있었고 API 응답에도
 * 실려 나오는데 화면에서만 안 쓰고 있었다. 같은 승리인데 Elo 변동폭이 왜 다른지
 * 설명하는 값이라, 승패만 보는 것보다 이쪽이 실제 폼에 가깝다.
 *
 * 값은 0~1 이고 0.5 가 같은 포지션 상대와 호각이라는 뜻이다.
 */

const NEUTRAL = 0.5;

function color(v: number): string {
  if (v >= 0.6) return 'var(--color-win)';
  if (v <= 0.4) return 'var(--color-loss)';
  return 'var(--color-primary)';
}

/** 0~1 을 0(바닥)~1(천장) 높이 막대로. 0.5 를 가운데 기준선으로 둔다. */
function Bar({ entry }: { entry: EloHistoryEntry }) {
  const v = Math.max(0, Math.min(1, entry.lanePerformance));
  const above = v >= NEUTRAL;
  // 기준선 위/아래로 뻗는 길이. 최대 편차가 0.5 라 두 배 해서 0~100% 로 편다.
  const height = Math.abs(v - NEUTRAL) * 2 * 100;
  const title = `라인전 ${(v * 100).toFixed(0)}점 · ${entry.win ? '승' : '패'} · Elo ${entry.delta > 0 ? '+' : ''}${entry.delta.toFixed(1)}`;

  return (
    <div
      title={title}
      style={{
        flex: '1 1 0', minWidth: 6, height: 56, position: 'relative',
        display: 'flex', flexDirection: 'column', justifyContent: 'center',
      }}
    >
      {/* 기준선 위쪽 절반 */}
      <div style={{ height: 28, display: 'flex', alignItems: 'flex-end' }}>
        {above && (
          <div style={{ width: '100%', height: `${height}%`, background: color(v), borderRadius: '2px 2px 0 0' }} />
        )}
      </div>
      {/* 기준선 아래쪽 절반 */}
      <div style={{ height: 28, display: 'flex', alignItems: 'flex-start' }}>
        {!above && (
          <div style={{ width: '100%', height: `${height}%`, background: color(v), borderRadius: '0 0 2px 2px' }} />
        )}
      </div>
    </div>
  );
}

export function LaneForm({ riotId }: { riotId: string }) {
  const { data, isPending, error } = usePlayerEloHistory(riotId);

  // 폼 그래프는 부가 정보다. 못 불러왔다고 화면 전체를 막지 않는다.
  if (isPending || error || !data || data.history.length === 0) return null;

  // 서버는 최신순으로 준다. 시간 순으로 읽히게 뒤집는다.
  const history = [...data.history].reverse();
  const avg = history.reduce((s, e) => s + e.lanePerformance, 0) / history.length;

  return (
    <section className="t-card">
      <div className="t-card-head">
        <h2 className="t-card-title">라인전 폼</h2>
        <span className="t-card-more">최근 {history.length}경기</span>
      </div>

      <p className="t-empty" style={{ padding: '0 0 10px', textAlign: 'left' }}>
        같은 자리 상대와 견줘 매긴 점수다. 가운데 선이 호각, 위로 뻗으면 이겼다는 뜻.
      </p>

      <div style={{ position: 'relative', padding: '4px 0' }}>
        {/* 호각 기준선 */}
        <div
          aria-hidden
          style={{
            position: 'absolute', left: 0, right: 0, top: '50%',
            borderTop: '1px dashed var(--color-border)',
          }}
        />
        <div style={{ display: 'flex', gap: 3, alignItems: 'stretch' }}>
          {history.map(e => <Bar key={e.matchId} entry={e} />)}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 18, marginTop: 10, fontSize: 12, color: 'var(--color-text-secondary)' }}>
        <span>
          평균 <strong style={{ color: color(avg), fontVariantNumeric: 'tabular-nums' }}>{(avg * 100).toFixed(0)}점</strong>
        </span>
        <span>
          호각 이상 <strong style={{ fontVariantNumeric: 'tabular-nums' }}>
            {history.filter(e => e.lanePerformance >= NEUTRAL).length}
          </strong> / {history.length}
        </span>
      </div>
    </section>
  );
}
