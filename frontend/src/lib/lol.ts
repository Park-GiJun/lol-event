import type { Match, Participant } from './types/match';

// ── 게임 시간 포맷 (초 → "mm:ss") ─────────────────────────────────────────
export function fmt(secs: number): string {
  return `${Math.floor(secs / 60)}:${String(secs % 60).padStart(2, '0')}`;
}

// ── RiotId 파싱 ────────────────────────────────────────────────────────────
export function parseRiotId(riotId: string): { name: string; tag: string } {
  const idx = riotId.indexOf('#');
  if (idx < 0) return { name: riotId, tag: '' };
  return { name: riotId.slice(0, idx), tag: riotId.slice(idx + 1) };
}

// ── MVP 계산 ──────────────────────────────────────────────────────────────
type Scored = { p: Participant; s: number };

/** 초기값 없는 reduce 는 빈 배열에서 터진다. 후보가 없으면 빈 riotId 를 준다. */
function bestId(scored: Scored[]): string {
  return scored.reduce<Scored | null>((a, b) => (a && a.s >= b.s ? a : b), null)?.p.riotId ?? '';
}

export function calcMvp(match: Match): { aceId: string; blueMvpId: string; redMvpId: string } {
  const participants = match.participants ?? [];
  const dur = Math.max(match.gameDuration / 60, 1);
  const score = (p: Participant) => {
    const teamDmg = participants.filter(x => x.team === p.team).reduce((s, x) => s + x.damage, 0) || 1;
    return (p.kills + p.assists) / Math.max(p.deaths, 1) * 10
      + (p.damage / teamDmg) * 40
      + p.visionScore / dur
      + p.cs / dur
      + (p.win ? 20 : 0);
  };
  const scored = participants.map(p => ({ p, s: score(p) }));
  return {
    aceId: bestId(scored),
    blueMvpId: bestId(scored.filter(x => x.p.team === 'blue')),
    redMvpId: bestId(scored.filter(x => x.p.team === 'red')),
  };
}

// ── 게임 모드 목록 ────────────────────────────────────────────────────────
export const MODES = [
  { value: 'normal', label: '5v5 내전' },
] as const;

// 하위 호환 — 기존 코드에서 MODES_2, MODES_WITH_ALL 참조
export const MODES_2 = MODES;

export const MODES_WITH_ALL = [
  { value: 'normal', label: '5v5 내전' },
] as const;

export type ModeValue = typeof MODES[number]['value'];
