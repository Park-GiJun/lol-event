import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { api } from '../lib/api/api';
import type { DragonChampion, DragonItem, DragonSummonerSpell } from '../lib/types/dragon';

interface DragonContextValue {
  champions: Map<number, DragonChampion>;
  items:     Map<number, DragonItem>;
  spells:    Map<number, DragonSummonerSpell>;
}

const DragonContext = createContext<DragonContextValue>({
  champions: new Map(),
  items:     new Map(),
  spells:    new Map(),
});

export function DragonProvider({ children }: { children: ReactNode }) {
  const [champions, setChampions] = useState<Map<number, DragonChampion>>(new Map());
  const [items,     setItems]     = useState<Map<number, DragonItem>>(new Map());
  const [spells,    setSpells]    = useState<Map<number, DragonSummonerSpell>>(new Map());

  useEffect(() => {
    api.get<DragonChampion[]>('/ddragon/champions')
      .then(data => setChampions(new Map(data.map(c => [c.championId, c]))))
      .catch(() => {});
    api.get<DragonItem[]>('/ddragon/items')
      .then(data => setItems(new Map(data.map(i => [i.itemId, i]))))
      .catch(() => {});
    api.get<DragonSummonerSpell[]>('/ddragon/spells')
      .then(data => setSpells(new Map(data.map(s => [s.spellId, s]))))
      .catch(() => {});
  }, []);

  return (
    <DragonContext.Provider value={{ champions, items, spells }}>
      {children}
    </DragonContext.Provider>
  );
}

export const useDragon = () => useContext(DragonContext);
