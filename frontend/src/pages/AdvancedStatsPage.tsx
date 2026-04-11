import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../lib/api/api';
import type {
  MvpStatsResult, MvpPlayerStat,
  ChampionSynergyResult, ChampionSynergy,
  DuoStatsResult, DuoStat,
} from '../lib/types/stats';
import { LoadingCenter } from '../components/common/Spinner';
import { Button } from '../components/common/Button';
import { WinRateBar, ChampImg, RankBadge } from './stats-tabs/shared';
import { MODES } from '../lib/lol';
import '../styles/pages/stats.css';

const TABS = [
  { key: 'mvp',      label: '🏆 MVP 랭킹' },
  { key: 'synergy',  label: '⚡ 챔피언 시너지' },
  { key: 'duo',      label: '🤝 듀오 시너지' },
];

// ── MVP 탭 ────────────────────────────────────────────
function MvpTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
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
              <th>주요 챔피언</th>
            </tr>
          </thead>
          <tbody>
            {data.rankings.map((p: MvpPlayerStat, i) => (
              <tr key={p.riotId} className="member-stats-row" onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    <span style={{ fontWeight: 700, fontSize: 13 }}>{p.riotId.split('#')[0]}</span>
                    <span style={{ fontSize: 10, color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</span>
                  </div>
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
                <td style={{ fontSize: 12, color: 'var(--color-text-secondary)' }}>{p.topChampion ?? '—'}</td>
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
  const [data, setData] = useState<ChampionSynergyResult | null>(null);
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
            <button key={n}
              className={`member-sort-tab ${minGames === n ? 'active' : ''}`}
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
            {data.synergies.map((s: ChampionSynergy, i) => (
              <tr key={`${s.champion1}-${s.champion2}`} className="member-stats-row">
                <td><RankBadge rank={i + 1} /></td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <ChampImg championId={s.champion1Id} champion={s.champion1} size={28} />
                    <div>
                      <span style={{ fontWeight: 700, fontSize: 12 }}>{s.champion1}</span>
                    </div>
                    <span style={{ color: 'var(--color-text-disabled)', fontSize: 14, fontWeight: 300 }}>+</span>
                    <ChampImg championId={s.champion2Id} champion={s.champion2} size={28} />
                    <div>
                      <span style={{ fontWeight: 700, fontSize: 12 }}>{s.champion2}</span>
                    </div>
                  </div>
                </td>
                <td className="table-number">{s.games}</td>
                <td><WinRateBar winRate={s.winRate} wins={s.wins} losses={s.games - s.wins} /></td>
                <td className="table-number" style={{ fontWeight: 600 }}>{s.avgCombinedKills.toFixed(1)}</td>
              </tr>
            ))}
            {!data.synergies.length && (
              <tr><td colSpan={5} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음 (최소 {minGames}게임 이상 조합 없음)</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 듀오 시너지 탭 ────────────────────────────────────
function DuoTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [data, setData] = useState<DuoStatsResult | null>(null);
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
            <button key={n}
              className={`member-sort-tab ${minGames === n ? 'active' : ''}`}
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
                    <button className="player-name-btn" onClick={e => { e.stopPropagation(); navigate(`/player-stats/${encodeURIComponent(d.player1)}`); }}>
                      {d.player1.split('#')[0]}
                    </button>
                    <span style={{ color: 'var(--color-text-disabled)', fontSize: 12 }}>+</span>
                    <button className="player-name-btn" onClick={e => { e.stopPropagation(); navigate(`/player-stats/${encodeURIComponent(d.player2)}`); }}>
                      {d.player2.split('#')[0]}
                    </button>
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
              <tr><td colSpan={6} style={{ textAlign: 'center', padding: '40px 0', color: 'var(--color-text-secondary)' }}>데이터 없음 (최소 {minGames}게임 이상 조합 없음)</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ── 메인 페이지 ───────────────────────────────────────
export function AdvancedStatsPage() {
  const [mode, setMode] = useState('normal');
  const [tab, setTab]   = useState('mvp');

  return (
    <div>
      {/* 헤더 */}
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">고급 통계</h1>
          <p className="page-subtitle">MVP 랭킹 · 챔피언 시너지 · 듀오 시너지</p>
        </div>
        <div className="flex gap-sm">
          {MODES.map(m => (
            <Button key={m.value} variant={mode === m.value ? 'primary' : 'secondary'}
              size="sm" onClick={() => setMode(m.value)}>{m.label}</Button>
          ))}
        </div>
      </div>

      {/* 탭 선택 */}
      <div className="adv-tab-header">
        {TABS.map(t => (
          <button key={t.key}
            onClick={() => setTab(t.key)}
            className={`adv-tab-btn${tab === t.key ? ' active' : ''}`}>
            {t.label}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      <div className="card">
        {tab === 'mvp'     && <MvpTab     mode={mode} />}
        {tab === 'synergy' && <SynergyTab mode={mode} />}
        {tab === 'duo'     && <DuoTab     mode={mode} />}
      </div>
    </div>
  );
}
