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
      className={`inline-error ${className}`.trim()}
      role="alert"
    >
      <AlertCircle size={16} aria-hidden="true" className="inline-error-icon" />
      <span className="inline-error-message">{message}</span>
      <Button variant="ghost" size="sm" onClick={onRetry} aria-label="다시 시도">
        다시 시도
      </Button>
    </div>
  );
});
