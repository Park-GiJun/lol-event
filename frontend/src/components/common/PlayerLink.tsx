import { useState, useRef, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { api } from '../../lib/api/api';
import type { PlayerDetailStats } from '../../lib/types/stats';
import { useDragon } from '../../context/DragonContext';

// 모듈 레벨 캐시 — 리렌더링 시에도 재요청 없음
const cache = new Map<string, PlayerDetailStats>();

function PlayerPopupContent({ riotId, data }: { riotId: string; data: PlayerDetailStats }) {
  const { champions } = useDragon();
  const navigate = useNavigate();
  const wr = data.winRate;
  const wrColor = wr >= 60 ? 'var(--color-win)' : wr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
  const kdaColor = data.kda >= 4 ? 'var(--color-win)' : data.kda >= 2.5 ? 'var(--color-primary)' : 'var(--color-text-primary)';
  const eloVal = Number.isFinite(data.elo) ? data.elo : null;
  const eloColor = (eloVal ?? 0) >= 1200 ? 'var(--color-win)' : (eloVal ?? 0) >= 1000 ? 'var(--color-primary)' : 'var(--color-loss)';

  return (
    <div className="popup-player" onClick={() => navigate(`/player-stats/${encodeURIComponent(riotId)}`)}>
      {/* 헤더 */}
      <div className="popup-player-header">
        <div className="popup-player-name">{riotId.split('#')[0]}</div>
        <div className="popup-player-tag">#{riotId.split('#')[1]}</div>
        <div className="popup-player-games">{data.games}판</div>
      </div>

      {/* 승률 바 */}
      <div className="popup-wr-row">
        <span className="popup-wr-value" style={{ color: wrColor }}>{wr}%</span>
        <div className="popup-wr-bar">
          <div className="popup-wr-fill" style={{ width: `${wr}%`, background: wrColor }} />
        </div>
        <span className="popup-wl">
          <span style={{ color: 'var(--color-win)' }}>{data.wins}W</span>
          {' '}<span style={{ color: 'var(--color-loss)' }}>{data.losses}L</span>
        </span>
      </div>

      {/* 핵심 스탯 */}
      <div className="popup-stats-row">
        <div className="popup-stat">
          <div className="popup-stat-value" style={{ color: kdaColor }}>{data.kda.toFixed(2)}</div>
          <div className="popup-stat-label">KDA</div>
        </div>
        <div className="popup-stat">
          <div className="popup-stat-value">{data.avgKills.toFixed(1)}</div>
          <div className="popup-stat-label">킬</div>
        </div>
        <div className="popup-stat">
          <div className="popup-stat-value">{data.avgDeaths.toFixed(1)}</div>
          <div className="popup-stat-label">데스</div>
        </div>
        <div className="popup-stat">
          <div className="popup-stat-value">{(data.avgDamage / 1000).toFixed(1)}K</div>
          <div className="popup-stat-label">딜량</div>
        </div>
        <div className="popup-stat">
          <div className="popup-stat-value">{data.avgCs.toFixed(1)}</div>
          <div className="popup-stat-label">CS</div>
        </div>
        <div className="popup-stat">
          <div className="popup-stat-value" style={{ color: eloColor }}>{eloVal !== null ? Math.round(eloVal) : '-'}</div>
          <div className="popup-stat-label">Elo</div>
        </div>
      </div>

      {/* 주요 챔피언 */}
      {data.championStats.length > 0 && (
        <div className="popup-champs">
          {data.championStats.slice(0, 5).map(c => {
            const img = champions.get(c.championId)?.imageUrl;
            const cwr = c.winRate;
            const cwrColor = cwr >= 60 ? 'var(--color-win)' : cwr >= 50 ? 'var(--color-primary)' : 'var(--color-loss)';
            return (
              <div key={c.champion} className="popup-champ-item" title={`${c.champion} ${c.games}판 ${cwr}%`}>
                {img
                  ? <img src={img} alt={c.champion} className="popup-champ-img" onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />
                  : <div className="popup-champ-fallback">{c.champion.slice(0, 2)}</div>
                }
                <span className="popup-champ-wr" style={{ color: cwrColor }}>{cwr}%</span>
              </div>
            );
          })}
        </div>
      )}

      <div className="popup-hint">클릭하여 상세 보기</div>
    </div>
  );
}

interface PlayerLinkProps {
  riotId: string;
  children: React.ReactNode;
  className?: string;
  mode?: string;
}

export function PlayerLink({ riotId, children, className, mode = 'normal' }: PlayerLinkProps) {
  const [visible, setVisible] = useState(false);
  const [pos, setPos] = useState({ x: 0, y: 0, flipY: false });
  const [data, setData] = useState<PlayerDetailStats | null>(null);
  const [loading, setLoading] = useState(false);
  const triggerRef = useRef<HTMLSpanElement>(null);
  const showTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const hideTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  const fetchData = useCallback(async () => {
    const key = `${riotId}:${mode}`;
    if (cache.has(key)) { setData(cache.get(key)!); return; }
    setLoading(true);
    try {
      const result = await api.get<PlayerDetailStats>(`/stats/player/${encodeURIComponent(riotId)}?mode=${mode}`);
      cache.set(key, result);
      setData(result);
    } catch { /* ignore */ } finally { setLoading(false); }
  }, [riotId, mode]);

  function handleMouseEnter() {
    clearTimeout(hideTimer.current);
    showTimer.current = setTimeout(() => {
      const rect = triggerRef.current?.getBoundingClientRect();
      if (!rect) return;
      const POPUP_H = 200, POPUP_W = 280;
      const flipY = rect.bottom + POPUP_H > window.innerHeight;
      const x = Math.min(rect.left, window.innerWidth - POPUP_W - 8);
      const y = flipY ? rect.top - POPUP_H - 4 : rect.bottom + 4;
      setPos({ x, y, flipY });
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
          style={{ left: pos.x, top: pos.y, width: 280 }}
          onMouseEnter={() => clearTimeout(hideTimer.current)}
          onMouseLeave={handleMouseLeave}>
          {loading || !data
            ? <div className="popup-loading"><span className="popup-loading-dot" />{riotId.split('#')[0]} 로딩 중…</div>
            : <PlayerPopupContent riotId={riotId} data={data} />
          }
        </div>,
        document.body
      )}
    </>
  );
}
