package com.gijun.main.application.port.out.messaging

import com.gijun.main.shared.domain.vo.MatchId

/**
 * 경기 저장 뒤에 이어질 일을 알리는 이벤트.
 *
 * 예전에는 핸들러가 `KafkaTemplate` 을 직접 들고 토픽 이름을 문자열로 적었다. 응용 계층이 Kafka 를
 * 알 이유가 없고, 토픽 이름이 두 핸들러에 흩어져 있었다.
 */
interface MatchEventPublishPort {
    /** 이 경기로 레이팅을 다시 계산하라. */
    fun publishRatingRequested(matchId: MatchId)

    /** 통계 스냅샷을 다시 집계하라. 받는 쪽이 스로틀한다. */
    fun publishStatsRebuildRequested(matchId: MatchId)
}
