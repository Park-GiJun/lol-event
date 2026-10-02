package com.gijun.main.shared.domain.exception

/**
 * 도메인 전용 예외를 만들 만큼이 아닌 공용 400. 코드는 던지는 쪽이 정한다.
 *
 * 경계에서 입력을 값 객체로 올리다 실패한 경우처럼, 특정 애그리거트의 규칙이 아닌 형식 오류에 쓴다.
 */
class InvalidRequestException(
    message: String,
    errorCode: ErrorCode,
) : ValidationException(message, errorCode)
