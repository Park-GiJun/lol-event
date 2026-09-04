import { Link } from 'react-router-dom';
import { useDragon } from '@/context/DragonContext';
import { useChampionName } from '@/hooks/useChampionName';
import { parseRiotId } from '@/lib/lol';

/** 챔피언 아이콘. DataDragon 이 아직 안 왔거나 이미지가 없으면 회색 사각형으로 자리를 지킨다. */
export function ChampionIcon({
  championId,
  champion,
  size = 'md',
}: {
  championId: number;
  champion?: string;
  size?: 'sm' | 'md' | 'lg';
}) {
  const { champions } = useDragon();
  const meta = champions.get(championId);
  const cls = `t-champ${size === 'sm' ? ' t-champ-sm' : size === 'lg' ? ' t-champ-lg' : ''}`;
  const name = meta?.nameKo ?? champion ?? '';

  if (!meta?.imageUrl) return <span className={cls} title={name} />;
  return <img className={cls} src={meta.imageUrl} alt={name} title={name} loading="lazy" />;
}

export function ChampionLabel({ championId, champion }: { championId: number; champion: string }) {
  const name = useChampionName(championId, champion);
  return (
    <Link to={`/champions/${encodeURIComponent(champion)}`} className="t-person">
      <ChampionIcon championId={championId} champion={champion} size="sm" />
      <span className="t-person-name">{name}</span>
    </Link>
  );
}

export function ItemIcons({ items }: { items: number[] }) {
  const { items: dict } = useDragon();
  return (
    <span className="t-items">
      {items.map((id, i) => {
        const meta = id ? dict.get(id) : undefined;
        return meta?.imageUrl
          ? <img key={i} className="t-item" src={meta.imageUrl} alt={meta.nameKo} title={meta.nameKo} loading="lazy" />
          : <span key={i} className="t-item" />;
      })}
    </span>
  );
}

/**
 * 사람 이름. 이 사이트에서는 어디에 나오든 눌러서 그 사람 화면으로 갈 수 있어야 한다.
 * 20명 남짓이 서로 얽혀 있어서 이동이 곧 탐색이다.
 */
export function PersonLink({ riotId, bold = true }: { riotId: string; bold?: boolean }) {
  const { name, tag } = parseRiotId(riotId);
  return (
    <Link to={`/players/${encodeURIComponent(riotId)}`} className="t-person">
      <span className="t-person-name" style={bold ? undefined : { fontWeight: 500 }}>{name}</span>
      {tag && <span className="t-person-tag">#{tag}</span>}
    </Link>
  );
}
