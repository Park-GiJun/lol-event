import { useState, useEffect, useRef } from 'react';
import { Monitor, Lock, ExternalLink, Activity } from 'lucide-react';
import '../styles/pages/monitoring.css';
import '../styles/pages/stats.css';

const MONITORING_PASSWORD = 'admin1234';
const SESSION_KEY = 'monitoring_auth';
const GRAFANA_URL = 'http://localhost:3001';
const PROMETHEUS_URL = 'http://localhost:9091';

export function MonitoringPage() {
  const [authed, setAuthed] = useState(() => sessionStorage.getItem(SESSION_KEY) === 'true');
  const [pw, setPw] = useState('');
  const [error, setError] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!authed) inputRef.current?.focus();
  }, [authed]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (pw === MONITORING_PASSWORD) {
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
          <h2 className="monitoring-gate-title">모니터링 접근 인증</h2>
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
            <button type="submit" className="btn btn-primary monitoring-gate-btn">
              확인
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="monitoring-page">
      <div className="monitoring-header">
        <div className="monitoring-header-left">
          <Monitor size={20} color="var(--color-primary)" />
          <h1>서비스 모니터링</h1>
        </div>
        <button
          className="btn btn-secondary monitoring-open-btn"
          onClick={() => sessionStorage.removeItem(SESSION_KEY)}
          style={{ fontSize: '0.78rem' }}
        >
          잠금
        </button>
      </div>

      <div className="monitoring-links">
        <a
          href={GRAFANA_URL}
          target="_blank"
          rel="noopener noreferrer"
          className="monitoring-link-card monitoring-link-card--grafana"
        >
          <Monitor size={24} />
          <div>
            <div className="monitoring-link-title">
              Grafana 대시보드
              <ExternalLink size={12} style={{ marginLeft: '0.3rem', verticalAlign: 'middle' }} />
            </div>
            <div className="monitoring-link-desc">
              HTTP 요청 처리량 · 에러율 · JVM 메모리 · DB 커넥션
            </div>
            <div className="monitoring-link-url">localhost:3001</div>
          </div>
        </a>
        <a
          href={PROMETHEUS_URL}
          target="_blank"
          rel="noopener noreferrer"
          className="monitoring-link-card monitoring-link-card--prometheus"
        >
          <Activity size={24} />
          <div>
            <div className="monitoring-link-title">
              Prometheus
              <ExternalLink size={12} style={{ marginLeft: '0.3rem', verticalAlign: 'middle' }} />
            </div>
            <div className="monitoring-link-desc">
              메트릭 수집 · PromQL 쿼리 · 타겟 상태 확인
            </div>
            <div className="monitoring-link-url">localhost:9091</div>
          </div>
        </a>
      </div>

      <div className="monitoring-setup-guide">
        <h3>Prometheus 연결 설정</h3>
        <p>기존 Prometheus(<code>stockSimulator-prometheus</code>)에 아래 scrape config를 추가하세요.</p>
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
          Grafana에서 Prometheus 데이터소스 URL: <code>http://stockSimulator-prometheus:9090</code> (또는 컨테이너명 확인 후 사용)
        </p>
        <p className="monitoring-setup-hint">
          대시보드 JSON: <code>backend/monitoring/grafana/dashboards/lol-event.json</code> 파일을 Grafana에서 import
        </p>
      </div>
    </div>
  );
}
