package com.gijun.main.application.port.out.batch

/** 통계 스냅샷 집계 잡. 구현은 Spring Batch 다. */
interface StatsAggregationJobPort {
    /**
     * 전체 집계 잡을 시작한다. **실패해도 던지지 않는다** — 정기 실행·Kafka 신호·수동 실행이 같이
     * 쓰는 경로라, 여기서 예외가 새면 호출자마다 따로 삼켜야 한다. 실패는 구현이 로그로 남긴다.
     */
    fun launch(reason: String)

    /** 챔피언 아이템 통계만 그 자리에서 다시 집계한다. 끝날 때까지 돌아오지 않는다. */
    fun aggregateChampionItemStats()
}
