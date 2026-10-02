package com.gijun.main.shared.infrastructure.web.common

import com.gijun.main.shared.domain.exception.ErrorCode
import java.time.Instant

/**
 * 모든 REST 응답의 봉투. 웹 프론트엔드와 데스크탑 수집기가 같은 파서로 읽는다.
 *
 * 팩토리는 [success] · [error] **둘뿐이다.** 늘리지 않는다.
 *
 * `errorCode` 필드가 [ErrorCode] 가 아니라 `String?` 인 이유는 **와이어 포맷이기 때문**이다 —
 * 타입 안전은 [error] 팩토리가 [ErrorCode] 만 받는 것으로 확보하고, 직렬화는 이름 문자열로
 * 고정한다(Jackson 설정이 바뀌어도 계약이 흔들리지 않는다).
 */
data class CommonApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val timestamp: Instant = Instant.now(),
) {
    companion object {
        fun <T> success(data: T) = CommonApiResponse(success = true, data = data)

        fun error(
            errorCode: ErrorCode,
            message: String,
        ) = CommonApiResponse<Nothing>(
            success = false,
            errorCode = errorCode.name,
            message = message,
        )
    }
}
