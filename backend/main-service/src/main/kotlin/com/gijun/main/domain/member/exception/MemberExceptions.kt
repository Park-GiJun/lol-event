package com.gijun.main.domain.member.exception

import com.gijun.main.shared.domain.exception.ConflictException
import com.gijun.main.shared.domain.exception.ErrorCode

/** 같은 PUUID 가 이미 등록돼 있다. Riot ID 를 바꾼 사람도 PUUID 는 같으므로 여기 걸린다. */
class MemberAlreadyExistsException(
    riotId: String,
) : ConflictException("이미 등록된 멤버입니다: $riotId", ErrorCode.DUPLICATE_MEMBER)
