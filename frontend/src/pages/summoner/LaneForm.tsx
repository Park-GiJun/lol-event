import { usePlayerEloHistory } from '@/hooks/usePlayerEloHistory';
import type { EloHistoryEntry } from '@/lib/types/stats';

/**
 * 라인전 폼 시계열.
 *
 * 예전에는 `lanePerformance`(킬관여·딜지분 등을 포지션 표준화해 0~1 로 합친 점수)를 막대로 그렸다.
 * 그 값은 검증에서 탈락해 사라졌다 — 개인 지표를 Elo 변동에 섞는 모든 조합이 안 섞는 쪽보다 나빴고,
 * 근거가 없어진 점수를 화면에만 남겨 둘 이유도 없었다.
 *
 * 지금 그리는 것은 **라인 맞대결의 실제 승패**다. 같은 자리 상대와 15분 시점(또는 경기 종료 시점)
 * 골드+경험치를 견줘 가른 결과라 해석이 단순하고, 레이팅을 실제로 움직인 것과 같은 신호다.
 *
 * 막대 높이는 그 경기에서 라인 레이팅이 움직인 폭이다. 강한 상대를 이기면 더 크게 오른다.
 */

/** 맞대결이 성립하지 않은 경기(칼바람이거나 포지션이 깨진 경기)는 회색으로 비워 둔다. */
function color(entry: EloHistoryEntry): string {
  if (entry.laneResult === 'WIN') return 'var(--color-win)';
  if (entry.laneResult === 'LOSS') return 'var(--color-loss)';
  return 'var(--color-border)';
}

function label(entry: EloHistoryEntry): string {
  if (entry.laneResult === 'NONE') return '라인 맞대결 없음';
  const opponent = entry.laneOpponent ? ` vs ${entry.laneOpponent.split('#')[0]}` : '';
  const result = entry.laneResult === 'WIN' ? '라인 승' : '라인 패';
  const delta = `${entry.delta > 0 ? '+' : ''}${entry.delta.toFixed(1)}`;
  return `${result}${opponent} · 팀 ${entry.win ? '승' : '패'} · Elo ${delta}`;
}

/** 기준선 위아래로 뻗는 막대. 길이는 그 경기의 라인 Elo 변동폭이다. */
function Bar({ entry, scale }: { entry: EloHistoryEntry; scale: number }) {
  const above = entry.laneResult === 'WIN';
  const height = scale > 0 ? Math.min(100, (Math.abs(entry.delta) / scale) * 100) : 0;

  return (
    <div
      title={label(entry)}
      style={{
        flex: '1 1 0', minWidth: 6, height: 56, position: 'relative',
        display: 'flex', flexDirection: 'column', justifyContent: 'center',
      }}
    >
      {/* 기준선 위쪽 절반 */}
      <div style={{ height: 28, display: 'flex', alignItems: 'flex-end' }}>
        {above && (
          <div style={{ width: '100%', height: `${height}%`, background: color(entry), borderRadius: '2px 2px 0 0' }} />
        )}
      </div>
      {/* 기준선 아래쪽 절반 */}
      <div style={{ height: 28, display: 'flex', alignItems: 'flex-start' }}>
        {!above && (
          <div style={{ width: '100%', height: `${height}%`, background: color(entry), borderRadius: '0 0 2px 2px' }} />
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
  const duels = history.filter(e => e.laneResult !== 'NONE');
  if (duels.length === 0) return null;

  // 막대 높이는 이 구간의 최대 변동폭 기준이다. 절댓값 눈금을 붙일 만한 값이 아니라
  // "이 사람의 최근 경기들 중 어느 판이 크게 움직였나"를 보여 주는 상대 눈금이다.
  const scale = Math.max(...duels.map(e => Math.abs(e.delta)));
  const wins = duels.filter(e => e.laneResult === 'WIN').length;

  return (
    <section className="t-card">
      <div className="t-card-head">
        <h2 className="t-card-title">라인전 폼</h2>
        <span className="t-card-more">최근 {history.length}경기</span>
      </div>

      <p className="t-empty" style={{ padding: '0 0 10px', textAlign: 'left' }}>
        같은 자리 상대와 라인전 결과를 겨룬 기록이다. 위로 뻗으면 이겼다는 뜻이고,
        막대가 길수록 그 판에서 레이팅이 크게 움직였다.
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
          {history.map(e => <Bar key={e.matchId} entry={e} scale={scale} />)}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 18, marginTop: 10, fontSize: 12, color: 'var(--color-text-secondary)' }}>
        <span>
          라인 승률 <strong style={{ fontVariantNumeric: 'tabular-nums' }}>
            {Math.round((wins / duels.length) * 100)}%
          </strong>
        </span>
        <span>
          <strong style={{ fontVariantNumeric: 'tabular-nums' }}>{wins}</strong> / {duels.length}승
        </span>
      </div>
    </section>
  );
}
