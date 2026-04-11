import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { OverviewStats, PlayerLeaderStat } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { ChampPickCard, HallCard } from './shared';

export default function OverviewTab({ mode }: { mode: string }) {
  const [data, setData]       = useState<OverviewStats | null>(null);
  const [loading, setLoading] = useState(true);
  const { champions }         = useDragon();
  const navigate              = useNavigate();

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<OverviewStats>(`/stats/overview?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const n = Math.max(data.matchCount, 1);

  const spotlights: { emoji: string; label: string; stat: PlayerLeaderStat | null }[] = [
    { emoji: '👑', label: '승률왕',  stat: data.winRateLeader },
    { emoji: '⚔️', label: 'KDA왕',  stat: data.kdaLeader },
    { emoji: '💥', label: '딜량왕',  stat: data.damageLeader },
    { emoji: '🎮', label: '판수왕',  stat: data.mostGamesPlayed },
  ];

  type HallEntry = { emoji: string; label: string; stat: PlayerLeaderStat | null; basis: 'per-min' | 'per-match' | 'total' };
  const hallGroups: { title: string; sub: string; items: HallEntry[] }[] = [
    {
      title: '경기당',
      sub: '경기 1판 기준 평균',
      items: [
        { emoji: '👑', label: '승률왕',  stat: data.winRateLeader,      basis: 'per-match' },
        { emoji: '⚔️', label: 'KDA왕',   stat: data.kdaLeader,          basis: 'per-match' },
        { emoji: '🗡️', label: '킬왕',    stat: data.killsLeader,        basis: 'per-match' },
        { emoji: '🏯', label: '포탑왕',  stat: data.turretKillsLeader,  basis: 'per-match' },
        { emoji: '🔭', label: '와드왕',  stat: data.wardsLeader,        basis: 'per-match' },
      ],
    },
    {
      title: '분당',
      sub: '분 기준 평균 (플레이 시간 보정)',
      items: [
        { emoji: '💥', label: '딜량왕',        stat: data.damageLeader,          basis: 'per-min' },
        { emoji: '💰', label: '골드왕',         stat: data.goldLeader,            basis: 'per-min' },
        { emoji: '🌾', label: 'CS왕',          stat: data.csLeader,              basis: 'per-min' },
        { emoji: '👁️', label: '시야왕',         stat: data.visionLeader,          basis: 'per-min' },
        { emoji: '🏰', label: '오브젝트딜왕',   stat: data.objectiveDamageLeader, basis: 'per-min' },
        { emoji: '🧊', label: 'CC왕',          stat: data.ccLeader,              basis: 'per-min' },
      ],
    },
    {
      title: '누적',
      sub: '총 합산 기록',
      items: [
        { emoji: '⭐', label: '펜타킬',  stat: data.pentaKillsLeader,  basis: 'total' },
        { emoji: '💀', label: '퍼블왕',  stat: data.firstBloodLeader,  basis: 'total' },
        { emoji: '🎮', label: '판수왕',  stat: data.mostGamesPlayed,   basis: 'total' },
      ],
    },
  ];

  return (
    <>
      {/* ① 핵심 지표 배너 */}
      <div className="overview-hero-row" style={{ background: 'linear-gradient(135deg, rgba(0,180,216,0.08) 0%, var(--glass-bg) 60%)', borderColor: 'rgba(0,180,216,0.2)', boxShadow: '0 0 32px rgba(0,180,216,0.06), inset 0 1px 0 rgba(0,180,216,0.15)' }}>
        <div className="overview-hero-main">
          <div className="overview-hero-number" style={{ background: 'var(--gradient-primary)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', textShadow: 'none' }}>{data.matchCount}</div>
          <div className="overview-hero-label">총 경기</div>
          <div className="overview-hero-sub">평균 {data.avgGameMinutes}분</div>
        </div>
        <div className="overview-hero-divider" />
        {[
          { emoji: '🐉', label: '드래곤', value: data.totalDragonKills, sub: `경기당 ${(data.totalDragonKills/n).toFixed(1)}` },
          { emoji: '🐲', label: '바론',   value: data.totalBaronKills,  sub: `경기당 ${(data.totalBaronKills/n).toFixed(1)}` },
          { emoji: '🏰', label: '포탑',   value: data.totalTowerKills,  sub: `경기당 ${(data.totalTowerKills/n).toFixed(1)}` },
          { emoji: '🌾', label: '총 CS',  value: data.totalCs, sub: `경기당 ${Math.round(data.totalCs / n)}` },
        ].map(({ emoji, label, value, sub }) => (
          <div key={label} className="overview-hero-stat">
            <div className="overview-hero-stat-emoji">{emoji}</div>
            <div className="overview-hero-stat-value">{value.toLocaleString()}</div>
            <div className="overview-hero-stat-label">{label}</div>
            <div className="overview-hero-stat-sub">{sub}</div>
          </div>
        ))}
      </div>

      {/* ② 스포트라이트 4인방 */}
      <div className="grid-16 overview-spotlight-row">
        {spotlights.map(({ emoji, label, stat }) => (
          <div key={label}
            className={`col-span-4 overview-spotlight-card${stat ? ' clickable' : ''}`}
            onClick={stat ? () => navigate(`/player-stats/${encodeURIComponent(stat.riotId)}`) : undefined}
            style={{ boxShadow: stat ? '0 0 0 1px transparent' : undefined }}
          >
            <div className="overview-spotlight-emoji" style={{ filter: 'drop-shadow(0 0 6px rgba(255,255,255,0.3))' }}>{emoji}</div>
            <div className="overview-spotlight-label">{label}</div>
            {stat ? (
              <>
                <div className="overview-spotlight-name">{stat.riotId.split('#')[0]}</div>
                <div className="overview-spotlight-value" style={{ background: 'var(--gradient-primary)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>{stat.displayValue}</div>
                <div className="overview-spotlight-games">{stat.games}판</div>
              </>
            ) : (
              <div className="overview-spotlight-empty">데이터 없음</div>
            )}
          </div>
        ))}
      </div>

      {/* ③ 챔피언 픽 + 사이드 정보 2열 */}
      <div className="grid-16 overview-main-row">
        {/* 왼쪽: 픽률 챔피언 */}
        <section className="col-span-11 stats-section card overview-main-left">
          <h2 className="stats-section-title">🏆 많이 사용된 챔피언</h2>
          <div className="overview-champ-grid">
            {data.topPickedChampions.slice(0, 20).map(s => (
              <ChampPickCard key={s.championId} stat={s} champions={champions}
                onClick={() => navigate(`/stats/champion/${encodeURIComponent(s.champion)}?mode=${mode}`)} />
            ))}
          </div>
        </section>

        {/* 오른쪽: 밴 + 승률 챔피언 */}
        <div className="col-span-5 overview-main-right">
          {data.topBannedChampions.length > 0 && (
            <section className="stats-section card">
              <h2 className="stats-section-title">🚫 많이 밴된 챔피언</h2>
              <div className="overview-champ-grid overview-champ-grid--sm">
                {data.topBannedChampions.slice(0, 6).map(s => (
                  <ChampPickCard key={s.championId} stat={s} champions={champions}
                    countLabel={`${s.picks}밴`}
                    imgStyle={{ filter: 'grayscale(60%)' }}
                    onClick={() => navigate(`/stats/champion/${encodeURIComponent(s.champion)}?mode=${mode}`)} />
                ))}
              </div>
            </section>
          )}
          {data.topWinRateChampions.length > 0 && (
            <section className="stats-section card">
              <h2 className="stats-section-title">📈 승률 높은 챔피언 <span className="stats-section-sub">최소 3픽</span></h2>
              <div className="overview-champ-grid overview-champ-grid--sm">
                {data.topWinRateChampions.slice(0, 6).map(s => (
                  <ChampPickCard key={s.championId} stat={s} champions={champions}
                    onClick={() => navigate(`/stats/champion/${encodeURIComponent(s.champion)}?mode=${mode}`)} />
                ))}
              </div>
            </section>
          )}
        </div>
      </div>

      {/* ④ 명예의 전당 */}
      <section className="stats-section card" style={{ background: 'linear-gradient(180deg, rgba(0,180,216,0.04) 0%, var(--glass-bg) 40%)' }}>
        <h2 className="stats-section-title">🏅 명예의 전당</h2>
        <div className="hall-groups">
          {hallGroups.map(group => (
            <div key={group.title} className="hall-group">
              <div className="hall-group-title">
                {group.title}
                <span className="hall-group-sub">{group.sub}</span>
              </div>
              <div className="hall-group-grid">
                {group.items.map(c => (
                  <HallCard key={c.label} emoji={c.emoji} label={c.label} stat={c.stat} basis={c.basis} mode={mode} />
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}
