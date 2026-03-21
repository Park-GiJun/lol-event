import { AlertCircle } from 'lucide-react';
import { Button } from './Button';

export interface InlineErrorProps {
  message: string;
  onRetry: () => void;
  className?: string;
}

export function InlineError({ message, onRetry, className = '' }: InlineErrorProps) {
  return (
    <div
      className={`flex items-center gap-sm ${className}`}
      style={{ color: 'var(--color-error)', padding: 'var(--spacing-md)' }}
    >
      <AlertCircle size={16} />
      <span className="text-sm">{message}</span>
      <Button variant="secondary" size="sm" onClick={onRetry}>
        다시 시도
      </Button>
    </div>
  );
}
