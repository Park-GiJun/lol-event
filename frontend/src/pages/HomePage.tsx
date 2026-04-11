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
      <div style={{
        position: 'relative',
        borderRadius: 'var(--radius-xl)',
        padding: '28px 32px',
        overflow: 'hidden',
        background: 'linear-gradient(135deg, rgba(0,180,216,0.12) 0%, rgba(0,144,173,0.06) 50%, rgba(13,18,37,0.8) 100%)',
        border: '1px solid rgba(0,180,216,0.15)',
      }}>
        {/* 배경 글로우 오브 */}
        <div style={{
          position: 'absolute', top: -40, right: -40,
          width: 220, height: 220, borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(0,180,216,0.12) 0%, transparent 70%)',
          pointerEvents: 'none',
        }} />
        <div style={{
          position: 'absolute', bottom: -30, left: 80,
          width: 160, height: 160, borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(200,155,60,0.07) 0%, transparent 70%)',
          pointerEvents: 'none',
        }} />

        <div className="grid-16" style={{ alignItems: 'center', position: 'relative', zIndex: 1 }}>
          <div className="col-span-10">
            <div style={{
              fontSize: 'var(--font-size-xs)',
              fontWeight: 700,
              letterSpacing: 'var(--tracking-widest)',
              textTransform: 'uppercase',
              color: 'var(--color-primary)',
              marginBottom: 8,
            }}>
              내전 통계 대시보드
            </div>
            <h1 style={{
              fontSize: 'var(--font-size-xl)',
              fontWeight: 'var(--font-weight-extrabold)',
              lineHeight: 'var(--line-height-tight)',
              letterSpacing: 'var(--tracking-tight)',
              margin: 0,
            }}>
              <span className="text-gradient">LoL 이벤트</span>
              <span style={{ color: 'var(--color-text-primary)' }}> 리그</span>
            </h1>
            <p style={{
              fontSize: 'var(--font-size-sm)',
              color: 'var(--color-text-secondary)',
              marginTop: 6,
              lineHeight: 'var(--line-height-relaxed)',
            }}>
              실시간 Elo 랭킹 · 챔피언 통계 · 파트너 시너지
            </p>
          </div>
        </div>
      </div>

      {/* 상단: 전체 통계 리더 */}
      <StatsOverview />

      {/* 어워즈 하이라이트 */}
      {(awards || mkHighlights) && (
        <div className="grid-16">
          {awards && (
            <div className="col-span-8 card" style={{ animation: 'fadeInUp 0.4s ease both' }}>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 8,
                fontWeight: 700, fontSize: 13, marginBottom: 12,
              }}>
                <span style={{
                  width: 28, height: 28, borderRadius: 7,
                  background: 'linear-gradient(135deg, rgba(200,155,60,0.25), rgba(200,155,60,0.1))',
                  border: '1px solid rgba(200,155,60,0.3)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14,
                }}>🏆</span>
                <span style={{ color: 'var(--color-text-primary)' }}>이번 기간 어워즈</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                {[
                  { label: '펜타킬', entry: awards.pentaKillHero,   emoji: '⚔️',  accent: '#FF6B6B' },
                  { label: '승률왕', entry: awards.highestWinRate,  emoji: '👑',  accent: '#FFD700' },
                  { label: '데스왕', entry: awards.mostDeaths,      emoji: '💀',  accent: '#94A3B8' },
                ].map(({ label, entry, emoji, accent }) => entry && (
                  <div key={label} style={{
                    display: 'flex', alignItems: 'center', gap: 8,
                    padding: '7px 10px',
                    background: 'rgba(255,255,255,0.02)',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid rgba(255,255,255,0.04)',
                    fontSize: 12,
                    transition: 'background var(--transition-fast)',
                  }}>
                    <span style={{
                      width: 22, height: 22, borderRadius: 5,
                      background: accent + '20',
                      display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, flexShrink: 0,
                    }}>{emoji}</span>
                    <span style={{ color: 'var(--color-text-secondary)', width: 46, fontSize: 11 }}>{label}</span>
                    <span style={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>{entry.riotId.split('#')[0]}</span>
                    <span style={{ marginLeft: 'auto', color: accent, fontSize: 11, fontWeight: 700 }}>{entry.displayValue}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {mkHighlights && mkHighlights.pentaKillEvents.length > 0 && (
            <div className="col-span-8 card" style={{ animation: 'fadeInUp 0.4s 0.07s ease both' }}>
              <div style={{
                display: 'flex', alignItems: 'center', gap: 8,
                fontWeight: 700, fontSize: 13, marginBottom: 12,
              }}>
                <span style={{
                  width: 28, height: 28, borderRadius: 7,
                  background: 'linear-gradient(135deg, rgba(255,107,43,0.25), rgba(255,107,43,0.1))',
                  border: '1px solid rgba(255,107,43,0.3)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14,
                }}>⚡</span>
                <span style={{ color: 'var(--color-text-primary)' }}>최근 펜타킬</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                {mkHighlights.pentaKillEvents.slice(0, 3).map((e, i) => {
                  const date = new Date(e.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });
                  return (
                    <div key={i} style={{
                      display: 'flex', alignItems: 'center', gap: 8,
                      padding: '7px 10px',
                      background: 'rgba(255,255,255,0.02)',
                      borderRadius: 'var(--radius-sm)',
                      border: '1px solid rgba(255,255,255,0.04)',
                      fontSize: 12,
                    }}>
                      <span style={{
                        fontSize: 10, fontWeight: 800,
                        background: 'linear-gradient(135deg, #FFD700, #FF8C00)',
                        WebkitBackgroundClip: 'text',
                        WebkitTextFillColor: 'transparent',
                        backgroundClip: 'text',
                        letterSpacing: '0.02em',
                      }}>PENTA</span>
                      <span style={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>{e.riotId.split('#')[0]}</span>
                      <span style={{ color: 'var(--color-text-secondary)' }}>{e.champion}</span>
                      <span style={{ marginLeft: 'auto', color: 'var(--color-text-disabled)', fontSize: 11 }}>{date}</span>
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
        <div style={{
          display: 'flex',
          gap: 0,
          borderBottom: '1px solid var(--color-border)',
          overflowX: 'auto',
          padding: '0 4px',
        }}>
          {TABS.map(t => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              style={{
                background: 'none',
                border: 'none',
                borderBottom: tab === t.key
                  ? '2px solid var(--color-primary)'
                  : '2px solid transparent',
                padding: '13px 18px',
                fontSize: 13,
                fontWeight: tab === t.key ? 700 : 500,
                color: tab === t.key ? 'var(--color-primary)' : 'var(--color-text-secondary)',
                cursor: 'pointer',
                transition: 'all var(--transition-fast)',
                marginBottom: -1,
                whiteSpace: 'nowrap',
                flexShrink: 0,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
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
