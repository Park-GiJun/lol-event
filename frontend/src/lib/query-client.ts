import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,  // 5분 — 통계 데이터는 게임 싱크 후 빈번한 재요청 불필요
      gcTime: 30 * 60 * 1000,    // 30분 — 리더보드/챔피언/플레이어/매치 전체 동일 적용
    },
  },
});
