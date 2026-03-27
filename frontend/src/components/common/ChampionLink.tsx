import { useState, useRef, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { ChampionDetailStats } from '../../lib/types/stats';
import { useDragon } from '../../context/DragonContext';

// 모듈 레벨 캐시
const cache = new Map<string, ChampionDetailStats>();

function ChampionPopupContent({ champion, data }: { champion: string; data: ChampionDetailStats }) {
  const { champions, items } = useDragon();
  const navigate = useNavigate();
  const img    = champions.get(data.championId)?.imageUrl;
  const nameKo = champions.get(data.championId)?.nameKo || champion;
  const wr     = data.winRate;
  const wrColor = wr >= 60 ? 'var(--color-win)' : wr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';

  return (
    <div className="popup-champ" onClick={() => navigate(`/stats/champion/${encodeURIComponent(champion)}`)}>
      {/* 헤더 */}
      <div className="popup-champ-header">
        {img && (
          <img src={img} alt={nameKo} className="popup-champ-hero-img"
            onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />
        )}
        <div className="popup-champ-header-info">
          <div className="popup-champ-name">{nameKo}</div>
          <div className="popup-champ-meta">
            {data.totalGames}판 ·{' '}
            <span style={{ color: wrColor }}>{wr}%</span>
            {' '}승률
          </div>
        </div>
      </div>

      {/* 장인 랭킹 */}
      {data.players.length > 0 && (
        <div className="popup-masters">
          <div className="popup-section-title">장인 랭킹</div>
          {data.players.slice(0, 3).map((p, i) => {
            const pWrColor = p.winRate >= 60 ? 'var(--color-win)' : p.winRate >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
            return (
              <div key={p.riotId} className="popup-master-row">
                <span className="popup-master-rank">{i + 1}</span>
                <span className="popup-master-name">{p.riotId.split('#')[0]}</span>
                <span className="popup-master-games">{p.games}판</span>
                <span className="popup-master-wr" style={{ color: pWrColor }}>{p.winRate}%</span>
              </div>
            );
          })}
        </div>
      )}

      {/* 인기 아이템 */}
      {data.itemStats && data.itemStats.length > 0 && (
        <div className="popup-items">
          <div className="popup-section-title">인기 아이템</div>
          <div className="popup-items-grid">
            {data.itemStats.slice(0, 6).map(item => {
              const itemData = items.get(item.itemId);
              const iwr      = item.winRate;
              const iwrColor = iwr >= 60 ? 'var(--color-win)' : iwr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
              return (
                <div key={item.itemId} className="popup-item-entry"
                  title={`${itemData?.nameKo ?? item.itemId} | ${item.picks}회 픽 | 승률 ${iwr}%`}>
                  {itemData?.imageUrl
                    ? <img src={itemData.imageUrl} alt={itemData.nameKo} className="popup-item-img"
                        onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />
                    : <div className="popup-item-fallback" />
                  }
                  <span className="popup-item-wr" style={{ color: iwrColor }}>{iwr}%</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <div className="popup-hint">클릭하여 장인 랭킹 보기</div>
    </div>
  );
}

interface ChampionLinkProps {
  champion: string;
  championId: number;
  children: React.ReactNode;
  className?: string;
  mode?: string;
}

export function ChampionLink({ champion, children, className, mode = 'normal' }: ChampionLinkProps) {
  const [visible, setVisible] = useState(false);
  const [pos, setPos]         = useState({ x: 0, y: 0 });
  const [data, setData]       = useState<ChampionDetailStats | null>(null);
  const [loading, setLoading] = useState(false);
  const triggerRef = useRef<HTMLSpanElement>(null);
  const showTimer  = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const hideTimer  = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const fetchData = useCallback(async () => {
    const key = `${champion}:${mode}`;
    if (cache.has(key)) { setData(cache.get(key)!); return; }
    setLoading(true);
    try {
      const result = await api.get<ChampionDetailStats>(
        `/stats/champion/${encodeURIComponent(champion)}?mode=${mode}`
      );
      cache.set(key, result);
      setData(result);
    } catch { /* ignore */ } finally { setLoading(false); }
  }, [champion, mode]);

  function handleMouseEnter() {
    clearTimeout(hideTimer.current);
    showTimer.current = setTimeout(() => {
      const rect = triggerRef.current?.getBoundingClientRect();
      if (!rect) return;
      const POPUP_H = 300, POPUP_W = 260;
      const flipY = rect.bottom + POPUP_H > window.innerHeight;
      const x = Math.min(rect.left, window.innerWidth - POPUP_W - 8);
      const y = flipY ? rect.top - POPUP_H - 4 : rect.bottom + 4;
      setPos({ x, y });
      setVisible(true);
      fetchData();
    }, 220);
  }

  function handleMouseLeave() {
    clearTimeout(showTimer.current);
    hideTimer.current = setTimeout(() => setVisible(false), 180);
  }

  return (
    <>
      <span ref={triggerRef} className={`popup-trigger${className ? ` ${className}` : ''}`}
        onMouseEnter={handleMouseEnter} onMouseLeave={handleMouseLeave}>
        {children}
      </span>

      {visible && createPortal(
        <div className="popup-panel"
          style={{ left: pos.x, top: pos.y, width: 260 }}
          onMouseEnter={() => clearTimeout(hideTimer.current)}
          onMouseLeave={handleMouseLeave}>
          {loading || !data
            ? <div className="popup-loading">
                <span className="popup-loading-dot" />
                {champion} 로딩 중…
              </div>
            : <ChampionPopupContent champion={champion} data={data} />
          }
        </div>,
        document.body
      )}
    </>
  );
}
