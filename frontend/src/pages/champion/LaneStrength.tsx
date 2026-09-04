import type { ChampionLaneStrength, LaneGap } from '@/lib/types/stats';
import { positionLabel } from '@/lib/position';
import type { Position } from '@/lib/position';
import { POSITION_ICON } from '@/components/icons/positionIcon';

/** 격차 한 칸. 양수는 파랑, 음수는 빨강. 0 이면 굳이 물들이지 않는다. */
function Gap({ label, value, digits = 0 }: { label: string; value: number; digits?: number }) {
  const color =
    value > 0 ? 'var(--color-win)' :
    value < 0 ? 'var(--color-loss)' : 'var(--gray-500)';
  return (
    <div className="t-detail-cell">
      <span className="t-detail-label">{label}</span>
      <span className="t-detail-value" style={{ color }}>
        {value > 0 ? '+' : ''}{value.toFixed(digits)}
      </span>
    </div>
  );
}

export function GapCells({ gap }: { gap: LaneGap }) {
  return (
    <div className="t-detail-cells">
      <Gap label="골드" value={gap.goldDiff} />
      <Gap label="CS" value={gap.csDiff} digits={1} />
      <Gap label="딜량" value={gap.damageDiff} />
      <Gap label="킬" value={gap.killDiff} digits={1} />
      <Gap label="시야" value={gap.visionDiff} digits={1} />
    </div>
  );
}

/**
 * 라인전 지표.
 *
 * 승률만으로는 "왜 유리한지" 를 못 본다. 상대 라이너와 비교했을 때 골드·CS·딜량을
 * 얼마나 더 벌었는지가 상성의 실제 내용이다.
 *
 * 개별 상성(A vs B)이 아니라 챔피언 x 라인 단위라 표본이 두텁다. 154경기에서 개별 상성은
 * 574조합에 중앙 1경기지만, 챔피언 x 라인은 203조합에 5경기 이상이 49개다.
 */
export function LaneStrengthSection({ laneStrength }: { laneStrength: ChampionLaneStrength[] }) {
  if (laneStrength.length === 0) {
    return <p className="t-empty">라인이 배정된 경기가 없습니다.</p>;
  }
  return (
    <div style={{ display: 'grid', gap: 14, gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))' }}>
      {laneStrength.map((l) => {
        const Icon = POSITION_ICON[l.position as Position];
        return (
          <div key={l.position} style={{ background: 'var(--gray-50)', borderRadius: 12, padding: '12px 14px 14px' }}>
            <div className="t-detail-head" style={{ marginBottom: 10 }}>
              {Icon && <Icon size={15} />}
              <span>{positionLabel(l.position)}</span>
              <span style={{ marginLeft: 'auto', fontWeight: 500, color: 'var(--gray-500)' }}>
                {l.games}경기 · {l.wins}승 {l.games - l.wins}패
              </span>
            </div>
            <div style={{ marginBottom: 10 }}>
              <span style={{ fontSize: 20, fontWeight: 800, color: 'var(--gray-900)', fontVariantNumeric: 'tabular-nums' }}>
                {l.winRate}%
              </span>
              <span className="t-detail-sub" style={{ marginLeft: 6 }}>
                보정 {l.adjustedWinRate.toFixed(1)}%
              </span>
            </div>
            <div className="t-detail-sub" style={{ marginBottom: 6 }}>상대 라이너 대비</div>
            <GapCells gap={l.gap} />
          </div>
        );
      })}
    </div>
  );
}
