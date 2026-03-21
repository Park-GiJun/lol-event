import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDragon } from '../../context/DragonContext';

export function MobileChampionListPage() {
  const { champions } = useDragon();
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  const list = Array.from(champions.values()).filter(c =>
    !search || c.nameKo.includes(search) || c.championKey.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      <input
        className="m-search"
        placeholder="챔피언 검색..."
        value={search}
        onChange={e => setSearch(e.target.value)}
      />

      {list.length === 0 && (
        <div className="m-empty">검색 결과가 없습니다</div>
      )}

      <div className="m-champ-grid">
        {list.map(c => (
          <div
            key={c.championId}
            className="m-champ-grid-item"
            onClick={() => navigate(`/m/champion/${encodeURIComponent(c.championKey)}`)}
          >
            {c.imageUrl ? (
              <img
                src={c.imageUrl}
                alt={c.nameKo}
                className="m-champ-grid-img"
                onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
              />
            ) : (
              <div className="m-champ-grid-img-placeholder">
                {c.nameKo.slice(0, 2)}
              </div>
            )}
            <span className="m-champ-grid-name">{c.nameKo}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
