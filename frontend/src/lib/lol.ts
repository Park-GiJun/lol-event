import type { Match, Participant } from './types/match';

// ── Elo 티어 ──────────────────────────────────────────────────────────────
export function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return                  { label: 'Bronze',     color: '#CD7F32' };
}

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
export function calcMvp(match: Match): { aceId: string; blueMvpId: string; redMvpId: string } {
  const dur = Math.max(match.gameDuration / 60, 1);
  const score = (p: Participant) => {
    const teamDmg = match.participants.filter(x => x.team === p.team).reduce((s, x) => s + x.damage, 0) || 1;
    return (p.kills + p.assists) / Math.max(p.deaths, 1) * 10
      + (p.damage / teamDmg) * 40
      + p.visionScore / dur
      + p.cs / dur
      + (p.win ? 20 : 0);
  };
  const scored = match.participants.map(p => ({ p, s: score(p) }));
  const aceId     = scored.reduce((a, b) => a.s >= b.s ? a : b).p.riotId;
  const blueMvpId = scored.filter(x => x.p.team === 'blue').reduce((a, b) => a.s >= b.s ? a : b).p.riotId;
  const redMvpId  = scored.filter(x => x.p.team === 'red').reduce((a, b) => a.s >= b.s ? a : b).p.riotId;
  return { aceId, blueMvpId, redMvpId };
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
