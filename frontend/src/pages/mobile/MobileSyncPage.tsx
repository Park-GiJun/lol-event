import { useState, useEffect, useCallback } from 'react';
import { RefreshCw, CheckCircle } from 'lucide-react';
import { api } from '../../lib/api/api';
import type { DragonSyncResponse, DragonChampion, DragonItem, DragonSummonerSpell } from '../../lib/types/dragon';
import { LoadingCenter } from '../../components/common/Spinner';

type Tab = 'champions' | 'items' | 'spells';

export function MobileSyncPage() {
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<DragonSyncResponse | null>(null);
  const [tab, setTab] = useState<Tab>('champions');

  const [champions, setChampions] = useState<DragonChampion[]>([]);
  const [items, setItems] = useState<DragonItem[]>([]);
  const [spells, setSpells] = useState<DragonSummonerSpell[]>([]);
  const [loadingData, setLoadingData] = useState(false);

  const loadData = useCallback(async () => {
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

  useEffect(() => { loadData(); }, [loadData]);

  const handleSync = async () => {
    setSyncing(true);
    try {
      const result = await api.post<DragonSyncResponse>('/ddragon/sync', {});
      setSyncResult(result);
      await loadData();
    } finally {
      setSyncing(false);
    }
  };

  const TAB_LABELS: Record<Tab, string> = {
    champions: `챔피언 (${champions.length})`,
    items: `아이템 (${items.length})`,
    spells: `스펠 (${spells.length})`,
  };

  return (
    <div>
      {/* Sync button */}
      <button
        className="m-admin-btn m-admin-btn-primary"
        onClick={handleSync}
        disabled={syncing}
        style={{ marginBottom: 12 }}
      >
        <RefreshCw size={16} style={{ animation: syncing ? 'spin 1s linear infinite' : 'none' }} />
        {syncing ? '동기화 중...' : 'DataDragon 동기화'}
      </button>

      {/* Sync result */}
      {syncResult && (
        <div className="m-card" style={{ marginBottom: 12, borderColor: 'var(--color-win)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--color-win)', marginBottom: 8 }}>
            <CheckCircle size={16} />
            <strong style={{ fontSize: 14 }}>동기화 완료</strong>
            <span style={{ marginLeft: 'auto', fontSize: 11, color: 'var(--color-text-secondary)' }}>
              v{syncResult.version}
            </span>
          </div>
          <div className="grid-16">
            {[
              { label: '챔피언', value: syncResult.champions },
              { label: '아이템', value: syncResult.items },
              { label: '스펠', value: syncResult.spells },
            ].map(({ label, value }) => (
              <div key={label} className="col-span-5" style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 18, fontWeight: 800, color: 'var(--color-primary)' }}>{value}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{label}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab bar */}
      <div className="m-tab-bar">
        {(Object.keys(TAB_LABELS) as Tab[]).map(t => (
          <button
            key={t}
            className={`m-tab${tab === t ? ' active' : ''}`}
            onClick={() => setTab(t)}
          >
            {TAB_LABELS[t]}
          </button>
        ))}
      </div>

      {/* Content */}
      {loadingData ? (
        <LoadingCenter />
      ) : (
        <>
          {tab === 'champions' && <ChampionGrid data={champions} />}
          {tab === 'items' && <ItemGrid data={items} />}
          {tab === 'spells' && <SpellGrid data={spells} />}
        </>
      )}
    </div>
  );
}

function ChampionGrid({ data }: { data: DragonChampion[] }) {
  if (!data.length) return <Empty />;
  return (
    <div className="grid-16">
      {data.map(c => (
        <div key={c.championId} className="col-span-4" style={{ textAlign: 'center', padding: '6px 4px' }}>
          {c.imageUrl ? (
            <img src={c.imageUrl} alt={c.nameKo} width={52} height={52}
              style={{ borderRadius: 8, objectFit: 'cover' }}
              onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
            />
          ) : (
            <div style={{ width: 52, height: 52, borderRadius: 8, background: 'rgba(255,255,255,0.05)', margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 10, color: 'var(--color-text-disabled)' }}>
              {c.nameKo.slice(0, 2)}
            </div>
          )}
          <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {c.nameKo}
          </div>
        </div>
      ))}
    </div>
  );
}

function ItemGrid({ data }: { data: DragonItem[] }) {
  if (!data.length) return <Empty />;
  return (
    <div className="grid-16">
      {data.map(item => (
        <div key={item.itemId} className="col-span-4" style={{ textAlign: 'center', padding: '6px 4px' }}>
          {item.imageUrl ? (
            <img src={item.imageUrl} alt={item.nameKo} width={52} height={52}
              style={{ borderRadius: 8, objectFit: 'cover' }}
              onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
            />
          ) : (
            <div style={{ width: 52, height: 52, borderRadius: 8, background: 'rgba(255,255,255,0.05)', margin: '0 auto' }} />
          )}
          <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {item.nameKo}
          </div>
        </div>
      ))}
    </div>
  );
}

function SpellGrid({ data }: { data: DragonSummonerSpell[] }) {
  if (!data.length) return <Empty />;
  return (
    <div className="grid-16">
      {data.map(s => (
        <div key={s.spellId} className="col-span-4" style={{ textAlign: 'center', padding: '6px 4px' }}>
          {s.imageUrl ? (
            <img src={s.imageUrl} alt={s.nameKo} width={52} height={52}
              style={{ borderRadius: 8, objectFit: 'cover' }}
              onError={e => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
            />
          ) : (
            <div style={{ width: 52, height: 52, borderRadius: 8, background: 'rgba(255,255,255,0.05)', margin: '0 auto' }} />
          )}
          <div style={{ fontSize: 10, color: 'var(--color-text-secondary)', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {s.nameKo}
          </div>
        </div>
      ))}
    </div>
  );
}

function Empty() {
  return (
    <div className="m-empty">데이터가 없습니다. 동기화를 먼저 실행해주세요.</div>
  );
}
