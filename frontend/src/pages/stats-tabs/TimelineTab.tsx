import { useState } from 'react';
import { useTimelineStats } from '@/hooks/usePlayerTimeline';
import type { TimelineAverages } from '@/lib/types/stats';
import { LoadingCenter } from '@/components/common/Spinner';
import { PersonLink } from '@/components/ds/Champion';
import { Stat } from '@/components/ds/Stat';
import { Diff15 } from '@/components/ds/Timeline15';
import { positionLabel } from '@/lib/position';

/**
 * 초반 격차 — 타임라인이 있어야만 볼 수 있는 지표.
 *
 * 경기 종료 스탯으로는 "누가 라인전을 이겼나"를 알 수 없다. 30분 경기의 최종 골드는
 * 라인전보다 한타·스노우볼에 끌려간다. 타임라인의 15분 프레임은 그 왜곡이 없다.
 *
 * 새 수집기로 받은 경기에만 타임라인이 있어서 표본이 작다. 경기 수를 맨 앞에 두고,
 * 몇 판 안 뛴 사람은 표본 적음을 단다.
 */

type SortKey = 'avgGoldDiff15' | 'avgCsDiff15' | 'avgXpDiff15' | 'laneLeadRate' | 'avgCsAt10'
  | 'avgEarlyKills' | 'avgSoloKills' | 'firstBloodRate' | 'avgFirstDeathMinute' | 'games';

const COLUMNS: { key: SortKey; label: string; title: string; render: (s: TimelineAverages) => React.ReactNode }[] = [
  { key: 'games', label: '경기', title: '타임라인이 있는 경기 수 (라인 상대가 있었던 경기 수)',
    render: s => <>{s.games}{s.laneGames !== s.games && <span className="t-stat-sample"> ({s.laneGames})</span>}</> },
  { key: 'avgGoldDiff15', label: '골드차@15', title: '15분에 같은 자리 상대보다 골드가 얼마나 많았나',
    render: s => <Diff15 value={s.avgGoldDiff15} /> },
  { key: 'avgCsDiff15', label: 'CS차@15', title: '15분 CS 격차 (정글 몹 포함)',
    render: s => <Diff15 value={s.avgCsDiff15} even={2} digits={1} /> },
  { key: 'avgXpDiff15', label: 'XP차@15', title: '15분 경험치 격차',
    render: s => <Diff15 value={s.avgXpDiff15} /> },
  { key: 'laneLeadRate', label: '라인 우세', title: '15분 골드가 상대보다 앞선 경기 비율',
    render: s => s.laneLeadRate == null ? '-' : `${s.laneLeadRate}%` },
  { key: 'avgCsAt10', label: 'CS@10', title: '10분 CS',
    render: s => s.avgCsAt10 ?? '-' },
  { key: 'avgEarlyKills', label: '15분 전 K/D/A', title: '15분 전에 올린 킬 / 데스 / 어시스트 (경기당)',
    render: s => <KdaCell s={s} /> },
  { key: 'avgSoloKills', label: '솔로킬', title: '도움 없이 혼자 딴 킬 (경기당, 경기 전체)',
    render: s => s.avgSoloKills },
  { key: 'firstBloodRate', label: '퍼블 관여', title: '첫 킬에 킬이나 어시스트로 낀 경기 비율',
    render: s => `${s.firstBloodRate}%` },
  { key: 'avgFirstDeathMinute', label: '첫 데스', title: '처음 죽은 시각 평균 (한 번도 안 죽은 경기는 빼고)',
    render: s => s.avgFirstDeathMinute == null ? '-' : `${s.avgFirstDeathMinute}분` },
];

/** 이 경기 수 미만이면 표본 적음. 타임라인 표본은 전체 경기보다 훨씬 작아 기준을 낮게 둔다. */
const LOW_SAMPLE = 5;

function KdaCell({ s }: { s: TimelineAverages }) {
  return <>{s.avgEarlyKills} / <span style={{ color: 'var(--color-loss)' }}>{s.avgEarlyDeaths}</span> / {s.avgEarlyAssists}</>;
}

export default function TimelineTab({ mode }: { mode: string }) {
  const { data, isPending, error } = useTimelineStats(mode);
  const [sort, setSort] = useState<SortKey>('avgGoldDiff15');
  const [desc, setDesc] = useState(true);

  if (isPending) return <LoadingCenter />;
  if (error || !data) return <p className="t-empty">타임라인 지표를 불러오지 못했습니다.</p>;
  if (data.games === 0) {
    return (
      <p className="t-empty">
        아직 타임라인이 있는 경기가 없습니다. 새 수집기(v2.0 이상)로 수집한 경기부터 쌓입니다.
      </p>
    );
  }

  const rows = [...data.players].sort((a, b) => {
    const av = a.stats[sort];
    const bv = b.stats[sort];
    // 값이 없는 사람은 방향과 무관하게 맨 뒤.
    if (av == null) return bv == null ? 0 : 1;
    if (bv == null) return -1;
    return desc ? bv - av : av - bv;
  });

  const onSort = (key: SortKey) => {
    if (key === sort) setDesc(d => !d);
    else { setSort(key); setDesc(true); }
  };

  return (
    <div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 28, marginBottom: 20 }}>
        <Stat label="타임라인 경기" value={data.games} sample="새 수집기로 받은 경기만" />
        <Stat
          label="15분 골드 앞선 팀 승률"
          value={data.goldLeadWinRate == null ? '-' : `${data.goldLeadWinRate}%`}
          sample={`${data.goldLeadGames}경기`}
        />
        <Stat label="15분 평균 팀 골드 차" value={Math.round(data.avgTeamGoldGapAt15).toLocaleString()} />
        <Stat label="역전승" value={`${data.comebackGames}경기`} sample="15분 1,500골드 이상 열세에서" />
      </div>

      {data.positions.length > 0 && (
        <>
          <h3 className="t-card-title" style={{ fontSize: 14, margin: '0 0 8px' }}>라인별 15분 평균</h3>
          <div className="t-tablewrap" style={{ marginBottom: 24 }}>
            <table className="t-table">
              <thead>
                <tr>
                  <th>포지션</th>
                  <th className="t-num">경기</th>
                  <th className="t-num" title="15분에 라인 상대보다 골드가 앞섰던 쪽의 승률. 그 라인을 이기는 게 게임에 얼마나 직결되나">라인 이긴 쪽 승률</th>
                  <th className="t-num">CS@10</th>
                  <th className="t-num">골드@15</th>
                  <th className="t-num">15분 전 K/D/A</th>
                  <th className="t-num">솔로킬</th>
                  <th className="t-num">퍼블 관여</th>
                  <th className="t-num">첫 데스</th>
                </tr>
              </thead>
              <tbody>
                {data.positions.map(({ position, stats: s }) => (
                  <tr key={position}>
                    <td><b>{positionLabel(position)}</b></td>
                    <td className="t-num">{s.games}</td>
                    <td className="t-num">
                      {s.leadWinRate == null ? '-' : <><b>{s.leadWinRate}%</b><span className="t-stat-sample"> {s.leadGames}경기</span></>}
                    </td>
                    <td className="t-num">{s.avgCsAt10 ?? '-'}</td>
                    <td className="t-num">{s.avgGoldAt15 == null ? '-' : Math.round(s.avgGoldAt15).toLocaleString()}</td>
                    <td className="t-num" style={{ whiteSpace: 'nowrap' }}><KdaCell s={s} /></td>
                    <td className="t-num">{s.avgSoloKills}</td>
                    <td className="t-num">{s.firstBloodRate}%</td>
                    <td className="t-num">{s.avgFirstDeathMinute == null ? '-' : `${s.avgFirstDeathMinute}분`}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <h3 className="t-card-title" style={{ fontSize: 14, margin: '0 0 8px' }}>선수별</h3>
      <div className="t-tablewrap">
        <table className="t-table">
          <thead>
            <tr>
              <th>플레이어</th>
              {COLUMNS.map(c => (
                <th
                  key={c.key}
                  className="t-num"
                  title={c.title}
                  onClick={() => onSort(c.key)}
                  style={{ cursor: 'pointer', whiteSpace: 'nowrap' }}
                >
                  {c.label}{sort === c.key ? (desc ? ' ▼' : ' ▲') : ''}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map(e => (
              <tr key={e.riotId}>
                <td>
                  <span className="t-person">
                    <PersonLink riotId={e.riotId} />
                    {e.position && <span className="t-stat-sample">{positionLabel(e.position)}</span>}
                    {e.stats.games < LOW_SAMPLE && (
                      <span className="t-chip t-chip-low" title={`${e.stats.games}경기 — 표본이 적어 값이 크게 흔들립니다`}>
                        표본 적음
                      </span>
                    )}
                  </span>
                </td>
                {COLUMNS.map(c => (
                  <td key={c.key} className="t-num" style={{ whiteSpace: 'nowrap' }}>{c.render(e.stats)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
