/**
 * 에러 코드 — **백엔드 코드를 한 자리에서 내보낸다.**
 *
 * 목록(`ERROR_CODE`)은 `errorCodes.generated.ts` 가 소유하고, 그 파일은 백엔드의 `ErrorCode` enum
 * 에서 생성된다. 화면은 여기서 가져다 쓴다.
 *
 * **문자열 리터럴로 비교하지 않는다.** `error.errorCode === 'DUPLICATE_MEMBR'` 같은 오타는 런타임에
 * 조용히 false 가 되어 분기가 통째로 죽는다. 상수를 쓰면 컴파일 단계에서 걸린다.
 */

import type { ErrorCode } from '@/lib/constants/errorCodes.generated';

export { ERROR_CODE, type ErrorCode } from '@/lib/constants/errorCodes.generated';

/**
 * `ApiError` 가 실어 나르는 코드의 타입.
 *
 * `(string & {})` 를 섞는 이유는 **백엔드가 먼저 배포됐을 때를 버티기 위함**이다. 아직 화면이 모르는
 * 코드가 내려와도 타입이 깨지지 않으면서, 아는 코드는 자동완성에 그대로 뜬다.
 */
export type AnyErrorCode = ErrorCode | (string & {});
