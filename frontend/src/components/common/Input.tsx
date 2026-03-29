import { memo, useId } from 'react';

interface InputProps {
  label?: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  error?: string;
  type?: string;
  disabled?: boolean;
  className?: string;
}

export const Input = memo(function Input({
  label,
  value,
  onChange,
  placeholder,
  error,
  type = 'text',
  disabled,
  className = '',
}: InputProps) {
  const inputId = useId();
  const errorId = useId();

  return (
    <div className={`input-wrapper ${className}`}>
      {label && (
        <label htmlFor={inputId} className="input-label">
          {label}
        </label>
      )}
      <input
        id={inputId}
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        aria-describedby={error ? errorId : undefined}
        aria-invalid={error ? true : undefined}
        className={`input ${error ? 'input-error' : ''}`}
      />
      {error && (
        <span id={errorId} className="input-error-msg" role="alert">
          {error}
        </span>
      )}
    </div>
  );
});

interface TextareaProps {
  label?: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  rows?: number;
}

export const Textarea = memo(function Textarea({
  label,
  value,
  onChange,
  placeholder,
  rows = 4,
}: TextareaProps) {
  const textareaId = useId();

  return (
    <div className="input-wrapper">
      {label && (
        <label htmlFor={textareaId} className="input-label">
          {label}
        </label>
      )}
      <textarea
        id={textareaId}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        rows={rows}
        className="input textarea"
      />
    </div>
  );
});
