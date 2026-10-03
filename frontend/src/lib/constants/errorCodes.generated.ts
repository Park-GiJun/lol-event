/**
 * 에러 코드 — **백엔드의 `ErrorCode` enum 에서 생성된다. 직접 고치지 않는다.**
 *
 * 정본: `backend/main-service/src/main/kotlin/com/gijun/main/shared/domain/exception/ErrorCode.kt`
 * 재생성: backend 에서 `./gradlew generateErrorCodes`
 *
 * 화면에 띄울 문구는 여기 없다 — `errorMessages.ts` 가 소유한다.
 * 아래 주석은 백엔드가 적어 둔 설명으로, 문구를 쓸 때 참고하는 값이다.
 */

export const ERROR_CODE = {
	/** 대상이 없다 */
	NOT_FOUND: 'NOT_FOUND',
	/** 지금 상태에서는 할 수 없다 */
	CONFLICT: 'CONFLICT',
	/** 요청 형식이 올바르지 않다 */
	VALIDATION_FAILED: 'VALIDATION_FAILED',
	/** 필수 파라미터가 없다 */
	MISSING_PARAMETER: 'MISSING_PARAMETER',
	/** 요청 본문을 읽을 수 없다 */
	MALFORMED_BODY: 'MALFORMED_BODY',
	/** 허용되지 않은 메서드다 */
	METHOD_NOT_ALLOWED: 'METHOD_NOT_ALLOWED',
	/** 지원하지 않는 Content-Type 이다 */
	UNSUPPORTED_MEDIA_TYPE: 'UNSUPPORTED_MEDIA_TYPE',
	/** 파라미터 형식이 올바르지 않다 */
	TYPE_MISMATCH: 'TYPE_MISMATCH',
	/** 서버 오류가 발생했다 */
	INTERNAL_ERROR: 'INTERNAL_ERROR',
	/** 접근 권한이 없다 */
	ACCESS_DENIED: 'ACCESS_DENIED',
	/** 이미 등록된 멤버다 */
	DUPLICATE_MEMBER: 'DUPLICATE_MEMBER',
	/** Riot API 키가 만료됐다 */
	RIOT_API_KEY_EXPIRED: 'RIOT_API_KEY_EXPIRED',
	/** 세션 날짜 형식이 올바르지 않다 */
	INVALID_SESSION_DATE: 'INVALID_SESSION_DATE',
	/** 식별자 형식이 올바르지 않다 */
	INVALID_IDENTIFIER: 'INVALID_IDENTIFIER',
	/** 경기 모드 값이 올바르지 않다 */
	INVALID_GAME_MODE: 'INVALID_GAME_MODE',
	/** RAG 가 꺼져 있다 */
	RAG_DISABLED: 'RAG_DISABLED',
	/** 다른 질문을 처리하는 중이다 */
	RAG_BUSY: 'RAG_BUSY',
	/** 팀 편성 조건이 올바르지 않다 */
	INVALID_TEAM_BUILD: 'INVALID_TEAM_BUILD'
} as const;

/** 백엔드가 내려보낼 수 있는 에러 코드 전부. */
export type ErrorCode = (typeof ERROR_CODE)[keyof typeof ERROR_CODE];
