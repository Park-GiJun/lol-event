import { memo } from 'react';

export const Spinner = memo(function Spinner({ size = 'md' }: { size?: 'sm' | 'md' | 'lg' }) {
  return (
    <span
      className={`spinner spinner-${size}`}
      role="status"
      aria-label="로딩 중"
    />
  );
});

export const LoadingCenter = memo(function LoadingCenter() {
  return (
    <div className="loading-center">
      <Spinner size="lg" />
    </div>
  );
});
