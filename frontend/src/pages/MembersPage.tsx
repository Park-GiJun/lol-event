import { useEffect, useState, useCallback } from 'react';
import { Trash2, UserPlus, Users } from 'lucide-react';
import { api } from '../lib/api/api';
import { PlayerLink } from '../components/common/PlayerLink';
import type { Member, BulkRegisterResponse } from '../lib/types/member';
import { Button } from '../components/common/Button';
import { Input, Textarea } from '../components/common/Input';
import { Modal } from '../components/common/Modal';
import { LoadingCenter } from '../components/common/Spinner';

export function MembersPage() {
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [showRegister, setShowRegister] = useState(false);
  const [showBulk, setShowBulk] = useState(false);
  const [riotId, setRiotId] = useState('');
  const [bulkInput, setBulkInput] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [bulkResult, setBulkResult] = useState<BulkRegisterResponse | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { setMembers(await api.get<Member[]>('/members')); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleRegister = async () => {
    if (!riotId.includes('#')) return;
    setSubmitting(true);
    try {
      await api.post('/members/register', { riotId });
      setRiotId(''); setShowRegister(false); load();
    } finally { setSubmitting(false); }
  };

  const handleBulk = async () => {
    const ids = bulkInput.split('\n').filter(l => l.trim());
    setSubmitting(true);
    try {
      const res = await api.post<BulkRegisterResponse>('/members/register-bulk', { riotIds: ids });
      setBulkResult(res); load();
    } finally { setSubmitting(false); }
  };

  const handleDelete = async (puuid: string) => {
    if (!confirm('삭제하시겠습니까?')) return;
    await api.delete(`/members/${puuid}`);
    load();
  };

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="page-title">멤버 관리</h1>
          <p className="page-subtitle">등록된 멤버 {members.length}명</p>
        </div>
        <div className="flex gap-sm">
          <Button variant="secondary" size="sm" onClick={() => setShowBulk(true)}>
            <Users size={14} /> 일괄 등록
          </Button>
          <Button size="sm" onClick={() => setShowRegister(true)}>
            <UserPlus size={14} /> 멤버 등록
          </Button>
        </div>
      </div>

      {loading ? <LoadingCenter /> : (
        <div className="card">
          <div className="table-wrapper">
            <table className="table">
              <thead><tr><th>Riot ID</th><th>PUUID</th><th>등록일</th><th></th></tr></thead>
              <tbody>
                {members.map(m => (
                  <tr key={m.puuid}>
                    <td className="font-semibold"><PlayerLink riotId={m.riotId}>{m.riotId}</PlayerLink></td>
                    <td className="text-secondary text-xs truncate" style={{ maxWidth: '200px' }}>{m.puuid}</td>
                    <td className="text-secondary">{new Date(m.registeredAt).toLocaleDateString('ko-KR')}</td>
                    <td>
                      <Button variant="ghost" size="sm" onClick={() => handleDelete(m.puuid)}>
                        <Trash2 size={14} color="var(--color-error)" />
                      </Button>
                    </td>
                  </tr>
                ))}
                {!members.length && (
                  <tr><td colSpan={4} style={{ textAlign: 'center', color: 'var(--color-text-secondary)', padding: '32px' }}>등록된 멤버 없음</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal isOpen={showRegister} onClose={() => setShowRegister(false)} title="멤버 등록" size="sm"
        footer={<><Button variant="secondary" onClick={() => setShowRegister(false)}>취소</Button><Button loading={submitting} onClick={handleRegister}>등록</Button></>}>
        <Input label="Riot ID" value={riotId} onChange={setRiotId} placeholder="게임명#KR1" />
      </Modal>

      <Modal isOpen={showBulk} onClose={() => { setShowBulk(false); setBulkResult(null); }} title="일괄 등록" size="md"
        footer={!bulkResult ? <><Button variant="secondary" onClick={() => setShowBulk(false)}>취소</Button><Button loading={submitting} onClick={handleBulk}>등록</Button></> : <Button onClick={() => { setShowBulk(false); setBulkResult(null); }}>닫기</Button>}>
        {!bulkResult ? (
          <Textarea label="Riot ID 목록 (줄 구분)" value={bulkInput} onChange={setBulkInput} placeholder={"플레이어1#KR1\n플레이어2#KR2"} rows={8} />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', maxHeight: '320px', overflowY: 'auto' }}>
            {bulkResult.results.map((r, i) => (
              <div key={i} className="flex items-center justify-between" style={{ fontSize: 'var(--font-size-sm)' }}>
                <span>{r.riotId}</span>
                <span className={`badge ${r.status === 'ok' ? 'badge-win' : r.status === 'error' ? 'badge-loss' : 'badge-normal'}`}>
                  {r.status === 'ok' ? '등록' : r.status === 'skip' ? `스킵: ${r.reason}` : `오류: ${r.reason}`}
                </span>
              </div>
            ))}
          </div>
        )}
      </Modal>
    </div>
  );
}
