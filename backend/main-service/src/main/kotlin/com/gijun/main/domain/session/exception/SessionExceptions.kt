package com.gijun.main.domain.session.exception

import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.NotFoundException
import com.gijun.main.shared.domain.exception.ValidationException

/** 그 날짜에 경기가 한 판도 없다. 날짜 형식은 맞다. */
class SessionNotFoundException(
    date: String,
) : NotFoundException("그 날짜에 경기가 없습니다: $date")

class InvalidSessionDateException(
    date: String,
) : ValidationException("세션 날짜는 yyyy-MM-dd 형식이어야 합니다: $date", ErrorCode.INVALID_SESSION_DATE)
