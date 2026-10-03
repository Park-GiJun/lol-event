/**
 * 모델이 쓰는 `**굵게**` 만 풀어 조각으로 나눈다.
 *
 * 마크다운 전체를 그리지 않는다. 답은 짧은 문장과 목록이고, 줄바꿈은 CSS(`white-space: pre-wrap`)가
 * 처리한다. HTML 로 바꿔 넣지 않고 조각을 돌려주는 이유는 모델이 쓴 글을 `innerHTML` 로 넣지 않기 위함이다.
 */
export interface RichSegment {
	text: string;
	bold: boolean;
}

export function splitBold(text: string): RichSegment[] {
	const segments: RichSegment[] = [];
	const pattern = /\*\*(.+?)\*\*/g;
	let last = 0;
	for (const match of text.matchAll(pattern)) {
		if (match.index > last) segments.push({ text: text.slice(last, match.index), bold: false });
		segments.push({ text: match[1], bold: true });
		last = match.index + match[0].length;
	}
	if (last < text.length) segments.push({ text: text.slice(last), bold: false });
	return segments;
}
