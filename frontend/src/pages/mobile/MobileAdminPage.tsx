import { useState, useEffect } from 'react';
import { Shield, Play, RefreshCw, Lock } from 'lucide-react';
import { api } from '../../lib/api/api';

const ADMIN_PASSWORD = 'admin1234';
const SESSION_KEY = 'mobile_admin_auth';

interface BatchStatus {
  playerSnapshotCount: number;
  championSnapshotCount: number;
  championItemSnapshotCount: number;
  lastAggregatedAt: string | null;
  message: string;
}

export function MobileAdminPage() {
  const [authed, setAuthed] = useState(() => sessionStorage.getItem(SESSION_KEY) === 'true');
  const [pw, setPw] = useState('');

  if (!authed) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '40px 16px' }}>
        <div style={{ width: 56, height: 56, borderRadius: 16, background: 'rgba(200,155,60,0.1)', border: '1px solid rgba(200,155,60,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
          <Shield size={26} color="var(--color-primary)" />
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 8 }}>관리자 인증</div>
        <div style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 24 }}>비밀번호를 입력해주세요</div>
        <input
          className="m-pw-input"
          type="password"
          placeholder="비밀번호"
          value={pw}
          onChange={e => setPw(e.target.value)}
          onKeyDown={e => {
            if (e.key === 'Enter') {
              if (pw === ADMIN_PASSWORD) {
                sessionStorage.setItem(SESSION_KEY, 'true');
                setAuthed(true);
              } else {
                alert('비밀번호가 틀렸습니다');
                setPw('');
              }
            }
          }}
          style={{ width: '100%', maxWidth: 280 }}
          autoFocus
        />
        <button
          className="m-admin-btn m-admin-btn-primary"
          style={{ width: '100%', maxWidth: 280 }}
          onClick={() => {
            if (pw === ADMIN_PASSWORD) {
              sessionStorage.setItem(SESSION_KEY, 'true');
              setAuthed(true);
            } else {
              alert('비밀번호가 틀렸습니다');
              setPw('');
            }
          }}
        >
          <Lock size={16} />
          확인
        </button>
      </div>
    );
  }

  return <AdminContent />;
}

function AdminContent() {
  const [batchRunning, setBatchRunning] = useState(false);
  const [batchStatus, setBatchStatus] = useState<BatchStatus | null>(null);
  const [batchStatusLoading, setBatchStatusLoading] = useState(false);
  const [eloResetting, setEloResetting] = useState(false);
  const [batchMsg, setBatchMsg] = useState('');

  const loadStatus = async () => {
    setBatchStatusLoading(true);
    try {
      const s = await api.get<BatchStatus>('/batch/status');
      setBatchStatus(s);
    } finally {
      setBatchStatusLoading(false);
    }
  };

  useEffect(() => { loadStatus(); }, []);

  const handleBatchRun = async () => {
    if (!confirm('배치를 실행하시겠습니까?')) return;
    setBatchRunning(true);
    setBatchMsg('');
    try {
      await api.post('/batch/trigger', {});
      setBatchMsg('배치가 완료되었습니다');
      await loadStatus();
    } catch {
      setBatchMsg('배치 실행 실패');
    } finally {
      setBatchRunning(false);
    }
  };

  const handleEloReset = async () => {
    if (!confirm('Elo를 전체 재집계하시겠습니까?\n이 작업은 되돌릴 수 없습니다.')) return;
    setEloResetting(true);
    try {
      await api.post('/admin/elo/reset', {});
      alert('Elo 재집계 완료');
    } catch {
      alert('Elo 재집계 실패');
    } finally {
      setEloResetting(false);
    }
  };

  return (
    <div>
      {/* Batch Section */}
      <p className="m-section-title">배치 작업</p>
      <div className="m-card" style={{ marginBottom: 12 }}>
        {batchStatus && (
          <div style={{ marginBottom: 12 }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 8, marginBottom: 8 }}>
              {[
                { label: '플레이어 스냅샷', value: batchStatus.playerSnapshotCount },
                { label: '챔피언 스냅샷', value: batchStatus.championSnapshotCount },
              ].map(({ label, value }) => (
                <div key={label} className="m-overview-stat">
                  <div className="m-overview-stat-value">{value.toLocaleString()}</div>
                  <div className="m-overview-stat-label">{label}</div>
                </div>
              ))}
            </div>
            {batchStatus.lastAggregatedAt && (
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', textAlign: 'center' }}>
                마지막 집계: {new Date(batchStatus.lastAggregatedAt).toLocaleString('ko-KR')}
              </div>
            )}
          </div>
        )}

        {batchMsg && (
          <div style={{ fontSize: 13, color: 'var(--color-win)', marginBottom: 10, textAlign: 'center' }}>
            {batchMsg}
          </div>
        )}

        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className="m-admin-btn m-admin-btn-primary"
            onClick={handleBatchRun}
            disabled={batchRunning}
            style={{ flex: 1 }}
          >
            <Play size={15} />
            {batchRunning ? '실행 중...' : '배치 실행'}
          </button>
          <button
            className="m-admin-btn m-admin-btn-secondary"
            onClick={loadStatus}
            disabled={batchStatusLoading}
            style={{ flex: 1 }}
          >
            <RefreshCw size={15} />
            상태 갱신
          </button>
        </div>
      </div>

      {/* Elo Section */}
      <p className="m-section-title">Elo 관리</p>
      <div className="m-card" style={{ marginBottom: 12 }}>
        <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', marginBottom: 12, lineHeight: 1.6 }}>
          전체 경기를 다시 순회하여 Elo를 재집계합니다. 기존 Elo 기록이 초기화되고 재계산됩니다.
        </p>
        <button
          className="m-admin-btn m-admin-btn-danger"
          onClick={handleEloReset}
          disabled={eloResetting}
        >
          <RefreshCw size={15} />
          {eloResetting ? '재집계 중...' : 'Elo 재집계'}
        </button>
      </div>
    </div>
  );
}
