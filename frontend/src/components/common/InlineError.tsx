import { memo } from 'react';
import { AlertCircle } from 'lucide-react';
import { Button } from './Button';

export interface InlineErrorProps {
  message: string;
  onRetry: () => void;
  className?: string;
}

export const InlineError = memo(function InlineError({ message, onRetry, className = '' }: InlineErrorProps) {
  return (
    <div
      className={`flex items-center gap-sm ${className}`}
      style={{
        color: 'var(--color-error)',
        padding: 'var(--spacing-md)',
        fontSize: 'var(--font-size-sm)',
      }}
      role="alert"
    >
      <AlertCircle size={16} aria-hidden="true" style={{ flexShrink: 0 }} />
      <span style={{ flex: 1 }}>{message}</span>
      <Button variant="ghost" size="sm" onClick={onRetry} aria-label="다시 시도">
        다시 시도
      </Button>
    </div>
  );
});
