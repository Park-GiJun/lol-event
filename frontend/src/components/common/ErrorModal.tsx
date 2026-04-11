import { memo } from 'react';
import { AlertCircle } from 'lucide-react';
import { Modal } from './Modal';
import { Button } from './Button';

interface ErrorModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  onClose: () => void;
}

export const ErrorModal = memo(function ErrorModal({ isOpen, title, message, onClose }: ErrorModalProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} size="sm"
      footer={<Button variant="danger" onClick={onClose} aria-label="오류 확인 후 닫기">확인</Button>}>
      <div
        className="flex items-center gap-md"
        style={{
          color: 'var(--color-error)',
          padding: 'var(--spacing-sm)',
          background: 'rgba(239, 68, 68, 0.06)',
          border: '1px solid rgba(239, 68, 68, 0.15)',
          borderRadius: 'var(--radius-md)',
        }}
        role="alert"
      >
        <AlertCircle size={20} aria-hidden="true" style={{ flexShrink: 0 }} />
        <span style={{ fontSize: 'var(--font-size-sm)', lineHeight: 'var(--line-height-normal)' }}>{message}</span>
      </div>
    </Modal>
  );
});
