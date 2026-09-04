import { Link } from 'react-router-dom';
import { useHome } from '@/hooks/usePages';
import { ChampionIcon, PersonLink } from '@/components/ds/Champion';
import { Rate, Stat, TierBadge } from '@/components/ds/Stat';
import { InlineError } from '@/components/common/InlineError';
import { fmt, parseRiotId } from '@/lib/lol';

function fmtDate(ms: number | null) {
  if (!ms) return '–';
  const d = new Date(ms);
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
}

function HomeSkeleton() {
  return (
    <div className="t-page">
      <div className="t-skel" style={{ height: 132, borderRadius: 16 }} />
      <div className="t-grid">
        <div className="t-skel" style={{ height: 320, borderRadius: 16 }} />
        <div className="t-skel" style={{ height: 320, borderRadius: 16 }} />
      </div>
    </div>
  );
}

export function HomePage() {
  const { data, isPending, error, refetch } = useHome('all');

  if (isPending) return <HomeSkeleton />;
  if (error) {
    return (
      <div className="t-page">
        <InlineError message="기록을 불러오지 못했습니다." onRetry={() => refetch()} />
      </div>
    );
  }
  if (!data) return null;

  const { overview, topPlayers, topChampions, recentMatches, period } = data;

  return (
    <div className="t-page">
      {/* 이 내전이 지금까지 뭘 쌓았는지 한 줄로 말한다.
          큰 숫자는 경기 수 하나뿐이고, 나머지는 같은 크기로 눕힌다. */}
      <section className="t-card">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 32, alignItems: 'flex-end' }}>
          <Stat
            label="지금까지 치른 내전"
            value={`${period.totalMatches}경기`}
            sample={`${fmtDate(period.firstMatchAt)} – ${fmtDate(period.lastMatchAt)}`}
            hero
          />
          <Stat label="함께한 사람" value={`${period.playerCount}명`} />
          <Stat label="평균 경기 시간" value={`${overview.avgGameMinutes}분`} />
        </div>
      </section>

      <div className="t-grid">
        {/* ── Elo 상위 ── */}
        <section className="t-card">
          <div className="t-card-head">
            <h2 className="t-card-title">Elo 순위</h2>
            <Link to="/rankings" className="t-card-more">전체 보기</Link>
          </div>

          {topPlayers.length === 0 ? (
            <p className="t-empty">아직 순위를 매길 만큼 경기가 쌓이지 않았습니다.</p>
          ) : (
            <div className="t-tablewrap">
              <table className="t-table">
                <thead>
                  <tr>
                    <th className="t-rank">#</th>
                    <th>플레이어</th>
                    <th className="t-num">Elo</th>
                    <th>전적</th>
                  </tr>
                </thead>
                <tbody>
                  {topPlayers.map((p) => (
                    <tr key={p.riotId}>
                      <td className="t-rank">{p.rank}</td>
                      <td><PersonLink riotId={p.riotId} /></td>
                      <td className="t-num"><b>{Math.round(p.elo)}</b></td>
                      <td>
                        <Rate value={Math.round(p.winRate)} games={p.games} grade={p.sampleGrade} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* ── 챔피언 티어 ── */}
        <section className="t-card">
          <div className="t-card-head">
            <h2 className="t-card-title">잘 나가는 챔피언</h2>
            <Link to="/champions" className="t-card-more">전체 보기</Link>
          </div>

          {topChampions.length === 0 ? (
            <p className="t-empty">표본을 채운 챔피언이 아직 없습니다.</p>
          ) : (
            <div className="t-tablewrap">
              <table className="t-table">
                <thead>
                  <tr>
                    <th style={{ width: 32 }}>티어</th>
                    <th>챔피언</th>
                    <th>승률</th>
                    <th className="t-num">KDA</th>
                  </tr>
                </thead>
                <tbody>
                  {topChampions.map((c) => (
                    <tr key={c.champion}>
                      <td><TierBadge tier={c.tier} /></td>
                      <td>
                        <Link to={`/champions/${encodeURIComponent(c.champion)}`} className="t-person">
                          <ChampionIcon championId={c.championId} champion={c.champion} size="sm" />
                          <span className="t-person-name">{c.champion}</span>
                        </Link>
                      </td>
                      <td>
                        <Rate
                          value={c.winRate}
                          games={c.games}
                          grade={c.sampleGrade}
                          adjusted={c.adjustedWinRate}
                        />
                      </td>
                      <td className="t-num">{c.kda}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      {/* ── 최근 경기 ── */}
      <section className="t-card">
        <div className="t-card-head">
          <h2 className="t-card-title">최근 경기</h2>
          <Link to="/matches" className="t-card-more">전체 보기</Link>
        </div>

        {recentMatches.length === 0 ? (
          <p className="t-empty">아직 기록된 경기가 없습니다.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {recentMatches.map((m) => {
              const blue = m.participants.filter((p) => p.team === 'blue');
              const red = m.participants.filter((p) => p.team === 'red');
              const blueWon = m.teams.find((t) => t.teamId === 100)?.win ?? false;
              return (
                <Link
                  key={m.matchId}
                  to={`/matches/${encodeURIComponent(m.matchId)}`}
                  className={`t-result ${blueWon ? 't-result-win' : 't-result-loss'}`}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 12,
                    padding: '10px 12px', borderRadius: 10, background: 'var(--gray-50)',
                  }}
                >
                  <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--gray-700)', width: 74 }}>
                    {blueWon ? '블루 승' : '레드 승'}
                  </span>
                  <span style={{ display: 'flex', gap: 2 }}>
                    {blue.map((p) => (
                      <ChampionIcon key={p.riotId} championId={p.championId} champion={p.champion} size="sm" />
                    ))}
                  </span>
                  <span className="t-stat-sample">vs</span>
                  <span style={{ display: 'flex', gap: 2 }}>
                    {red.map((p) => (
                      <ChampionIcon key={p.riotId} championId={p.championId} champion={p.champion} size="sm" />
                    ))}
                  </span>
                  <span className="t-stat-sample" style={{ marginLeft: 'auto' }}>
                    {fmt(m.gameDuration)}
                  </span>
                </Link>
              );
            })}
          </div>
        )}
      </section>

      {/* ── 부문별 1위 ── */}
      <section className="t-card">
        <div className="t-card-head">
          <h2 className="t-card-title">부문별 1위</h2>
        </div>
        <div style={{ display: 'grid', gap: 16, gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))' }}>
          {([
            ['최다 출전', overview.mostGamesPlayed],
            ['승률', overview.winRateLeader],
            ['KDA', overview.kdaLeader],
            ['딜량', overview.damageLeader],
            ['CS', overview.csLeader],
            ['시야 점수', overview.visionLeader],
          ] as const).map(([label, leader]) =>
            leader ? (
              <div key={label} className="t-stat">
                <span className="t-stat-label">{label}</span>
                <span className="t-stat-value" style={{ fontSize: 18 }}>{leader.displayValue}</span>
                <span className="t-stat-sample">
                  {parseRiotId(leader.riotId).name} · {leader.games}경기
                </span>
              </div>
            ) : null,
          )}
        </div>
      </section>
    </div>
  );
}
