import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api/api';
import type {
  OverviewStats, PlayerLeaderStat, ChampionPickStat,
  MvpStatsResult, MvpPlayerStat,
  ChampionSynergyResult, ChampionSynergy,
  DuoStatsResult, DuoStat,
  LaneLeaderboardResult, PlayerLaneStat,
  EloLeaderboardResult, EloRankEntry,
} from '../lib/types/stats';
import { LoadingCenter } from '../components/common/Spinner';
import { Button } from '../components/common/Button';
import { useDragon } from '../context/DragonContext';
import { PlayerLink } from '../components/common/PlayerLink';
import { ChampionLink } from '../components/common/ChampionLink';
import '../styles/pages/stats.css';

const MODES = [
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
  { value: 'all',    label: '전체' },
];

const TABS = [
  { key: 'overview',  label: '📊 개요' },
  { key: 'elo',       label: '🏅 Elo 랭킹' },
  { key: 'mvp',       label: '🏆 MVP 랭킹' },
  { key: 'lane',      label: '🗺️ 라인별' },
  { key: 'synergy',   label: '⚡ 챔피언 시너지' },
  { key: 'duo',       label: '🤝 듀오 시너지' },
];

// ── 공통 컴포넌트 ─────────────────────────────────────
function champImgUrl(championId: number, champions: ReturnType<typeof useDragon>['champions']): string | null {
  return champions.get(championId)?.imageUrl ?? null;
}

function WinRateBar({ winRate, wins, losses }: { winRate: number; wins: number; losses: number }) {
  const color = winRate >= 60 ? 'var(--color-win)' : winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 90 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ fontWeight: 700, fontSize: 13, color, minWidth: 36 }}>{winRate}%</span>
        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
          <span style={{ color: 'var(--color-win)' }}>{wins}W</span>
          {' '}<span style={{ color: 'var(--color-loss)' }}>{losses}L</span>
        </span>
      </div>
      <div style={{ height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden' }}>
        <div style={{ width: `${winRate}%`, height: '100%', background: color, borderRadius: 2, transition: 'width 0.3s' }} />
      </div>
    </div>
  );
}

function ChampImg({ championId, champion, size }: { championId: number; champion: string; size: number }) {
  const { champions } = useDragon();
  const data = champions.get(championId);
  if (data?.imageUrl) return (
    <img src={data.imageUrl} alt={champion} width={size} height={size}
      style={{ borderRadius: 4, border: '1px solid var(--color-border)', objectFit: 'cover' }}
      onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />
  );
  return <div style={{ width: size, height: size, background: 'var(--color-bg-hover)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 9, color: 'var(--color-text-secondary)' }}>{champion.slice(0, 2)}</div>;
}

const RANK_COLORS: Record<number, string> = { 1: '#FFD700', 2: '#C0C0C0', 3: '#CD7F32' };
function RankBadge({ rank }: { rank: number }) {
  const color = RANK_COLORS[rank];
  if (color) return (
    <div style={{ width: 26, height: 26, borderRadius: '50%', background: color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 800, color: '#111', flexShrink: 0 }}>{rank}</div>
  );
  return <span style={{ color: 'var(--color-text-disabled)', fontSize: 12, width: 26, textAlign: 'center', display: 'inline-block' }}>{rank}</span>;
}

// ── 개요 탭 컴포넌트 ──────────────────────────────────
function ChampPickCard({ stat, champions, onClick, countLabel, imgStyle }: {
  stat: ChampionPickStat;
  champions: ReturnType<typeof useDragon>['champions'];
  onClick?: () => void;
  countLabel?: string;
  imgStyle?: React.CSSProperties;
}) {
  const imgUrl = champImgUrl(stat.championId, champions);
  const nameKo = champions.get(stat.championId)?.nameKo || stat.champion;
  const wrColor = stat.winRate >= 60 ? 'var(--color-win)' : stat.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <ChampionLink champion={stat.champion} championId={stat.championId} className="popup-trigger--card">
      <div className="champ-pick-card" onClick={onClick} style={{ cursor: 'pointer' }}>
        <div className="champ-pick-img-wrap">
          {imgUrl
            ? <img src={imgUrl} alt={nameKo} className="champ-pick-img" style={imgStyle} onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }} />
            : <div className="champ-pick-img-fallback">{nameKo.slice(0, 2)}</div>
          }
        </div>
        <div className="champ-pick-name">{nameKo}</div>
        <div className="champ-pick-meta">
          <span className="champ-pick-count">{countLabel ?? `${stat.picks}픽`}</span>
          {!countLabel && stat.winRate > 0 && <span className="champ-pick-wr" style={{ color: wrColor }}>{stat.winRate}%</span>}
        </div>
      </div>
    </ChampionLink>
  );
}

function BasisBadge({ basis }: { basis: 'per-min' | 'per-match' | 'total' }) {
  const map = { 'per-min': { text: '/분', color: '#7c6af7' }, 'per-match': { text: '/경기', color: '#4a9eff' }, 'total': { text: '누적', color: '#888' } };
  const { text, color } = map[basis];
  return <span style={{ fontSize: 9, fontWeight: 700, background: color + '22', color, borderRadius: 4, padding: '1px 5px', border: `1px solid ${color}44` }}>{text}</span>;
}

function HallCard({ emoji, label, stat, basis, mode }: { emoji: string; label: string; stat: PlayerLeaderStat | null; basis: 'per-min' | 'per-match' | 'total'; mode?: string }) {
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
      <PlayerLink riotId={stat.riotId} mode={mode}>
        <div className="hall-player">{stat.riotId.split('#')[0]}</div>
      </PlayerLink>
      <div className="hall-value">{stat.displayValue}</div>
      <div className="hall-games">{stat.games}판</div>
    </div>
  );
}


function OverviewTab({ mode }: { mode: string }) {
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
      <div className="overview-hero-row">
        <div className="overview-hero-main">
          <div className="overview-hero-number">{data.matchCount}</div>
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
      <div className="overview-spotlight-row">
        {spotlights.map(({ emoji, label, stat }) => (
          <div key={label}
            className={`overview-spotlight-card${stat ? ' clickable' : ''}`}
            onClick={stat ? () => navigate(`/player-stats/${encodeURIComponent(stat.riotId)}`) : undefined}
          >
            <div className="overview-spotlight-emoji">{emoji}</div>
            <div className="overview-spotlight-label">{label}</div>
            {stat ? (
              <>
                <div className="overview-spotlight-name">{stat.riotId.split('#')[0]}</div>
                <div className="overview-spotlight-value">{stat.displayValue}</div>
                <div className="overview-spotlight-games">{stat.games}판</div>
              </>
            ) : (
              <div className="overview-spotlight-empty">데이터 없음</div>
            )}
          </div>
        ))}
      </div>

      {/* ③ 챔피언 픽 + 사이드 정보 2열 */}
      <div className="overview-main-row">
        {/* 왼쪽: 픽률 챔피언 */}
        <section className="stats-section card overview-main-left">
          <h2 className="stats-section-title">🏆 많이 사용된 챔피언</h2>
          <div className="overview-champ-grid">
            {data.topPickedChampions.slice(0, 20).map(s => (
              <ChampPickCard key={s.championId} stat={s} champions={champions}
                onClick={() => navigate(`/stats/champion/${encodeURIComponent(s.champion)}?mode=${mode}`)} />
            ))}
          </div>
        </section>

        {/* 오른쪽: 밴 + 승률 챔피언 */}
        <div className="overview-main-right">
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
      <section className="stats-section card">
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

// ── Elo 탭 ────────────────────────────────────────────
function eloTier(elo: number): { label: string; color: string } {
  if (elo >= 1300) return { label: 'Challenger', color: '#FFD700' };
  if (elo >= 1200) return { label: 'Master',     color: '#AA47BC' };
  if (elo >= 1100) return { label: 'Diamond',    color: '#0BC4B4' };
  if (elo >= 1000) return { label: 'Platinum',   color: '#4A9EFF' };
  if (elo >= 900)  return { label: 'Gold',       color: '#C89B3C' };
  if (elo >= 800)  return { label: 'Silver',     color: '#A8A8A8' };
  return                  { label: 'Bronze',     color: '#CD7F32' };
}

function EloTab() {
  const navigate = useNavigate();
  const [data, setData] = useState<EloLeaderboardResult | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<EloLeaderboardResult>('/stats/elo')
      .then(setData)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingCenter />;
  if (!data || data.players.length === 0) return (
    <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', padding: '24px 0' }}>
      Elo 데이터가 없습니다. 어드민에서 재집계를 실행하세요.
    </p>
  );

  return (
    <div>
      <div style={{ fontWeight: 700, marginBottom: 16, fontSize: 'var(--font-size-sm)' }}>Elo 랭킹</div>
      <div className="table-wrapper">
        <table className="table">
          <thead>
            <tr>
              <th style={{ width: 48 }}>순위</th>
              <th>플레이어</th>
              <th>티어</th>
              <th className="table-number">Elo</th>
              <th className="table-number">판수</th>
            </tr>
          </thead>
          <tbody>
            {data.players.map((entry: EloRankEntry) => {
              const tier = eloTier(entry.elo);
              return (
                <tr key={entry.riotId}
                  style={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)}>
                  <td><RankBadge rank={entry.rank} /></td>
                  <td>
                    <PlayerLink riotId={entry.riotId} mode="all">
                      <span style={{ fontWeight: 600 }}>
                        {entry.riotId.split('#')[0]}
                      </span>
                      <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                        #{entry.riotId.split('#')[1]}
                      </span>
                    </PlayerLink>
                  </td>
                  <td>
                    <span style={{ fontSize: 11, fontWeight: 700, color: tier.color,
                      background: tier.color + '22', borderRadius: 4, padding: '2px 7px',
                      border: `1px solid ${tier.color}44` }}>
                      {tier.label}
                    </span>
                  </td>
                  <td className="table-number" style={{ fontWeight: 700, color: tier.color }}>
                    {entry.elo.toFixed(1)}
                  </td>
                  <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>
                    {entry.games}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── MVP 탭 ────────────────────────────────────────────
function MvpTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData] = useState<MvpStatsResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<MvpStatsResult>(`/stats/mvp?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        총 {data.totalGames}경기 · MVP 점수 = KDA기여 + 팀데미지기여(최대40) + 시야/분 + CS/분 + 승리보너스(+20)
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">MVP 횟수</th>
              <th className="table-number">ACE 횟수</th>
              <th style={{ minWidth: 100 }}>MVP 달성률</th>
              <th className="table-number">평균 점수</th>
              <th>MVP 챔피언</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: MvpPlayerStat, i) => (
              <tr key={p.riotId} className="member-stats-row" onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <PlayerLink riotId={p.riotId} mode={mode}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                      <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                      <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</span>
                    </div>
                  </PlayerLink>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">
                  <span style={{ fontWeight: 700, color: 'var(--color-primary)', fontSize: 14 }}>{p.mvpCount}</span>
                  <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>회</span>
                </td>
                <td className="table-number">
                  <span style={{ fontWeight: 700, color: 'var(--color-win)', fontSize: 14 }}>{p.aceCount}</span>
                  <span style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginLeft: 2 }}>회</span>
                </td>
                <td>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                    <span style={{ fontWeight: 700, fontSize: 13, color: p.mvpRate >= 50 ? 'var(--color-win)' : 'var(--color-primary)' }}>{p.mvpRate}%</span>
                    <div style={{ height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden', minWidth: 80 }}>
                      <div style={{ width: `${Math.min(p.mvpRate, 100)}%`, height: '100%', background: p.mvpRate >= 50 ? 'var(--color-win)' : 'var(--color-primary)', borderRadius: 2 }} />
                    </div>
                  </div>
                </td>
                <td className="table-number" style={{ fontWeight: 700 }}>{p.avgMvpScore.toFixed(2)}</td>
                <td>
                  {p.topChampion && p.topChampionId ? (
                    <ChampionLink champion={p.topChampion} championId={p.topChampionId} mode={mode}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <ChampImg championId={p.topChampionId} champion={p.topChampion} size={24} />
                        <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                          {champions.get(p.topChampionId)?.nameKo ?? p.topChampion}
                        </span>
                      </div>
                    </ChampionLink>
                  ) : <span style={{ color: 'var(--color-text-disabled)' }}>—</span>}
                </td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 챔피언 시너지 탭 ──────────────────────────────────
function SynergyTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const [data, setData]       = useState<ChampionSynergyResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [minGames, setMinGames] = useState(3);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<ChampionSynergyResult>(`/stats/synergy?mode=${mode}&minGames=${minGames}`)); }
    finally { setLoading(false); }
  }, [mode, minGames]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <p style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
          총 {data.totalGames}경기 · 같은 팀에 함께 픽된 챔피언 조합의 승률
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginLeft: 'auto' }}>
          <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>최소 게임수</span>
          {[2, 3, 5].map(n => (
            <button key={n} className={`member-sort-tab ${minGames === n ? 'active' : ''}`}
              onClick={() => setMinGames(n)}>{n}+</button>
          ))}
        </div>
      </div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th style={{ minWidth: 200 }}>챔피언 조합</th>
              <th className="table-number">게임</th>
              <th style={{ minWidth: 110 }}>승률</th>
              <th className="table-number">합산 킬/경기</th>
            </tr>
          </thead>
          <tbody>
            {data.synergies.map((s: ChampionSynergy, i) => {
              const ko1 = champions.get(s.champion1Id)?.nameKo || s.champion1;
              const ko2 = champions.get(s.champion2Id)?.nameKo || s.champion2;
              return (
              <tr key={`${s.champion1}-${s.champion2}`} className="member-stats-row">
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <ChampImg championId={s.champion1Id} champion={s.champion1} size={28} />
                    <ChampionLink champion={s.champion1} championId={s.champion1Id} mode={mode}>
                      <span style={{ fontWeight: 700, fontSize: 12 }}>{ko1}</span>
                    </ChampionLink>
                    <span style={{ color: 'var(--color-text-disabled)', fontSize: 16, fontWeight: 300 }}>+</span>
                    <ChampImg championId={s.champion2Id} champion={s.champion2} size={28} />
                    <ChampionLink champion={s.champion2} championId={s.champion2Id} mode={mode}>
                      <span style={{ fontWeight: 700, fontSize: 12 }}>{ko2}</span>
                    </ChampionLink>
                  </div>
                </td>
                <td className="table-number">{s.games}</td>
                <td><WinRateBar winRate={s.winRate} wins={s.wins} losses={s.games - s.wins} /></td>
                <td className="table-number" style={{ fontWeight: 600 }}>{s.avgCombinedKills.toFixed(1)}</td>
              </tr>
              );
            })}
            {!data.synergies.length && (
              <tr><td colSpan={5} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>
                데이터 없음 (최소 {minGames}게임 이상 조합 없음)
              </td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 라인별 탭 ─────────────────────────────────────────
const LANE_META: Record<string, { label: string; emoji: string; keyCol: { key: keyof PlayerLaneStat; label: string; format: (v: number) => string } }> = {
  TOP:     { label: '탑',   emoji: '🛡️', keyCol: { key: 'avgDamageTaken',    label: '평균 받은딜',  format: v => v.toLocaleString() } },
  JUNGLE:  { label: '정글', emoji: '🌲', keyCol: { key: 'avgNeutralMinions', label: '중립 몬스터', format: v => v.toFixed(1) } },
  MID:     { label: '미드', emoji: '⚡', keyCol: { key: 'avgDamage',         label: '평균 딜량',   format: v => v.toLocaleString() } },
  BOTTOM:  { label: '원딜', emoji: '🏹', keyCol: { key: 'avgCs',             label: '평균 CS',    format: v => v.toFixed(1) } },
  SUPPORT: { label: '서폿', emoji: '💫', keyCol: { key: 'avgWardsPlaced',    label: '평균 와드',  format: v => v.toFixed(1) } },
};
const LANES = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'] as const;

function LaneTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [selectedLane, setSelectedLane] = useState<string>('TOP');
  const [data, setData] = useState<LaneLeaderboardResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async (lane: string) => {
    setLoading(true);
    setData(null);
    try { setData(await api.get<LaneLeaderboardResult>(`/stats/lane?lane=${lane}&mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(selectedLane); }, [load, selectedLane]);

  const meta = LANE_META[selectedLane];

  return (
    <div>
      {/* 포지션 선택 탭 */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        {LANES.map(lane => {
          const m = LANE_META[lane];
          return (
            <button key={lane}
              className={`member-sort-tab ${selectedLane === lane ? 'active' : ''}`}
              onClick={() => setSelectedLane(lane)}
              style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '6px 14px' }}>
              <span>{m.emoji}</span>
              <span>{m.label}</span>
            </button>
          );
        })}
      </div>

      {loading ? <LoadingCenter /> : !data ? null : (
        <div className="table-wrapper">
          <table className="table member-stats-table">
            <thead>
              <tr>
                <th style={{ width: 36 }}>#</th>
                <th>플레이어</th>
                <th>대표 챔피언</th>
                <th className="table-number">판수</th>
                <th style={{ minWidth: 110 }}>승률</th>
                <th className="table-number">KDA</th>
                <th className="table-number">K/D/A</th>
                <th className="table-number">평균 딜량</th>
                <th className="table-number">{meta.keyCol.label}</th>
              </tr>
            </thead>
            <tbody>
              {data.players.map((p: PlayerLaneStat, i) => {
                const champName = p.topChampion ?? '';
                const nameKo = p.topChampionId ? (champions.get(p.topChampionId)?.nameKo ?? champName) : champName;
                return (
                  <tr key={p.riotId} className="member-stats-row"
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                    <td><RankBadge rank={i + 1} /></td>
                    <td>
                      <PlayerLink riotId={p.riotId} mode={mode}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                          <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                          <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</span>
                        </div>
                      </PlayerLink>
                    </td>
                    <td>
                      {p.topChampionId ? (
                        <ChampionLink champion={champName} championId={p.topChampionId} mode={mode}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <ChampImg championId={p.topChampionId} champion={champName} size={24} />
                            <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{nameKo}</span>
                          </div>
                        </ChampionLink>
                      ) : <span style={{ color: 'var(--color-text-disabled)' }}>—</span>}
                    </td>
                    <td className="table-number">{p.games}</td>
                    <td><WinRateBar winRate={p.winRate} wins={p.wins} losses={p.games - p.wins} /></td>
                    <td className="table-number" style={{ fontWeight: 700, color: p.kda >= 5 ? 'var(--color-win)' : p.kda >= 3 ? 'var(--color-primary)' : undefined }}>
                      {p.kda.toFixed(2)}
                    </td>
                    <td className="table-number" style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                      {p.avgKills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{p.avgDeaths.toFixed(1)}</span> / {p.avgAssists.toFixed(1)}
                    </td>
                    <td className="table-number">{p.avgDamage.toLocaleString()}</td>
                    <td className="table-number">{meta.keyCol.format(p[meta.keyCol.key] as number)}</td>
                  </tr>
                );
              })}
              {data.players.length === 0 && (
                <tr><td colSpan={9} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ── 듀오 시너지 탭 ────────────────────────────────────
function DuoTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<DuoStatsResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [minGames, setMinGames] = useState(2);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<DuoStatsResult>(`/stats/duo?mode=${mode}&minGames=${minGames}`)); }
    finally { setLoading(false); }
  }, [mode, minGames]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <p style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
          같은 팀에서 함께 플레이한 멤버 조합의 시너지
        </p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginLeft: 'auto' }}>
          <span style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>최소 게임수</span>
          {[2, 3, 5].map(n => (
            <button key={n} className={`member-sort-tab ${minGames === n ? 'active' : ''}`}
              onClick={() => setMinGames(n)}>{n}+</button>
          ))}
        </div>
      </div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th style={{ minWidth: 220 }}>플레이어 조합</th>
              <th className="table-number">게임</th>
              <th style={{ minWidth: 110 }}>승률</th>
              <th className="table-number">합산 KDA</th>
              <th className="table-number">합산 K/D/A</th>
            </tr>
          </thead>
          <tbody>
            {data.duos.map((d: DuoStat, i) => (
              <tr key={`${d.player1}-${d.player2}`} className="member-stats-row">
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <PlayerLink riotId={d.player1} mode={mode}>
                      <button className="player-name-btn" onClick={e => { e.stopPropagation(); navigate(`/player-stats/${encodeURIComponent(d.player1)}`); }}>
                        {d.player1.split('#')[0]}
                      </button>
                    </PlayerLink>
                    <span style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>+</span>
                    <PlayerLink riotId={d.player2} mode={mode}>
                      <button className="player-name-btn" onClick={e => { e.stopPropagation(); navigate(`/player-stats/${encodeURIComponent(d.player2)}`); }}>
                        {d.player2.split('#')[0]}
                      </button>
                    </PlayerLink>
                  </div>
                </td>
                <td className="table-number">{d.games}</td>
                <td><WinRateBar winRate={d.winRate} wins={d.wins} losses={d.games - d.wins} /></td>
                <td className="table-number" style={{ fontWeight: 700, color: d.kda >= 5 ? 'var(--color-win)' : d.kda >= 3 ? 'var(--color-primary)' : 'var(--color-text-primary)' }}>
                  {d.kda.toFixed(2)}
                </td>
                <td className="table-number" style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                  {d.avgKills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{d.avgDeaths.toFixed(1)}</span> / {d.avgAssists.toFixed(1)}
                </td>
              </tr>
            ))}
            {!data.duos.length && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>
                데이터 없음 (최소 {minGames}게임 이상 조합 없음)
              </td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 메인 페이지 ───────────────────────────────────────
export function StatsPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('overview');

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">통계</h1>
          <p className="page-subtitle">내전 현황 한눈에 보기</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {/* 탭 */}
      <div className="stats-tab-bar">
        {TABS.map(t => (
          <button key={t.key} className={`stats-tab-btn ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      {tab === 'overview'  && <OverviewTab mode={mode} />}
      {tab === 'elo'       && <div className="card" style={{ marginTop: 4 }}><EloTab /></div>}
      {tab === 'mvp'       && <div className="card" style={{ marginTop: 4 }}><MvpTab     mode={mode} /></div>}
      {tab === 'lane'      && <div className="card" style={{ marginTop: 4 }}><LaneTab    mode={mode} /></div>}
      {tab === 'synergy'   && <div className="card" style={{ marginTop: 4 }}><SynergyTab mode={mode} /></div>}
      {tab === 'duo'       && <div className="card" style={{ marginTop: 4 }}><DuoTab     mode={mode} /></div>}
    </div>
  );
}
