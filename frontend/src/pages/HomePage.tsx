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
  { key: 'elo',       label: 'Elo 리더보드',   icon: '🏆' },
  { key: 'champion',  label: '챔피언 티어표',   icon: '⚔️' },
  { key: 'ban',       label: '밴픽 트렌드',     icon: '🚫' },
  { key: 'picks',     label: '챔피언 픽 순위',  icon: '🎯' },
  { key: 'partner',   label: '파트너 시너지',   icon: '🤝' },
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
    <div className="flex flex-col gap-6 page-enter">
      {/* 히어로 배너 */}
      <div className="hero-banner">
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div className="hero-eyebrow">내전 통계 대시보드</div>
          <h1 className="hero-title">
            <span className="text-gradient">LoL 이벤트</span>
            <span style={{ color: 'var(--color-text-primary)' }}> 리그</span>
          </h1>
          <p className="hero-subtitle">실시간 Elo 랭킹 · 챔피언 통계 · 파트너 시너지</p>
        </div>
      </div>

      {/* 상단: 전체 통계 리더 */}
      <StatsOverview />

      {/* 어워즈 하이라이트 */}
      {(awards || mkHighlights) && (
        <div className="grid-16">
          {awards && (
            <div className="col-span-8 card" style={{ animation: 'fadeInUp 0.4s ease both' }}>
              <div className="section-head">
                <span className="icon-chip">🏆</span>
                <span className="section-head-title">이번 기간 어워즈</span>
              </div>
              <div>
                {[
                  { label: '펜타킬', entry: awards.pentaKillHero,   emoji: '⚔️',  accent: 'var(--color-loss)' },
                  { label: '승률왕', entry: awards.highestWinRate,  emoji: '👑',  accent: 'var(--color-primary)' },
                  { label: '데스왕', entry: awards.mostDeaths,      emoji: '💀',  accent: 'var(--color-text-secondary)' },
                ].map(({ label, entry, emoji, accent }) => entry && (
                  <div key={label} className="pill-row">
                    <span className="icon-chip icon-chip-sm">{emoji}</span>
                    <span className="pill-row-label" style={{ width: 46 }}>{label}</span>
                    <span className="pill-row-name">{entry.riotId.split('#')[0]}</span>
                    <span className="pill-row-value" style={{ color: accent, fontSize: 11 }}>{entry.displayValue}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {mkHighlights && mkHighlights.pentaKillEvents.length > 0 && (
            <div className="col-span-8 card" style={{ animation: 'fadeInUp 0.4s 0.07s ease both' }}>
              <div className="section-head">
                <span className="icon-chip">⚡</span>
                <span className="section-head-title">최근 펜타킬</span>
              </div>
              <div>
                {mkHighlights.pentaKillEvents.slice(0, 3).map((e, i) => {
                  const date = new Date(e.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
                  return (
                    <div key={i} className="pill-row">
                      <span style={{
                        fontSize: 10, fontWeight: 800,
                        background: 'linear-gradient(135deg, #FFD700, #FF8C00)',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent',
                        backgroundClip: 'text',
                        letterSpacing: '0.02em',
                      }}>PENTA</span>
                      <span className="pill-row-name">{e.riotId.split('#')[0]}</span>
                      <span style={{ color: 'var(--color-text-secondary)' }}>{e.champion}</span>
                      <span className="pill-row-value" style={{ color: 'var(--color-text-disabled)', fontSize: 11, fontWeight: 400 }}>{date}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}

      {/* 탭 전환 섹션 */}
      <div className="card" style={{ padding: 0, overflow: 'visible' }}>
        {/* 탭 헤더 */}
        <div className="tab-bar">
          {TABS.map(t => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`tab-bar-item${tab === t.key ? ' active' : ''}`}
            >
              <span style={{ fontSize: 12 }}>{t.icon}</span>
              {t.label}
            </button>
          ))}
        </div>

        {/* 탭 콘텐츠 */}
        <div style={{ padding: '20px', animation: 'fadeIn 0.18s ease both' }} key={tab}>
          {tab === 'elo'      && <EloLeaderboard currentRiotId={currentRiotId} />}
          {tab === 'champion' && <ChampionTierTable />}
          {tab === 'ban'      && <BanTrendCard />}
          {tab === 'picks'    && <ChampionPicksTab />}
          {tab === 'partner'  && <PartnerSynergyTab />}
        </div>
      </div>
    </div>
  );
}
