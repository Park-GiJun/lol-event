package com.gijun.main.application.port.`in`

interface DescribePlayerUseCase {
    /**
     * 플레이어 한 명의 기록을 글로. 지금 통계로 그 자리에서 쓴다(저장된 문서가 아니다).
     *
     * @param name Riot ID 전체나 '#' 앞부분. 못 찾으면 그 사실을 말하는 글을 돌려준다.
     */
    fun describePlayer(name: String): String
}

interface DescribeChampionUseCase {
    /** @param name 한글 이름이나 영문 키. */
    fun describeChampion(name: String): String

    /** 같은 팀이었던 챔피언별 성적을 글로. */
    fun describeChampionAllies(name: String): String
}
