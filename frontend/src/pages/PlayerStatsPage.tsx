import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Shield, Sword, Eye, Coins } from 'lucide-react';
import { api } from '../lib/api/api';
import type { PlayerDetailStats, ChampionStat, RecentMatchStat, LaneStat } from '../lib/types/stats';
import { useDragon } from '../context/DragonContext';
import { LoadingCenter } from '../components/common/Spinner';
import { Button } from '../components/common/Button';
import '../styles/pages/stats.css';

const MODES = [
  { value: 'all',    label: '전체' },
  { value: 'normal', label: '5v5 내전' },
  { value: 'aram',   label: '칼바람' },
];

const QUEUE_LABEL: Record<number, string> = { 0: '커스텀', 3130: '5v5 내전', 3270: '칼바람' };

function fmt(secs: number) {
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function WinRateBar({ winRate }: { winRate: number }) {
  const color = winRate >= 60 ? 'var(--color-win)' : winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{ flex: 1, height: 5, background: 'var(--color-bg-hover)', borderRadius: 3, overflow: 'hidden' }}>
        <div style={{ width: `${winRate}%`, height: '100%', background: color, borderRadius: 3 }} />
      </div>
      <span style={{ fontSize: 11, fontWeight: 700, color, minWidth: 32 }}>{winRate}%</span>
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
  return <div style={{ width: size, height: size, background: 'var(--color-bg-hover)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, color: 'var(--color-text-secondary)' }}>{champion.slice(0, 2)}</div>;
}

function StatCard({ icon, label, value }: { icon: React.ReactNode; label: string; value: string | number }) {
  return (
    <div className="player-stat-card">
      <div className="player-stat-icon">{icon}</div>
      <div className="player-stat-value">{value}</div>
      <div className="player-stat-label">{label}</div>
    </div>
  );
}

function ChampionTable({ stats }: { stats: ChampionStat[] }) {
  const maxDmg = Math.max(...stats.map(s => s.avgDamage), 1);
  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 'var(--font-size-sm)' }}>챔피언별 통계</div>
      <div className="table-wrapper">
        <table className="table">
          <thead>
            <tr>
              <th>챔피언</th>
              <th className="table-number">판수</th>
              <th style={{ minWidth: 100 }}>승률</th>
              <th className="table-number">KDA</th>
              <th className="table-number">K/D/A</th>
              <th className="table-number">평균 딜</th>
              <th className="table-number">CS</th>
              <th className="table-number">골드</th>
            </tr>
          </thead>
          <tbody>
            {stats.map(s => (
              <tr key={s.champion}>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <ChampImg championId={s.championId} champion={s.champion} size={28} />
                    <div>
                      <div style={{ fontWeight: 600, fontSize: 'var(--font-size-xs)' }}>{s.champion}</div>
                      <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{s.games}판 {s.wins}승</div>
                    </div>
                  </div>
                </td>
                <td className="table-number">{s.games}</td>
                <td><WinRateBar winRate={s.winRate} /></td>
                <td className="table-number" style={{ fontWeight: 700 }}>{s.kda.toFixed(2)}</td>
                <td className="table-number" style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
                  {s.avgKills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{s.avgDeaths.toFixed(1)}</span> / {s.avgAssists.toFixed(1)}
                </td>
                <td>
                  <div style={{ fontSize: 11 }}>{s.avgDamage.toLocaleString()}</div>
                  <div style={{ height: 3, background: 'var(--color-bg-hover)', borderRadius: 2, marginTop: 2 }}>
                    <div style={{ width: `${(s.avgDamage / maxDmg) * 100}%`, height: '100%', background: 'var(--color-primary)', borderRadius: 2 }} />
                  </div>
                </td>
                <td className="table-number">{s.avgCs.toFixed(1)}</td>
                <td className="table-number">{s.avgGold.toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 포지션 통계 ───────────────────────────────────────
const POSITION_META: Record<string, {
  label: string; emoji: string;
  keyStats: { key: keyof LaneStat; label: string; format: (v: number) => string }[];
}> = {
  TOP: {
    label: '탑', emoji: '🛡️',
    keyStats: [
      { key: 'avgDamage',      label: '평균 딜량',    format: v => v.toLocaleString() },
      { key: 'avgDamageTaken', label: '평균 피해량',   format: v => v.toLocaleString() },
      { key: 'avgCs',          label: '평균 CS',      format: v => v.toFixed(1) },
      { key: 'avgGold',        label: '평균 골드',    format: v => v.toLocaleString() },
    ],
  },
  JUNGLE: {
    label: '정글', emoji: '🌲',
    keyStats: [
      { key: 'avgNeutralMinions',  label: '중립 몬스터',   format: v => v.toFixed(1) },
      { key: 'avgObjectiveDamage', label: '오브젝트 딜',   format: v => v.toLocaleString() },
      { key: 'avgDamage',          label: '평균 딜량',     format: v => v.toLocaleString() },
      { key: 'avgKills',           label: '평균 킬',       format: v => v.toFixed(1) },
    ],
  },
  MID: {
    label: '미드', emoji: '⚡',
    keyStats: [
      { key: 'avgDamage',   label: '평균 딜량',  format: v => v.toLocaleString() },
      { key: 'avgKills',    label: '평균 킬',    format: v => v.toFixed(1) },
      { key: 'kda',         label: 'KDA',        format: v => v.toFixed(2) },
      { key: 'avgCs',       label: '평균 CS',    format: v => v.toFixed(1) },
    ],
  },
  BOTTOM: {
    label: '원딜', emoji: '🏹',
    keyStats: [
      { key: 'avgDamage',      label: '평균 딜량',  format: v => v.toLocaleString() },
      { key: 'avgCs',          label: '평균 CS',    format: v => v.toFixed(1) },
      { key: 'avgGold',        label: '평균 골드',  format: v => v.toLocaleString() },
      { key: 'kda',            label: 'KDA',        format: v => v.toFixed(2) },
    ],
  },
  SUPPORT: {
    label: '서폿', emoji: '💫',
    keyStats: [
      { key: 'avgWardsPlaced',    label: '평균 와드',  format: v => v.toFixed(1) },
      { key: 'avgCcTime',         label: 'CC 시간(초)', format: v => v.toFixed(1) },
      { key: 'avgVisionScore',    label: '시야 점수',  format: v => v.toFixed(1) },
      { key: 'avgAssists',        label: '평균 어시',  format: v => v.toFixed(1) },
    ],
  },
};

function LaneStatSection({ laneStats }: { laneStats: LaneStat[] }) {
  const [selected, setSelected] = useState<string | null>(laneStats[0]?.position ?? null);
  if (laneStats.length === 0) return null;
  const stat = laneStats.find(s => s.position === selected);
  const meta = selected ? POSITION_META[selected] : null;
  const wrColor = (wr: number) => wr >= 60 ? 'var(--color-win)' : wr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 'var(--font-size-sm)' }}>포지션별 통계</div>

      {/* 탭 */}
      <div className="lane-tabs">
        {laneStats.map(s => {
          const m = POSITION_META[s.position];
          return (
            <button key={s.position}
              className={`lane-tab ${selected === s.position ? 'active' : ''}`}
              onClick={() => setSelected(s.position)}>
              <span className="lane-tab-emoji">{m?.emoji}</span>
              <span className="lane-tab-label">{m?.label ?? s.position}</span>
              <span className="lane-tab-games">{s.games}판</span>
            </button>
          );
        })}
      </div>

      {stat && meta && (
        <div className="lane-stat-body">
          {/* 승률 + KDA 요약 */}
          <div className="lane-summary">
            <div className="lane-summary-wr">
              <div style={{ fontSize: 26, fontWeight: 800, color: wrColor(stat.winRate) }}>{stat.winRate}%</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{stat.wins}승 {stat.games - stat.wins}패</div>
              <div style={{ marginTop: 4, height: 5, width: 80, background: 'var(--color-bg-hover)', borderRadius: 3, overflow: 'hidden' }}>
                <div style={{ width: `${stat.winRate}%`, height: '100%', background: wrColor(stat.winRate), borderRadius: 3 }} />
              </div>
            </div>
            <div className="lane-summary-kda">
              <div style={{ fontSize: 22, fontWeight: 800 }}>{stat.kda.toFixed(2)} KDA</div>
              <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                {stat.avgKills.toFixed(1)} /&nbsp;
                <span style={{ color: 'var(--color-error)' }}>{stat.avgDeaths.toFixed(1)}</span>
                &nbsp;/ {stat.avgAssists.toFixed(1)}
              </div>
            </div>
          </div>

          {/* 포지션 핵심 스탯 */}
          <div className="lane-key-stats">
            {meta.keyStats.map(({ key, label, format }) => (
              <div key={key} className="lane-key-stat">
                <div className="lane-key-stat-value">{format(stat[key] as number)}</div>
                <div className="lane-key-stat-label">{label}</div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function RecentMatchCard({ m }: { m: RecentMatchStat }) {
  const date = new Date(m.gameCreation);
  const kda = m.deaths === 0 ? 'Perfect' : ((m.kills + m.assists) / m.deaths).toFixed(2);
  return (
    <div className={`recent-match-card ${m.win ? 'win' : 'loss'}`}>
      <div className="recent-match-champ">
        <ChampImg championId={m.championId} champion={m.champion} size={40} />
        <span className={`recent-match-result ${m.win ? 'win' : 'loss'}`}>{m.win ? '승' : '패'}</span>
      </div>
      <div className="recent-match-info">
        <div style={{ fontWeight: 700, fontSize: 'var(--font-size-xs)' }}>{m.champion}</div>
        <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
          {QUEUE_LABEL[m.queueId] ?? m.queueId} · {fmt(m.gameDuration)}
        </div>
        <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>
          {date.toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' })}
        </div>
      </div>
      <div className="recent-match-kda">
        <div style={{ fontWeight: 700, fontSize: 'var(--font-size-sm)' }}>
          <span>{m.kills}</span>
          <span style={{ color: 'var(--color-text-disabled)', margin: '0 2px' }}>/</span>
          <span style={{ color: 'var(--color-error)' }}>{m.deaths}</span>
          <span style={{ color: 'var(--color-text-disabled)', margin: '0 2px' }}>/</span>
          <span>{m.assists}</span>
        </div>
        <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{kda} KDA</div>
      </div>
      <div className="recent-match-stats">
        <div style={{ fontSize: 11 }}>{m.damage.toLocaleString()} 딜</div>
        <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>CS {m.cs}</div>
        <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{m.gold.toLocaleString()} G</div>
      </div>
    </div>
  );
}

export function PlayerStatsPage() {
  const { riotId: encodedId } = useParams<{ riotId: string }>();
  const riotId = decodeURIComponent(encodedId ?? '');
  const [data, setData] = useState<PlayerDetailStats | null>(null);
  const [mode, setMode] = useState('all');
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    api.get<PlayerDetailStats>(`/stats/player/${encodeURIComponent(riotId)}?mode=${mode}`)
      .then(setData)
      .finally(() => setLoading(false));
  }, [riotId, mode]);

  const winColor = (r: number) =>
    r >= 60 ? 'var(--color-win)' : r >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button className="back-btn" onClick={() => navigate('/player-stats')}><ChevronLeft size={18} /></button>
          <div>
            <h1 className="page-title">{riotId.split('#')[0]}</h1>
            <p className="page-subtitle" style={{ fontFamily: 'monospace' }}>#{riotId.split('#')[1]}</p>
          </div>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {loading ? <LoadingCenter /> : !data || data.games === 0 ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--color-text-secondary)' }}>데이터 없음</div>
      ) : (
        <>
          {/* 요약 */}
          <div className="player-summary-card card" style={{ marginBottom: 20 }}>
            <div className="player-summary-header">
              <div className="player-wr-ring" style={{ '--wr-color': winColor(data.winRate) } as React.CSSProperties}>
                <span style={{ fontSize: 18, fontWeight: 700, color: winColor(data.winRate) }}>{data.winRate}%</span>
                <span style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>승률</span>
              </div>
              <div>
                <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
                  {data.games}판 · <span style={{ color: 'var(--color-win)' }}>{data.wins}승</span> <span style={{ color: 'var(--color-loss)' }}>{data.losses}패</span>
                </div>
                <div style={{ fontSize: 22, fontWeight: 800, marginTop: 2 }}>{data.kda.toFixed(2)} KDA</div>
                <div style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>
                  {data.avgKills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{data.avgDeaths.toFixed(1)}</span> / {data.avgAssists.toFixed(1)}
                </div>
              </div>
            </div>
            <div className="player-stat-cards">
              <StatCard icon={<Sword size={14} />} label="평균 딜량" value={data.avgDamage.toLocaleString()} />
              <StatCard icon={<Shield size={14} />} label="평균 CS" value={data.avgCs.toFixed(1)} />
              <StatCard icon={<Coins size={14} />} label="평균 골드" value={data.avgGold.toLocaleString()} />
              <StatCard icon={<Eye size={14} />} label="평균 시야" value={data.avgVisionScore.toFixed(1)} />
            </div>
          </div>

          {/* 포지션 통계 */}
          {data.laneStats?.length > 0 && <LaneStatSection laneStats={data.laneStats} />}

          {/* 챔피언 통계 */}
          {data.championStats.length > 0 && <ChampionTable stats={data.championStats} />}

          {/* 최근 경기 */}
          {data.recentMatches.length > 0 && (
            <div className="card">
              <div style={{ fontWeight: 700, marginBottom: 12, fontSize: 'var(--font-size-sm)' }}>최근 경기</div>
              <div className="recent-matches-list">
                {data.recentMatches.map(m => <RecentMatchCard key={`${m.matchId}-${m.champion}`} m={m} />)}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
