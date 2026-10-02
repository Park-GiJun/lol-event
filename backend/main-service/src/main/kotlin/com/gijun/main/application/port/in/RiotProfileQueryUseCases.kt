package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.result.RiotProfileResult
import com.gijun.main.shared.domain.vo.RiotId

interface GetRiotProfileUseCase {
    /**
     * 랭크와 숙련도. Riot API 가 실패한 항목은 비운 채로 돌려준다 — 랭크 조회가 실패했다고
     * 숙련도까지 버리지 않는다.
     */
    fun getRiotProfile(riotId: RiotId): RiotProfileResult
}

interface GetRiotProfilesUseCase {
    /** 여러 명을 한 번에. 조회에 실패한 사람은 결과에서 빠진다. 상한은 구현이 정한다. */
    fun getRiotProfiles(riotIds: List<String>): Map<String, RiotProfileResult>
}
