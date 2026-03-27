import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { EloLeaderboard } from '@/components/dashboard/EloLeaderboard';
import { ChampionTierTable } from '@/components/dashboard/ChampionTierTable';
import { BanTrendCard } from '@/components/dashboard/BanTrendCard';
import { StatsOverview } from '@/components/dashboard/StatsOverview';
import { ChampionPicksTab } from '@/components/dashboard/ChampionPicksTab';
import { PartnerSynergyTab } from '@/components/dashboard/PartnerSynergyTab';
import { api } from '../lib/api/api';
import type { WeeklyAwardsResult, MultiKillHighlightsResult } from '../lib/types/stats';

const CURRENT_RIOT_ID_KEY = 'lol-event:currentRiotId';

const TABS = [
  { key: 'elo',       label: 'Elo 리더보드' },
  { key: 'champion',  label: '챔피언 티어표' },
  { key: 'ban',       label: '밴픽 트렌드' },
  { key: 'picks',     label: '챔피언 픽 순위' },
  { key: 'partner',   label: '파트너 시너지' },
];

export function HomePage() {
  const currentRiotId = localStorage.getItem(CURRENT_RIOT_ID_KEY) || undefined;
  const [tab, setTab] = useState('elo');

  const { data: awards } = useQuery({
    queryKey: ['home-awards'],
    queryFn: () => api.get<WeeklyAwardsResult>('/stats/awards?mode=normal'),
    staleTime: 5 * 60 * 1000,
  });
  const { data: mkHighlights } = useQuery({
    queryKey: ['home-multikill'],
    queryFn: () => api.get<MultiKillHighlightsResult>('/stats/multikill-highlights?mode=normal'),
    staleTime: 5 * 60 * 1000,
  });

  return (
    <div className="flex flex-col gap-6">
      {/* 상단: 전체 통계 리더 */}
      <StatsOverview />

      {/* 하이라이트 위젯 */}
      {(awards || mkHighlights) && (
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          {awards && (
            <div style={{ flex: 1, minWidth: 220, background: 'var(--color-bg-card)', borderRadius: 12, padding: '12px 16px', border: '1px solid var(--color-border)' }}>
              <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8 }}>🏆 이번 기간 어워즈</div>
              {[
                { label: '펜타킬', entry: awards.pentaKillHero, emoji: '⚔️' },
                { label: '승률왕', entry: awards.highestWinRate, emoji: '👑' },
                { label: '데스왕', entry: awards.mostDeaths, emoji: '💀' },
              ].map(({ label, entry, emoji }) => entry && (
                <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0', borderBottom: '1px solid var(--color-border)', fontSize: 12 }}>
                  <span>{emoji}</span>
                  <span style={{ color: 'var(--color-text-secondary)', width: 50 }}>{label}</span>
                  <span style={{ fontWeight: 700 }}>{entry.riotId.split('#')[0]}</span>
                  <span style={{ marginLeft: 'auto', color: 'var(--color-primary)', fontSize: 11 }}>{entry.displayValue}</span>
                </div>
              ))}
            </div>
          )}
          {mkHighlights && mkHighlights.pentaKillEvents.length > 0 && (
            <div style={{ flex: 1, minWidth: 220, background: 'var(--color-bg-card)', borderRadius: 12, padding: '12px 16px', border: '1px solid var(--color-border)' }}>
              <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8 }}>⚡ 최근 펜타킬</div>
              {mkHighlights.pentaKillEvents.slice(0, 3).map((e, i) => {
                const date = new Date(e.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
                return (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '4px 0', borderBottom: '1px solid var(--color-border)', fontSize: 12 }}>
                    <span style={{ fontWeight: 700, color: '#FFD700' }}>펜타!</span>
                    <span style={{ fontWeight: 600 }}>{e.riotId.split('#')[0]}</span>
                    <span style={{ color: 'var(--color-text-secondary)' }}>{e.champion}</span>
                    <span style={{ marginLeft: 'auto', color: 'var(--color-text-secondary)', fontSize: 11 }}>{date}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* 하단: 탭 전환 */}
      <div>
        <div style={{
          display: 'flex',
          gap: 0,
          borderBottom: '1px solid var(--color-border)',
          marginBottom: 16,
          overflowX: 'auto',
        }}>
          {TABS.map(t => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              style={{
                background: 'none',
                border: 'none',
                borderBottom: tab === t.key ? '2px solid var(--color-primary)' : '2px solid transparent',
                padding: '10px 18px',
                fontSize: 13,
                fontWeight: tab === t.key ? 700 : 500,
                color: tab === t.key ? 'var(--color-primary)' : 'var(--color-text-secondary)',
                cursor: 'pointer',
                transition: 'all 0.15s',
                marginBottom: -1,
                whiteSpace: 'nowrap',
                flexShrink: 0,
              }}
            >
              {t.label}
            </button>
          ))}
        </div>

        {tab === 'elo'      && <EloLeaderboard currentRiotId={currentRiotId} />}
        {tab === 'champion' && <ChampionTierTable />}
        {tab === 'ban'      && <BanTrendCard />}
        {tab === 'picks'    && <ChampionPicksTab />}
        {tab === 'partner'  && <PartnerSynergyTab />}
      </div>
    </div>
  );
}
