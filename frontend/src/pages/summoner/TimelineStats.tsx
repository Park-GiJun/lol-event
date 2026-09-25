import { Link } from 'react-router-dom';
import { usePlayerTimeline } from '@/hooks/usePlayerTimeline';
import { ChampionIcon } from '@/components/ds/Champion';
import { DivergingArea, MapScatter, Sparkline } from '@/components/ds/Chart';
import { Stat } from '@/components/ds/Stat';
import { diffColor, signed } from '@/lib/timeline';
import type { PlayerTimelineGame, PlayerTimelineResult } from '@/lib/types/stats';

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

      {data.goldDiffCurve.length > 1 && (
        <DivergingArea
          label="분별 평균 골드 격차"
          unit="분"
          points={data.goldDiffCurve.map(p => ({ at: p.minute, value: p.avgGoldDiff, note: `${p.games}경기` }))}
        />
      )}

      <Movement data={data} />

      {data.games.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 16 }}>
          {data.games.map(g => <GameRow key={g.matchId} game={g} />)}
        </div>
      )}
    </section>
  );
}

/**
 * 좌표에서 나온 지표와 데스 히트맵.
 *
 * 위의 15분 격차와 **표를 나눠 둔 이유**가 있다. 격차는 요청 시 계산한 값이고, 여기 있는
 * 것은 배치가 미리 접어 둔 값이다. 게다가 프레임은 분당 1점이라 체류 비율의 신뢰도가
 * 이벤트 좌표(데스 위치)보다 낮다 — 두 계열을 한 표에 섞으면 그 차이가 가려진다.
 */
function Movement({ data }: { data: PlayerTimelineResult }) {
  const stats = data.positionStats;
  const heatmap = data.deathHeatmap;

  // 배치가 아직 안 돌았다. 빈 화면 대신 그 사실을 알린다.
  if (!stats) {
    return (
      <p className="t-empty" style={{ textAlign: 'left', marginTop: 18 }}>
        동선 지표는 집계 대기 중입니다. 매일 새벽에 갱신됩니다.
      </p>
    );
  }

  return (
    <div style={{ marginTop: 22 }}>
      <div className="t-card-head" style={{ marginBottom: 10 }}>
        <h3 className="t-card-title" style={{ fontSize: 'var(--font-size-sm)' }}>동선</h3>
        <span className="t-card-more">{stats.games}경기 · 프레임 {stats.framesSampled.toLocaleString()}점</span>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 28, alignItems: 'flex-start' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 28, flex: '1 1 260px' }}>
          <Stat
            label="라인 점유"
            value={stats.laneShareRate == null ? '-' : `${stats.laneShareRate}%`}
            sample={stats.laneShareRate == null ? '정글은 라인이 없다' : '라인전(15분) 동안'}
          />
          <Stat
            label="라인 이탈"
            value={stats.roamRate == null ? '-' : `${stats.roamRate}%`}
            sample="귀환은 빼고 센다"
          />
          <Stat label="상대 진영" value={`${stats.enemyHalfRate}%`} sample="경기 전체" />
          <Stat
            label="상대 정글"
            value={`${stats.counterJungleRate}%`}
            sample="상대 진영 중 정글만"
          />
          <Stat
            label="한타"
            value={`${stats.teamfights}회`}
            sample={`킬 ${stats.teamfightKills} · 데스 ${stats.teamfightDeaths}`}
          />
        </div>

        <div style={{ flex: '0 0 auto' }}>
          <div style={{ fontSize: 12, color: 'var(--color-text-secondary)', marginBottom: 6 }}>죽은 자리</div>
          {heatmap && heatmap.cells.length > 0 ? (
            <MapScatter
              label="죽은 자리 히트맵"
              grid={heatmap.grid}
              cells={heatmap.cells.map(c => ({ x: c.x, y: c.y, count: c.count }))}
              size={260}
            />
          ) : (
            <p className="t-empty" style={{ textAlign: 'left' }}>집계 대기 중</p>
          )}
        </div>
      </div>

      <p className="t-stat-sample" style={{ display: 'block', marginTop: 10 }}>
        체류 비율은 분 단위 스냅샷이라 짧은 이동은 잡히지 않는다. 죽은 자리는 실제 좌표라 정확하다.
      </p>
    </div>
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
