import { useState, useEffect, useCallback } from 'react';
import { RefreshCw, CheckCircle, Database, Swords, Zap } from 'lucide-react';
import { api } from '../lib/api/api';
import type { DragonSyncResponse, DragonChampion, DragonItem, DragonSummonerSpell } from '../lib/types/dragon';
import { Spinner } from '../components/common/Spinner';

type Tab = 'champions' | 'items' | 'spells';

export function SyncPage() {
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<DragonSyncResponse | null>(null);
  const [tab, setTab] = useState<Tab>('champions');

  const [champions, setChampions] = useState<DragonChampion[]>([]);
  const [items, setItems] = useState<DragonItem[]>([]);
  const [spells, setSpells] = useState<DragonSummonerSpell[]>([]);
  const [loadingData, setLoadingData] = useState(false);

  const loadCachedData = useCallback(async () => {
    setLoadingData(true);
    try {
      const [ch, it, sp] = await Promise.all([
        api.get<DragonChampion[]>('/ddragon/champions'),
        api.get<DragonItem[]>('/ddragon/items'),
        api.get<DragonSummonerSpell[]>('/ddragon/spells'),
      ]);
      setChampions(ch);
      setItems(it);
      setSpells(sp);
    } finally {
      setLoadingData(false);
    }
  }, []);

  useEffect(() => { loadCachedData(); }, [loadCachedData]);

  const handleSync = async () => {
    setSyncing(true);
    try {
      const result = await api.post<DragonSyncResponse>('/ddragon/sync', {});
      setSyncResult(result);
      await loadCachedData();
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">DataDragon 동기화</h1>
          <p className="page-subtitle">Riot DataDragon에서 최신 챔피언·아이템·스펠 데이터를 받아 DB에 저장하고 캐시를 갱신합니다</p>
        </div>
        <button className="btn btn-primary" onClick={handleSync} disabled={syncing}>
          {syncing ? <Spinner size={16} /> : <RefreshCw size={16} />}
          {syncing ? '동기화 중...' : 'DataDragon 동기화'}
        </button>
      </div>

      {syncResult && (
        <div className="card" style={{ marginBottom: 'var(--spacing-lg)', borderColor: 'var(--color-win)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', color: 'var(--color-win)' }}>
            <CheckCircle size={18} />
            <strong>동기화 완료</strong>
            <span style={{ color: 'var(--color-text-muted)', marginLeft: 'auto' }}>버전: {syncResult.version}</span>
          </div>
          <div style={{ display: 'flex', gap: 'var(--spacing-lg)', marginTop: 'var(--spacing-sm)' }}>
            <SyncStat icon={<Swords size={14} />} label="챔피언" value={syncResult.champions} />
            <SyncStat icon={<Database size={14} />} label="아이템" value={syncResult.items} />
            <SyncStat icon={<Zap size={14} />} label="소환사 스펠" value={syncResult.spells} />
          </div>
        </div>
      )}

      <div className="card">
        <div style={{ display: 'flex', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-md)' }}>
          {(['champions', 'items', 'spells'] as Tab[]).map(t => (
            <button key={t} className={`btn ${tab === t ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setTab(t)}>
              {t === 'champions' ? `챔피언 (${champions.length})` : t === 'items' ? `아이템 (${items.length})` : `스펠 (${spells.length})`}
            </button>
          ))}
        </div>

        {loadingData ? (
          <div style={{ textAlign: 'center', padding: 'var(--spacing-xl)' }}><Spinner /></div>
        ) : (
          <>
            {tab === 'champions' && <ChampionTable data={champions} />}
            {tab === 'items' && <ItemTable data={items} />}
            {tab === 'spells' && <SpellTable data={spells} />}
          </>
        )}
      </div>
    </div>
  );
}

function SyncStat({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-xs)', color: 'var(--color-text-muted)' }}>
      {icon}
      <span>{label}:</span>
      <strong style={{ color: 'var(--color-text)' }}>{value}</strong>
    </div>
  );
}

function ChampionTable({ data }: { data: DragonChampion[] }) {
  if (!data.length) return <Empty />;
  return (
    <div className="table-wrapper">
      <table className="table">
        <thead><tr>
          <th>이미지</th><th>ID</th><th>키</th><th>이름</th><th>타이틀</th><th>버전</th>
        </tr></thead>
        <tbody>
          {data.map(c => (
            <tr key={c.championId}>
              <td>{c.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={32} height={32} style={{ borderRadius: 4 }} />}</td>
              <td>{c.championId}</td>
              <td><code>{c.championKey}</code></td>
              <td><strong>{c.nameKo}</strong></td>
              <td style={{ color: 'var(--color-text-muted)' }}>{c.titleKo}</td>
              <td><span className="badge">{c.version}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function ItemTable({ data }: { data: DragonItem[] }) {
  if (!data.length) return <Empty />;
  return (
    <div className="table-wrapper">
      <table className="table">
        <thead><tr>
          <th>이미지</th><th>ID</th><th>이름</th><th>골드</th><th>버전</th>
        </tr></thead>
        <tbody>
          {data.map(item => (
            <tr key={item.itemId}>
              <td>{item.imageUrl && <img src={item.imageUrl} alt={item.nameKo} width={32} height={32} style={{ borderRadius: 4 }} />}</td>
              <td>{item.itemId}</td>
              <td><strong>{item.nameKo}</strong></td>
              <td>{item.goldTotal > 0 ? `${item.goldTotal.toLocaleString()}g` : '-'}</td>
              <td><span className="badge">{item.version}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function SpellTable({ data }: { data: DragonSummonerSpell[] }) {
  if (!data.length) return <Empty />;
  return (
    <div className="table-wrapper">
      <table className="table">
        <thead><tr>
          <th>이미지</th><th>ID</th><th>키</th><th>이름</th><th>버전</th>
        </tr></thead>
        <tbody>
          {data.map(s => (
            <tr key={s.spellId}>
              <td>{s.imageUrl && <img src={s.imageUrl} alt={s.nameKo} width={32} height={32} style={{ borderRadius: 4 }} />}</td>
              <td>{s.spellId}</td>
              <td><code>{s.spellKey}</code></td>
              <td><strong>{s.nameKo}</strong></td>
              <td><span className="badge">{s.version}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Empty() {
  return (
    <div style={{ textAlign: 'center', padding: 'var(--spacing-xl)', color: 'var(--color-text-muted)' }}>
      데이터가 없습니다. 동기화를 먼저 실행해주세요.
    </div>
  );
}
