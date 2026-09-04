import { CheckCircleIcon, DatabaseIcon, RefreshIcon, SwordsIcon, ZapIcon } from '@/components/icons/LolIcons';
import { useState, useEffect, useCallback } from 'react';
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
    <div className="t-page">
      <div className="hero-banner" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 'var(--spacing-lg)', flexWrap: 'wrap', marginBottom: 'var(--spacing-lg)' }}>
        <div>
          <div className="hero-eyebrow">DataDragon</div>
          <h1 className="hero-title">DataDragon 동기화</h1>
          <p className="hero-subtitle">Riot DataDragon에서 최신 챔피언·아이템·스펠 데이터를 받아 DB에 저장하고 캐시를 갱신합니다</p>
        </div>
        <button className="btn btn-primary" onClick={handleSync} disabled={syncing}>
          {syncing ? <Spinner size="sm" /> : <RefreshIcon size={16} />}
          {syncing ? '동기화 중...' : 'DataDragon 동기화'}
        </button>
      </div>

      {syncResult && (
        <div className="card" style={{ marginBottom: 'var(--spacing-lg)' }}>
          <div className="section-head">
            <span className="icon-chip"><CheckCircleIcon size={16} /></span>
            <span className="section-head-title">동기화 완료</span>
            <span className="section-head-action">
              <span className="badge badge-gold">버전 {syncResult.version}</span>
            </span>
          </div>
          <div className="grid-16">
            <div className="col-span-5"><SyncStat icon={<SwordsIcon size={16} />} label="챔피언" value={syncResult.champions} /></div>
            <div className="col-span-5"><SyncStat icon={<DatabaseIcon size={16} />} label="아이템" value={syncResult.items} /></div>
            <div className="col-span-6"><SyncStat icon={<ZapIcon size={16} />} label="소환사 스펠" value={syncResult.spells} /></div>
          </div>
        </div>
      )}

      <div className="card">
        <div className="tab-bar" style={{ marginBottom: 'var(--spacing-md)' }}>
          {(['champions', 'items', 'spells'] as Tab[]).map(t => (
            <button key={t} className={`tab-bar-item ${tab === t ? 'active' : ''}`}
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
    <div className="stat-card">
      <div className="stat-card-label" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
        <span className="icon-chip icon-chip-sm">{icon}</span>
        {label}
      </div>
      <div className="stat-card-value">{value.toLocaleString()}</div>
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
              <td>{c.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={32} height={32} style={{ borderRadius: 'var(--radius-sm)' }} />}</td>
              <td>{c.championId}</td>
              <td><code>{c.championKey}</code></td>
              <td><strong>{c.nameKo}</strong></td>
              <td className="text-secondary">{c.titleKo}</td>
              <td><span className="badge badge-normal badge-sm">{c.version}</span></td>
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
              <td>{item.imageUrl && <img src={item.imageUrl} alt={item.nameKo} width={32} height={32} style={{ borderRadius: 'var(--radius-sm)' }} />}</td>
              <td>{item.itemId}</td>
              <td><strong>{item.nameKo}</strong></td>
              <td className="table-number">{item.goldTotal > 0 ? <span className="badge badge-gold badge-sm">{item.goldTotal.toLocaleString()}g</span> : <span className="text-disabled">-</span>}</td>
              <td><span className="badge badge-normal badge-sm">{item.version}</span></td>
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
              <td>{s.imageUrl && <img src={s.imageUrl} alt={s.nameKo} width={32} height={32} style={{ borderRadius: 'var(--radius-sm)' }} />}</td>
              <td>{s.spellId}</td>
              <td><code>{s.spellKey}</code></td>
              <td><strong>{s.nameKo}</strong></td>
              <td><span className="badge badge-normal badge-sm">{s.version}</span></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Empty() {
  return (
    <div style={{ textAlign: 'center', padding: 'var(--spacing-xl)', color: 'var(--color-text-secondary)' }}>
      데이터가 없습니다. 동기화를 먼저 실행해주세요.
    </div>
  );
}
