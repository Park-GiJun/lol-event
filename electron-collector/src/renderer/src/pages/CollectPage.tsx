import { useState, useRef, useEffect } from 'react';
import { Radio } from 'lucide-react';

interface LogLine { type: string; message: string; }

const TYPE_CLASS: Record<string, string> = {
  info: 'sse-info', warn: 'sse-warn', error: 'sse-error', done: 'sse-done', progress: 'sse-progress',
};

export function CollectPage() {
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [collecting, setCollecting] = useState(false);
  const [lcuConnected, setLcuConnected] = useState(false);
  const [updateInfo, setUpdateInfo] = useState<{ version: string; ready: boolean } | null>(null);
  const logRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    window.lol.onStatus((s) => setLcuConnected(!!(s as { connected: boolean }).connected));
    window.lol.onLog((type, message) => {
      setLogs(prev => [...prev, { type, message }]);
      setTimeout(() => logRef.current?.scrollTo(0, logRef.current.scrollHeight), 0);
      if (type === 'done' || type === 'error') setCollecting(false);
    });
    window.lol.onUpdateAvailable((info) => setUpdateInfo({ version: String((info as { version: string }).version), ready: false }));
    window.lol.onUpdateDownloaded((info) => setUpdateInfo({ version: String((info as { version: string }).version), ready: true }));
  }, []);

  const handleCollect = () => {
    setLogs([]);
    setCollecting(true);
    window.lol.startCollect();
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">매치 수집</h1>
        <p className="page-subtitle">LCU에서 내전 데이터를 수집해 서버로 전송합니다</p>
      </div>

      {updateInfo && (
        <div className="update-banner">
          {updateInfo.ready
            ? `v${updateInfo.version} 업데이트 준비 완료`
            : `v${updateInfo.version} 다운로드 중...`}
          {updateInfo.ready && (
            <button className="btn btn-sm btn-primary" onClick={() => window.lol.installUpdate()}>
              지금 설치
            </button>
          )}
        </div>
      )}

      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <button
          className={`btn btn-primary${collecting ? ' btn-loading' : ''}`}
          style={{ width: '100%' }}
          onClick={handleCollect}
          disabled={collecting || !lcuConnected}
        >
          <Radio size={14} />
          {collecting ? '수집 중...' : '수집 시작'}
        </button>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">수집 로그</span>
        </div>
        <div className="sse-log" ref={logRef}>
          {logs.length === 0
            ? <div className="sse-progress">{lcuConnected ? '대기 중...' : '롤 클라이언트를 실행해주세요'}</div>
            : logs.map((l, i) => (
              <div key={i} className={`sse-line ${TYPE_CLASS[l.type] ?? 'sse-info'}`}>{l.message}</div>
            ))
          }
        </div>
      </div>
    </div>
  );
}
