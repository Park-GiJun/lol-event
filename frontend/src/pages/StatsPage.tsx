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
  WeeklyAwardsResult,
  DefeatContributionResult, DefeatContributionEntry,
  MultiKillHighlightsResult, MultiKillEvent, PlayerMultiKillStat,
  ChaosMatchResult, ChaosMatchEntry,
  SurvivalIndexResult, SurvivalIndexEntry,
  JungleDominanceResult, JungleDominanceEntry,
  SupportImpactResult, SupportImpactEntry,
  RivalMatchupResult, RivalMatchupEntry,
  TeamChemistryResult, TeamChemistryEntry,
  PositionBadgeResult, PositionBadgeEntry,
  ChampionCertificateResult, ChampionCertEntry,
  PlaystyleDnaResult, PlaystyleDnaEntry,
  MetaShiftResult, MetaShiftChampion,
  PlayerComparisonResult, PlayerStatSnapshot,
  SessionReportResult, SessionEntry,
  ChampionTierResult, ChampionTierEntry,
  GameLengthTendencyResult, GameLengthTendencyEntry,
  EarlyGameDominanceResult, EarlyGameDominanceEntry,
  ComebackIndexResult, ComebackIndexEntry,
  GoldEfficiencyResult, GoldEfficiencyEntry,
  BanAnalysisResult, BanEntry,
  TimePatternResult, DayPatternEntry, HourPatternEntry,
  KillParticipationResult, KillParticipationEntry,
  PositionChampionPoolResult, PlayerPositionEntry, PositionChampEntry,
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
  { key: 'overview',    label: '📊 개요' },
  { key: 'elo',         label: '🏅 Elo 랭킹' },
  { key: 'mvp',         label: '🏆 MVP 랭킹' },
  { key: 'lane',        label: '🗺️ 라인별' },
  { key: 'synergy',     label: '⚡ 챔피언 시너지' },
  { key: 'duo',         label: '🤝 듀오 시너지' },
  { key: 'awards',      label: '🏆 어워즈' },
  { key: 'chaos',       label: '💥 혼돈 지수' },
  { key: 'multikill',   label: '⚡ 멀티킬' },
  { key: 'defeat',      label: '😵 떡락 지수' },
  { key: 'rival',       label: '⚔️ 라이벌' },
  { key: 'chemistry',   label: '🤝 팀 케미' },
  { key: 'survival',    label: '🛡️ 생존력' },
  { key: 'jungle',      label: '🌲 정글' },
  { key: 'support',     label: '💚 서폿 기여' },
  { key: 'position',    label: '📍 포지션 배지' },
  { key: 'certificate', label: '🎖️ 장인 인증' },
  { key: 'dna',         label: '🧬 플레이스타일' },
  { key: 'meta',        label: '📈 메타 추적' },
  { key: 'compare',    label: '⚔️ 비교' },
  { key: 'sessions',   label: '📅 세션' },
  { key: 'tier',       label: '🏅 티어리스트' },
  { key: 'gamelength', label: '⏱️ 게임 길이' },
  { key: 'earlygame',  label: '🌅 초반 지배' },
  { key: 'comeback',   label: '🔄 컴백' },
  { key: 'goldeff',    label: '💰 골드 효율' },
  { key: 'ban',        label: '🚫 밴 분석' },
  { key: 'timepattern', label: '📅 시간패턴' },
  { key: 'kp',         label: '⚡ KP 랭킹' },
  { key: 'pospool',    label: '📍 포지션 풀' },
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
        <table className="table member-stats-table">
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
                  className="member-stats-row"
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

// ── 어워즈 탭 ─────────────────────────────────────────
const AWARD_LABELS: Record<string, string> = {
  mostDeaths:        '💀 단일 경기 최다 사망',
  worstKda:          '😢 평균 KDA 최하위',
  highGoldLowDamage: '💰 먹튀 골드왕',
  mostSurrenders:    '🏳️ 항복 유발자',
  pentaKillHero:     '⚔️ 펜타킬 영웅',
  loneHero:          '🦸 그래도 난 했다',
  highestWinRate:    '🏆 승률 1위',
  mostGamesChampion: '🎮 챔피언 장인',
};

function AwardsTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<WeeklyAwardsResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<WeeklyAwardsResult>(`/stats/awards?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const entries = Object.entries(AWARD_LABELS) as [keyof WeeklyAwardsResult, string][];

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        내전 어워즈 — 명예(?)의 전당
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: 12 }}>
        {entries.map(([key, label]) => {
          const entry = data[key];
          return (
            <div key={key} className="card" style={{ padding: '14px 16px' }}>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 6 }}>{label}</div>
              {entry ? (
                <div style={{ cursor: 'pointer' }} onClick={() => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)}>
                  <div style={{ fontWeight: 700, fontSize: 15 }}>{entry.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 13, color: 'var(--color-primary)', marginTop: 2 }}>{entry.displayValue}</div>
                  <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginTop: 2 }}>{entry.games}판</div>
                </div>
              ) : (
                <div style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>데이터 없음</div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── 혼돈 지수 탭 ──────────────────────────────────────
const CHAOS_TAG_COLORS: Record<string, string> = {
  '혈전':   '#e74c3c',
  '학살':   '#e67e22',
  '운영 접전': '#4a9eff',
};

function chaosTagColor(tag: string): string {
  return CHAOS_TAG_COLORS[tag] ?? '#888';
}

function formatDuration(min: number): string {
  const m = Math.floor(min);
  const s = Math.round((min - m) * 60);
  return `${m}:${s.toString().padStart(2, '0')}`;
}

function ChaosTab({ mode }: { mode: string }) {
  const [data, setData]       = useState<ChaosMatchResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<ChaosMatchResult>(`/stats/chaos-match?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const renderCard = (entry: ChaosMatchEntry) => {
    const tagColor = chaosTagColor(entry.gameTypeTag);
    return (
      <div key={entry.matchId} className="card" style={{ padding: '12px 14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
          <span style={{ fontWeight: 800, fontSize: 20, color: 'var(--color-primary)' }}>
            {entry.chaosIndex.toFixed(1)}
          </span>
          <span style={{ fontSize: 11, fontWeight: 700, color: tagColor,
            background: tagColor + '22', border: `1px solid ${tagColor}44`,
            borderRadius: 4, padding: '2px 8px' }}>
            {entry.gameTypeTag}
          </span>
        </div>
        <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 4 }}>
          킬 {entry.totalKills} · {formatDuration(entry.gameDurationMin)}
        </div>
        <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>
          {entry.participants.slice(0, 5).map(p => p.split('#')[0]).join(', ')}
        </div>
      </div>
    );
  };

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        평균 혼돈 지수: {data.avgChaosIndex.toFixed(1)}
      </p>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>🔥 최고 혼돈 경기 TOP 10</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 10, marginBottom: 20 }}>
        {data.topChaosMatches.map(renderCard)}
      </div>
    </div>
  );
}

// ── 멀티킬 탭 ─────────────────────────────────────────
function MultikillTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const [data, setData]       = useState<MultiKillHighlightsResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<MultiKillHighlightsResult>(`/stats/multikill-highlights?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const MULTIKILL_COLORS: Record<string, string> = {
    PENTA: '#FFD700',
    QUADRA: '#AA47BC',
    TRIPLE: '#4a9eff',
    DOUBLE: '#888',
  };

  return (
    <div>
      {data.pentaKillEvents.length > 0 && (
        <section style={{ marginBottom: 24 }}>
          <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>⭐ 펜타킬 명예의 전당</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 10 }}>
            {data.pentaKillEvents.map((ev: MultiKillEvent) => {
              const nameKo = ev.championId ? (champions.get(ev.championId)?.nameKo ?? ev.champion) : ev.champion;
              return (
                <div key={`${ev.matchId}-${ev.riotId}`} className="card" style={{ padding: '12px 14px', borderLeft: '3px solid #FFD700' }}>
                  <div style={{ fontWeight: 700, fontSize: 14, color: '#FFD700', marginBottom: 4 }}>펜타킬!</div>
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{ev.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                    <ChampImg championId={ev.championId} champion={ev.champion} size={18} />
                    <span style={{ marginLeft: 4 }}>{nameKo}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>📊 플레이어 멀티킬 랭킹</h3>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number" style={{ color: '#FFD700' }}>펜타</th>
              <th className="table-number" style={{ color: '#AA47BC' }}>쿼드라</th>
              <th className="table-number" style={{ color: '#4a9eff' }}>트리플</th>
              <th className="table-number">더블</th>
            </tr>
          </thead>
          <tbody>
            {data.playerRankings.map((p: PlayerMultiKillStat, i) => (
              <tr key={p.riotId} className="member-stats-row">
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                </td>
                <td className="table-number" style={{ fontWeight: 700, color: MULTIKILL_COLORS.PENTA }}>{p.pentaKills}</td>
                <td className="table-number" style={{ fontWeight: 700, color: MULTIKILL_COLORS.QUADRA }}>{p.quadraKills}</td>
                <td className="table-number" style={{ fontWeight: 700, color: MULTIKILL_COLORS.TRIPLE }}>{p.tripleKills}</td>
                <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{p.doubleKills}</td>
              </tr>
            ))}
            {!data.playerRankings.length && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 떡락 지수 탭 ──────────────────────────────────────
function DefeatTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<DefeatContributionResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<DefeatContributionResult>(`/stats/defeat-contribution?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        패배 기여도 — 높을수록 팀을 더 힘들게 합니다
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">패배수</th>
              <th className="table-number">평균 Defeat Score</th>
              <th className="table-number">평균 사망</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: DefeatContributionEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number" style={{ color: 'var(--color-loss)', fontWeight: 700 }}>{p.losses}</td>
                <td className="table-number" style={{ fontWeight: 700, color: 'var(--color-error)' }}>
                  {p.avgDefeatScore.toFixed(1)}
                </td>
                <td className="table-number" style={{ color: 'var(--color-text-secondary)' }}>{p.avgDeaths.toFixed(1)}</td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 라이벌 탭 ─────────────────────────────────────────
function RivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<RivalMatchupResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<RivalMatchupResult>(`/stats/rival-matchup?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      {data.topRivalry && (
        <div className="card" style={{ marginBottom: 20, padding: '14px 18px', borderLeft: '3px solid var(--color-primary)' }}>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 4 }}>🔥 최대 라이벌</div>
          <div style={{ fontWeight: 700, fontSize: 15 }}>
            {data.topRivalry.player1.split('#')[0]} vs {data.topRivalry.player2.split('#')[0]}
          </div>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginTop: 4 }}>
            {data.topRivalry.games}회 맞대결
          </div>
        </div>
      )}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {data.rivalries.map((r: RivalMatchupEntry) => {
          const p2WinRate = Math.round(100 - r.player1WinRate);
          return (
            <div key={`${r.player1}-${r.player2}`} className="card" style={{ padding: '12px 16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ flex: 1, textAlign: 'right' }}>
                  <div style={{ fontWeight: 700, fontSize: 13, cursor: 'pointer' }}
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(r.player1)}`)}>
                    {r.player1.split('#')[0]}
                  </div>
                  <div style={{ fontSize: 12, color: r.player1WinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 600 }}>
                    {r.player1WinRate}% ({r.player1Wins}승)
                  </div>
                </div>
                <div style={{ textAlign: 'center', minWidth: 60 }}>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{r.games}회</div>
                  <div style={{ fontSize: 12, fontWeight: 700, color: 'var(--color-text-secondary)' }}>VS</div>
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 700, fontSize: 13, cursor: 'pointer' }}
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(r.player2)}`)}>
                    {r.player2.split('#')[0]}
                  </div>
                  <div style={{ fontSize: 12, color: p2WinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 600 }}>
                    {p2WinRate}% ({r.player2Wins}승)
                  </div>
                </div>
              </div>
              <div style={{ marginTop: 8, height: 6, background: 'var(--color-bg-hover)', borderRadius: 3, overflow: 'hidden', display: 'flex' }}>
                <div style={{ width: `${r.player1WinRate}%`, background: 'var(--color-primary)', transition: 'width 0.3s' }} />
                <div style={{ flex: 1, background: 'var(--color-loss)' }} />
              </div>
            </div>
          );
        })}
        {!data.rivalries.length && (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', padding: '24px 0' }}>데이터 없음</p>
        )}
      </div>
    </div>
  );
}

// ── 팀 케미스트리 탭 ──────────────────────────────────
function ChemistryTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<TeamChemistryResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<TeamChemistryResult>(`/stats/team-chemistry?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const renderSection = (title: string, items: TeamChemistryEntry[]) => (
    <section style={{ marginBottom: 24 }}>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>{title}</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {items.slice(0, 5).map((entry, i) => (
          <div key={entry.players.join('-')} className="card" style={{ padding: '10px 14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <RankBadge rank={i + 1} />
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: 13 }}>
                  {entry.players.map(p => (
                    <span key={p} style={{ cursor: 'pointer', marginRight: 6 }}
                      onClick={() => navigate(`/player-stats/${encodeURIComponent(p)}`)}>
                      {p.split('#')[0]}
                    </span>
                  ))}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{entry.games}판</div>
                <WinRateBar winRate={entry.winRate} wins={entry.wins} losses={entry.games - entry.wins} />
              </div>
            </div>
          </div>
        ))}
        {!items.length && <p style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>데이터 없음</p>}
      </div>
    </section>
  );

  return (
    <div>
      {renderSection('👥 최강 2인조', data.bestDuos)}
      {renderSection('👥 최강 3인조', data.bestTrios)}
      {renderSection('👥 최강 5인팀', data.bestFullTeams)}
    </div>
  );
}

// ── 생존력 탭 ─────────────────────────────────────────
function SurvivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<SurvivalIndexResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<SurvivalIndexResult>(`/stats/survival-index?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        생존력 & 탱킹 지수 — 높을수록 팀의 방패
      </p>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th className="table-number">판수</th>
              <th className="table-number">탱킹 기여(%)</th>
              <th className="table-number">피해 감소율(%)</th>
              <th className="table-number">생존 지수</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: SurvivalIndexEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</div>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{(p.avgTankShare * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgMitigationRatio * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ fontWeight: 700, color: 'var(--color-primary)' }}>
                  {p.survivalIndex.toFixed(1)}
                </td>
              </tr>
            ))}
            {!data.rankings.length && (
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 정글 탭 ───────────────────────────────────────────
function JungleTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData]       = useState<JungleDominanceResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<JungleDominanceResult>(`/stats/jungle-dominance?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th>스타일</th>
              <th className="table-number">판수</th>
              <th className="table-number">침입률</th>
              <th className="table-number">오브젝트 기여</th>
              <th className="table-number">킬 관여</th>
              <th className="table-number">지배 지수</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: JungleDominanceEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                    {p.topChampion && p.topChampionId && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <ChampImg championId={p.topChampionId} champion={p.topChampion} size={16} />
                        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
                          {champions.get(p.topChampionId)?.nameKo ?? p.topChampion}
                        </span>
                      </div>
                    )}
                  </div>
                </td>
                <td>
                  <span style={{ fontSize: 11, background: 'var(--color-bg-hover)', borderRadius: 4, padding: '2px 6px' }}>
                    {p.playStyleTag}
                  </span>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{(p.avgInvadeRatio * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgObjShare * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgKp * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ fontWeight: 700, color: 'var(--color-primary)' }}>
                  {p.avgJungleDominance.toFixed(1)}
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

// ── 서폿 기여 탭 ──────────────────────────────────────
function SupportTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData]       = useState<SupportImpactResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<SupportImpactResult>(`/stats/support-impact?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div className="table-wrapper">
        <table className="table member-stats-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>#</th>
              <th>플레이어</th>
              <th>역할</th>
              <th className="table-number">판수</th>
              <th className="table-number">힐 기여</th>
              <th className="table-number">CC 기여</th>
              <th className="table-number">시야 기여</th>
              <th className="table-number">서폿 지수</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: SupportImpactEntry, i) => (
              <tr key={p.riotId} className="member-stats-row"
                onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                    {p.topChampion && p.topChampionId && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <ChampImg championId={p.topChampionId} champion={p.topChampion} size={16} />
                        <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>
                          {champions.get(p.topChampionId)?.nameKo ?? p.topChampion}
                        </span>
                      </div>
                    )}
                  </div>
                </td>
                <td>
                  <span style={{ fontSize: 11, background: 'var(--color-bg-hover)', borderRadius: 4, padding: '2px 6px' }}>
                    {p.roleTag}
                  </span>
                </td>
                <td className="table-number">{p.games}</td>
                <td className="table-number">{(p.avgHealShare * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgCcShare * 100).toFixed(1)}%</td>
                <td className="table-number">{(p.avgVisionShare * 100).toFixed(1)}%</td>
                <td className="table-number" style={{ fontWeight: 700, color: '#4CAF50' }}>
                  {p.supportImpact.toFixed(1)}
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

// ── 포지션 배지 탭 ────────────────────────────────────
const POSITION_META: Record<string, { label: string; emoji: string }> = {
  TOP:     { label: '탑',   emoji: '🛡️' },
  JUNGLE:  { label: '정글', emoji: '🌲' },
  MID:     { label: '미드', emoji: '⚡' },
  BOTTOM:  { label: '원딜', emoji: '🏹' },
  SUPPORT: { label: '서폿', emoji: '💫' },
};

function PositionTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData]       = useState<PositionBadgeResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<PositionBadgeResult>(`/stats/position-badge?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 12 }}>🥇 포지션별 1위</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 12, marginBottom: 28 }}>
        {data.topPositions.map((entry: PositionBadgeEntry) => {
          const meta = POSITION_META[entry.position] ?? { label: entry.position, emoji: '📍' };
          const nameKo = entry.topChampionId ? (champions.get(entry.topChampionId)?.nameKo ?? entry.topChampion) : entry.topChampion;
          return (
            <div key={entry.position} className="card" style={{ padding: '14px', textAlign: 'center', cursor: 'pointer' }}
              onClick={() => navigate(`/player-stats/${encodeURIComponent(entry.riotId)}`)}>
              <div style={{ fontSize: 28, marginBottom: 4 }}>{meta.emoji}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 4 }}>{meta.label}</div>
              <div style={{ fontWeight: 700, fontSize: 14 }}>{entry.riotId.split('#')[0]}</div>
              <div style={{ fontSize: 12, color: 'var(--color-primary)', marginTop: 2 }}>
                {entry.positionScore.toFixed(1)}점
              </div>
              {nameKo && (
                <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginTop: 4 }}>
                  {entry.topChampionId && <ChampImg championId={entry.topChampionId} champion={entry.topChampion ?? ''} size={16} />}
                  <span style={{ marginLeft: 4 }}>{nameKo}</span>
                </div>
              )}
              <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginTop: 2 }}>{entry.games}판</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── 챔피언 장인 인증 탭 ───────────────────────────────
function CertificateTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [data, setData]       = useState<ChampionCertificateResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<ChampionCertificateResult>(`/stats/champion-certificate?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        일정 판수 이상 + 높은 KDA + 승률 조건 충족 시 장인 인증
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 12 }}>
        {data.certifiedMasters.map((cert: ChampionCertEntry) => {
          const nameKo = champions.get(cert.championId)?.nameKo ?? cert.champion;
          return (
            <div key={`${cert.riotId}-${cert.champion}`} className="card"
              style={{ padding: '14px', borderLeft: '3px solid #FFD700', cursor: 'pointer' }}
              onClick={() => navigate(`/player-stats/${encodeURIComponent(cert.riotId)}`)}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                <ChampImg championId={cert.championId} champion={cert.champion} size={36} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{cert.riotId.split('#')[0]}</div>
                  <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{nameKo}</div>
                </div>
                <span style={{ marginLeft: 'auto', fontSize: 16 }}>🎖️</span>
              </div>
              <div style={{ display: 'flex', gap: 12, fontSize: 12 }}>
                <span>{cert.games}판</span>
                <span style={{ color: cert.winRate >= 60 ? 'var(--color-win)' : 'var(--color-primary)' }}>
                  {cert.winRate}%
                </span>
                <span style={{ color: 'var(--color-text-secondary)' }}>KDA {cert.kda.toFixed(2)}</span>
              </div>
            </div>
          );
        })}
        {!data.certifiedMasters.length && (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', gridColumn: '1/-1' }}>
            인증된 장인이 없습니다
          </p>
        )}
      </div>
    </div>
  );
}

// ── 플레이스타일 DNA 탭 ───────────────────────────────
const DNA_LABELS: { key: keyof PlaystyleDnaEntry; label: string; color: string }[] = [
  { key: 'aggression',     label: '공격성',    color: '#e74c3c' },
  { key: 'durability',     label: '생존력',    color: '#3498db' },
  { key: 'teamPlay',       label: '팀 플레이', color: '#2ecc71' },
  { key: 'objectiveFocus', label: '오브젝트',  color: '#f39c12' },
  { key: 'economy',        label: '경제력',    color: '#9b59b6' },
  { key: 'visionControl',  label: '시야',      color: '#1abc9c' },
];

function DnaTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData]       = useState<PlaystyleDnaResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<PlaystyleDnaResult>(`/stats/playstyle-dna?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {data.players.map((p: PlaystyleDnaEntry) => (
          <div key={p.riotId} className="card" style={{ padding: '12px 16px', cursor: 'pointer' }}
            onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 10 }}>
              <span style={{ fontWeight: 700, fontSize: 14 }}>{p.riotId.split('#')[0]}</span>
              <span style={{ fontSize: 11, fontWeight: 700, background: 'var(--color-primary)22',
                color: 'var(--color-primary)', borderRadius: 4, padding: '2px 8px',
                border: '1px solid var(--color-primary)44' }}>
                {p.styleTag}
              </span>
              <span style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginLeft: 'auto' }}>
                {p.games}판
              </span>
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {DNA_LABELS.map(({ key, label, color }) => {
                const val = p[key] as number;
                return (
                  <div key={key} style={{ minWidth: 80 }}>
                    <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginBottom: 2 }}>{label}</div>
                    <div style={{ height: 4, background: 'var(--color-bg-hover)', borderRadius: 2, overflow: 'hidden', marginBottom: 2 }}>
                      <div style={{ width: `${Math.min(val * 100, 100)}%`, height: '100%', background: color, borderRadius: 2 }} />
                    </div>
                    <div style={{ fontSize: 10, color, fontWeight: 600 }}>{(val * 100).toFixed(0)}</div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}
        {!data.players.length && (
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>데이터 없음</p>
        )}
      </div>
    </div>
  );
}

// ── 메타 추적 탭 ──────────────────────────────────────
function MetaTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const [data, setData]       = useState<MetaShiftResult | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try { setData(await api.get<MetaShiftResult>(`/stats/meta-shift?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const renderChampList = (title: string, list: MetaShiftChampion[], trendColor: string) => (
    <section style={{ marginBottom: 24 }}>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>{title}</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 10 }}>
        {list.map((c: MetaShiftChampion) => {
          const nameKo = champions.get(c.championId)?.nameKo ?? c.champion;
          const trendSign = c.trend >= 0 ? '+' : '';
          return (
            <div key={c.champion} className="card" style={{ padding: '10px 12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                <ChampImg championId={c.championId} champion={c.champion} size={28} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: 13 }}>{nameKo}</div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{c.metaTag}</div>
                </div>
              </div>
              <div style={{ fontSize: 12, display: 'flex', gap: 8 }}>
                <span style={{ color: 'var(--color-text-secondary)' }}>픽률 {(c.pickRate * 100).toFixed(1)}%</span>
                <span style={{ fontWeight: 700, color: trendColor }}>
                  {trendSign}{(c.trend * 100).toFixed(1)}%
                </span>
              </div>
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                승률 {c.winRate.toFixed(1)}%
              </div>
            </div>
          );
        })}
        {!list.length && <p style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>데이터 없음</p>}
      </div>
    </section>
  );

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 16 }}>
        총 {data.totalMatchesAnalyzed}경기 분석 — 최근 vs 이전 기간 픽률 변화
      </p>
      {renderChampList('📈 급상승 챔피언', data.risingChampions, 'var(--color-win)')}
      {renderChampList('📉 급하락 챔피언', data.fallingChampions, 'var(--color-loss)')}
      {renderChampList('📊 안정 메타 챔피언', data.stableTopChampions, 'var(--color-text-secondary)')}
    </div>
  );
}

// ── 플레이어 비교 탭 ────────────────────────────────────
function CompareTab({ mode }: { mode: string }) {
  const [p1Input, setP1Input] = useState('');
  const [p2Input, setP2Input] = useState('');
  const [data, setData] = useState<PlayerComparisonResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    if (!p1Input.trim() || !p2Input.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const result = await api.get<PlayerComparisonResult>(
        `/stats/compare?player1=${encodeURIComponent(p1Input.trim())}&player2=${encodeURIComponent(p2Input.trim())}&mode=${mode}`
      );
      setData(result);
    } catch {
      setError('데이터를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const StatRow = ({ label, v1, v2, higher }: { label: string; v1: string | number; v2: string | number; higher?: 'p1' | 'p2' | 'none' }) => (
    <tr>
      <td style={{ fontWeight: higher === 'p1' ? 700 : 400, color: higher === 'p1' ? 'var(--color-win)' : undefined, textAlign: 'center', padding: '5px 8px', fontSize: 12 }}>{v1}</td>
      <td style={{ textAlign: 'center', padding: '5px 8px', fontSize: 11, color: 'var(--color-text-secondary)', whiteSpace: 'nowrap' }}>{label}</td>
      <td style={{ fontWeight: higher === 'p2' ? 700 : 400, color: higher === 'p2' ? 'var(--color-win)' : undefined, textAlign: 'center', padding: '5px 8px', fontSize: 12 }}>{v2}</td>
    </tr>
  );

  const compareSnap = (s1: PlayerStatSnapshot | null, s2: PlayerStatSnapshot | null, title: string) => {
    if (!s1 && !s2) return null;
    const safe = (v: number | undefined) => v?.toFixed(2) ?? '-';
    const hi = (a: number | undefined, b: number | undefined): 'p1' | 'p2' | 'none' =>
      a == null || b == null ? 'none' : a > b ? 'p1' : a < b ? 'p2' : 'none';
    return (
      <div style={{ marginBottom: 20 }}>
        <h4 style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>{title}</h4>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'center', padding: '4px 8px', fontSize: 12, color: 'var(--color-primary)' }}>{data?.player1.split('#')[0]}</th>
              <th style={{ textAlign: 'center', padding: '4px 8px', fontSize: 11, color: 'var(--color-text-disabled)' }}>지표</th>
              <th style={{ textAlign: 'center', padding: '4px 8px', fontSize: 12, color: 'var(--color-primary)' }}>{data?.player2.split('#')[0]}</th>
            </tr>
          </thead>
          <tbody>
            <StatRow label="경기수" v1={s1?.games ?? '-'} v2={s2?.games ?? '-'} higher={hi(s1?.games, s2?.games)} />
            <StatRow label="승률%" v1={s1 ? s1.winRate.toFixed(1) : '-'} v2={s2 ? s2.winRate.toFixed(1) : '-'} higher={hi(s1?.winRate, s2?.winRate)} />
            <StatRow label="KDA" v1={s1 ? safe(s1.kda) : '-'} v2={s2 ? safe(s2.kda) : '-'} higher={hi(s1?.kda, s2?.kda)} />
            <StatRow label="평균킬" v1={s1 ? safe(s1.avgKills) : '-'} v2={s2 ? safe(s2.avgKills) : '-'} higher={hi(s1?.avgKills, s2?.avgKills)} />
            <StatRow label="평균데스" v1={s1 ? safe(s1.avgDeaths) : '-'} v2={s2 ? safe(s2.avgDeaths) : '-'} higher={hi(s2?.avgDeaths, s1?.avgDeaths)} />
            <StatRow label="평균딜" v1={s1 ? Math.round(s1.avgDamage).toLocaleString() : '-'} v2={s2 ? Math.round(s2.avgDamage).toLocaleString() : '-'} higher={hi(s1?.avgDamage, s2?.avgDamage)} />
            <StatRow label="평균CS" v1={s1 ? safe(s1.avgCs) : '-'} v2={s2 ? safe(s2.avgCs) : '-'} higher={hi(s1?.avgCs, s2?.avgCs)} />
            <StatRow label="시야" v1={s1 ? safe(s1.avgVisionScore) : '-'} v2={s2 ? safe(s2.avgVisionScore) : '-'} higher={hi(s1?.avgVisionScore, s2?.avgVisionScore)} />
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <input
          value={p1Input} onChange={e => setP1Input(e.target.value)}
          placeholder="플레이어1 (RiotID#태그)"
          style={{ flex: 1, minWidth: 140, padding: '7px 10px', borderRadius: 6, border: '1px solid var(--color-border)', background: 'var(--color-bg-secondary)', color: 'var(--color-text-primary)', fontSize: 13 }}
        />
        <input
          value={p2Input} onChange={e => setP2Input(e.target.value)}
          placeholder="플레이어2 (RiotID#태그)"
          style={{ flex: 1, minWidth: 140, padding: '7px 10px', borderRadius: 6, border: '1px solid var(--color-border)', background: 'var(--color-bg-secondary)', color: 'var(--color-text-primary)', fontSize: 13 }}
        />
        <Button variant="primary" size="sm" onClick={load} disabled={loading}>
          {loading ? '로딩...' : '비교'}
        </Button>
      </div>

      {error && <p style={{ color: 'var(--color-loss)', fontSize: 13 }}>{error}</p>}
      {loading && <LoadingCenter />}

      {data && !loading && (
        <div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 16 }}>
            <div className="card" style={{ padding: 12, textAlign: 'center' }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-primary)' }}>{data.player1.split('#')[0]}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>{data.player1}</div>
            </div>
            <div className="card" style={{ padding: 12, textAlign: 'center' }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-primary)' }}>{data.player2.split('#')[0]}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>{data.player2}</div>
            </div>
          </div>

          {data.togetherGames > 0 && (
            <div className="card" style={{ padding: 12, marginBottom: 12 }}>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
                함께한 경기: <strong>{data.togetherGames}판</strong> | 승률: <strong style={{ color: data.togetherWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>{data.togetherWinRate.toFixed(1)}%</strong>
              </div>
              {compareSnap(data.p1TogetherStats, data.p2TogetherStats, '🤝 함께 플레이')}
            </div>
          )}

          {data.versusGames > 0 && (
            <div className="card" style={{ padding: 12, marginBottom: 12 }}>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
                맞대결: <strong>{data.versusGames}판</strong> | {data.player1.split('#')[0]} 승률: <strong style={{ color: data.player1VsWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>{data.player1VsWinRate.toFixed(1)}%</strong>
              </div>
              {compareSnap(data.p1VersusStats, data.p2VersusStats, '⚔️ 맞대결')}
            </div>
          )}

          <div className="card" style={{ padding: 12 }}>
            {compareSnap(data.overallP1Stats, data.overallP2Stats, '📊 전체 통계')}
          </div>
        </div>
      )}
    </div>
  );
}

// ── 세션 분석 탭 ──────────────────────────────────────
function SessionsTab({ mode }: { mode: string }) {
  const [data, setData] = useState<SessionReportResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<SessionReportResult>(`/stats/sessions?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 12 }}>
        총 {data.totalSessions}개 세션
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {data.sessions.map((s: SessionEntry) => (
          <div key={s.date} className="card" style={{ padding: 12 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 6 }}>
              <div>
                <div style={{ fontWeight: 700, fontSize: 14 }}>{s.date}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 2 }}>
                  {s.games}경기 · 약 {s.totalDurationMin}분
                </div>
              </div>
              <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>팀100</div>
                  <div style={{ fontWeight: 700, fontSize: 14, color: 'var(--color-win)' }}>{s.team100Wins}</div>
                </div>
                <div style={{ fontSize: 12, color: 'var(--color-text-disabled)' }}>vs</div>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>팀200</div>
                  <div style={{ fontWeight: 700, fontSize: 14, color: 'var(--color-loss)' }}>{s.team200Wins}</div>
                </div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 12, marginTop: 8, flexWrap: 'wrap' }}>
              {s.sessionMvp && (
                <div style={{ fontSize: 12 }}>
                  <span style={{ color: 'var(--color-text-disabled)' }}>MVP: </span>
                  <span style={{ fontWeight: 700, color: '#FFD700' }}>{s.sessionMvp.split('#')[0]}</span>
                  <span style={{ color: 'var(--color-text-disabled)', fontSize: 11 }}> (KDA {s.sessionMvpKda.toFixed(2)})</span>
                </div>
              )}
              {s.pentaKills > 0 && (
                <div style={{ fontSize: 12 }}>
                  <span style={{ color: 'var(--color-text-disabled)' }}>펜타킬: </span>
                  <span style={{ fontWeight: 700, color: '#f472b6' }}>{s.pentaKills}회</span>
                </div>
              )}
              <div style={{ fontSize: 12 }}>
                <span style={{ color: 'var(--color-text-disabled)' }}>총킬: </span>
                <span style={{ fontWeight: 600 }}>{s.totalKills}</span>
              </div>
            </div>
            {s.participants.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginTop: 8 }}>
                {s.participants.map(p => (
                  <span key={p} style={{ fontSize: 10, padding: '2px 6px', borderRadius: 10, background: 'var(--color-bg-hover)', color: 'var(--color-text-secondary)' }}>
                    {p.split('#')[0]}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

// ── 챔피언 티어리스트 탭 ──────────────────────────────
const TIER_COLORS: Record<string, string> = { S: '#FFD700', A: '#4ade80', B: '#60a5fa', C: '#9ca3af', D: '#f87171' };

function TierTab({ mode }: { mode: string }) {
  const [data, setData] = useState<ChampionTierResult | null>(null);
  const [loading, setLoading] = useState(false);
  const { champions } = useDragon();

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<ChampionTierResult>(`/stats/champion-tier?mode=${mode}&minGames=3`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const tiers = ['S', 'A', 'B', 'C', 'D'];

  return (
    <div>
      <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 12 }}>
        총 {data.totalMatches}경기 기준 (최소 3게임)
      </p>
      {tiers.map(tier => {
        const list: ChampionTierEntry[] = data.byTier[tier] ?? [];
        if (!list.length) return null;
        const color = TIER_COLORS[tier] ?? '#888';
        return (
          <div key={tier} style={{ marginBottom: 20 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
              <div style={{ width: 28, height: 28, borderRadius: 6, background: color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 14, color: '#111' }}>{tier}</div>
              <span style={{ fontWeight: 700, fontSize: 13, color }}>{tier} 티어</span>
              <span style={{ fontSize: 11, color: 'var(--color-text-disabled)' }}>({list.length})</span>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {list.map((c: ChampionTierEntry) => {
                const nameKo = champions.get(c.championId)?.nameKo ?? c.champion;
                return (
                  <ChampionLink key={c.champion} champion={c.champion} championId={c.championId} className="popup-trigger--card">
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, padding: '8px 10px', background: 'var(--color-bg-secondary)', borderRadius: 8, border: `1px solid ${color}44`, minWidth: 70, cursor: 'pointer' }}>
                      <ChampImg championId={c.championId} champion={c.champion} size={36} />
                      <div style={{ fontSize: 11, fontWeight: 600, textAlign: 'center', lineHeight: 1.2 }}>{nameKo}</div>
                      <div style={{ fontSize: 10, color: c.winRate >= 60 ? 'var(--color-win)' : c.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)' }}>
                        {c.winRate.toFixed(1)}%
                      </div>
                      <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{c.games}픽</div>
                    </div>
                  </ChampionLink>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ── 게임 길이별 성향 탭 ───────────────────────────────
function GameLengthTab({ mode }: { mode: string }) {
  const [data, setData] = useState<GameLengthTendencyResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<GameLengthTendencyResult>(`/stats/game-length-tendency?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>플레이어별 게임 길이 성향</h3>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--color-bg-hover)' }}>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>성향</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>단기전<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(~20분)</span></th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>중기전<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(20~35분)</span></th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>장기전<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(35분+)</span></th>
            </tr>
          </thead>
          <tbody>
            {data.players.map((p: GameLengthTendencyEntry) => {
              const wrStyle = (wr: number) => ({
                color: wr >= 60 ? 'var(--color-win)' : wr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)',
                fontWeight: 600 as const,
              });
              return (
                <tr key={p.riotId} style={{ borderTop: '1px solid var(--color-border)' }}>
                  <td style={{ padding: '7px 10px' }}>
                    <PlayerLink riotId={p.riotId} mode={mode}>
                      <span style={{ fontWeight: 600 }}>{p.riotId.split('#')[0]}</span>
                    </PlayerLink>
                    <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{p.totalGames}게임</div>
                  </td>
                  <td style={{ padding: '7px 10px', textAlign: 'center' }}>
                    <span style={{ fontSize: 11, padding: '2px 6px', borderRadius: 10, background: 'var(--color-primary)22', color: 'var(--color-primary)' }}>{p.tendency}</span>
                  </td>
                  <td style={{ padding: '7px 10px', textAlign: 'center' }}>
                    {p.shortGame.games > 0 ? <span style={wrStyle(p.shortGame.winRate)}>{p.shortGame.winRate.toFixed(1)}%</span> : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                    {p.shortGame.games > 0 && <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{p.shortGame.games}게임</div>}
                  </td>
                  <td style={{ padding: '7px 10px', textAlign: 'center' }}>
                    {p.midGame.games > 0 ? <span style={wrStyle(p.midGame.winRate)}>{p.midGame.winRate.toFixed(1)}%</span> : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                    {p.midGame.games > 0 && <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{p.midGame.games}게임</div>}
                  </td>
                  <td style={{ padding: '7px 10px', textAlign: 'center' }}>
                    {p.longGame.games > 0 ? <span style={wrStyle(p.longGame.winRate)}>{p.longGame.winRate.toFixed(1)}%</span> : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                    {p.longGame.games > 0 && <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{p.longGame.games}게임</div>}
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

// ── 초반 지배력 탭 ────────────────────────────────────
function EarlyGameTab({ mode }: { mode: string }) {
  const [data, setData] = useState<EarlyGameDominanceResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<EarlyGameDominanceResult>(`/stats/early-game?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  return (
    <div>
      {/* 하이라이트 카드 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 20 }}>
        <div className="card" style={{ padding: 14, textAlign: 'center' }}>
          <div style={{ fontSize: 24, marginBottom: 4 }}>🗡️</div>
          <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginBottom: 4 }}>퍼블킹</div>
          <div style={{ fontWeight: 700, fontSize: 15, color: '#FFD700' }}>{data.firstBloodKing?.split('#')[0] ?? '-'}</div>
          <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 4 }}>
            전체 퍼블 승률: {data.overallFirstBloodWinRate.toFixed(1)}%
          </div>
        </div>
        <div className="card" style={{ padding: 14, textAlign: 'center' }}>
          <div style={{ fontSize: 24, marginBottom: 4 }}>🏯</div>
          <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginBottom: 4 }}>포탑 파괴자</div>
          <div style={{ fontWeight: 700, fontSize: 15, color: '#60a5fa' }}>{data.towerDestroyer?.split('#')[0] ?? '-'}</div>
          <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 4 }}>
            전체 퍼타 승률: {data.overallFirstTowerWinRate.toFixed(1)}%
          </div>
        </div>
      </div>

      {/* 플레이어 랭킹 */}
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>초반 지배력 랭킹</h3>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--color-bg-hover)' }}>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>초반점수</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>퍼블%</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>퍼타%</th>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>뱃지</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: EarlyGameDominanceEntry, i: number) => (
              <tr key={e.riotId} style={{ borderTop: '1px solid var(--color-border)' }}>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 600 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32 }}>{e.games}게임</div>
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', fontWeight: 700, color: 'var(--color-primary)' }}>
                  {e.earlyGameScore.toFixed(1)}
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', color: e.firstBloodRate >= 0.3 ? 'var(--color-win)' : undefined }}>
                  {(e.firstBloodRate * 100).toFixed(1)}%
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', color: e.firstTowerRate >= 0.3 ? 'var(--color-win)' : undefined }}>
                  {(e.firstTowerRate * 100).toFixed(1)}%
                </td>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                    {e.badges.map((b: string) => (
                      <span key={b} style={{ fontSize: 10, padding: '1px 5px', borderRadius: 8, background: 'var(--color-primary)22', color: 'var(--color-primary)' }}>{b}</span>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 컴백 지수 탭 ──────────────────────────────────────
function ComebackTab({ mode }: { mode: string }) {
  const [data, setData] = useState<ComebackIndexResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<ComebackIndexResult>(`/stats/comeback?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const kings = data.rankings.filter((e: ComebackIndexEntry) => e.isKing);

  return (
    <div>
      {/* 컴백킹 하이라이트 */}
      {(data.comebackKing || kings.length > 0) && (
        <div style={{ marginBottom: 20 }}>
          <div className="card" style={{ padding: 14, textAlign: 'center', background: 'linear-gradient(135deg, var(--color-bg-secondary), var(--color-bg-hover))' }}>
            <div style={{ fontSize: 24, marginBottom: 4 }}>🔄</div>
            <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginBottom: 4 }}>컴백킹</div>
            <div style={{ fontWeight: 700, fontSize: 16, color: '#FFD700' }}>
              {data.comebackKing ? data.comebackKing.split('#')[0] : kings[0]?.riotId.split('#')[0] ?? '-'}
            </div>
            {kings[0] && (
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginTop: 6 }}>
                접전 승률 {kings[0].contestWinRate.toFixed(1)}% | 접전 {kings[0].contestGames}게임
              </div>
            )}
          </div>
        </div>
      )}

      {/* 비교 테이블 */}
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>접전 vs 압도 경기 승률</h3>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--color-bg-hover)' }}>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>전체 승률</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>접전 승률<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(항복경기 제외)</span></th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>압도 승률<br/><span style={{ fontSize: 10, fontWeight: 400, color: 'var(--color-text-disabled)' }}>(항복경기)</span></th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>컴백보너스</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: ComebackIndexEntry, i: number) => (
              <tr key={e.riotId} style={{ borderTop: '1px solid var(--color-border)', background: e.isKing ? 'var(--color-win)11' : undefined }}>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 600 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                    {e.isKing && <span style={{ fontSize: 10, color: '#FFD700' }}>👑</span>}
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32 }}>{e.totalGames}게임</div>
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', color: e.totalWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)', fontWeight: 600 }}>
                  {e.totalWinRate.toFixed(1)}%
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>
                  {e.contestGames > 0 ? (
                    <>
                      <span style={{ fontWeight: 600, color: e.contestWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>{e.contestWinRate.toFixed(1)}%</span>
                      <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{e.contestGames}게임</div>
                    </>
                  ) : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>
                  {e.surrenderGames > 0 ? (
                    <>
                      <span style={{ fontWeight: 600, color: e.surrenderWinRate >= 50 ? 'var(--color-win)' : 'var(--color-loss)' }}>{e.surrenderWinRate.toFixed(1)}%</span>
                      <div style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>{e.surrenderGames}게임</div>
                    </>
                  ) : <span style={{ color: 'var(--color-text-disabled)' }}>-</span>}
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', fontWeight: 700, color: e.comebackBonus > 0 ? 'var(--color-win)' : e.comebackBonus < 0 ? 'var(--color-loss)' : undefined }}>
                  {e.comebackBonus > 0 ? '+' : ''}{e.comebackBonus.toFixed(1)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 골드 효율 탭 ──────────────────────────────────────
function GoldEffTab({ mode }: { mode: string }) {
  const [data, setData] = useState<GoldEfficiencyResult | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    if (data) return;
    setLoading(true);
    try { setData(await api.get<GoldEfficiencyResult>(`/stats/gold-efficiency?mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode, data]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <LoadingCenter />;
  if (!data) return null;

  const kings = [
    { emoji: '💥', label: '딜 효율왕', name: data.dmgEfficiencyKing },
    { emoji: '👁️', label: '시야 효율왕', name: data.visionEfficiencyKing },
    { emoji: '🌾', label: 'CS 효율왕', name: data.csEfficiencyKing },
    { emoji: '🏰', label: '오브젝트 효율왕', name: data.objEfficiencyKing },
  ];

  return (
    <div>
      {/* 4개 효율왕 뱃지 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8, marginBottom: 20 }}>
        {kings.map(k => (
          <div key={k.label} className="card" style={{ padding: 12, textAlign: 'center' }}>
            <div style={{ fontSize: 22, marginBottom: 4 }}>{k.emoji}</div>
            <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', marginBottom: 3 }}>{k.label}</div>
            <div style={{ fontWeight: 700, fontSize: 14, color: '#FFD700' }}>{k.name?.split('#')[0] ?? '-'}</div>
          </div>
        ))}
      </div>

      {/* 플레이어 랭킹 테이블 */}
      <h3 style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>골드 효율 랭킹</h3>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--color-bg-hover)' }}>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>플레이어</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>효율점수</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>딜/골드</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>시야/골드</th>
              <th style={{ padding: '7px 10px', textAlign: 'center' }}>CS/골드</th>
              <th style={{ padding: '7px 10px', textAlign: 'left' }}>태그</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((e: GoldEfficiencyEntry, i: number) => (
              <tr key={e.riotId} style={{ borderTop: '1px solid var(--color-border)' }}>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <RankBadge rank={i + 1} />
                    <PlayerLink riotId={e.riotId} mode={mode}>
                      <span style={{ fontWeight: 600 }}>{e.riotId.split('#')[0]}</span>
                    </PlayerLink>
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--color-text-disabled)', paddingLeft: 32 }}>{e.games}게임</div>
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center', fontWeight: 700, color: 'var(--color-primary)' }}>
                  {e.goldEfficiencyScore.toFixed(2)}
                </td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>{e.avgDmgPerGold.toFixed(3)}</td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>{e.avgVisionPerGold.toFixed(4)}</td>
                <td style={{ padding: '7px 10px', textAlign: 'center' }}>{e.avgCsPerGold.toFixed(4)}</td>
                <td style={{ padding: '7px 10px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
                    {e.tags.map((t: string) => (
                      <span key={t} style={{ fontSize: 10, padding: '1px 5px', borderRadius: 8, background: 'var(--color-primary)22', color: 'var(--color-primary)' }}>{t}</span>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
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
      <div className="stats-tab-bar" style={{ overflowX: 'auto', display: 'flex', flexWrap: 'nowrap' }}>
        {TABS.map(t => (
          <button key={t.key} className={`stats-tab-btn ${tab === t.key ? 'active' : ''}`}
            onClick={() => setTab(t.key)} style={{ flexShrink: 0 }}>
            {t.label}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      {tab === 'overview'    && <OverviewTab mode={mode} />}
      {tab === 'elo'         && <div className="card" style={{ marginTop: 4 }}><EloTab /></div>}
      {tab === 'mvp'         && <div className="card" style={{ marginTop: 4 }}><MvpTab         mode={mode} /></div>}
      {tab === 'lane'        && <div className="card" style={{ marginTop: 4 }}><LaneTab        mode={mode} /></div>}
      {tab === 'synergy'     && <div className="card" style={{ marginTop: 4 }}><SynergyTab     mode={mode} /></div>}
      {tab === 'duo'         && <div className="card" style={{ marginTop: 4 }}><DuoTab         mode={mode} /></div>}
      {tab === 'awards'      && <div className="card" style={{ marginTop: 4 }}><AwardsTab      mode={mode} /></div>}
      {tab === 'chaos'       && <div className="card" style={{ marginTop: 4 }}><ChaosTab       mode={mode} /></div>}
      {tab === 'multikill'   && <div className="card" style={{ marginTop: 4 }}><MultikillTab   mode={mode} /></div>}
      {tab === 'defeat'      && <div className="card" style={{ marginTop: 4 }}><DefeatTab      mode={mode} /></div>}
      {tab === 'rival'       && <div className="card" style={{ marginTop: 4 }}><RivalTab       mode={mode} /></div>}
      {tab === 'chemistry'   && <div className="card" style={{ marginTop: 4 }}><ChemistryTab   mode={mode} /></div>}
      {tab === 'survival'    && <div className="card" style={{ marginTop: 4 }}><SurvivalTab    mode={mode} /></div>}
      {tab === 'jungle'      && <div className="card" style={{ marginTop: 4 }}><JungleTab      mode={mode} /></div>}
      {tab === 'support'     && <div className="card" style={{ marginTop: 4 }}><SupportTab     mode={mode} /></div>}
      {tab === 'position'    && <div className="card" style={{ marginTop: 4 }}><PositionTab    mode={mode} /></div>}
      {tab === 'certificate' && <div className="card" style={{ marginTop: 4 }}><CertificateTab mode={mode} /></div>}
      {tab === 'dna'         && <div className="card" style={{ marginTop: 4 }}><DnaTab         mode={mode} /></div>}
      {tab === 'meta'        && <div className="card" style={{ marginTop: 4 }}><MetaTab        mode={mode} /></div>}
      {tab === 'compare'    && <div className="card" style={{ marginTop: 4 }}><CompareTab     mode={mode} /></div>}
      {tab === 'sessions'   && <div className="card" style={{ marginTop: 4 }}><SessionsTab    mode={mode} /></div>}
      {tab === 'tier'       && <div className="card" style={{ marginTop: 4 }}><TierTab        mode={mode} /></div>}
      {tab === 'gamelength' && <div className="card" style={{ marginTop: 4 }}><GameLengthTab  mode={mode} /></div>}
      {tab === 'earlygame'  && <div className="card" style={{ marginTop: 4 }}><EarlyGameTab   mode={mode} /></div>}
      {tab === 'comeback'   && <div className="card" style={{ marginTop: 4 }}><ComebackTab    mode={mode} /></div>}
      {tab === 'goldeff'    && <div className="card" style={{ marginTop: 4 }}><GoldEffTab     mode={mode} /></div>}
      {tab === 'ban'         && <div className="card" style={{ marginTop: 4 }}><BanAnalysisTab mode={mode} /></div>}
      {tab === 'timepattern' && <div className="card" style={{ marginTop: 4 }}><TimePatternTab mode={mode} /></div>}
      {tab === 'kp'          && <div className="card" style={{ marginTop: 4 }}><KillParticipationTab mode={mode} /></div>}
      {tab === 'pospool'     && <div className="card" style={{ marginTop: 4 }}><PositionPoolTab mode={mode} /></div>}
    </div>
  );
}

/* ── 밴 분석 ─── */
function BanAnalysisTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['ban-analysis', mode],
    queryFn: () => api.get<BanAnalysisResult>(`/stats/ban-analysis?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data || data.topBanned.length === 0) return <div style={{ padding: 24, color: 'var(--color-text-secondary)' }}>밴 데이터가 없습니다</div>;

  return (
    <div>
      <div style={{ marginBottom: 8, fontSize: 12, color: 'var(--color-text-secondary)' }}>
        총 {data.totalGamesAnalyzed}게임 분석 · 가장 많이 밴된 챔피언: {data.mostBannedChampion ?? '-'}
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 11 }}>
            <th style={{ textAlign: 'left', padding: '6px 8px' }}>#</th>
            <th style={{ textAlign: 'left', padding: '6px 8px' }}>챔피언</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>밴 횟수</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>밴율</th>
          </tr>
        </thead>
        <tbody>
          {data.topBanned.map((e: BanEntry, i: number) => {
            const c = champions.get(e.championId);
            return (
              <tr key={e.champion} style={{ borderBottom: '1px solid var(--color-border)' }}>
                <td style={{ padding: '8px', color: 'var(--color-text-secondary)', fontSize: 11 }}>{i + 1}</td>
                <td style={{ padding: '8px', display: 'flex', alignItems: 'center', gap: 8 }}>
                  {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={28} height={28} style={{ borderRadius: 4 }} />}
                  <ChampionLink champion={e.champion}>{c?.nameKo ?? e.champion}</ChampionLink>
                </td>
                <td style={{ padding: '8px', textAlign: 'right', fontWeight: 600 }}>{e.banCount}회</td>
                <td style={{ padding: '8px', textAlign: 'right', color: e.banRate >= 50 ? '#FF4757' : e.banRate >= 30 ? '#FF6B2B' : 'inherit' }}>
                  {e.banRate.toFixed(1)}%
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

/* ── 시간 패턴 ─── */
function TimePatternTab({ mode }: { mode: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['time-pattern', mode],
    queryFn: () => api.get<TimePatternResult>(`/stats/time-pattern?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data) return <div style={{ padding: 24, color: 'var(--color-text-secondary)' }}>데이터 없음</div>;

  const maxDayGames = Math.max(...data.byDay.map((d: DayPatternEntry) => d.games), 1);
  const maxHourGames = Math.max(...data.byHour.map((h: HourPatternEntry) => h.games), 1);

  return (
    <div style={{ display: 'flex', gap: 32, flexWrap: 'wrap' }}>
      <div style={{ flex: 1, minWidth: 200 }}>
        <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 14 }}>요일별 내전 횟수</div>
        {data.busiestDay && <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>가장 활발한 요일: <strong>{data.busiestDay}요일</strong></div>}
        {data.byDay.map((d: DayPatternEntry) => (
          <div key={d.dayOfWeek} style={{ marginBottom: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 3 }}>
              <span>{d.dayName}요일</span>
              <span style={{ color: 'var(--color-text-secondary)' }}>{d.games}게임 ({d.sessions}세션)</span>
            </div>
            <div style={{ height: 8, borderRadius: 4, background: 'var(--color-bg-hover)', overflow: 'hidden' }}>
              <div style={{ height: '100%', borderRadius: 4, background: 'var(--color-primary)', width: `${d.games / maxDayGames * 100}%`, transition: 'width 0.3s' }} />
            </div>
          </div>
        ))}
      </div>
      <div style={{ flex: 1, minWidth: 200 }}>
        <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 14 }}>시간대별 내전 횟수</div>
        {data.busiestHour !== null && <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>가장 활발한 시간: <strong>{data.busiestHour}시</strong></div>}
        {data.byHour.map((h: HourPatternEntry) => (
          <div key={h.hour} style={{ marginBottom: 6 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 2 }}>
              <span>{h.hour}시</span>
              <span style={{ color: 'var(--color-text-secondary)' }}>{h.games}게임</span>
            </div>
            <div style={{ height: 6, borderRadius: 3, background: 'var(--color-bg-hover)', overflow: 'hidden' }}>
              <div style={{ height: '100%', borderRadius: 3, background: 'var(--color-primary)', width: `${h.games / maxHourGames * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ── KP 랭킹 ─── */
function KillParticipationTab({ mode }: { mode: string }) {
  const { data, isLoading } = useQuery({
    queryKey: ['kp-ranking', mode],
    queryFn: () => api.get<KillParticipationResult>(`/stats/kill-participation?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data || data.rankings.length === 0) return <div style={{ padding: 24, color: 'var(--color-text-secondary)' }}>데이터 없음</div>;

  return (
    <div>
      {data.kpKing && (
        <div style={{ padding: '8px 12px', borderRadius: 8, background: 'var(--color-bg-hover)', marginBottom: 12, fontSize: 13 }}>
          ⚡ KP왕: <strong><PlayerLink riotId={data.kpKing}>{data.kpKing.split('#')[0]}</PlayerLink></strong>
        </div>
      )}
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 11 }}>
            <th style={{ textAlign: 'left', padding: '6px 8px' }}>#</th>
            <th style={{ textAlign: 'left', padding: '6px 8px' }}>플레이어</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>게임</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>평균 KP</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>승리 KP</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>패배 KP</th>
            <th style={{ textAlign: 'right', padding: '6px 8px' }}>킬/어시</th>
          </tr>
        </thead>
        <tbody>
          {data.rankings.map((p: KillParticipationEntry, i: number) => (
            <tr key={p.riotId} style={{ borderBottom: '1px solid var(--color-border)' }}>
              <td style={{ padding: '8px', color: i < 3 ? 'var(--color-primary)' : 'var(--color-text-secondary)', fontWeight: i < 3 ? 700 : 400 }}>{i + 1}</td>
              <td style={{ padding: '8px' }}><PlayerLink riotId={p.riotId}>{p.riotId.split('#')[0]}</PlayerLink></td>
              <td style={{ padding: '8px', textAlign: 'right', color: 'var(--color-text-secondary)' }}>{p.games}</td>
              <td style={{ padding: '8px', textAlign: 'right', fontWeight: 700, color: p.avgKp >= 70 ? '#4CAF50' : 'inherit' }}>{p.avgKp.toFixed(1)}%</td>
              <td style={{ padding: '8px', textAlign: 'right', color: 'var(--color-win)' }}>{p.avgKpWin.toFixed(1)}%</td>
              <td style={{ padding: '8px', textAlign: 'right', color: 'var(--color-loss)' }}>{p.avgKpLoss.toFixed(1)}%</td>
              <td style={{ padding: '8px', textAlign: 'right', color: 'var(--color-text-secondary)' }}>{p.avgKills.toFixed(1)} / {p.avgAssists.toFixed(1)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/* ── 포지션 챔피언 풀 ─── */
function PositionPoolTab({ mode }: { mode: string }) {
  const { champions } = useDragon();
  const [selectedPos, setSelectedPos] = useState('TOP');
  const [selectedPlayer, setSelectedPlayer] = useState<string | null>(null);
  const { data, isLoading } = useQuery({
    queryKey: ['pos-pool', mode],
    queryFn: () => api.get<PositionChampionPoolResult>(`/stats/position-champion-pool?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data) return <div style={{ padding: 24, color: 'var(--color-text-secondary)' }}>데이터 없음</div>;

  const POSITIONS = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'];
  const POS_LABEL: Record<string, string> = { TOP: '탑', JUNGLE: '정글', MID: '미드', BOTTOM: '원딜', SUPPORT: '서폿' };

  const posPlayers = data.allPlayers.filter((p: PlayerPositionEntry) => p.position === selectedPos).sort((a: PlayerPositionEntry, b: PlayerPositionEntry) => b.games - a.games);
  const allRiotIds = [...new Set(data.allPlayers.map((p: PlayerPositionEntry) => p.riotId))].sort();

  const playerEntries = selectedPlayer
    ? data.allPlayers.filter((p: PlayerPositionEntry) => p.riotId === selectedPlayer).sort((a: PlayerPositionEntry, b: PlayerPositionEntry) => b.games - a.games)
    : [];

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        {POSITIONS.map(pos => (
          <button key={pos} onClick={() => { setSelectedPos(pos); setSelectedPlayer(null); }}
            style={{ padding: '6px 14px', borderRadius: 20, border: '1px solid var(--color-border)', background: selectedPos === pos && !selectedPlayer ? 'var(--color-primary)' : 'var(--color-bg-hover)', color: selectedPos === pos && !selectedPlayer ? '#fff' : 'var(--color-text-primary)', cursor: 'pointer', fontWeight: selectedPos === pos ? 600 : 400, fontSize: 12 }}>
            {POS_LABEL[pos]}
          </button>
        ))}
        <select onChange={e => setSelectedPlayer(e.target.value || null)} value={selectedPlayer ?? ''}
          style={{ padding: '6px 10px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--color-bg-hover)', color: 'var(--color-text-primary)', cursor: 'pointer', fontSize: 12 }}>
          <option value="">플레이어 선택</option>
          {(allRiotIds as string[]).map(id => <option key={id} value={id}>{id.split('#')[0]}</option>)}
        </select>
      </div>

      {!selectedPlayer ? (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--color-border)', color: 'var(--color-text-secondary)', fontSize: 11 }}>
              <th style={{ textAlign: 'left', padding: '6px 8px' }}>플레이어</th>
              <th style={{ textAlign: 'center', padding: '6px 8px' }}>주챔피언</th>
              <th style={{ textAlign: 'right', padding: '6px 8px' }}>게임</th>
              <th style={{ textAlign: 'right', padding: '6px 8px' }}>승률</th>
            </tr>
          </thead>
          <tbody>
            {posPlayers.map((p: PlayerPositionEntry) => {
              const c = p.topChampionId ? champions.get(p.topChampionId) : null;
              return (
                <tr key={p.riotId} style={{ borderBottom: '1px solid var(--color-border)', cursor: 'pointer' }}
                  onClick={() => setSelectedPlayer(p.riotId)}>
                  <td style={{ padding: '8px' }}><PlayerLink riotId={p.riotId}>{p.riotId.split('#')[0]}</PlayerLink></td>
                  <td style={{ padding: '8px', textAlign: 'center' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 4 }}>
                      {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={24} height={24} style={{ borderRadius: 4 }} />}
                      <span>{c?.nameKo ?? p.topChampion ?? '-'}</span>
                    </div>
                  </td>
                  <td style={{ padding: '8px', textAlign: 'right' }}>{p.games}</td>
                  <td style={{ padding: '8px', textAlign: 'right', color: p.winRate >= 60 ? 'var(--color-win)' : p.winRate < 45 ? 'var(--color-loss)' : 'inherit' }}>{p.winRate.toFixed(1)}%</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      ) : (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <button onClick={() => setSelectedPlayer(null)} style={{ padding: '4px 10px', borderRadius: 6, border: '1px solid var(--color-border)', background: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', fontSize: 12 }}>← 뒤로</button>
            <strong><PlayerLink riotId={selectedPlayer}>{selectedPlayer.split('#')[0]}</PlayerLink></strong> 포지션별 챔피언 풀
          </div>
          {playerEntries.map((pe: PlayerPositionEntry) => (
            <div key={pe.position} style={{ marginBottom: 20 }}>
              <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 8 }}>{POS_LABEL[pe.position] ?? pe.position} ({pe.games}게임 · {pe.winRate.toFixed(1)}%)</div>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {pe.champions.slice(0, 8).map((ce: PositionChampEntry) => {
                  const c = champions.get(ce.championId);
                  return (
                    <div key={ce.champion} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, width: 56 }}>
                      {c?.imageUrl ? (
                        <img src={c.imageUrl} alt={c.nameKo} width={44} height={44} style={{ borderRadius: 8, border: '1px solid var(--color-border)' }} />
                      ) : (
                        <div style={{ width: 44, height: 44, borderRadius: 8, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10 }}>{ce.champion.slice(0, 2)}</div>
                      )}
                      <span style={{ fontSize: 9, color: 'var(--color-text-secondary)', textAlign: 'center' }}>{c?.nameKo ?? ce.champion}</span>
                      <span style={{ fontSize: 9, color: ce.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>{ce.winRate.toFixed(0)}% ({ce.games})</span>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
