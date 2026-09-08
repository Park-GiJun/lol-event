export interface DragonSyncResponse {
  version: string;
  champions: number;
  items: number;
  spells: number;
  runes: number;
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

export interface DragonRune {
  runeId: number;
  runeKey: string;
  nameKo: string;
  description: string | null;
  imageUrl: string | null;
  /** 소속 계열 id. 계열 자신은 runeId 와 같다. */
  styleId: number;
  styleNameKo: string | null;
  /** 계열 안 줄 번호. 0 = 핵심 룬(키스톤), -1 = 계열 자신. */
  slot: number;
  version: string | null;
}
