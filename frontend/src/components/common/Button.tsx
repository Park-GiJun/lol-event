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
}

export function Button({ children, onClick, type = 'button', variant = 'primary', size = 'md', disabled, loading, className = '' }: ButtonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      className={`btn btn-${variant} ${size !== 'md' ? `btn-${size}` : ''} ${loading ? 'btn-loading' : ''} ${className}`}
    >
      {loading && <Loader2 size={14} className="icon-spin" />}
      {children}
    </button>
  );
}
