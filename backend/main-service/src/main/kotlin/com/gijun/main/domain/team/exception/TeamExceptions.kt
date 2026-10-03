package com.gijun.main.domain.team.exception

import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.ValidationException

/** 인원·묶음·포지션 조건이 서로 맞지 않아 편성을 시작할 수 없다. 메시지가 무엇이 틀렸는지 말한다. */
class InvalidTeamBuildRequestException(
    message: String,
) : ValidationException(message, ErrorCode.INVALID_TEAM_BUILD)
