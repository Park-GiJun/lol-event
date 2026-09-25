import { Link } from 'react-router-dom';
import { useTimelineChampions } from '@/hooks/usePlayerTimeline';
import { ChampionIcon } from '@/components/ds/Champion';
import { DivergingArea, Sparkline } from '@/components/ds/Chart';
import { Stat } from '@/components/ds/Stat';
import { Diff15 } from '@/components/ds/Timeline15';
import { positionLabel } from '@/lib/position';
import { diffColor, signed } from '@/lib/timeline';
import type { TimelineChampionGame } from '@/lib/types/stats';

/**
 * 챔피언 15분 지표.
 *
 * 바로 위 "라인전 지표"는 경기 종료 시점 누적값이라 후반 챔피언은 라인전을 져도 격차가 좋게 나온다.
 * 이 카드는 15분 프레임이라 "라인전에서 실제로 강한가"를 따로 보여 준다.
 * 타임라인이 있는 경기가 없으면 카드를 그리지 않는다.
 */
export function ChampionTimeline({ champion }: { champion: string }) {
  const { data } = useTimelineChampions('all', champion);
  const entry = data?.champions[0];
  if (!entry) return null;
  const s = entry.stats;

  return (
    <section className="t-card">
      <div className="t-card-head">
        <h2 className="t-card-title">15분 지표</h2>
        <span className="t-card-more">타임라인 {s.games}경기</span>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 28 }}>
        <Stat
          label="15분 골드 격차"
          value={<Diff15 value={s.avgGoldDiff15} />}
          sample={`라인 상대 있는 ${s.laneGames}경기`}
        />
        <Stat
          label="CS · 경험치 격차"
          value={<><Diff15 value={s.avgCsDiff15} even={2} digits={1} /><span className="t-stat-sample"> · </span><Diff15 value={s.avgXpDiff15} /></>}
        />
        <Stat
          label="라인 우세"
          value={s.laneLeadRate == null ? '-' : `${s.laneLeadRate}%`}
          sample={s.leadWinRate != null ? `앞섰을 때 승률 ${s.leadWinRate}% (${s.leadGames}경기)` : undefined}
        />
        <Stat
          label="CS@10 · 골드@15"
          value={`${s.avgCsAt10 ?? '-'} · ${s.avgGoldAt15 == null ? '-' : Math.round(s.avgGoldAt15).toLocaleString()}`}
        />
        <Stat
          label="15분 전 K / D / A"
          value={`${s.avgEarlyKills} / ${s.avgEarlyDeaths} / ${s.avgEarlyAssists}`}
          sample={`퍼블 관여 ${s.firstBloodRate}%`}
        />
      </div>

      {entry.byPosition.length > 1 && (
        <div className="t-tablewrap" style={{ marginTop: 16 }}>
          <table className="t-table">
            <thead>
              <tr>
                <th>포지션</th>
                <th className="t-num">경기</th>
                <th className="t-num">골드차@15</th>
                <th className="t-num">CS차@15</th>
                <th className="t-num">라인 우세</th>
                <th className="t-num">CS@10</th>
                <th className="t-num">15분 전 K/D/A</th>
              </tr>
            </thead>
            <tbody>
              {entry.byPosition.map(({ position, stats: p }) => (
                <tr key={position}>
                  <td><b>{positionLabel(position)}</b></td>
                  <td className="t-num">{p.games}</td>
                  <td className="t-num"><Diff15 value={p.avgGoldDiff15} /></td>
                  <td className="t-num"><Diff15 value={p.avgCsDiff15} even={2} digits={1} /></td>
                  <td className="t-num">{p.laneLeadRate == null ? '-' : `${p.laneLeadRate}%`}</td>
                  <td className="t-num">{p.avgCsAt10 ?? '-'}</td>
                  <td className="t-num">{p.avgEarlyKills} / {p.avgEarlyDeaths} / {p.avgEarlyAssists}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.curve.length > 1 && (
        <DivergingArea
          label="분별 평균 골드 격차"
          unit="분"
          points={data.curve.map(p => ({ at: p.minute, value: p.avgGoldDiff, note: `${p.games}경기` }))}
        />
      )}

      {data && data.matches.length > 0 && <Matches matches={data.matches} />}
    </section>
  );
}

/**
 * 이 챔피언이 나온 판들.
 *
 * 표본이 적을 때 평균을 억지로 내지 않는다 — 챔피언 하나는 보통 한두 판이라, 평균 한 줄보다
 * 판을 늘어놓는 쪽이 정직하다. 표본이 차면 위의 평균 카드가 의미를 갖는다.
 */
function Matches({ matches }: { matches: TimelineChampionGame[] }) {
  return (
    <div style={{ marginTop: 18 }}>
      <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 8 }}>
        나온 판 {matches.length}개
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {matches.map(m => (
          <Link
            key={`${m.matchId}-${m.riotId}`}
            to={`/matches/${encodeURIComponent(m.matchId)}`}
            className={`t-result ${m.win ? 't-result-win' : 't-result-loss'}`}
            style={{
              display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap',
              padding: '8px 12px', borderRadius: 10, background: 'var(--gray-50)',
              textDecoration: 'none', color: 'inherit',
            }}
          >
            <span style={{ width: 20, fontWeight: 700, color: m.win ? 'var(--color-win)' : 'var(--color-loss)' }}>
              {m.win ? '승' : '패'}
            </span>
            <span style={{ minWidth: 110 }}>{m.riotId.split('#')[0]}</span>
            <span className="t-stat-sample" style={{ minWidth: 44 }}>{positionLabel(m.position)}</span>

            <span style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 120 }}>
              {m.opponentChampionId != null ? (
                <>
                  <span className="t-stat-sample">vs</span>
                  <ChampionIcon championId={m.opponentChampionId} champion={m.opponentChampion ?? undefined} size="sm" />
                </>
              ) : (
                <span className="t-stat-sample">라인 상대 없음</span>
              )}
            </span>

            <span style={{ fontVariantNumeric: 'tabular-nums', minWidth: 90 }} title="15분 골드 격차">
              골드 <b style={{ color: diffColor(m.goldDiff15, 100) }}>{signed(m.goldDiff15)}</b>
            </span>
            <span className="t-stat-sample" title="15분 CS 격차">CS {signed(m.csDiff15)}</span>

            <span style={{ marginLeft: 'auto' }}><Sparkline values={m.goldDiffByMinute} /></span>
          </Link>
        ))}
      </div>
    </div>
  );
}
