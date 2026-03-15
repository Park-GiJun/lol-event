import { useEffect, useState } from 'react';
import { Swords } from 'lucide-react';

interface LcuStatus { connected: boolean; gameName?: string; tagLine?: string; reason?: string; }

export function Titlebar() {
  const [version, setVersion] = useState('');
  const [status, setStatus] = useState<LcuStatus>({ connected: false });

  useEffect(() => {
    window.lol.getVersion().then(setVersion);
    window.lol.onStatus((s) => setStatus(s as LcuStatus));
    window.lol.requestStatus();
    const id = setInterval(() => window.lol.requestStatus(), 5000);
    return () => clearInterval(id);
  }, []);

  return (
    <div className="titlebar">
      <div className="titlebar-drag">
        <Swords size={14} color="var(--color-primary)" />
        <span className="titlebar-name">LoL 수집기</span>
        <span className="titlebar-version">v{version}</span>
      </div>

      <div className="titlebar-status">
        <span className={`status-dot ${status.connected ? 'ok' : 'err'}`} />
        {status.connected
          ? <span className="status-summoner">{status.gameName}#{status.tagLine}</span>
          : <span className="status-label">{status.reason ?? '미연결'}</span>
        }
      </div>

      <div className="titlebar-controls">
        <button onClick={() => window.lol.minimize()} className="wc-btn" title="최소화">
          <svg width="10" height="1" viewBox="0 0 10 1"><rect width="10" height="1" fill="currentColor"/></svg>
        </button>
        <button onClick={() => window.lol.close()} className="wc-btn wc-close" title="닫기">
          <svg width="10" height="10" viewBox="0 0 10 10">
            <line x1="0" y1="0" x2="10" y2="10" stroke="currentColor" strokeWidth="1.5"/>
            <line x1="10" y1="0" x2="0" y2="10" stroke="currentColor" strokeWidth="1.5"/>
          </svg>
        </button>
      </div>
    </div>
  );
}
