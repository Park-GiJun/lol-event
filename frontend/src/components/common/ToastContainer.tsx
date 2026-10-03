import { useToastStore } from '@/stores/toastStore';

/**
 * 전역 토스트. 앱 루트에 **한 번만** 마운트하고, 띄울 때는 `showToast()`(`@/stores/toastStore`)를 부른다.
 * 모양은 `styles/components/feedback.css` 의 `.toast-*` 가 소유한다.
 */
export function ToastContainer() {
	const toasts = useToastStore((s) => s.toasts);

	return (
		<div className="toast-stack" aria-live="polite" aria-atomic="true">
			{toasts.map((toast) => (
				<div key={toast.id} className={`toast toast-${toast.type}`} role="status">
					{toast.message}
				</div>
			))}
		</div>
	);
}
