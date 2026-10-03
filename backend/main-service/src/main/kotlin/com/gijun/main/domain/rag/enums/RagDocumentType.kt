package com.gijun.main.domain.rag.enums

/**
 * 검색 대상 문서의 종류. DB 에는 이름 그대로 들어간다 — 이름을 바꾸면 이미 쌓인 문서가 고아가 된다.
 *
 * 새 종류를 만들면 여기에 추가한다. 종류마다 `sourceKey` 가 무엇인지 적어 둔다.
 */
enum class RagDocumentType {
    /** 플레이어 성향 요약. `sourceKey` 는 riotId. */
    PLAYER_PROFILE,

    /** 챔피언 요약 — 누가 하고, 어느 라인에서, 누구에게 강하고 약한지. `sourceKey` 는 챔피언 영문 키. */
    CHAMPION_PROFILE,

    /** 경기 리뷰. `sourceKey` 는 matchId. */
    MATCH_REVIEW,
}
