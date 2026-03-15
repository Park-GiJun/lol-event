import { AlertCircle } from 'lucide-react';
import { Modal } from './Modal';
import { Button } from './Button';

interface ErrorModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  onClose: () => void;
}

export function ErrorModal({ isOpen, title, message, onClose }: ErrorModalProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} size="sm"
      footer={<Button onClick={onClose}>확인</Button>}>
      <div className="flex items-center gap-md" style={{ color: 'var(--color-error)' }}>
        <AlertCircle size={20} />
        <span>{message}</span>
      </div>
    </Modal>
  );
}
