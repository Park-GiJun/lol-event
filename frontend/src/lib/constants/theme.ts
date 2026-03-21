export const TIER_COLORS = {
  S: '#FFD700',
  A: '#00B4D8',
  B: '#8899BB',
  C: '#4A5568',
} as const;

export type TierKey = keyof typeof TIER_COLORS;
