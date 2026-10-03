import { useQuery } from '@tanstack/react-query';
import {
	getDragonChampions,
	getDragonItems,
	getDragonRunes,
	getDragonSpells,
	type DragonChampion,
	type DragonItem,
	type DragonRune,
	type DragonSummonerSpell
} from '@/api/dragon/dragonApi';
import { SILENT_META } from '@/api/queryClient';

/** Data Dragon 조회 키. 동기화 뒤 `invalidateQueries({ queryKey: DRAGON_QUERY_KEY })` 로 통째로 버린다. */
export const DRAGON_QUERY_KEY = ['dragon'] as const;

// 아직 안 왔을 때 돌려주는 빈 표. 매번 새 Map 을 만들면 이걸 의존성으로 쓰는 쪽이 매 렌더마다 다시 돈다.
const NO_CHAMPIONS = new Map<number, DragonChampion>();
const NO_ITEMS = new Map<number, DragonItem>();
const NO_SPELLS = new Map<number, DragonSummonerSpell>();
const NO_RUNES = new Map<number, DragonRune>();

// select 는 함수 참조가 같아야 결과를 재사용한다 — 그래서 모듈 수준에 둔다.
const byChampionId = (rows: DragonChampion[]) => new Map(rows.map((c) => [c.championId, c]));
const byItemId = (rows: DragonItem[]) => new Map(rows.map((i) => [i.itemId, i]));
const bySpellId = (rows: DragonSummonerSpell[]) => new Map(rows.map((s) => [s.spellId, s]));
const byRuneId = (rows: DragonRune[]) => new Map(rows.map((r) => [r.runeId, r]));

// 패치 때만 바뀐다. 세션 동안 다시 받지 않는다.
const STATIC = { staleTime: Infinity, gcTime: Infinity, meta: SILENT_META } as const;

/**
 * Data Dragon 정적 데이터를 id 로 찾는 표 넷.
 *
 * 부가 정보다 — 못 받아도 화면은 영문 키와 빈 아이콘으로 성립한다. 그래서 실패를 알리지 않는다
 * (`SILENT_META`). 어느 화면에서 부르든 같은 캐시를 보므로 요청은 종류당 한 번만 나간다.
 */
export function useDragon() {
	const champions = useQuery({
		queryKey: [...DRAGON_QUERY_KEY, 'champions'],
		queryFn: ({ signal }) => getDragonChampions({ signal }),
		select: byChampionId,
		...STATIC
	});
	const items = useQuery({
		queryKey: [...DRAGON_QUERY_KEY, 'items'],
		queryFn: ({ signal }) => getDragonItems({ signal }),
		select: byItemId,
		...STATIC
	});
	const spells = useQuery({
		queryKey: [...DRAGON_QUERY_KEY, 'spells'],
		queryFn: ({ signal }) => getDragonSpells({ signal }),
		select: bySpellId,
		...STATIC
	});
	const runes = useQuery({
		queryKey: [...DRAGON_QUERY_KEY, 'runes'],
		queryFn: ({ signal }) => getDragonRunes({ signal }),
		select: byRuneId,
		...STATIC
	});

	return {
		champions: champions.data ?? NO_CHAMPIONS,
		items: items.data ?? NO_ITEMS,
		spells: spells.data ?? NO_SPELLS,
		runes: runes.data ?? NO_RUNES
	};
}
