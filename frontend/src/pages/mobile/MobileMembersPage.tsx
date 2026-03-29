import { useEffect, useState, useCallback } from 'react';
import { UserPlus, Trash2, Users } from 'lucide-react';
import { api } from '../../lib/api/api';
import type { Member, BulkRegisterResponse } from '../../lib/types/member';
import { LoadingCenter } from '../../components/common/Spinner';

export function MobileMembersPage() {
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);

  // Add single
  const [showAdd, setShowAdd] = useState(false);
  const [riotId, setRiotId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Bulk
  const [showBulk, setShowBulk] = useState(false);
  const [bulkInput, setBulkInput] = useState('');
  const [bulkResult, setBulkResult] = useState<BulkRegisterResponse | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setMembers(await api.get<Member[]>('/members'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleRegister = async () => {
    if (!riotId.includes('#')) {
      alert('올바른 형식으로 입력해주세요 (예: 게임명#KR1)');
      return;
    }
    setSubmitting(true);
    try {
      await api.post('/members/register', { riotId });
      setRiotId('');
      setShowAdd(false);
      load();
    } finally {
      setSubmitting(false);
    }
  };

  const handleBulk = async () => {
    const ids = bulkInput.split('\n').map(l => l.trim()).filter(Boolean);
    if (!ids.length) return;
    setSubmitting(true);
    try {
      const res = await api.post<BulkRegisterResponse>('/members/register-bulk', { riotIds: ids });
      setBulkResult(res);
      load();
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (puuid: string, name: string) => {
    if (!confirm(`${name} 멤버를 삭제하시겠습니까?`)) return;
    await api.delete(`/members/${puuid}`);
    load();
  };

  if (loading) return <LoadingCenter />;

  return (
    <div style={{ paddingBottom: 80 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <span style={{ fontSize: 13, color: 'var(--color-text-secondary)' }}>
          등록된 멤버 <strong style={{ color: 'var(--color-primary)' }}>{members.length}</strong>명
        </span>
        <button
          onClick={() => setShowBulk(true)}
          style={{ fontSize: 12, color: 'var(--color-text-secondary)', background: 'var(--color-bg-hover)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '6px 12px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}
        >
          <Users size={13} /> 일괄 등록
        </button>
      </div>

      {/* Member list */}
      {members.length === 0 ? (
        <div className="m-empty">등록된 멤버가 없습니다</div>
      ) : (
        members.map(m => {
          const [name, tag] = m.riotId.split('#');
          return (
            <div key={m.puuid} className="m-player-card" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
                  <span className="m-player-name">{name}</span>
                  {tag && <span className="m-player-tag">#{tag}</span>}
                </div>
                <div style={{ fontSize: 11, color: 'var(--color-text-disabled)', marginTop: 2 }}>
                  {new Date(m.registeredAt).toLocaleDateString('ko-KR')}
                </div>
              </div>
              <button
                onClick={() => handleDelete(m.puuid, m.riotId)}
                style={{ width: 32, height: 32, borderRadius: 8, background: 'rgba(232,64,64,0.1)', border: '1px solid rgba(232,64,64,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', flexShrink: 0 }}
              >
                <Trash2 size={14} color="var(--color-error)" />
              </button>
            </div>
          );
        })
      )}

      {/* FAB */}
      <button className="m-fab" onClick={() => setShowAdd(true)}>
        <UserPlus size={22} />
      </button>

      {/* Add single modal */}
      {showAdd && (
        <BottomSheet title="멤버 등록" onClose={() => { setShowAdd(false); setRiotId(''); }}>
          <input
            className="m-search"
            placeholder="게임명#KR1"
            value={riotId}
            onChange={e => setRiotId(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleRegister()}
            autoFocus
          />
          <button
            className="m-admin-btn m-admin-btn-primary"
            onClick={handleRegister}
            disabled={submitting || !riotId.includes('#')}
          >
            {submitting ? '등록 중...' : '등록'}
          </button>
        </BottomSheet>
      )}

      {/* Bulk modal */}
      {showBulk && (
        <BottomSheet title="일괄 등록" onClose={() => { setShowBulk(false); setBulkInput(''); setBulkResult(null); }}>
          {!bulkResult ? (
            <>
              <textarea
                value={bulkInput}
                onChange={e => setBulkInput(e.target.value)}
                placeholder={'플레이어1#KR1\n플레이어2#KR2'}
                rows={6}
                style={{ width: '100%', background: 'var(--color-bg-card)', border: '1px solid var(--color-border)', borderRadius: 10, padding: '10px 14px', fontSize: 13, color: 'var(--color-text-primary)', outline: 'none', resize: 'none', fontFamily: 'var(--font-family)', marginBottom: 12 }}
              />
              <button
                className="m-admin-btn m-admin-btn-primary"
                onClick={handleBulk}
                disabled={submitting || !bulkInput.trim()}
              >
                {submitting ? '등록 중...' : '일괄 등록'}
              </button>
            </>
          ) : (
            <>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 240, overflowY: 'auto', marginBottom: 12 }}>
                {bulkResult.results.map((r, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}>
                    <div className={`m-status-dot ${r.status}`} />
                    <span style={{ flex: 1 }}>{r.riotId}</span>
                    <span style={{ fontSize: 11, color: r.status === 'ok' ? 'var(--color-win)' : r.status === 'error' ? 'var(--color-error)' : 'var(--color-text-disabled)' }}>
                      {r.status === 'ok' ? '등록' : r.status === 'skip' ? `스킵` : `오류`}
                    </span>
                  </div>
                ))}
              </div>
              <button
                className="m-admin-btn m-admin-btn-secondary"
                onClick={() => { setShowBulk(false); setBulkInput(''); setBulkResult(null); }}
              >
                닫기
              </button>
            </>
          )}
        </BottomSheet>
      )}
    </div>
  );
}

function BottomSheet({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 200, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}
      onClick={onClose}>
      <div
        style={{ background: 'var(--color-bg-secondary)', borderRadius: '16px 16px 0 0', padding: '20px 16px 32px', maxHeight: '80vh', overflowY: 'auto' }}
        onClick={e => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <span style={{ fontWeight: 700, fontSize: 16 }}>{title}</span>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--color-text-secondary)', fontSize: 20, cursor: 'pointer', lineHeight: 1 }}>×</button>
        </div>
        {children}
      </div>
    </div>
  );
}
