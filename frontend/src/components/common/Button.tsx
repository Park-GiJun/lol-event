import { memo } from 'react';
import { Loader2 } from 'lucide-react';
import type { ReactNode } from 'react';

interface ButtonProps {
  children: ReactNode;
  onClick?: () => void;
  type?: 'button' | 'submit';
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  className?: string;
  'aria-label'?: string;
}

export const Button = memo(function Button({
  children,
  onClick,
  type = 'button',
  variant = 'primary',
  size = 'md',
  disabled,
  loading,
  className = '',
  'aria-label': ariaLabel,
}: ButtonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      aria-label={ariaLabel}
      aria-busy={loading ? true : undefined}
      className={`btn btn-${variant} ${size !== 'md' ? `btn-${size}` : ''} ${loading ? 'btn-loading' : ''} ${className}`}
    >
      {loading && <Loader2 size={14} className="icon-spin" aria-hidden="true" />}
      {children}
    </button>
  );
});
