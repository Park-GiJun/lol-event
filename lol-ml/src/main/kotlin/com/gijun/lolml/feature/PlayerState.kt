package com.gijun.lolml.feature

import com.gijun.lolml.data.Participant

/** 한 사람의 "지금까지" 상태. 경기가 끝날 때마다 [FeatureBuilder] 가 갱신한다. */
class PlayerState {
    var elo: Double = Elo.INITIAL
        private set
    var games: Int = 0
        private set

    /** 최근 N 경기 KDA 평균. 경기가 없으면 기본값. */
    fun recentKda(): Double = TODO()

    /** 이번 경기 결과를 반영한다. 피처를 뽑은 **다음에만** 부른다. */
    fun update(
        participant: Participant,
        teamExpected: Double,
    ): Unit = TODO()
}
