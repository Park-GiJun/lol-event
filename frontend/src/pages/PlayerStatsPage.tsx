import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Shield, Sword, Eye, Coins } from 'lucide-react';
import { api } from '../lib/api/api';
import type { PlayerDetailStats, ChampionStat, RecentMatchStat } from '../lib/types/stats';
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
          <button className="back-btn" onClick={() => navigate(-1)}><ChevronLeft size={18} /></button>
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
