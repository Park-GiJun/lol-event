import { Skeleton } from '@/components/common/Skeleton';

export function ChartSkeleton() {
  return (
    <div style={{ marginBottom: 16 }}>
      <Skeleton className="h-[220px] w-full rounded-lg" />
    </div>
  );
}
