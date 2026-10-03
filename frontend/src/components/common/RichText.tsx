import { splitBold } from '@/lib/richText';

/** 모델이 쓴 글. `**굵게**` 만 풀고 줄바꿈은 그대로 살린다. */
export function RichText({ text, className }: { text: string; className?: string }) {
	return (
		<div className={`rich-text${className ? ` ${className}` : ''}`}>
			{splitBold(text).map((segment, i) =>
				segment.bold ? <strong key={i}>{segment.text}</strong> : <span key={i}>{segment.text}</span>
			)}
		</div>
	);
}
