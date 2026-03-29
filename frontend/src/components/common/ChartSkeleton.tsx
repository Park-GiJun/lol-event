import { memo } from 'react';
import { Skeleton } from '@/components/common/Skeleton';

export const ChartSkeleton = memo(function ChartSkeleton() {
  return (
    <div style={{ marginBottom: 16 }} role="status" aria-label="차트 로딩 중">
      <Skeleton className="h-[220px] w-full rounded-lg" />
    </div>
  );
});
