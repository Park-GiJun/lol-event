import { useId } from 'react';
import { Link } from 'react-router-dom';
import { usePlayerTimeline } from '@/hooks/usePlayerTimeline';
import { ChampionIcon } from '@/components/ds/Champion';
import { Stat } from '@/components/ds/Stat';
import { diffColor, signed } from '@/lib/timeline';
import type { GoldDiffPoint, PlayerTimelineGame } from '@/lib/types/stats';

/**
 * 초반 격차 카드 — 타임라인이 있는 경기에서만 나오는 개인 지표.
 *
 * 격차는 전부 "나 − 같은 자리 상대"다. 라인전 폼(LaneForm)이 이긴/진 결과만 보여 준다면,
 * 이 카드는 얼마나 벌렸는지와 언제 벌어졌는지(분별 곡선)를 보여 준다.
 *
 * 타임라인 경기가 없으면(옛 수집기로만 뛴 사람) 카드 자체를 그리지 않는다.
 */
export function TimelineStats({ riotId }: { riotId: string }) {
  const { data, isPending, error } = usePlayerTimeline(riotId);

  // 부가 정보다. 못 불러왔다고 화면 전체를 막지 않는다.
  if (isPending || error || !data?.summary) return null;
  const s = data.summary;

  return (
    <section className="t-card">
      <div className="t-card-head">
        <h2 className="t-card-title">초반 격차</h2>
        <span className="t-card-more">타임라인 {s.games}경기</span>
      </div>

      <p className="t-empty" style={{ padding: '0 0 10px', textAlign: 'left' }}>
        같은 자리 상대와 15분에 얼마나 벌어졌는지다. 새 수집기로 받은 경기만 들어간다.
      </p>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 28 }}>
        <Stat
          label="15분 골드 격차"
          value={<span style={{ color: diffColor(s.avgGoldDiff15, 100) }}>{signed(s.avgGoldDiff15)}</span>}
          sample={data.goldDiffRank ? `${data.goldDiffRank}위 / ${data.rankedPlayers}명` : '라인 상대 없음'}
        />
        <Stat
          label="CS · 경험치 격차"
          value={
            <>
              <span style={{ color: diffColor(s.avgCsDiff15, 2) }}>{signed(s.avgCsDiff15, 1)}</span>
              <span className="t-stat-sample"> · </span>
              <span style={{ color: diffColor(s.avgXpDiff15, 100) }}>{signed(s.avgXpDiff15)}</span>
            </>
          }
          sample={s.avgCsAt10 != null ? `10분 CS ${s.avgCsAt10}` : undefined}
        />
        <Stat
          label="라인 우세"
          value={s.laneLeadRate == null ? '-' : `${s.laneLeadRate}%`}
          sample={
            s.leadWinRate != null
              ? `${s.laneGames}경기 중 · 앞섰을 때 승률 ${s.leadWinRate}%`
              : `${s.laneGames}경기 중`
          }
        />
        <Stat
          label="15분 전 K / D / A"
          value={`${s.avgEarlyKills} / ${s.avgEarlyDeaths} / ${s.avgEarlyAssists}`}
          sample={`솔로킬 경기당 ${s.avgSoloKills}`}
        />
        <Stat
          label="퍼블 관여"
          value={`${s.firstBloodRate}%`}
          sample={s.avgFirstDeathMinute != null ? `첫 데스 평균 ${s.avgFirstDeathMinute}분` : '죽은 적 없음'}
        />
      </div>

      {data.goldDiffCurve.length > 1 && <GoldDiffCurve points={data.goldDiffCurve} />}

      {data.games.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 16 }}>
          {data.games.map(g => <GameRow key={g.matchId} game={g} />)}
        </div>
      )}
    </section>
  );
}

/** 눈금 최댓값. 500 단위로 올려 잡아 축 숫자가 읽기 좋게 한다. */
function niceScale(values: number[]): number {
  const max = Math.max(0, ...values.map(Math.abs));
  return Math.max(500, Math.ceil(max / 500) * 500);
}

/**
 * 분별 평균 골드 격차. 기준선(호각) 위는 이긴 색, 아래는 진 색으로 칠한다.
 * 뒤로 갈수록 그 시간까지 간 경기가 줄어든다 — 점마다 경기 수를 툴팁에 단다.
 */
function GoldDiffCurve({ points }: { points: GoldDiffPoint[] }) {
  const id = useId();
  const W = 600;
  const H = 150;
  const PAD = 6;
  const mid = H / 2;
  const maxMinute = points[points.length - 1].minute || 1;
  const scale = niceScale(points.map(p => p.avgGoldDiff));

  const x = (minute: number) => (minute / maxMinute) * W;
  const y = (v: number) => mid - (v / scale) * (mid - PAD);

  const line = points.map(p => `${x(p.minute)},${y(p.avgGoldDiff)}`).join(' ');
  const area = `M${x(points[0].minute)},${mid} L${line.replace(/ /g, ' L')} L${x(maxMinute)},${mid} Z`;
  const ticks = [5, 10, 15, 20, 25, 30].filter(m => m < maxMinute);

  return (
    <div style={{ marginTop: 18 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 4 }}>
        <span>분별 평균 골드 격차</span>
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>±{scale.toLocaleString()}</span>
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={H} preserveAspectRatio="none" role="img" aria-label="분별 평균 골드 격차">
        <defs>
          <clipPath id={`${id}-up`}><rect x="0" y="0" width={W} height={mid} /></clipPath>
          <clipPath id={`${id}-down`}><rect x="0" y={mid} width={W} height={mid} /></clipPath>
        </defs>

        {ticks.map(m => (
          <line key={m} x1={x(m)} x2={x(m)} y1={0} y2={H} stroke="var(--color-border)" strokeWidth={1} vectorEffect="non-scaling-stroke" />
        ))}
        <line x1={0} x2={W} y1={mid} y2={mid} stroke="var(--color-border)" strokeDasharray="4 4" vectorEffect="non-scaling-stroke" />

        <path d={area} fill="var(--color-win)" fillOpacity={0.18} clipPath={`url(#${id}-up)`} />
        <path d={area} fill="var(--color-loss)" fillOpacity={0.18} clipPath={`url(#${id}-down)`} />
        <polyline points={line} fill="none" stroke="var(--color-text-primary)" strokeWidth={1.5} vectorEffect="non-scaling-stroke" />

        {/* 툴팁용 투명 막대. 점을 직접 찍으면 preserveAspectRatio="none" 에 눌려 타원이 된다. */}
        {points.map(p => (
          <rect key={p.minute} x={x(p.minute) - W / maxMinute / 2} y={0} width={W / maxMinute} height={H} fill="transparent">
            <title>{`${p.minute}분 · ${signed(p.avgGoldDiff)} (${p.games}경기)`}</title>
          </rect>
        ))}
      </svg>
      <div style={{ position: 'relative', height: 16, fontSize: 11, color: 'var(--color-text-secondary)' }}>
        {ticks.map(m => (
          <span key={m} style={{ position: 'absolute', left: `${(m / maxMinute) * 100}%`, transform: 'translateX(-50%)' }}>
            {m}분
          </span>
        ))}
      </div>
    </div>
  );
}

/** 경기 한 줄에 붙는 작은 곡선. 축은 경기마다 따로 잡는다 — 모양만 본다. */
function Sparkline({ values }: { values: number[] }) {
  if (values.length < 2) return null;
  const W = 80;
  const H = 24;
  const scale = niceScale(values);
  const pts = values
    .map((v, i) => `${(i / (values.length - 1)) * W},${H / 2 - (v / scale) * (H / 2 - 1)}`)
    .join(' ');
  const last = values[values.length - 1];
  return (
    <svg viewBox={`0 0 ${W} ${H}`} width={W} height={H} aria-hidden>
      <line x1={0} x2={W} y1={H / 2} y2={H / 2} stroke="var(--color-border)" strokeDasharray="2 2" />
      <polyline points={pts} fill="none" stroke={diffColor(last)} strokeWidth={1.5} />
    </svg>
  );
}

function GameRow({ game: g }: { game: PlayerTimelineGame }) {
  return (
    <Link
      to={`/matches/${encodeURIComponent(g.matchId)}`}
      className={`t-result ${g.win ? 't-result-win' : 't-result-loss'}`}
      style={{
        display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap',
        padding: '8px 12px', borderRadius: 10, background: 'var(--gray-50)',
      }}
    >
      <span style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 150 }}>
        <ChampionIcon championId={g.championId} champion={g.champion} size="sm" />
        {g.opponentChampionId != null ? (
          <>
            <span className="t-stat-sample">vs</span>
            <ChampionIcon championId={g.opponentChampionId} champion={g.opponentChampion ?? undefined} size="sm" />
            <span className="t-stat-sample">{g.opponentRiotId?.split('#')[0]}</span>
          </>
        ) : (
          <span className="t-stat-sample">라인 상대 없음</span>
        )}
      </span>
      <span style={{ width: 20, fontWeight: 700, color: g.win ? 'var(--color-win)' : 'var(--color-loss)' }}>
        {g.win ? '승' : '패'}
      </span>
      <span style={{ fontVariantNumeric: 'tabular-nums', minWidth: 90 }} title="15분 골드 격차">
        골드 <b style={{ color: diffColor(g.goldDiff15, 100) }}>{signed(g.goldDiff15)}</b>
      </span>
      <span className="t-stat-sample" style={{ minWidth: 60 }} title="15분 CS 격차">CS {signed(g.csDiff15)}</span>
      <span className="t-stat-sample" title="15분 전 킬 / 데스 / 어시스트">
        15분 전 {g.earlyKills}/{g.earlyDeaths}/{g.earlyAssists}
      </span>
      <span style={{ marginLeft: 'auto' }}><Sparkline values={g.goldDiffByMinute} /></span>
    </Link>
  );
}
