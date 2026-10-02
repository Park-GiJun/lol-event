package com.gijun.main.shared.domain.exception

/**
 * 도메인 예외의 뿌리. **카테고리는 sealed 라 전수 열거가 가능하다** —
 * [com.gijun.main.shared.infrastructure.web.common.GlobalExceptionHandler] 가 개별 예외 클래스를
 * 알 필요 없이 카테고리만 매핑하고, 매핑 누락은 `GlobalExceptionHandlerMappingTest` 가 잡는다.
 *
 * ⚠️ **계층 전부가 이 한 파일에 있어야 한다**(Kotlin sealed 제약). 카테고리를 다른 파일로 옮기면
 * `sealedSubclasses` 전수 검사가 성립하지 않아 이 설계의 가치가 사라진다.
 *
 * 예전에는 `RuntimeException` 을 직접 상속한 일곱 클래스가 따로 놀았고 에러 코드는 핸들러 안의
 * 문자열 리터럴이었다. 그러면 새 예외를 만들 때마다 핸들러를 열어야 하고, 등록을 빠뜨리면 곧바로
 * catch-all 500 이 된다. 예외가 [ErrorCode] 를 직접 들고 다니게 해서 그 누락을 없앤다.
 */
sealed class DomainException(
    val errorCode: ErrorCode,
    override val message: String,
) : RuntimeException(message)

/** 404. 대상이 없다. */
open class NotFoundException(
    message: String,
    errorCode: ErrorCode = ErrorCode.NOT_FOUND,
) : DomainException(errorCode, message)

/** 409. 지금 상태에서는 할 수 없다 — 재시도로 풀릴 수 있다. */
open class ConflictException(
    message: String,
    errorCode: ErrorCode = ErrorCode.CONFLICT,
) : DomainException(errorCode, message)

/**
 * 400. 입력이 규칙에 어긋난다.
 *
 * **기본 `errorCode` 를 두지 않는다** — 400 은 사유가 여럿이라 화면이 분기해야 하므로
 * 던지는 쪽이 고유 코드를 정하게 강제한다.
 */
open class ValidationException(
    message: String,
    errorCode: ErrorCode,
) : DomainException(errorCode, message)

/** 403. 인증은 됐으나 권한이 없다. */
open class ForbiddenException(
    message: String,
    errorCode: ErrorCode,
) : DomainException(errorCode, message)

/** 401. 인증이 없거나 유효하지 않다. */
open class UnauthorizedException(
    message: String,
    errorCode: ErrorCode,
) : DomainException(errorCode, message)

/** 423. 잠겨 있다 — 다른 주체가 점유 중이다. */
open class LockedException(
    message: String,
    errorCode: ErrorCode,
) : DomainException(errorCode, message)

/** 413. 요청이 허용 크기를 넘는다. */
open class PayloadTooLargeException(
    message: String,
    errorCode: ErrorCode,
) : DomainException(errorCode, message)
