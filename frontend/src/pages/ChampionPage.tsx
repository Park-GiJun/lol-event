import { useParams } from 'react-router-dom';
import { useChampionPage } from '@/hooks/usePages';
import { useDragon } from '@/context/DragonContext';
import { ChampionIcon, PersonLink } from '@/components/ds/Champion';
import { Rate, Stat, TierBadge, WinBar } from '@/components/ds/Stat';
import { InlineError } from '@/components/common/InlineError';
import { positionLabel } from '@/lib/position';
import { LaneStrengthSection } from './champion/LaneStrength';

/** 표 안에서 쓰는 짧은 격차 표기. 양수는 파랑, 음수는 빨강. */
function Diff({ v, digits = 0 }: { v: number; digits?: number }) {
  const color = v > 0 ? 'var(--color-win)' : v < 0 ? 'var(--color-loss)' : 'var(--gray-500)';
  return <span style={{ color, fontWeight: 600 }}>{v > 0 ? '+' : ''}{v.toFixed(digits)}</span>;
}

export function ChampionPage() {
  const { champion = '' } = useParams();
  const { data, isPending, error, refetch } = useChampionPage(champion, 'all');
  const { champions: dragon, items: itemDict, runes: runeDict } = useDragon();

  if (isPending) {
    return (
      <div className="t-page">
        <div className="t-skel" style={{ height: 140, borderRadius: 16 }} />
        <div className="t-skel" style={{ height: 360, borderRadius: 16 }} />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="t-page">
        <InlineError message="이 챔피언의 통계를 불러오지 못했습니다." onRetry={() => refetch()} />
      </div>
    );
  }

  const { detail, tier, laneStrength, matchups, matchupMinGames } = data;
  const nameKo = dragon.get(detail.championId)?.nameKo ?? detail.champion;

  if (detail.totalGames === 0) {
    return (
      <div className="t-page">
        <h1 className="t-page-title">{nameKo}</h1>
        <div className="t-card"><p className="t-empty">이 챔피언이 등장한 내전이 없습니다.</p></div>
      </div>
    );
  }

  return (
    <div className="t-page">
      <section className="t-card">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 28, alignItems: 'flex-end' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <ChampionIcon championId={detail.championId} champion={detail.champion} size="lg" />
            <div className="t-stat">
              <span className="t-stat-label">
                {tier && tier.tier !== '?'
                  ? `${tier.tier} 티어`
                  : `표본 부족 · 티어 없음`}
              </span>
              <h1 className="t-stat-value t-stat-hero" style={{ fontSize: 32 }}>{nameKo}</h1>
            </div>
          </div>

          <Stat
            label="전적"
            value={`${detail.totalWins}승 ${detail.totalGames - detail.totalWins}패`}
            sample={
              tier
                ? `승률 ${detail.winRate}% · 보정 ${tier.adjustedWinRate}%`
                : `승률 ${detail.winRate}%`
            }
          />
          <Stat label="등장" value={`${detail.totalGames}경기`} />
          {tier && (
            <div className="t-stat">
              <span className="t-stat-label">티어</span>
              <span style={{ paddingTop: 4 }}><TierBadge tier={tier.tier} /></span>
            </div>
          )}
        </div>
      </section>

      {/* 누가 이 챔피언을 하는가 — 인원이 한정돼 있어 이게 사실상 "장인" 목록이다. */}
      <section className="t-card">
        <div className="t-card-head">
          <h2 className="t-card-title">이 챔피언을 하는 사람</h2>
          <span className="t-card-more">{detail.players.length}명</span>
        </div>
        <div className="t-tablewrap">
          <table className="t-table">
            <thead>
              <tr>
                <th>플레이어</th>
                <th style={{ width: 90 }}>승률</th>
                <th>전적</th>
                <th className="t-num">KDA</th>
                <th className="t-num">평균 CS</th>
                <th className="t-num">평균 딜</th>
              </tr>
            </thead>
            <tbody>
              {detail.players.map((p) => (
                <tr key={p.riotId}>
                  <td><PersonLink riotId={p.riotId} /></td>
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
      </section>

      <div className="t-grid">
        {detail.laneStats.length > 0 && (
          <section className="t-card">
            <div className="t-card-head"><h2 className="t-card-title">포지션</h2></div>
            <div className="t-tablewrap">
              <table className="t-table">
                <thead>
                  <tr><th>포지션</th><th>전적</th><th className="t-num">KDA</th></tr>
                </thead>
                <tbody>
                  {detail.laneStats.map((l) => (
                    <tr key={l.position}>
                      <td><b>{positionLabel(l.position)}</b></td>
                      <td><Rate value={l.winRate} games={l.games} /></td>
                      <td className="t-num">{l.kda}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}

        {detail.itemStats.length > 0 && (
          <section className="t-card">
            <div className="t-card-head">
              <h2 className="t-card-title">자주 올린 아이템</h2>
            </div>
            <div className="t-tablewrap">
              <table className="t-table">
                <thead>
                  <tr><th>아이템</th><th>채택</th><th>승률</th></tr>
                </thead>
                <tbody>
                  {detail.itemStats.slice(0, 10).map((it) => (
                    <tr key={it.itemId}>
                      <td>
                        <span className="t-person">
                          {itemDict.get(it.itemId)?.imageUrl
                            ? <img className="t-item" src={itemDict.get(it.itemId)!.imageUrl!} alt="" loading="lazy" />
                            : <span className="t-item" />}
                          <span className="t-person-name">
                            {itemDict.get(it.itemId)?.nameKo ?? it.itemId}
                          </span>
                        </span>
                      </td>
                      <td className="t-stat-sample">{it.picks}회</td>
                      <td><Rate value={it.winRate} games={it.picks} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}

        {/* 룬은 경기당 조합이 하나뿐이라 채택 수가 곧 그 룬을 든 경기 수다.
            아이템처럼 한 경기에서 여러 번 세지 않는다. */}
        {detail.runeStats.length > 0 && (
          <section className="t-card">
            <div className="t-card-head">
              <h2 className="t-card-title">자주 든 룬</h2>
              <span className="t-card-more">핵심 룬 + 보조 계열</span>
            </div>
            <div className="t-tablewrap">
              <table className="t-table">
                <thead>
                  <tr><th>룬</th><th>채택</th><th>승률</th></tr>
                </thead>
                <tbody>
                  {detail.runeStats.map((r) => {
                    const keystone = runeDict.get(r.keystone);
                    const sub = runeDict.get(r.subStyle);
                    return (
                      <tr key={`${r.keystone}-${r.primaryStyle}-${r.subStyle}`}>
                        <td>
                          <span className="t-person">
                            {keystone?.imageUrl
                              ? <img className="t-item" src={keystone.imageUrl} alt="" loading="lazy" />
                              : <span className="t-item" />}
                            <span className="t-person-name">
                              {keystone?.nameKo ?? r.keystone}
                              {sub && <span style={{ color: 'var(--gray-500)' }}> · {sub.nameKo}</span>}
                            </span>
                          </span>
                        </td>
                        <td className="t-stat-sample">{r.picks}회</td>
                        <td><Rate value={r.winRate} games={r.picks} /></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        )}
      </div>

      {/* 라인전 지표를 상성표보다 먼저 둔다. 개별 상성은 표본이 잘 안 모이는데
          라인전 지표는 챔피언 x 라인 단위라 대부분의 챔피언에서 읽을 값이 나온다. */}
      <section className="t-card">
        <div className="t-card-head">
          <h2 className="t-card-title">라인전 지표</h2>
          <span className="t-card-more">같은 라인 상대와 비교</span>
        </div>
        <LaneStrengthSection laneStrength={laneStrength} />
      </section>

      <div className="t-grid">
        <section className="t-card">
          <div className="t-card-head">
            <h2 className="t-card-title">같은 라인에서 만난 챔피언</h2>
            <span className="t-card-more">{matchupMinGames}경기 이상만</span>
          </div>
          {matchups.length === 0 ? (
            <p className="t-empty">
              {matchupMinGames}경기 이상 맞붙은 상대가 아직 없습니다.
              <br />
              <span className="t-detail-sub">
                개별 상성은 조합이 574가지로 흩어져 표본이 잘 안 모입니다. 위의 라인전 지표를 보세요.
              </span>
            </p>
          ) : (
            <div className="t-tablewrap">
              <table className="t-table">
                <thead>
                  <tr>
                    <th>챔피언</th>
                    <th>전적</th>
                    <th className="t-num">골드</th>
                    <th className="t-num">CS</th>
                    <th className="t-num">딜량</th>
                  </tr>
                </thead>
                <tbody>
                  {matchups.slice(0, 15).map((m) => (
                    <tr key={m.opponent}>
                      <td>
                        <span className="t-person">
                          <ChampionIcon championId={m.opponentId} champion={m.opponent} size="sm" />
                          <span className="t-person-name">
                            {dragon.get(m.opponentId)?.nameKo ?? m.opponent}
                          </span>
                        </span>
                      </td>
                      <td><Rate value={m.winRate} games={m.games} grade={m.sampleGrade} adjusted={m.adjustedWinRate} /></td>
                      <td className="t-num"><Diff v={m.gap.goldDiff} /></td>
                      <td className="t-num"><Diff v={m.gap.csDiff} digits={1} /></td>
                      <td className="t-num"><Diff v={m.gap.damageDiff} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

      </div>
    </div>
  );
}
