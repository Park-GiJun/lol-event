import { useTimelineChampions } from '@/hooks/usePlayerTimeline';
import { Stat } from '@/components/ds/Stat';
import { Diff15 } from '@/components/ds/Timeline15';
import { positionLabel } from '@/lib/position';

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
    </section>
  );
}
