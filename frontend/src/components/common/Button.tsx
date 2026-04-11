import { memo } from 'react';
import { Loader2 } from 'lucide-react';
import type { ReactNode } from 'react';

interface ButtonProps {
  children: ReactNode;
  onClick?: () => void;
  type?: 'button' | 'submit';
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'xs' | 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  className?: string;
  icon?: boolean;
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
  icon,
  'aria-label': ariaLabel,
}: ButtonProps) {
  const sizeClass = size !== 'md' ? `btn-${size}` : '';
  const iconClass = icon ? 'btn-icon' : '';
  const loadingClass = loading ? 'btn-loading' : '';

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      aria-label={ariaLabel}
      aria-busy={loading ? true : undefined}
      className={`btn btn-${variant} ${sizeClass} ${iconClass} ${loadingClass} ${className}`.trim().replace(/\s+/g, ' ')}
    >
      {loading && <Loader2 size={14} className="icon-spin" aria-hidden="true" />}
      {children}
    </button>
  );
});
