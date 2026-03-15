import { useState, useEffect, useRef } from 'react';
import { Shield, Lock, ExternalLink, Activity, Monitor, Play, RefreshCw } from 'lucide-react';
import { api } from '../lib/api/api';
import '../styles/pages/monitoring.css';

const ADMIN_PASSWORD = 'admin1234';
const SESSION_KEY = 'monitoring_auth';
const GRAFANA_URL = 'http://localhost:3001';
const PROMETHEUS_URL = 'http://localhost:9091';

interface BatchStatus {
  playerSnapshotCount: number;
  championSnapshotCount: number;
  championItemSnapshotCount: number;
  lastAggregatedAt: string | null;
  message: string;
}

export function AdminPage() {
  const [authed, setAuthed] = useState(() => sessionStorage.getItem(SESSION_KEY) === 'true');
  const [pw, setPw] = useState('');
  const [error, setError] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  const [batchStatus, setBatchStatus] = useState<BatchStatus | null>(null);
  const [triggering, setTriggering] = useState(false);
  const [triggerMsg, setTriggerMsg] = useState('');
  const [triggeringItems, setTriggeringItems] = useState(false);
  const [triggerItemMsg, setTriggerItemMsg] = useState('');

  useEffect(() => {
    if (!authed) { inputRef.current?.focus(); return; }
    fetchBatchStatus();
  }, [authed]);

  async function fetchBatchStatus() {
    try {
      const res = await api.get<BatchStatus>('/batch/status');
      setBatchStatus(res);
    } catch {
      // ignore
    }
  }

  async function triggerBatch() {
    setTriggering(true);
    setTriggerMsg('');
    try {
      await api.post('/batch/trigger', {});
      setTriggerMsg('배치 실행 요청이 전송되었습니다.');
      setTimeout(fetchBatchStatus, 2000);
    } catch {
      setTriggerMsg('배치 실행에 실패했습니다.');
    } finally {
      setTriggering(false);
    }
  }

  async function triggerItemStats() {
    setTriggeringItems(true);
    setTriggerItemMsg('');
    try {
      await api.post('/batch/trigger-item-stats', {});
      setTriggerItemMsg('아이템 통계 집계가 완료되었습니다.');
      setTimeout(fetchBatchStatus, 1000);
    } catch {
      setTriggerItemMsg('아이템 통계 집계에 실패했습니다.');
    } finally {
      setTriggeringItems(false);
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (pw === ADMIN_PASSWORD) {
      sessionStorage.setItem(SESSION_KEY, 'true');
      setAuthed(true);
      setError('');
    } else {
      setError('비밀번호가 올바르지 않습니다.');
      setPw('');
      inputRef.current?.focus();
    }
  }

  if (!authed) {
    return (
      <div className="monitoring-gate">
        <div className="monitoring-gate-card">
          <div className="monitoring-gate-icon">
            <Lock size={32} color="var(--color-primary)" />
          </div>
          <h2 className="monitoring-gate-title">어드민 접근 인증</h2>
          <p className="monitoring-gate-desc">접근하려면 관리자 비밀번호를 입력하세요.</p>
          <form onSubmit={handleSubmit} className="monitoring-gate-form">
            <input
              ref={inputRef}
              type="password"
              value={pw}
              onChange={e => setPw(e.target.value)}
              placeholder="비밀번호"
              className={`monitoring-gate-input${error ? ' error' : ''}`}
              autoComplete="current-password"
            />
            {error && <p className="monitoring-gate-error">{error}</p>}
            <button type="submit" className="btn btn-primary monitoring-gate-btn">확인</button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="monitoring-page">
      <div className="monitoring-header">
        <div className="monitoring-header-left">
          <Shield size={20} color="var(--color-primary)" />
          <h1>어드민</h1>
        </div>
        <button
          className="btn btn-secondary"
          onClick={() => { sessionStorage.removeItem(SESSION_KEY); setAuthed(false); }}
          style={{ fontSize: '0.78rem' }}
        >
          잠금
        </button>
      </div>

      {/* 배치 스케쥴러 */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title">
          <RefreshCw size={16} />
          통계 배치 스케쥴러
          <span className="stats-section-sub">매일 04:00 자동 실행 | Kafka 이벤트 수신 시 자동 실행</span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <button
            className="btn btn-primary"
            onClick={triggerBatch}
            disabled={triggering}
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}
          >
            <Play size={14} />
            {triggering ? '실행 중...' : '배치 수동 실행'}
          </button>

          <button
            className="btn btn-secondary"
            onClick={fetchBatchStatus}
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}
          >
            <RefreshCw size={14} />
            상태 새로고침
          </button>

          {triggerMsg && (
            <span style={{ fontSize: 13, color: triggerMsg.includes('실패') ? 'var(--color-loss)' : 'var(--color-win)' }}>
              {triggerMsg}
            </span>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap', marginTop: 12 }}>
          <button
            className="btn btn-secondary"
            onClick={triggerItemStats}
            disabled={triggeringItems}
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}
          >
            <Play size={14} />
            {triggeringItems ? '집계 중...' : '아이템 통계만 재집계'}
          </button>

          {triggerItemMsg && (
            <span style={{ fontSize: 13, color: triggerItemMsg.includes('실패') ? 'var(--color-loss)' : 'var(--color-win)' }}>
              {triggerItemMsg}
            </span>
          )}
        </div>

        {batchStatus && (
          <div style={{ marginTop: 16, display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
            {[
              { label: '플레이어 스냅샷',   value: `${batchStatus.playerSnapshotCount}건` },
              { label: '챔피언 스냅샷',     value: `${batchStatus.championSnapshotCount}건` },
              { label: '챔피언 아이템 통계', value: `${batchStatus.championItemSnapshotCount}건` },
              { label: '마지막 집계',       value: batchStatus.lastAggregatedAt ?? '없음' },
            ].map(({ label, value }) => (
              <div key={label} className="summary-stat-card" style={{ alignItems: 'flex-start', gap: 4 }}>
                <span style={{ fontSize: 10, color: 'var(--color-text-secondary)', fontWeight: 700, textTransform: 'uppercase' }}>{label}</span>
                <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-text-primary)' }}>{value}</span>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* 모니터링 링크 */}
      <section className="stats-section card" style={{ marginBottom: 20 }}>
        <div className="stats-section-title">
          <Monitor size={16} />
          모니터링 대시보드
        </div>
        <div className="monitoring-links" style={{ marginBottom: 0 }}>
          <a href={GRAFANA_URL} target="_blank" rel="noopener noreferrer" className="monitoring-link-card">
            <Monitor size={24} />
            <div>
              <div className="monitoring-link-title">
                Grafana 대시보드
                <ExternalLink size={12} style={{ marginLeft: '0.3rem', verticalAlign: 'middle' }} />
              </div>
              <div className="monitoring-link-desc">HTTP 요청 처리량 · 에러율 · JVM 메모리 · DB 커넥션</div>
              <div className="monitoring-link-url">localhost:3001</div>
            </div>
          </a>
          <a href={PROMETHEUS_URL} target="_blank" rel="noopener noreferrer" className="monitoring-link-card">
            <Activity size={24} />
            <div>
              <div className="monitoring-link-title">
                Prometheus
                <ExternalLink size={12} style={{ marginLeft: '0.3rem', verticalAlign: 'middle' }} />
              </div>
              <div className="monitoring-link-desc">메트릭 수집 · PromQL 쿼리 · 타겟 상태 확인</div>
              <div className="monitoring-link-url">localhost:9091</div>
            </div>
          </a>
        </div>
      </section>

      {/* Prometheus 설정 가이드 */}
      <section className="stats-section card">
        <div className="stats-section-title">Prometheus 연결 설정</div>
        <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 12 }}>
          기존 Prometheus(<code>stockSimulator-prometheus</code>)에 아래 scrape config를 추가하세요.
        </p>
        <pre>{`# /etc/prometheus/prometheus.yml 에 추가
scrape_configs:
  - job_name: 'lol-api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:9832']
        labels:
          application: 'api-gateway'

  - job_name: 'lol-main-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']
        labels:
          application: 'main-service'`}</pre>
        <p className="monitoring-setup-hint">
          설정 적용: <code>curl -X POST http://localhost:9091/-/reload</code>
        </p>
        <p className="monitoring-setup-hint">
          대시보드 JSON: <code>backend/monitoring/grafana/dashboards/lol-event.json</code> 파일을 Grafana에서 import
        </p>
      </section>
    </div>
  );
}
