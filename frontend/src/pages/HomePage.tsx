import { useState } from 'react';
import { EloLeaderboard } from '@/components/dashboard/EloLeaderboard';
import { ChampionTierTable } from '@/components/dashboard/ChampionTierTable';
import { BanTrendCard } from '@/components/dashboard/BanTrendCard';
import { StatsOverview } from '@/components/dashboard/StatsOverview';
import { ChampionPicksTab } from '@/components/dashboard/ChampionPicksTab';
import { PartnerSynergyTab } from '@/components/dashboard/PartnerSynergyTab';

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

  return (
    <div className="flex flex-col gap-6">
      {/* 상단: 전체 통계 리더 */}
      <StatsOverview />

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
