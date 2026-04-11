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
      <div className="error-alert" role="alert">
        <AlertCircle size={20} aria-hidden="true" className="error-alert-icon" />
        <span className="error-alert-message">{message}</span>
      </div>
    </Modal>
  );
});
