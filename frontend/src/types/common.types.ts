/**
 * 여러 도메인이 함께 쓰는 타입만 둔다. API 하나에서만 쓰는 DTO 는 그 `api/<domain>/*Api.ts` 에 둔다.
 */

/**
 * 통계를 낼 경기 범위.
 *
 * `all` 은 **이름과 달리 칼바람을 포함하지 않는다** — `normal` 과 같은 범위다. 칼바람은 통계
 * 집계에서 항상 빠진다.
 */
export type GameMode = 'normal' | 'aram' | 'all';

/** 표본 등급 — 경기 수가 그 숫자를 믿을 만큼 쌓였는지. */
export type SampleGrade = 'HIGH' | 'MEDIUM' | 'LOW' | 'INSUFFICIENT';
