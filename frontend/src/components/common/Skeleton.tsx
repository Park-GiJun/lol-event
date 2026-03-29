import { memo } from 'react';
import * as React from 'react';
import { cn } from '@/lib/utils';

export const Skeleton = memo(function Skeleton({ className, style, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('skeleton rounded-md', className)}
      style={style}
      aria-hidden="true"
      {...props}
    />
  );
});
