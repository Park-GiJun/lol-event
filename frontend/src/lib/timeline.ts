/** 격차 표기. 양수에 + 를 붙이고, 반올림해서 0 이 되면 부호를 떼어 "-0" 이 안 나오게 한다. */
export function signed(v: number | null | undefined, digits = 0): string {
  if (v == null) return '-';
  const rounded = Number(v.toFixed(digits));
  if (rounded === 0) return digits ? (0).toFixed(digits) : '0';
  const text = digits ? rounded.toFixed(digits) : rounded.toLocaleString();
  return rounded > 0 ? `+${text}` : text;
}

/** 격차 색. 기준값 이하로 작은 차이는 호각으로 본다. */
export function diffColor(v: number | null | undefined, even = 0): string {
  if (v == null || Math.abs(v) <= even) return 'var(--color-text-secondary)';
  return v > 0 ? 'var(--color-win)' : 'var(--color-loss)';
}
