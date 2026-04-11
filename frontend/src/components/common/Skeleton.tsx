import { memo } from 'react';
import * as React from 'react';
import { cn } from '@/lib/utils';

interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'text' | 'title' | 'card' | 'avatar';
}

export const Skeleton = memo(function Skeleton({ className, style, variant, ...props }: SkeletonProps) {
  const variantClass = variant ? `skeleton-${variant}` : '';
  return (
    <div
      className={cn('skeleton', variantClass, className)}
      style={style}
      aria-hidden="true"
      {...props}
    />
  );
});
