package com.gijun.main.domain.model.match

/**
 * 그 경기의 라인 승자를 무엇으로 판정했는지. 경기마다 저장해 두고 나중에 방법별로 성능을 비교한다.
 *
 * 원래 설계에는 `PHASE_DELTA` 가 하나 더 있었다. `participant.timeline.goldPerMinDeltas["0-10"]`
 * 처럼 LCU 가 내려주는 구간 델타를 쓰는 분기였는데, 실제 LCU 응답을 확인해 보니
 * `creepsPerMinDeltas` / `goldPerMinDeltas` / `xpPerMinDeltas` / `csDiffPerMinDeltas` /
 * `xpDiffPerMinDeltas` / `damageTakenPerMinDeltas` / `damageTakenDiffPerMinDeltas` 가
 * **전부 빈 객체** 였다 (10명 전원, 키만 있고 값이 없다). 살아 있는 건 `lane` / `role` 뿐이고
 * 그 둘은 이미 포지션 추정에 쓰고 있다.
 *
 * 값이 절대 채워지지 않는 분기를 남겨 두면 "언젠가 쓰일지도 모르는 코드"가 되어 계속 검증 대상으로
 * 남는다. 그래서 열거값째로 지웠다. 나중에 Riot 이 다시 채워 주면 그때 되살리면 된다.
 */
enum class LaneMethod {
    /** `/lol-match-history/v1/game-timelines/{gameId}` 의 15분 프레임. 신규 수집 경기. */
    TIMELINE_15,

    /** 타임라인이 없는 과거 경기. 경기 종료 시점 누적값으로 대신한다. */
    LEGACY_FINAL,
}
