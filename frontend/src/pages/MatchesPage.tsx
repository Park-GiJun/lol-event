import { Fragment } from 'react';
import { Link } from 'react-router-dom';
import { useMatches } from '@/hooks/useMatches';
import { ChampionIcon, PersonLink } from '@/components/ds/Champion';
import { InlineError } from '@/components/common/InlineError';
import { fmt } from '@/lib/lol';
import type { MatchSummary, ParticipantSummary } from '@/lib/types/match';
import { byPosition } from '@/lib/position';



function dayKey(ms: number) {
  const d = new Date(ms);
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
}

function TeamColumn({
  players,
  won,
  label,
}: {
  players: ParticipantSummary[];
  won: boolean;
  label: string;
}) {
  return (
    <div style={{ flex: '1 1 260px', minWidth: 0 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
        <span className={`t-chip ${won ? 't-chip-win' : 't-chip-loss'}`}>{won ? '승' : '패'}</span>
        <span className="t-stat-sample">{label}</span>
        <span className="t-stat-sample" style={{ marginLeft: 'auto' }}>
          {players.reduce((s, p) => s + p.kills, 0)} 킬
        </span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {[...players].sort(byPosition).map((p) => (
          <div key={p.riotId} style={{ display: 'flex', alignItems: 'center', gap: 7, minWidth: 0 }}>
            <ChampionIcon championId={p.championId} champion={p.champion} size="sm" />
            <span style={{ minWidth: 0, flex: 1, overflow: 'hidden' }}>
              <PersonLink riotId={p.riotId} bold={false} />
            </span>
            <span className="t-stat-sample" style={{ fontVariantNumeric: 'tabular-nums' }}>
              {p.kills}/{p.deaths}/{p.assists}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function MatchRow({ match }: { match: MatchSummary }) {
  const blue = match.participants.filter((p) => p.team === 'blue');
  const red = match.participants.filter((p) => p.team === 'red');
  const blueWon = match.teams.find((t) => t.teamId === 100)?.win ?? false;

  return (
    <div className="t-card t-card-sunken" style={{ padding: 14 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
        <span className="t-stat-sample">{new Date(match.gameCreation).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}</span>
        <span className="t-stat-sample">{fmt(match.gameDuration)}</span>
        <Link
          to={`/matches/${encodeURIComponent(match.matchId)}`}
          className="t-card-more"
          style={{ marginLeft: 'auto' }}
        >
          상세 보기
        </Link>
      </div>

      <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap' }}>
        <TeamColumn players={blue} won={blueWon} label="블루팀" />
        <TeamColumn players={red} won={!blueWon} label="레드팀" />
      </div>
    </div>
  );
}

export function MatchesPage() {
  const {
    data, isPending, error, refetch,
    fetchNextPage, hasNextPage, isFetchingNextPage,
  } = useMatches('all');

  if (isPending) {
    return (
      <div className="t-page">
        <h1 className="t-page-title">경기</h1>
        {[0, 1, 2].map((i) => (
          <div key={i} className="t-skel" style={{ height: 180, borderRadius: 16 }} />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="t-page">
        <InlineError message="경기 목록을 불러오지 못했습니다." onRetry={() => refetch()} />
      </div>
    );
  }

  const matches = data?.pages.flatMap((p) => p.matches) ?? [];
  const total = data?.pages[0]?.totalElements ?? 0;

  // 같은 날 치른 경기를 묶는다. 내전은 하루에 몰아서 하니 날짜가 곧 세션 구분이다.
  const groups: [string, MatchSummary[]][] = [];
  for (const m of matches) {
    const key = dayKey(m.gameCreation);
    const last = groups[groups.length - 1];
    if (last && last[0] === key) last[1].push(m);
    else groups.push([key, [m]]);
  }

  return (
    <div className="t-page">
      <div className="t-page-head">
        <h1 className="t-page-title">경기</h1>
        <p className="t-page-sub">전체 {total}경기 중 {matches.length}경기</p>
      </div>

      {matches.length === 0 ? (
        <div className="t-card"><p className="t-empty">기록된 경기가 없습니다.</p></div>
      ) : (
        groups.map(([day, list]) => (
          <Fragment key={day}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginTop: 4 }}>
              <h2 className="t-card-title">{day}</h2>
              <span className="t-stat-sample">{list.length}경기</span>
            </div>
            {list.map((m) => <MatchRow key={m.matchId} match={m} />)}
          </Fragment>
        ))
      )}

      {hasNextPage && (
        <button
          className="t-tab"
          onClick={() => fetchNextPage()}
          disabled={isFetchingNextPage}
          style={{
            alignSelf: 'center', padding: '12px 28px', borderRadius: 10,
            background: 'var(--gray-0)', color: 'var(--gray-700)',
          }}
        >
          {isFetchingNextPage ? '불러오는 중…' : '더 보기'}
        </button>
      )}
    </div>
  );
}
