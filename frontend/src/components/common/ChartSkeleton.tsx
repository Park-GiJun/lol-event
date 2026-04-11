import { memo } from 'react';
import { Skeleton } from '@/components/common/Skeleton';

export const ChartSkeleton = memo(function ChartSkeleton() {
  return (
    <div style={{ marginBottom: 'var(--spacing-md)' }} role="status" aria-label="차트 로딩 중">
      <Skeleton variant="card" style={{ height: 220, width: '100%' }} />
    </div>
  );
});
