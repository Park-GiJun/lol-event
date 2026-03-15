import { useState, useRef, useEffect } from 'react';
import { Radio, RefreshCw, Upload, Download, CheckCircle, XCircle } from 'lucide-react';
import { api, lcuApi } from '../lib/api/api';
import type { SaveMatchesResponse } from '../lib/types/match';
import { Button } from '../components/common/Button';

interface LogLine { type: string; message: string; }

const INSTALLER_URL = `https://gijun.net/downloads/${encodeURIComponent('LoL 수집기 Setup 1.0.0.exe')}`;

export function LcuPage() {
  const [logs, setLogs] = useState<LogLine[]>([]);
  const [collecting, setCollecting] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<SaveMatchesResponse | null>(null);
  const [appOnline, setAppOnline] = useState<boolean | null>(null);
  const logRef = useRef<HTMLDivElement>(null);

  // 로컬 수집기 앱 실행 여부 확인
  useEffect(() => {
    const check = () =>
      lcuApi.status()
        .then(() => setAppOnline(true))
        .catch(() => setAppOnline(false));
    check();
    const id = setInterval(check, 5000);
    return () => clearInterval(id);
  }, []);

  const addLog = (line: LogLine) => {
    setLogs(prev => {
      const next = [...prev, line];
      setTimeout(() => logRef.current?.scrollTo(0, logRef.current.scrollHeight), 0);
      return next;
    });
  };

  const handleCollect = () => {
    setLogs([]); setSyncResult(null); setCollecting(true);
    lcuApi.fetchSSE(
      (data) => addLog({ type: data.type as string, message: data.message as string ?? JSON.stringify(data) }),
      () => setCollecting(false)
    );
  };

  const handleSync = async () => {
    setSyncing(true); setSyncResult(null);
    try {
      const matches = await lcuApi.getMatches();
      if (!matches.length) { addLog({ type: 'warn', message: '동기화할 경기 없음' }); return; }
      const res = await api.post<SaveMatchesResponse>('/matches/bulk', { matches });
      setSyncResult(res);
      addLog({ type: 'done', message: `서버 저장 완료 — 신규 ${res.saved}건, 스킵 ${res.skipped}건, 누적 ${res.total}건` });
    } finally { setSyncing(false); }
  };

  const typeClass: Record<string, string> = {
    info: 'sse-info', warn: 'sse-warn', error: 'sse-error', done: 'sse-done', progress: 'sse-progress',
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">LCU 수집</h1>
        <p className="page-subtitle">LoL 클라이언트에서 내전 데이터를 수집하고 서버에 저장합니다</p>
      </div>

      {/* 수집기 앱 상태 / 다운로드 */}
      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <span className="card-title">수집기 앱</span>
          {appOnline !== null && (
            <span style={{
              display: 'flex', alignItems: 'center', gap: '6px', fontSize: 'var(--font-size-sm)',
              color: appOnline ? 'var(--color-win)' : 'var(--color-lose)',
            }}>
              {appOnline
                ? <><CheckCircle size={14} /> 실행 중</>
                : <><XCircle size={14} /> 미실행</>}
            </span>
          )}
        </div>

        {!appOnline && (
          <div style={{
            background: 'rgba(255,255,255,0.03)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            padding: 'var(--spacing-md)',
          }}>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', marginBottom: 'var(--spacing-sm)' }}>
              LCU 수집기는 <strong style={{ color: 'var(--color-text)' }}>로컬 PC</strong>에서 실행되는 앱입니다.
              설치 후 실행하면 자동으로 연결됩니다.
            </p>
            <a href={INSTALLER_URL} download>
              <Button>
                <Download size={14} /> 수집기 다운로드 (.exe)
              </Button>
            </a>
          </div>
        )}

        {appOnline && (
          <p style={{ color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)' }}>
            수집기가 백그라운드에서 실행 중입니다. 아래 버튼으로 수집을 시작하세요.
          </p>
        )}
      </div>

      {/* 수집 절차 */}
      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <span className="card-title">수집 절차</span>
        </div>
        <ol style={{ paddingLeft: '20px', color: 'var(--color-text-secondary)', fontSize: 'var(--font-size-sm)', lineHeight: 2 }}>
          <li>수집기 앱 설치 후 실행 (시스템 트레이에 상주)</li>
          <li>LoL 클라이언트가 실행 중인지 확인</li>
          <li><strong style={{ color: 'var(--color-primary)' }}>LCU 수집</strong> 버튼으로 내전 목록 수집</li>
          <li>수집 완료 후 <strong style={{ color: 'var(--color-primary)' }}>서버 동기화</strong> 버튼으로 백엔드에 저장</li>
        </ol>
      </div>

      <div className="card">
        <div className="flex gap-sm" style={{ marginBottom: 'var(--spacing-md)' }}>
          <Button onClick={handleCollect} loading={collecting} disabled={syncing || !appOnline}>
            <Radio size={14} /> LCU 수집
          </Button>
          <Button variant="secondary" onClick={handleSync} loading={syncing} disabled={collecting || !appOnline}>
            <Upload size={14} /> 서버 동기화
          </Button>
          <Button variant="ghost" size="sm" onClick={() => { setLogs([]); setSyncResult(null); }}>
            <RefreshCw size={14} /> 초기화
          </Button>
        </div>

        {syncResult && (
          <div style={{ background: 'rgba(11,196,180,0.1)', border: '1px solid var(--color-win)', borderRadius: 'var(--radius-md)', padding: 'var(--spacing-md)', marginBottom: 'var(--spacing-md)', color: 'var(--color-win)', fontSize: 'var(--font-size-sm)' }}>
            저장 완료 — 신규 {syncResult.saved}건 / 스킵 {syncResult.skipped}건 / 누적 {syncResult.total}건
          </div>
        )}

        <div className="sse-log" ref={logRef}>
          {!logs.length && (
            <div className="sse-progress">
              {appOnline === false ? '수집기 앱을 먼저 실행해주세요' : '대기 중...'}
            </div>
          )}
          {logs.map((l, i) => (
            <div key={i} className={`sse-line ${typeClass[l.type] ?? 'sse-info'}`}>
              {l.message}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
