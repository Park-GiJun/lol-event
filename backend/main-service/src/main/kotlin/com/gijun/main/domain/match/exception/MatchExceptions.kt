package com.gijun.main.domain.match.exception

import com.gijun.main.shared.domain.exception.NotFoundException

class MatchNotFoundException(
    matchId: String,
) : NotFoundException("경기를 찾을 수 없습니다: $matchId")
