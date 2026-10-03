/** Data Dragon 정적 데이터(챔피언 · 아이템 · 룬 · 스펠). 패치 때만 바뀐다. */

import type { AxiosRequestConfig } from 'axios';
import { api } from '@/api/client';

export function getDragonChampions(config?: AxiosRequestConfig) {
	return api.get<DragonChampion[]>('/api/ddragon/champions', config);
}

export function getDragonItems(config?: AxiosRequestConfig) {
	return api.get<DragonItem[]>('/api/ddragon/items', config);
}

export function getDragonRunes(config?: AxiosRequestConfig) {
	return api.get<DragonRune[]>('/api/ddragon/runes', config);
}

export function getDragonSpells(config?: AxiosRequestConfig) {
	return api.get<DragonSummonerSpell[]>('/api/ddragon/spells', config);
}

/** 최신 버전을 받아 DB 와 서버 캐시를 갈아 끼운다. */
export function syncDataDragon() {
	return api.post<DragonSyncResponse>('/api/ddragon/sync');
}

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
