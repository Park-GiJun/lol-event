package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.DescribePickQuery

interface DescribePlayerUseCase {
    /**
     * 플레이어 한 명의 기록을 글로. 지금 통계로 그 자리에서 쓴다(저장된 문서가 아니다).
     *
     * @param name Riot ID 전체나 '#' 앞부분. 못 찾으면 그 사실을 말하는 글을 돌려준다.
     */
    fun describePlayer(name: String): String
}

interface DescribeLaneChampionsUseCase {
    /**
     * 한 라인에서 성적이 좋은 챔피언 순위를 글로.
     *
     * @param position 사람이 쓰는 대로 받는다 — "미드", "mid", "MID". 모르는 라인이면 그 사실을 말하는 글을 돌려준다.
     */
    fun describeLaneChampions(position: String): String
}

interface DescribePickUseCase {
    /**
     * 아군 챔피언이 정해졌을 때 한 라인에서 고를 챔피언을 글로.
     * 못 알아본 라인이나 챔피언 이름은 그 사실을 글에 적어 돌려준다.
     */
    fun describePick(query: DescribePickQuery): String
}

interface DescribeChampionUseCase {
    /** @param name 한글 이름이나 영문 키. */
    fun describeChampion(name: String): String

    /** 같은 팀이었던 챔피언별 성적을 글로. */
    fun describeChampionAllies(name: String): String
}
