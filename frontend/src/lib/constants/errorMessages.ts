/**
 * 에러 코드별 화면 문구 — **화면에 띄우는 글이므로 프론트가 소유한다.**
 *
 * 백엔드도 메시지를 같이 내려보내지만 그건 서버 로그가 읽는 문체다("…다."). 코드가 여기 있으면
 * 이 문구를, 없으면 백엔드 문구를 쓴다.
 *
 * ⚠️ **`Record<ErrorCode, string>` 이라 코드를 빠뜨리면 `pnpm check` 가 red 다.** 백엔드가 enum 에
 * 코드를 추가하고 생성기를 돌리면, 여기 문구를 채우기 전까지 타입이 안 맞는다. 일원화가 실제로
 * 굴러가는 자리가 이 한 줄이므로 `Partial<>` 로 풀지 않는다.
 *
 * 문구는 사용자가 **다음에 무엇을 할지** 알 수 있게 쓴다. 코드명을 그대로 옮기지 않는다.
 */

import { ERROR_CODE, type AnyErrorCode, type ErrorCode } from '@/lib/constants/errorCodes';

export const ERROR_MESSAGE: Record<ErrorCode, string> = {
	[ERROR_CODE.NOT_FOUND]: '찾는 내용이 없습니다.',
	[ERROR_CODE.CONFLICT]: '지금은 처리할 수 없습니다. 새로 고친 뒤 다시 시도해 주세요.',
	[ERROR_CODE.VALIDATION_FAILED]: '입력한 내용을 다시 확인해 주세요.',
	[ERROR_CODE.MISSING_PARAMETER]: '필요한 값이 빠졌습니다.',
	[ERROR_CODE.MALFORMED_BODY]: '요청을 읽지 못했습니다. 새로 고친 뒤 다시 시도해 주세요.',
	[ERROR_CODE.METHOD_NOT_ALLOWED]: '여기서는 쓸 수 없는 동작입니다.',
	[ERROR_CODE.UNSUPPORTED_MEDIA_TYPE]: '지원하지 않는 형식입니다.',
	[ERROR_CODE.TYPE_MISMATCH]: '값의 형식이 맞지 않습니다.',
	[ERROR_CODE.INTERNAL_ERROR]: '서버에 문제가 생겼습니다. 잠시 뒤 다시 시도해 주세요.',
	[ERROR_CODE.ACCESS_DENIED]: '권한이 없습니다.',
	[ERROR_CODE.DUPLICATE_MEMBER]: '이미 등록된 멤버입니다.',
	[ERROR_CODE.RIOT_API_KEY_EXPIRED]: 'Riot API 키가 만료됐습니다. 키를 갱신해 주세요.',
	[ERROR_CODE.INVALID_SESSION_DATE]: '세션 날짜 형식이 올바르지 않습니다.',
	[ERROR_CODE.INVALID_IDENTIFIER]: '주소가 올바르지 않습니다.',
	[ERROR_CODE.INVALID_GAME_MODE]: '경기 모드 값이 올바르지 않습니다.',
	[ERROR_CODE.RAG_DISABLED]: '질문 기능이 지금 꺼져 있습니다.'
};

/**
 * 코드에 대응하는 문구. 모르는 코드면 `fallback`(보통 백엔드가 보낸 문구)을 돌려준다.
 *
 * 백엔드가 먼저 배포되어 화면이 모르는 코드가 오는 구간이 실제로 있다 — 그때 빈 알림이 뜨는 것보다
 * 서버 문구라도 뜨는 편이 낫다.
 */
export function errorMessageOf(code: AnyErrorCode | undefined, fallback: string): string {
	if (!code) return fallback;
	return ERROR_MESSAGE[code as ErrorCode] ?? fallback;
}
