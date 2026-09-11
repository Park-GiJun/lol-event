import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useSummoner } from '@/hooks/usePages';
import { ChampionIcon, ChampionLabel, PersonLink } from '@/components/ds/Champion';
import { Rate, RecentForm, Stat, WinBar } from '@/components/ds/Stat';
import { InlineError } from '@/components/common/InlineError';
import { fmt, parseRiotId } from '@/lib/lol';
import type { SummonerOpponent, SummonerTeammate } from '@/lib/types/page';
import { positionLabel } from '@/lib/position';
import { LaneForm } from './summoner/LaneForm';
import type { EloRankEntry } from '@/lib/types/stats';

type Tab = 'overview' | 'champions' | 'people';

const TABS: [Tab, string][] = [
  ['overview', '전적'],
  ['champions', '챔피언'],
  ['people', '같이 한 사람'],
];

/**
 * 개인 레이팅 두 칸.
 *
 * 실력(laneElo)과 전적(teamElo)을 **나란히 놓되 합치지 않는다**. 이 내전은 편성자가 팀을
 * 직접 짜기 때문에 팀 승패에는 실력이 아니라 편성자의 추정 오차가 남고, 두 값을 하나로
 * 섞으면 실력 레이팅이 그 오차로 오염된다. 그래서 화면에서도 끝까지 두 칸이다.
 *
 * 편차(gap = 전적 − 실력)는 그 둘이 얼마나 어긋나 있는지다. 양수가 크면 "라인 실적에 비해
 * 이기는 팀에 자주 들어갔다", 음수가 크면 그 반대다. 팀을 짤 때 참고하라고 같이 보여 준다.
 */
function RatingStats({ rating }: { rating: EloRankEntry }) {
  const gap = Math.round(rating.gap);
  const gapColor = Math.abs(gap) < 50
    ? 'var(--color-text-secondary)'
    : gap > 0 ? 'var(--color-win)' : 'var(--color-loss)';

  return (
    <>
      <Stat
        label="실력 Elo"
        value={Math.round(rating.laneEloDisplay)}
        sample={
          rating.laneDuels > 0
            ? `라인 ${rating.laneWins}승 ${rating.laneLosses}패 · ${Math.round(rating.laneWinRate * 100)}%`
            : '라인 맞대결 없음'
        }
      />
      <Stat
        label="전적 Elo"
        value={Math.round(rating.teamEloDisplay)}
        sample={
          <>
            편차{' '}
            <b style={{ color: gapColor }} title="전적 Elo − 실력 Elo. 크게 벌어져 있으면 편성자의 평가와 라인 실적이 어긋나 있다는 뜻이다.">
              {gap > 0 ? '+' : ''}{gap}
            </b>
          </>
        }
      />
    </>
  );
}

function ago(ms: number) {
  const days = Math.floor((Date.now() - ms) / 86400000);
  if (days <= 0) return '오늘';
  if (days < 30) return `${days}일 전`;
  if (days < 365) return `${Math.floor(days / 30)}개월 전`;
  return `${Math.floor(days / 365)}년 전`;
}

export function SummonerPage() {
  const { riotId = '' } = useParams();
  const [tab, setTab] = useState<Tab>('overview');
  const { data, isPending, error, refetch } = useSummoner(riotId, 'all');

  if (isPending) {
    return (
      <div className="t-page">
        <div className="t-skel" style={{ height: 160, borderRadius: 16 }} />
        <div className="t-skel" style={{ height: 400, borderRadius: 16 }} />
      </div>
    );
  }
  if (error) {
    return (
      <div className="t-page">
        <InlineError message="이 플레이어의 기록을 불러오지 못했습니다." onRetry={() => refetch()} />
      </div>
    );
  }
  if (!data) return null;

  const { profile, streak, championStats, positionStats, recentMatches, teammates, opponents } = data;
  const { name, tag } = parseRiotId(profile.riotId);

  if (profile.games === 0) {
    return (
      <div className="t-page">
        <h1 className="t-page-title">{name}</h1>
        <div className="t-card">
          <p className="t-empty">이 플레이어의 내전 기록이 없습니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="t-page">
      {/* ── 머리 ── */}
      <section className="t-card">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 28, alignItems: 'flex-end' }}>
          <div className="t-stat">
            <span className="t-stat-label">
              {profile.eloRank
                ? `Elo ${profile.eloRank}위 / ${profile.eloRankedTotal}명`
                // 배치 기준은 경기 수가 아니라 라인 맞대결 수다.
                : `배치 중 · 라인 ${profile.rating?.laneDuels ?? 0}대결`}
              {profile.rating?.mainPosition && ` · ${positionLabel(profile.rating.mainPosition)}`}
            </span>
            <h1 className="t-stat-value t-stat-hero" style={{ fontSize: 32 }}>
              {name}
              <span className="t-person-tag" style={{ fontSize: 15, marginLeft: 6 }}>#{tag}</span>
            </h1>
          </div>

          {profile.rating
            ? <RatingStats rating={profile.rating} />
            : <Stat label="실력 Elo" value={Math.round(profile.elo)} />}
          <Stat
            label="전적"
            value={`${profile.wins}승 ${profile.losses}패`}
            sample={`승률 ${profile.winRate}% · 보정 ${profile.adjustedWinRate}%`}
          />
          <Stat label="평균 KDA" value={profile.kda} sample={`${profile.avgKills} / ${profile.avgDeaths} / ${profile.avgAssists}`} />

          <div className="t-stat">
            <span className="t-stat-label">
              최근 10경기
              {streak.current !== 0 && (
                <>
                  {' · '}
                  <b className={streak.type === 'WIN' ? 't-delta-up' : 't-delta-down'}>
                    {Math.abs(streak.current)}{streak.type === 'WIN' ? '연승' : '연패'}
                  </b>
                </>
              )}
            </span>
            <span style={{ paddingTop: 4 }}><RecentForm form={streak.recentForm} /></span>
          </div>
        </div>
      </section>

      <div className="t-tabs">
        {TABS.map(([key, label]) => (
          <button
            key={key}
            className={`t-tab${tab === key ? ' active' : ''}`}
            onClick={() => setTab(key)}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <>
          <LaneForm riotId={riotId} />
          <section className="t-card">
            <div className="t-card-head"><h2 className="t-card-title">포지션</h2></div>
            {positionStats.length === 0 ? (
              <p className="t-empty">포지션이 배정된 경기가 없습니다.</p>
            ) : (
              <div className="t-tablewrap">
                <table className="t-table">
                  <thead>
                    <tr>
                      <th>포지션</th>
                      <th style={{ width: 90 }}>승률</th>
                      <th>전적</th>
                      <th className="t-num">KDA</th>
                      <th className="t-num">평균 CS</th>
                      <th className="t-num">평균 딜</th>
                    </tr>
                  </thead>
                  <tbody>
                    {positionStats.map((p) => (
                      <tr key={p.position}>
                        <td><b>{positionLabel(p.position)}</b></td>
                        <td><WinBar winRate={p.winRate} /></td>
                        <td><Rate value={p.winRate} games={p.games} /></td>
                        <td className="t-num">{p.kda}</td>
                        <td className="t-num">{p.avgCs}</td>
                        <td className="t-num">{p.avgDamage.toLocaleString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className="t-card">
            <div className="t-card-head">
              <h2 className="t-card-title">최근 경기</h2>
              <span className="t-card-more">{recentMatches.length}경기</span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {recentMatches.map((m) => (
                <Link
                  key={m.matchId}
                  to={`/matches/${encodeURIComponent(m.matchId)}`}
                  className={`t-result ${m.win ? 't-result-win' : 't-result-loss'}`}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 12,
                    padding: '10px 12px', borderRadius: 10, background: 'var(--gray-50)',
                  }}
                >
                  <ChampionIcon championId={m.championId} champion={m.champion} />
                  <span style={{ width: 44, fontWeight: 700, color: m.win ? 'var(--color-win)' : 'var(--color-loss)' }}>
                    {m.win ? '승' : '패'}
                  </span>
                  <b style={{ fontVariantNumeric: 'tabular-nums' }}>
                    {m.kills} / {m.deaths} / {m.assists}
                  </b>
                  <span className="t-stat-sample">CS {m.cs}</span>
                  <span className="t-stat-sample">{fmt(m.gameDuration)}</span>
                  <span className="t-stat-sample" style={{ marginLeft: 'auto' }}>{ago(m.gameCreation)}</span>
                </Link>
              ))}
            </div>
          </section>
        </>
      )}

      {tab === 'champions' && (
        <section className="t-card">
          <div className="t-card-head">
            <h2 className="t-card-title">챔피언별 전적</h2>
            <span className="t-card-more">{championStats.length}종</span>
          </div>
          <div className="t-tablewrap">
            <table className="t-table">
              <thead>
                <tr>
                  <th>챔피언</th>
                  <th style={{ width: 90 }}>승률</th>
                  <th>전적</th>
                  <th className="t-num">KDA</th>
                  <th className="t-num">평균 CS</th>
                  <th className="t-num">평균 딜</th>
                </tr>
              </thead>
              <tbody>
                {championStats.map((c) => (
                  <tr key={c.champion}>
                    <td>
                      <ChampionLabel championId={c.championId} champion={c.champion} />
                    </td>
                    <td><WinBar winRate={c.winRate} /></td>
                    <td><Rate value={c.winRate} games={c.games} /></td>
                    <td className="t-num">{c.kda}</td>
                    <td className="t-num">{c.avgCs}</td>
                    <td className="t-num">{c.avgDamage.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === 'people' && (
        <div className="t-grid">
          <RelationTable
            title="함께 이긴 사람"
            hint="같은 팀이었을 때의 전적"
            rows={teammates}
            emptyText="같은 팀으로 뛴 기록이 없습니다."
          />
          <RelationTable
            title="맞붙은 사람"
            hint="상대 팀이었을 때 내 전적"
            rows={opponents}
            emptyText="상대로 만난 기록이 없습니다."
          />
        </div>
      )}
    </div>
  );
}

/**
 * 한정된 인원이 반복해서 붙는 내전이라, 이 표가 사실상 이 화면의 본론이다.
 * op.gg 에는 없는 축이고 여기에서만 의미가 있다.
 */
function RelationTable({
  title,
  hint,
  rows,
  emptyText,
}: {
  title: string;
  hint: string;
  rows: (SummonerTeammate | SummonerOpponent)[];
  emptyText: string;
}) {
  return (
    <section className="t-card">
      <div className="t-card-head">
        <h2 className="t-card-title">{title}</h2>
        <span className="t-card-more">{hint}</span>
      </div>
      {rows.length === 0 ? (
        <p className="t-empty">{emptyText}</p>
      ) : (
        <div className="t-tablewrap">
          <table className="t-table">
            <thead>
              <tr>
                <th>플레이어</th>
                <th style={{ width: 80 }}>승률</th>
                <th>전적</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.riotId}>
                  <td><PersonLink riotId={r.riotId} /></td>
                  <td><WinBar winRate={r.winRate} /></td>
                  <td>
                    <Rate
                      value={r.winRate}
                      games={r.games}
                      grade={r.sampleGrade}
                      adjusted={r.adjustedWinRate}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
