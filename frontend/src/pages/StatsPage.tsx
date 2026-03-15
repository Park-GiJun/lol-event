import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api/api';
import type { OverviewStats, PlayerLeaderStat, ChampionPickStat } from '../lib/types/stats';
import { LoadingCenter } from '../components/common/Spinner';
import { Button } from '../components/common/Button';
import { useDragon } from '../context/DragonContext';
import '../styles/pages/stats.css';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

// ── 챔피언 이미지 URL 헬퍼 ────────────────────────────
function champImgUrl(championId: number, champions: ReturnType<typeof useDragon>['champions']): string | null {
  const c = champions.get(championId);
  return c?.imageUrl ?? null;
}

// ── 챔피언 픽 카드 ────────────────────────────────────
function ChampPickCard({ stat, champions, onClick }: { stat: ChampionPickStat; champions: ReturnType<typeof useDragon>['champions']; onClick?: () => void }) {
  const imgUrl = champImgUrl(stat.championId, champions);
  const wrColor = stat.winRate >= 60 ? 'var(--color-win)' : stat.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <div className="champ-pick-card" onClick={onClick} style={{ cursor: onClick ? 'pointer' : undefined }}>
      <div className="champ-pick-img-wrap">
        {imgUrl
          ? <img src={imgUrl} alt={stat.champion} className="champ-pick-img" onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
          : <div className="champ-pick-img-fallback">{stat.champion.slice(0, 2)}</div>
        }
      </div>
      <div className="champ-pick-name">{stat.champion}</div>
      <div className="champ-pick-meta">
        <span className="champ-pick-count">{stat.picks}픽</span>
        <span className="champ-pick-wr" style={{ color: wrColor }}>{stat.winRate}%</span>
      </div>
    </div>
  );
}

// 기준 배지
function BasisBadge({ basis }: { basis: 'per-min' | 'per-match' | 'total' }) {
  const map = { 'per-min': { text: '/분', color: '#7c6af7' }, 'per-match': { text: '/경기', color: '#4a9eff' }, 'total': { text: '누적', color: '#888' } };
  const { text, color } = map[basis];
  return <span style={{ fontSize: 9, fontWeight: 700, background: color + '22', color, borderRadius: 4, padding: '1px 5px', border: `1px solid ${color}44` }}>{text}</span>;
}

// ── 명예의 전당 카드 ──────────────────────────────────
function HallCard({ emoji, label, stat, basis }: { emoji: string; label: string; stat: PlayerLeaderStat | null; basis: 'per-min' | 'per-match' | 'total' }) {
  const navigate = useNavigate();
  if (!stat) return (
    <div className="hall-card hall-card--empty">
      <div className="hall-emoji">{emoji}</div>
      <div className="hall-label">{label}</div>
      <div className="hall-empty-text">데이터 없음</div>
    </div>
  );
  return (
    <div className="hall-card" onClick={() => navigate(`/player-stats/${encodeURIComponent(stat.riotId)}`)}>
      <div className="hall-emoji">{emoji}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4, justifyContent: 'center' }}>
        <div className="hall-label">{label}</div>
        <BasisBadge basis={basis} />
      </div>
      <div className="hall-player">{stat.riotId.split('#')[0]}</div>
      <div className="hall-value">{stat.displayValue}</div>
      <div className="hall-games">{stat.games}판</div>
    </div>
  );
}

// ── 오브젝트 통계 카드 ────────────────────────────────
function ObjectCard({ emoji, label, value }: { emoji: string; label: string; value: number }) {
  return (
    <div className="obj-card">
      <div className="obj-emoji">{emoji}</div>
      <div className="obj-value">{value.toLocaleString()}</div>
      <div className="obj-label">{label}</div>
    </div>
  );
}

export function StatsPage() {
  const [data, setData]       = useState<OverviewStats | null>(null);
  const [mode, setMode]       = useState('normal');
  const [loading, setLoading] = useState(true);
  const { champions }         = useDragon();
  const navigate              = useNavigate();

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<OverviewStats>(`/stats/overview?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  const hallCards: { emoji: string; label: string; stat: PlayerLeaderStat | null; basis: 'per-min' | 'per-match' | 'total' }[] = data ? [
    { emoji: '👑', label: '승률왕',        stat: data.winRateLeader,          basis: 'per-match' },
    { emoji: '⚔️', label: 'KDA왕',         stat: data.kdaLeader,              basis: 'per-match' },
    { emoji: '🗡️', label: '킬왕',          stat: data.killsLeader,            basis: 'per-match' },
    { emoji: '💥', label: '딜량왕',         stat: data.damageLeader,           basis: 'per-min' },
    { emoji: '💰', label: '골드왕',         stat: data.goldLeader,             basis: 'per-min' },
    { emoji: '🌾', label: 'CS왕',          stat: data.csLeader,               basis: 'per-min' },
    { emoji: '👁️', label: '시야왕',         stat: data.visionLeader,           basis: 'per-min' },
    { emoji: '🏰', label: '오브젝트 딜왕',  stat: data.objectiveDamageLeader,  basis: 'per-min' },
    { emoji: '🏯', label: '포탑왕',         stat: data.turretKillsLeader,      basis: 'per-match' },
    { emoji: '⭐', label: '펜타킬',         stat: data.pentaKillsLeader,       basis: 'total' },
    { emoji: '🔭', label: '와드왕',         stat: data.wardsLeader,            basis: 'per-match' },
    { emoji: '🧊', label: 'CC왕',          stat: data.ccLeader,               basis: 'per-min' },
    { emoji: '🎮', label: '판수왕',         stat: data.mostGamesPlayed,        basis: 'total' },
  ] : [];

  return (
    <div>
      {/* ── 헤더 ── */}
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">전체 통계</h1>
          <p className="page-subtitle">
            총 {data?.matchCount ?? 0}경기
            {data?.avgGameMinutes ? ` · 평균 ${data.avgGameMinutes}분` : ''}
          </p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {loading ? <LoadingCenter /> : !data ? null : (
        <>
          {/* ── 챔피언 픽 통계 ── */}
          <section className="stats-section">
            <h2 className="stats-section-title">🏆 많이 사용된 챔피언</h2>
            <div className="champ-pick-grid">
              {data.topPickedChampions.map(s => (
                <ChampPickCard key={s.championId} stat={s} champions={champions}
                  onClick={() => navigate(`/stats/champion/${encodeURIComponent(s.champion)}?mode=${mode}`)} />
              ))}
            </div>
          </section>

          {/* ── 많이 밴된 챔피언 ── */}
          {data.topBannedChampions.length > 0 && (
            <section className="stats-section">
              <h2 className="stats-section-title">🚫 많이 밴된 챔피언</h2>
              <div className="champ-pick-grid">
                {data.topBannedChampions.map(s => (
                  <div key={s.championId} className="champ-pick-card">
                    <div className="champ-pick-img-wrap">
                      {champImgUrl(s.championId, champions)
                        ? <img src={champImgUrl(s.championId, champions)!} alt={s.champion} className="champ-pick-img"
                            style={{ filter: 'grayscale(60%)' }}
                            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                        : <div className="champ-pick-img-fallback">{s.champion.slice(0, 2)}</div>
                      }
                    </div>
                    <div className="champ-pick-name">{s.champion}</div>
                    <div className="champ-pick-meta">
                      <span className="champ-pick-count">{s.picks}밴</span>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* ── 승률 높은 챔피언 ── */}
          {data.topWinRateChampions.length > 0 && (
            <section className="stats-section">
              <h2 className="stats-section-title">📈 승률 높은 챔피언 <span className="stats-section-sub">(최소 3픽)</span></h2>
              <div className="champ-pick-grid">
                {data.topWinRateChampions.map(s => (
                  <ChampPickCard key={s.championId} stat={s} champions={champions}
                    onClick={() => navigate(`/stats/champion/${encodeURIComponent(s.champion)}?mode=${mode}`)} />
                ))}
              </div>
            </section>
          )}

          {/* ── 명예의 전당 ── */}
          <section className="stats-section">
            <h2 className="stats-section-title">🏅 명예의 전당</h2>
            <div className="hall-grid">
              {hallCards.map(c => (
                <HallCard key={c.label} emoji={c.emoji} label={c.label} stat={c.stat} basis={c.basis} />
              ))}
            </div>
          </section>

          {/* ── 오브젝트 통계 ── */}
          <section className="stats-section">
            <h2 className="stats-section-title">🎯 전체 오브젝트 통계</h2>
            <div className="obj-grid">
              <ObjectCard emoji="🐉" label="드래곤"     value={data.totalDragonKills} />
              <ObjectCard emoji="🐲" label="바론"       value={data.totalBaronKills} />
              <ObjectCard emoji="🏰" label="포탑"       value={data.totalTowerKills} />
              <ObjectCard emoji="🦅" label="전령"       value={data.totalRiftHeraldKills} />
              <ObjectCard emoji="🏚️" label="억제기"     value={data.totalInhibitorKills} />
              <ObjectCard emoji="💀" label="퍼스트 블러드" value={data.totalFirstBloods} />
            </div>
          </section>
        </>
      )}
    </div>
  );
}
