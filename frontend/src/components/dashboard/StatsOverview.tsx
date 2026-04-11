import { useState } from 'react';
import { useChampions } from '@/hooks/useChampions';
import { useLaneLeaderboard } from '@/hooks/useLaneLeaderboard';
import { PlayerLink } from '@/components/common/PlayerLink';
import { Skeleton } from '@/components/common/Skeleton';
import type { PlayerLeaderStat, PlayerLaneStat } from '@/lib/types/stats';

// ── 레인 설정 ───────────────────────────────────────────
const LANES = [
  { key: 'ALL',     label: '전체' },
  { key: 'TOP',     label: 'TOP' },
  { key: 'JUNGLE',  label: 'JG' },
  { key: 'MID',     label: 'MID' },
  { key: 'BOTTOM',  label: 'BOT' },
  { key: 'SUPPORT', label: 'SUP' },
];

// ── 리더 카드 ────────────────────────────────────────────
interface LeaderCardProps {
  icon: string;
  label: string;
  leader: PlayerLeaderStat | null | undefined;
  isLoading?: boolean;
  accent?: string;
}

function LeaderCard({ icon, label, leader, isLoading, accent = 'var(--color-primary)' }: LeaderCardProps) {
  return (
    <div className="col-span-4" style={{
      background: 'var(--color-bg-secondary)',
      border: '1px solid var(--color-border)',
      borderRadius: 8,
      padding: '11px 14px',
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      minWidth: 0,
      overflow: 'hidden',
    }}>
      {/* 아이콘 */}
      <div style={{
        width: 36, height: 36, flexShrink: 0,
        background: accent + '18',
        border: `1px solid ${accent}33`,
        borderRadius: 8,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 17,
      }}>
        {icon}
      </div>

      {/* 텍스트 */}
      <div style={{ flex: 1, minWidth: 0, overflow: 'hidden' }}>
        <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', fontWeight: 600, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 3 }}>
          {label}
        </div>
        {isLoading ? (
          <>
            <Skeleton className="h-4 w-20" style={{ marginBottom: 4 }} />
            <Skeleton className="h-3 w-12" />
          </>
        ) : leader ? (
          <>
            <PlayerLink riotId={leader.riotId} mode="all">
              <span style={{
                fontWeight: 700, fontSize: 13,
                color: 'var(--color-text-primary)',
                overflow: 'hidden', textOverflow: 'ellipsis',
                whiteSpace: 'nowrap', display: 'block',
              }}>
                {leader.riotId.split('#')[0]}
              </span>
            </PlayerLink>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
              <span style={{ fontSize: 12, color: accent, fontWeight: 700 }}>
                {leader.displayValue}
              </span>
              <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
                {leader.games}G
              </span>
            </div>
          </>
        ) : (
          <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>데이터 없음</span>
        )}
      </div>
    </div>
  );
}

// ── 레인 통계에서 리더 추출 ──────────────────────────────
function laneLeader(
  players: PlayerLaneStat[],
  key: keyof PlayerLaneStat,
  format: (v: number) => string,
): PlayerLeaderStat | null {
  if (!players.length) return null;
  const sorted = [...players].sort((a, b) => (b[key] as number) - (a[key] as number));
  const top = sorted[0];
  return { riotId: top.riotId, displayValue: format(top[key] as number), games: top.games };
}

// ── 전체 모드 카드 목록 ──────────────────────────────────
function OverallCards() {
  const { data: ov, isLoading } = useChampions();

  const cards: LeaderCardProps[] = [
    { icon: '🏆', label: 'KDA',      leader: ov?.kdaLeader,              accent: '#FFD700' },
    { icon: '⚔️',  label: '킬',      leader: ov?.killsLeader,            accent: '#FF6B6B' },
    { icon: '✨',  label: '승률',    leader: ov?.winRateLeader,          accent: 'var(--color-win)' },
    { icon: '💥',  label: '딜량',    leader: ov?.damageLeader,           accent: '#FF8C42' },
    { icon: '🌾',  label: 'CS',      leader: ov?.csLeader,               accent: '#6BCF7F' },
    { icon: '👁️',  label: '시야',    leader: ov?.visionLeader,           accent: '#7BB8F5' },
    { icon: '💰',  label: '골드',    leader: ov?.goldLeader,             accent: '#C89B3C' },
    { icon: '🏗️',  label: '포탑파괴', leader: ov?.turretKillsLeader,    accent: '#E8A030' },
    { icon: '🎯',  label: '목표딜',  leader: ov?.objectiveDamageLeader,  accent: '#A78BFA' },
    { icon: '⭐',  label: '펜타킬',  leader: ov?.pentaKillsLeader,       accent: '#F472B6' },
    { icon: '🩸',  label: '퍼블',    leader: ov?.firstBloodLeader,       accent: '#EF4444' },
    { icon: '📊',  label: '최다판',  leader: ov?.mostGamesPlayed,        accent: '#94A3B8' },
  ];

  return (
    <>
      {ov && (
        <div className="grid-16" style={{ marginBottom: 14, padding: '8px 12px', background: 'var(--color-bg-hover)', borderRadius: 7 }}>
          <span className="col-span-3"><Stat label="총 경기" value={`${ov.matchCount}게임`} /></span>
          <span className="col-span-1"><Divider /></span>
          <span className="col-span-3"><Stat label="평균시간" value={`${ov.avgGameMinutes.toFixed(1)}분`} /></span>
          <span className="col-span-1"><Divider /></span>
          <span className="col-span-3"><Stat label="바론" value={ov.totalBaronKills} color="#AA47BC" /></span>
          <span className="col-span-3"><Stat label="드래곤" value={ov.totalDragonKills} color="var(--color-win)" /></span>
          <span className="col-span-2"><Stat label="포탑" value={ov.totalTowerKills} color="#E8A030" /></span>
        </div>
      )}
      <CardGrid cards={cards} isLoading={isLoading} />
    </>
  );
}

function Divider() {
  return <span style={{ color: 'var(--color-border)', fontSize: 14 }}>|</span>;
}
function Stat({ label, value, color }: { label: string; value: string | number; color?: string }) {
  return (
    <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
      {label} <strong style={{ color: color ?? 'var(--color-text-primary)' }}>{value}</strong>
    </span>
  );
}

// ── 레인 모드 카드 목록 ──────────────────────────────────
function LaneCards({ lane }: { lane: string }) {
  const { data, isLoading } = useLaneLeaderboard(lane);
  const players = data?.players ?? [];

  const cards: LeaderCardProps[] = [
    { icon: '🏆', label: 'KDA',      leader: laneLeader(players, 'kda',      v => v.toFixed(2)),             accent: '#FFD700' },
    { icon: '⚔️',  label: '킬',      leader: laneLeader(players, 'avgKills', v => v.toFixed(1) + '킬'),     accent: '#FF6B6B' },
    { icon: '✨',  label: '승률',    leader: laneLeader(players, 'winRate',  v => v + '%'),                  accent: 'var(--color-win)' },
    { icon: '💥',  label: '딜량',    leader: laneLeader(players, 'avgDamage',v => Math.round(v).toLocaleString()), accent: '#FF8C42' },
    { icon: '🌾',  label: 'CS',      leader: laneLeader(players, 'avgCs',    v => v.toFixed(1)),             accent: '#6BCF7F' },
    { icon: '👁️',  label: '시야',    leader: laneLeader(players, 'avgVisionScore', v => v.toFixed(1)),      accent: '#7BB8F5' },
    { icon: '💰',  label: '골드',    leader: laneLeader(players, 'avgGold',  v => Math.round(v).toLocaleString()), accent: '#C89B3C' },
    { icon: '🛡️',  label: '피해흡수', leader: laneLeader(players, 'avgDamageTaken', v => Math.round(v).toLocaleString()), accent: '#64748B' },
    { icon: '🎯',  label: '목표딜',  leader: laneLeader(players, 'avgObjectiveDamage', v => Math.round(v).toLocaleString()), accent: '#A78BFA' },
    { icon: '🗺️',  label: '와드',    leader: laneLeader(players, 'avgWardsPlaced', v => v.toFixed(1)),      accent: '#38BDF8' },
    { icon: '⏱️',  label: 'CC시간',  leader: laneLeader(players, 'avgCcTime', v => v.toFixed(1) + 's'),     accent: '#FB923C' },
    { icon: '🏕️',  label: '정글링',  leader: laneLeader(players, 'avgNeutralMinions', v => v.toFixed(1)),   accent: '#84CC16' },
  ];

  return <CardGrid cards={cards} isLoading={isLoading} />;
}

function CardGrid({ cards, isLoading }: { cards: LeaderCardProps[]; isLoading?: boolean }) {
  return (
    <div className="grid-16">
      {cards.map(c => (
        <LeaderCard key={c.label} {...c} isLoading={isLoading} />
      ))}
    </div>
  );
}

// ── 메인 컴포넌트 ────────────────────────────────────────
export function StatsOverview() {
  const [lane, setLane] = useState('ALL');

  return (
    <div style={{
      background: 'var(--color-bg-card)',
      border: '1px solid var(--color-border)',
      borderRadius: 12,
      padding: '16px',
    }}>
      {/* 헤더 + 레인 필터 */}
      <div className="grid-16" style={{ alignItems: 'center', marginBottom: 14 }}>
        <h3 className="col-span-8" style={{ fontSize: 14, fontWeight: 700, margin: 0, color: 'var(--color-text-primary)' }}>
          명예의 전당
        </h3>
        <div className="col-span-8" style={{ display: 'flex', gap: 3, justifyContent: 'flex-end' }}>
          {LANES.map(l => (
            <button
              key={l.key}
              onClick={() => setLane(l.key)}
              style={{
                background: lane === l.key ? 'var(--color-primary)' : 'transparent',
                border: `1px solid ${lane === l.key ? 'var(--color-primary)' : 'var(--color-border)'}`,
                borderRadius: 5,
                padding: '4px 9px',
                fontSize: 11,
                fontWeight: lane === l.key ? 700 : 500,
                color: lane === l.key ? '#fff' : 'var(--color-text-secondary)',
                cursor: 'pointer',
                transition: 'all 0.15s',
              }}
            >
              {l.label}
            </button>
          ))}
        </div>
      </div>

      {lane === 'ALL' ? <OverallCards /> : <LaneCards lane={lane} />}
    </div>
  );
}
