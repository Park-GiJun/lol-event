import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useChampionTier } from '@/hooks/useChampions';
import { useDragon } from '@/context/DragonContext';
import { ChampionIcon } from '@/components/ds/Champion';
import { Rate, TierBadge, WinBar } from '@/components/ds/Stat';
import { InlineError } from '@/components/common/InlineError';

const MIN_GAMES = 5;

export function ChampionListPage() {
  const { data, isPending, error, refetch } = useChampionTier('all', MIN_GAMES);
  const { champions: dragon } = useDragon();
  const [q, setQ] = useState('');
  const [showAll, setShowAll] = useState(false);

  const rows = useMemo(() => {
    const list = data?.tierList ?? [];
    const needle = q.trim().toLowerCase();
    return list.filter((c) => {
      if (!showAll && c.games < MIN_GAMES) return false;
      if (!needle) return true;
      const ko = dragon.get(c.championId)?.nameKo ?? '';
      return c.champion.toLowerCase().includes(needle) || ko.includes(needle);
    });
  }, [data, q, showAll, dragon]);

  if (isPending) {
    return (
      <div className="t-page">
        <h1 className="t-page-title">챔피언</h1>
        <div className="t-skel" style={{ height: 520, borderRadius: 16 }} />
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="t-page">
        <InlineError message="챔피언 통계를 불러오지 못했습니다." onRetry={() => refetch()} />
      </div>
    );
  }

  const qualified = data.tierList.filter((c) => c.games >= MIN_GAMES).length;

  return (
    <div className="t-page">
      <div className="t-page-head">
        <h1 className="t-page-title">챔피언</h1>
        <p className="t-page-sub">
          {data.totalMatches}경기에서 {data.tierList.length}종 등장 · 표본을 채운 {qualified}종에만 티어를 매깁니다
        </p>
      </div>

      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
        <div className="t-search" style={{ maxWidth: 260 }}>
          <input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="챔피언 이름"
            aria-label="챔피언 검색"
            style={{ paddingLeft: 12 }}
          />
        </div>
        <div className="t-seg">
          <button className={showAll ? '' : 'active'} onClick={() => setShowAll(false)}>
            {MIN_GAMES}경기 이상
          </button>
          <button className={showAll ? 'active' : ''} onClick={() => setShowAll(true)}>
            전체
          </button>
        </div>
      </div>

      <section className="t-card">
        {rows.length === 0 ? (
          <p className="t-empty">조건에 맞는 챔피언이 없습니다.</p>
        ) : (
          <div className="t-tablewrap">
            <table className="t-table">
              <thead>
                <tr>
                  <th className="t-rank">#</th>
                  <th style={{ width: 32 }}>티어</th>
                  <th>챔피언</th>
                  <th style={{ width: 90 }}>승률</th>
                  <th>전적</th>
                  <th className="t-num">KDA</th>
                  <th className="t-num">평균 딜</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((c, i) => (
                  <tr key={c.champion}>
                    <td className="t-rank">{i + 1}</td>
                    <td><TierBadge tier={c.tier} /></td>
                    <td>
                      <Link to={`/champions/${encodeURIComponent(c.champion)}`} className="t-person">
                        <ChampionIcon championId={c.championId} champion={c.champion} size="sm" />
                        <span className="t-person-name">
                          {dragon.get(c.championId)?.nameKo ?? c.champion}
                        </span>
                      </Link>
                    </td>
                    <td><WinBar winRate={c.winRate} /></td>
                    <td>
                      <Rate
                        value={c.winRate}
                        games={c.games}
                        grade={c.sampleGrade}
                        adjusted={c.adjustedWinRate}
                      />
                    </td>
                    <td className="t-num">{c.kda}</td>
                    <td className="t-num">{Math.round(c.avgDamage).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
