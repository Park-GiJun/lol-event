import { useMemo, useState } from 'react';
import { useLeaderboard } from '@/hooks/useLeaderboard';
import { PersonLink } from '@/components/ds/Champion';
import { Rate, WinBar } from '@/components/ds/Stat';
import { InlineError } from '@/components/common/InlineError';

export function PlayersPage() {
  const { data, isPending, error, refetch } = useLeaderboard();
  const [q, setQ] = useState('');

  const filtered = useMemo(() => {
    const players = data?.players ?? [];
    const needle = q.trim().toLowerCase();
    if (!needle) return players;
    return players.filter((p) => p.riotId.toLowerCase().includes(needle));
  }, [data, q]);

  if (isPending) {
    return (
      <div className="t-page">
        <h1 className="t-page-title">플레이어</h1>
        <div className="t-skel" style={{ height: 480, borderRadius: 16 }} />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="t-page">
        <InlineError message="플레이어 목록을 불러오지 못했습니다." onRetry={() => refetch()} />
      </div>
    );
  }

  const ranked = filtered.filter((p) => !p.placement);
  const placement = filtered.filter((p) => p.placement);

  return (
    <div className="t-page">
      <div className="t-page-head">
        <h1 className="t-page-title">플레이어</h1>
        <p className="t-page-sub">
          순위 {data.rankedCount}명 · 배치 중 {data.placementCount}명
          {' · '}순위에 들려면 {data.minGames}경기 이상
        </p>
      </div>

      <div className="t-search" style={{ maxWidth: 340 }}>
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="닉네임으로 찾기"
          aria-label="플레이어 검색"
          style={{ paddingLeft: 12 }}
        />
      </div>

      <section className="t-card">
        {ranked.length === 0 ? (
          <p className="t-empty">
            {q ? '찾는 플레이어가 없습니다.' : '아직 순위를 매길 만큼 경기가 쌓이지 않았습니다.'}
          </p>
        ) : (
          <div className="t-tablewrap">
            <table className="t-table">
              <thead>
                <tr>
                  <th className="t-rank">#</th>
                  <th>플레이어</th>
                  <th className="t-num">Elo</th>
                  <th style={{ width: 90 }}>승률</th>
                  <th>전적</th>
                  <th>연속</th>
                </tr>
              </thead>
              <tbody>
                {ranked.map((p) => (
                  <tr key={p.riotId}>
                    <td className="t-rank">{p.rank}</td>
                    <td><PersonLink riotId={p.riotId} /></td>
                    <td className="t-num"><b>{Math.round(p.elo)}</b></td>
                    <td><WinBar winRate={p.winRate} /></td>
                    <td>
                      <Rate value={Math.round(p.winRate)} games={p.games} grade={p.sampleGrade} />
                    </td>
                    <td>
                      {p.winStreak > 1 && <span className="t-chip t-chip-win">{p.winStreak}연승</span>}
                      {p.lossStreak > 1 && <span className="t-chip t-chip-loss">{p.lossStreak}연패</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* 배치 중인 사람도 목록에서 지우지 않는다. 순위만 매기지 않을 뿐이다. */}
      {placement.length > 0 && (
        <section className="t-card">
          <div className="t-card-head">
            <h2 className="t-card-title">배치 중</h2>
            <span className="t-card-more">{data.minGames}경기를 채우면 순위에 들어갑니다</span>
          </div>
          <div className="t-tablewrap">
            <table className="t-table">
              <thead>
                <tr>
                  <th>플레이어</th>
                  <th className="t-num">Elo</th>
                  <th>전적</th>
                </tr>
              </thead>
              <tbody>
                {placement.map((p) => (
                  <tr key={p.riotId}>
                    <td><PersonLink riotId={p.riotId} /></td>
                    <td className="t-num">{Math.round(p.elo)}</td>
                    <td>
                      <Rate value={Math.round(p.winRate)} games={p.games} grade={p.sampleGrade} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
