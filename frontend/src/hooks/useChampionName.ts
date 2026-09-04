import { useDragon } from '@/context/DragonContext';

/** 챔피언 한글명. DataDragon 이 아직 안 왔으면 영문 키로 대체한다. */
export function useChampionName(championId: number, fallback: string) {
  const { champions } = useDragon();
  return champions.get(championId)?.nameKo ?? fallback;
}
