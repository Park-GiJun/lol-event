package com.gijun.main.shared.infrastructure.web.common

import com.gijun.main.shared.domain.exception.ConflictException
import com.gijun.main.shared.domain.exception.DomainException
import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.ForbiddenException
import com.gijun.main.shared.domain.exception.LockedException
import com.gijun.main.shared.domain.exception.NotFoundException
import com.gijun.main.shared.domain.exception.PayloadTooLargeException
import com.gijun.main.shared.domain.exception.UnauthorizedException
import com.gijun.main.shared.domain.exception.ValidationException
import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 예외를 [CommonApiResponse] 봉투로 통일하고 `app_exceptions_total` 로 계량한다.
 *
 * **개별 예외 클래스를 알지 않는다** — [DomainException] 의 카테고리 7 개와 프레임워크 예외만
 * 매핑한다. 새 도메인 예외는 카테고리를 상속하기만 하면 되고, 8 번째 카테고리를 추가하면
 * `GlobalExceptionHandlerMappingTest` 가 red 가 되어 매핑 누락을 알린다.
 */
@RestControllerAdvice
class GlobalExceptionHandler(
    private val meterRegistry: MeterRegistry,
) {
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(
        e: NotFoundException,
        request: HttpServletRequest,
    ) = domain(e, HttpStatus.NOT_FOUND, request)

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        e: ConflictException,
        request: HttpServletRequest,
    ) = domain(e, HttpStatus.CONFLICT, request)

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(
        e: ValidationException,
        request: HttpServletRequest,
    ) = domain(e, HttpStatus.BAD_REQUEST, request)

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(
        e: ForbiddenException,
        request: HttpServletRequest,
    ) = domain(e, HttpStatus.FORBIDDEN, request)

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(
        e: UnauthorizedException,
        request: HttpServletRequest,
    ) = domain(e, HttpStatus.UNAUTHORIZED, request)

    @ExceptionHandler(LockedException::class)
    fun handleLocked(
        e: LockedException,
        request: HttpServletRequest,
    ) = domain(e, HttpStatus.LOCKED, request)

    @ExceptionHandler(PayloadTooLargeException::class)
    fun handlePayloadTooLarge(
        e: PayloadTooLargeException,
        request: HttpServletRequest,
    ) = domain(e, HttpStatus.PAYLOAD_TOO_LARGE, request)

    /** Spring Security 가 던진다. 도메인 예외가 아니므로 코드가 고정이다. */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        e: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, SEVERITY_WARN, request)
        return respond(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "접근 권한이 없다.")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleBeanValidation(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, SEVERITY_WARN, request)
        val message =
            e.bindingResult.fieldErrors
                .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
                .ifBlank { "요청 형식이 올바르지 않다." }
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message)
    }

    /**
     * 필수 파라미터 누락. 예전에는 이 핸들러가 없어 catch-all 이 삼켰고, `/api/stats/lane` 을
     * `lane` 없이 부르면 400 이 아니라 500 이 났다.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(
        e: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, SEVERITY_WARN, request)
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.MISSING_PARAMETER, "필수 파라미터가 없다 — ${e.parameterName}")
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedBody(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, SEVERITY_WARN, request)
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_BODY, "요청 본문을 읽을 수 없다.")
    }

    /**
     * 허용되지 않은 메서드. [NoResourceFoundException] 과 같은 이유로 따로 잡는다 —
     * catch-all 이 없으면 Spring 이 405 를 내지만, 있으면 그것이 먼저 삼켜 500 이 된다.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(
        e: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, SEVERITY_WARN, request)
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED, "허용되지 않은 메서드다 — ${e.method}")
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(
        e: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, SEVERITY_WARN, request)
        return respond(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            "지원하지 않는 Content-Type 이다 — ${e.contentType}",
        )
    }

    /**
     * 파라미터 타입 불일치(예: 숫자 자리에 문자). 호출자 잘못이므로 400 이다.
     *
     * 변환기가 [DomainException] 을 던진 경우에는 그 예외의 코드와 문구를 쓴다. Spring 이 변환
     * 실패를 한 겹 싸서 올리므로 원인 사슬에서 꺼내야 한다 — 그대로 두면 `mode=arm` 같은 오타가
     * "파라미터 형식이 올바르지 않다" 로만 나가고 어떤 값이 되는지 알 수 없다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        e: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        val domainCause =
            generateSequence<Throwable>(e) { it.cause }
                .take(MAX_CAUSE_DEPTH)
                .filterIsInstance<DomainException>()
                .firstOrNull()
        if (domainCause != null) return domain(domainCause, HttpStatus.BAD_REQUEST, request)

        report(e, SEVERITY_WARN, request)
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.TYPE_MISMATCH, "파라미터 형식이 올바르지 않다 — ${e.name}")
    }

    /**
     * 없는 경로. **catch-all 이 있으면 이것이 500 으로 떨어진다** — Spring 이 정적 리소스를 찾다
     * 실패해 던지는 예외라 `@ExceptionHandler(Exception::class)` 가 먼저 삼킨다. 오타 난 URL 이
     * 서버 장애로 보고되는 자리라 따로 잡는다.
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(
        e: NoResourceFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, SEVERITY_WARN, request)
        return respond(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "없는 경로다 — ${request.requestURI}")
    }

    /** 500 은 상세를 클라이언트에 노출하지 않는다 — 스택도 원문 메시지도 서버 로그에만 남긴다. */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, SEVERITY_ERROR, request)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "서버 오류가 발생했다.")
    }

    private fun domain(
        e: DomainException,
        status: HttpStatus,
        request: HttpServletRequest,
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        report(e, if (status.is5xxServerError) SEVERITY_ERROR else SEVERITY_WARN, request)
        return respond(status, e.errorCode, e.message)
    }

    private fun respond(
        status: HttpStatus,
        errorCode: ErrorCode,
        message: String,
    ): ResponseEntity<CommonApiResponse<Nothing>> = ResponseEntity.status(status).body(CommonApiResponse.error(errorCode, message))

    private fun report(
        e: Throwable,
        severity: String,
        request: HttpServletRequest,
    ) {
        val method = request.method ?: "?"
        val uri = request.requestURI ?: "unknown"
        if (severity == SEVERITY_ERROR) {
            log.error("처리되지 않은 예외 — {} {}", method, uri, e)
        } else {
            log.warn("요청 오류 {} — {} {}: {}", e.javaClass.simpleName, method, uri, e.message)
        }
        meterRegistry
            .counter("app_exceptions_total", "type", e.javaClass.simpleName, "severity", severity)
            .increment()
    }

    private companion object {
        private const val SEVERITY_WARN = "warn"
        private const val SEVERITY_ERROR = "error"

        /** 원인 사슬을 거슬러 올라갈 상한. 예외가 자기 자신을 원인으로 가리키는 경우에도 멈춘다. */
        private const val MAX_CAUSE_DEPTH = 8
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
