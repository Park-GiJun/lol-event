export interface DragonSyncResponse {
  version: string;
  champions: number;
  items: number;
  spells: number;
}

export interface DragonChampion {
  championId: number;
  championKey: string;
  nameKo: string;
  titleKo: string | null;
  imageUrl: string | null;
  version: string | null;
}

export interface DragonItem {
  itemId: number;
  nameKo: string;
  description: string | null;
  imageUrl: string | null;
  goldTotal: number;
  version: string | null;
}

export interface DragonSummonerSpell {
  spellId: number;
  spellKey: string;
  nameKo: string;
  description: string | null;
  imageUrl: string | null;
  version: string | null;
}
