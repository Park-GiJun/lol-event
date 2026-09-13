import { POSITIONS, positionLabel, type Position } from '@/lib/position';
import { POSITION_ICON } from '@/components/icons/positionIcon';
import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { LaneLeaderboardResult, PlayerLaneStat, TimelineAverages } from '../../lib/types/stats';
import { LoadingCenter } from '../../components/common/Spinner';
import { useDragon } from '../../context/DragonContext';
import { PlayerLink } from '../../components/common/PlayerLink';
import { ChampionLink } from '../../components/common/ChampionLink';
import { Stat } from '@/components/ds/Stat';
import { Diff15 } from '@/components/ds/Timeline15';
import { useTimelineLane } from '@/hooks/usePlayerTimeline';
import { RankBadge, ChampImg, WinRateBar } from './shared';

/** 라인마다 대표로 하나 더 보여줄 열. 키는 백엔드 Position 과 같아야 한다. */
const LANE_KEY_COL: Record<Position, { key: keyof PlayerLaneStat; label: string; format: (v: number) => string }> = {
  TOP:     { key: 'avgDamageTaken',    label: '평균 받은딜',  format: v => v.toLocaleString() },
  JUNGLE:  { key: 'avgNeutralMinions', label: '중립 몬스터', format: v => v.toFixed(1) },
  MID:     { key: 'avgDamage',         label: '평균 딜량',   format: v => v.toLocaleString() },
  ADC:     { key: 'avgCs',             label: '평균 CS',    format: v => v.toFixed(1) },
  SUPPORT: { key: 'avgWardsPlaced',    label: '평균 와드',  format: v => v.toFixed(1) },
};
const LANES = POSITIONS;

/**
 * 그 라인의 15분 평균. 라인 평균이라 격차는 0 근처라서 절댓값 지표와
 * "라인을 이긴 쪽이 게임도 이겼나"를 보여 준다.
 */
function LaneTimelineSummary({ lane, summary }: { lane: string; summary: TimelineAverages }) {
  const s = summary;
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 24, margin: '0 0 var(--spacing-md)' }}>
      <Stat label="타임라인 경기" value={s.games} sample={`${positionLabel(lane)} · 15분 지표 기준`} />
      <Stat
        label="15분 라인 이긴 쪽 승률"
        value={s.leadWinRate == null ? '-' : `${s.leadWinRate}%`}
        sample={`${s.leadGames}경기`}
      />
      <Stat label="CS@10" value={s.avgCsAt10 ?? '-'} />
      <Stat label="골드@15" value={s.avgGoldAt15 == null ? '-' : Math.round(s.avgGoldAt15).toLocaleString()} />
      <Stat label="15분 전 K / D" value={`${s.avgEarlyKills} / ${s.avgEarlyDeaths}`} sample={`퍼블 관여 ${s.firstBloodRate}%`} />
    </div>
  );
}

export default function LaneTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [selectedLane, setSelectedLane] = useState<string>('TOP');
  const [data, setData] = useState<LaneLeaderboardResult | null>(null);
  const [loading, setLoading] = useState(false);
  // 15분 지표는 부가 열이다. 못 불러오면 그 열만 '-' 로 비운다.
  const { data: timeline } = useTimelineLane(selectedLane, mode);

  const load = useCallback(async (lane: string) => {
    setLoading(true);
    setData(null);
    try { setData(await api.get<LaneLeaderboardResult>(`/stats/lane?lane=${lane}&mode=${mode}`)); }
    finally { setLoading(false); }
  }, [mode]);

  useEffect(() => { load(selectedLane); }, [load, selectedLane]);

  const keyCol = LANE_KEY_COL[selectedLane as Position];
  const timelineByPlayer = new Map(
    (timeline?.position === selectedLane ? timeline.players : []).map(p => [p.riotId, p.stats]),
  );

  return (
    <div>
      <div className="tab-bar" style={{ marginBottom: 'var(--spacing-md)' }}>
        {LANES.map(lane => {
          const Icon = POSITION_ICON[lane];
          return (
            <button key={lane}
              className={`tab-bar-item ${selectedLane === lane ? 'active' : ''}`}
              onClick={() => setSelectedLane(lane)}>
              <Icon size={16} />
              <span>{positionLabel(lane)}</span>
            </button>
          );
        })}
      </div>

      {timeline?.position === selectedLane && timeline.summary && (
        <LaneTimelineSummary lane={selectedLane} summary={timeline.summary} />
      )}

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
                <th className="table-number">{keyCol.label}</th>
                <th className="table-number" title="이 라인에서 15분에 상대 라이너보다 골드가 얼마나 앞섰나 (타임라인이 있는 경기만)">골드차@15</th>
                <th className="table-number" title="15분 골드가 상대보다 앞선 경기 비율 (타임라인이 있는 경기만)">라인 우세</th>
              </tr>
            </thead>
            <tbody>
              {data.players.map((p: PlayerLaneStat, i) => {
                const champName = p.topChampion ?? '';
                const nameKo = p.topChampionId ? (champions.get(p.topChampionId)?.nameKo ?? champName) : champName;
                const t = timelineByPlayer.get(p.riotId);
                return (
                  <tr key={p.riotId} className="member-stats-row"
                    onClick={() => navigate(`/player-stats/${encodeURIComponent(p.riotId)}`)}>
                    <td><RankBadge rank={i + 1} /></td>
                    <td>
                      <PlayerLink riotId={p.riotId} mode={mode}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                          <span style={{ fontWeight: 'var(--font-weight-bold)', fontSize: 'var(--font-size-sm)' }}>{p.riotId.split('#')[0]}</span>
                          <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-disabled)' }}>#{p.riotId.split('#')[1]}</span>
                        </div>
                      </PlayerLink>
                    </td>
                    <td>
                      {p.topChampionId ? (
                        <ChampionLink champion={champName} championId={p.topChampionId} mode={mode}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <ChampImg championId={p.topChampionId} champion={champName} size={24} />
                            <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>{nameKo}</span>
                          </div>
                        </ChampionLink>
                      ) : <span style={{ color: 'var(--color-text-disabled)' }}>—</span>}
                    </td>
                    <td className="table-number">{p.games}</td>
                    <td><WinRateBar winRate={p.winRate} wins={p.wins} losses={p.games - p.wins} /></td>
                    <td className="table-number" style={{ fontWeight: 700, color: p.kda >= 5 ? 'var(--color-win)' : p.kda >= 3 ? 'var(--color-primary)' : undefined }}>
                      {p.kda.toFixed(2)}
                    </td>
                    <td className="table-number" style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
                      {p.avgKills.toFixed(1)} / <span style={{ color: 'var(--color-error)' }}>{p.avgDeaths.toFixed(1)}</span> / {p.avgAssists.toFixed(1)}
                    </td>
                    <td className="table-number">{p.avgDamage.toLocaleString()}</td>
                    <td className="table-number">{keyCol.format(p[keyCol.key] as number)}</td>
                    <td className="table-number"><Diff15 value={t?.avgGoldDiff15} games={t?.laneGames} /></td>
                    <td className="table-number">{t?.laneLeadRate == null ? '-' : `${t.laneLeadRate}%`}</td>
                  </tr>
                );
              })}
              {data.players.length === 0 && (
                <tr><td colSpan={11} style={{ textAlign: 'center', padding: 'var(--spacing-2xl) 0', color: 'var(--color-text-secondary)' }}>데이터 없음</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
