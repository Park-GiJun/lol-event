import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useChampions } from '@/hooks/useChampions';
import { useDragon } from '@/context/DragonContext';
import { Skeleton } from '@/components/common/Skeleton';
import { InlineError } from '@/components/common/InlineError';
import { TIER_COLORS } from '@/lib/constants/theme';
import type { TierKey } from '@/lib/constants/theme';
import type { ChampionPickStat } from '@/lib/types/stats';

const TIER_ORDER: TierKey[] = ['S', 'A', 'B', 'C'];

function getChampionTier(winRate: number): TierKey {
  if (winRate >= 60) return 'S';
  if (winRate >= 55) return 'A';
  if (winRate >= 50) return 'B';
  return 'C';
}

interface ChampionRowProps {
  entry: ChampionPickStat;
  displayName: string;
  imgUrl: string | null;
  championKey: string;
  onNavigate: () => void;
}

function ChampionRow({ entry, displayName, imgUrl, onNavigate }: ChampionRowProps) {
  const [imgError, setImgError] = useState(false);
  const wrColor = entry.winRate >= 50 ? '#10B981' : '#EF4444';
  const wrLabel = entry.winRate >= 50 ? '승' : '패';
  const showFallback = !imgUrl || imgError;

  return (
    <div
      role="button"
      tabIndex={0}
      className="m-player-card"
      style={{ minHeight: 44 }}
      onClick={onNavigate}
      onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') onNavigate(); }}
    >
      <div className="m-player-card-header">
        {showFallback ? (
          <div style={{
            width: 32, height: 32, borderRadius: 4,
            background: 'var(--color-bg-hover)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 10, color: 'var(--color-text-secondary)', flexShrink: 0,
          }}>
            {displayName.slice(0, 2)}
          </div>
        ) : (
          <img
            src={imgUrl!}
            alt={displayName}
            width={32}
            height={32}
            style={{ borderRadius: 4, border: '1px solid var(--color-border)', objectFit: 'cover', flexShrink: 0 }}
            onError={() => setImgError(true)}
          />
        )}
        <span className="m-player-name" style={{ flex: 1, marginLeft: 10 }}>
          {displayName}
        </span>
        <div style={{ textAlign: 'right' }}>
          <span style={{ fontSize: 14, fontWeight: 700, color: wrColor }}>
            {entry.winRate.toFixed(1)}%
          </span>
          <span style={{ fontSize: 10, color: wrColor, marginLeft: 4 }}>
            {wrLabel}
          </span>
        </div>
      </div>
    </div>
  );
}

export function MobileChampionListPage() {
  const navigate = useNavigate();
  const { data, isLoading, error, refetch } = useChampions();
  const { champions: dragonChampions } = useDragon();
  const [expanded, setExpanded] = useState<Record<TierKey, boolean>>(
    { S: true, A: true, B: true, C: true }
  );

  const tierGroups = useMemo(() => {
    if (!data) return [];
    return TIER_ORDER
      .map(tier => ({
        tier,
        champions: data.topPickedChampions.filter(
          (c: ChampionPickStat) => getChampionTier(c.winRate) === tier
        ),
      }))
      .filter(g => g.champions.length > 0);
  }, [data]);

  if (isLoading) {
    return (
      <div>
        {['S', 'A'].map(tier => (
          <div key={tier} style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 4px', marginBottom: 8, minHeight: 44 }}>
              <Skeleton style={{ width: 28, height: 22, borderRadius: 4 }} />
              <Skeleton style={{ width: 60, height: 16, borderRadius: 4 }} />
            </div>
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="m-player-card" style={{ minHeight: 44 }}>
                <div className="m-player-card-header">
                  <Skeleton style={{ width: 32, height: 32, borderRadius: 4, flexShrink: 0 }} />
                  <Skeleton style={{ flex: 1, height: 14, borderRadius: 4, marginLeft: 10 }} />
                  <Skeleton style={{ width: 40, height: 14, borderRadius: 4 }} />
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return <InlineError message="챔피언 티어표를 불러오지 못했습니다." onRetry={() => void refetch()} />;
  }

  if (!data || data.topPickedChampions.length === 0) {
    return <div className="m-empty">챔피언 데이터가 없습니다</div>;
  }

  return (
    <div>
      {tierGroups.map(({ tier, champions }) => {
        const tierColor = TIER_COLORS[tier];
        const isExpanded = expanded[tier];

        return (
          <div key={tier} style={{ marginBottom: 16 }}>
            <button
              type="button"
              style={{
                display: 'flex', alignItems: 'center', gap: 8,
                width: '100%', textAlign: 'left',
                padding: '10px 4px', background: 'none', border: 'none', cursor: 'pointer',
                minHeight: 44,
              }}
              onClick={() => setExpanded(prev => ({ ...prev, [tier]: !prev[tier] }))}
            >
              <span style={{
                fontSize: 11, fontWeight: 700, color: tierColor,
                background: tierColor + '18', borderRadius: 4, padding: '2px 8px',
                border: `1px solid ${tierColor}66`,
              }}>
                {tier}
              </span>
              <span style={{ fontSize: 14, fontWeight: 600, color: tierColor }}>
                {tier} 티어
              </span>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginLeft: 4 }}>
                ({champions.length})
              </span>
              <span style={{ marginLeft: 'auto', color: 'var(--color-text-secondary)', fontSize: 12 }}>
                {isExpanded ? '▲' : '▼'}
              </span>
            </button>

            {isExpanded && champions.map((entry: ChampionPickStat) => {
              const dragon = dragonChampions.get(entry.championId);
              const displayName = dragon?.nameKo ?? entry.champion;
              const imgUrl = dragon?.imageUrl ?? null;
              const championKey = dragon?.championKey || entry.champion || String(entry.championId);

              return (
                <ChampionRow
                  key={entry.championId}
                  entry={entry}
                  displayName={displayName}
                  imgUrl={imgUrl}
                  championKey={championKey}
                  onNavigate={() => navigate(`/m/champion/${encodeURIComponent(championKey)}`)}
                />
              );
            })}
          </div>
        );
      })}
    </div>
  );
}
