package com.gijun.main.domain.service

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * "이 경기는 어느 세션인가"를 정하는 시계.
 *
 * 이 저장소에는 **세션 정의가 두 개** 있고, 둘은 서로 다른 질문에 답한다. 하나로 합치면
 * 한쪽이 틀린다. 그래서 한 파일에 나란히 두고 왜 둘인지 여기 적는다.
 *
 * - [sessionDate] — 사람이 읽고 URL 에 실을 **라벨**이 필요해서 있다 (`/sessions/2026-09-14`).
 *   하루에 몇 판을 했든 그날 것은 한 묶음이다.
 * - [SESSION_GAP_MS] — [RatingValidationHandler][com.gijun.main.application.handler.query.RatingValidationHandler]
 *   가 "직전 경기와 끊겼는가"로 예측 누출을 막는 데 쓴다. 세션 안에서는 같은 편성이 반복되기
 *   쉬워서, 하루에 두 번 모인 날을 날짜로 합치면 그게 바로 막으려던 누출이 된다.
 */
object SessionClock {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    /**
     * 하루의 시작. **자정이 아니라 오전 6시다.**
     *
     * 내전은 밤에 한다. 자정으로 자르면 23:40 에 시작한 판과 00:20 에 끝난 판이 다른 날 세션으로
     * 갈라진다 — 같은 자리에서 연달아 한 판인데. 실제로 수집된 경기의 시작 시각 분포가
     * 21시 43판 / 22시 87판 / 23시 54판 / **00시 14판** 이라, 자정 기준에서는 그 14판이
     * 다음 날 세션으로 떨어져 있었다.
     *
     * 경계 예시:
     * ```
     * 2026-09-15 02:00 → 2026-09-14
     * 2026-09-15 05:59 → 2026-09-14
     * 2026-09-15 06:00 → 2026-09-15
     * ```
     *
     * 정오까지 이어지는 극단적인 경우는 여전히 갈리지만, 그건 다른 날로 보는 게 맞다.
     */
    const val DAY_START_HOUR = 6L

    /** 이 간격 이상 끊기면 새 세션. [SessionClock] KDoc 의 "정의가 두 개"를 보라. */
    const val SESSION_GAP_MS = 6L * 60 * 60 * 1000

    /** 이 경기가 속한 세션의 날짜. */
    fun sessionDate(gameCreationMs: Long): LocalDate =
        Instant
            .ofEpochMilli(gameCreationMs)
            .atZone(ZONE)
            .minusHours(DAY_START_HOUR)
            .toLocalDate()

    /**
     * [date] 세션의 `[시작, 끝)` epoch ms. 기간 조회 쿼리에 그대로 넣는다.
     *
     * 끝이 열린 구간인 것이 중요하다 — 닫으면 다음 세션의 첫 경기가 양쪽에 들어간다.
     */
    fun rangeMs(date: LocalDate): Pair<Long, Long> {
        val from =
            date
                .atStartOfDay(ZONE)
                .plusHours(DAY_START_HOUR)
                .toInstant()
                .toEpochMilli()
        val until =
            date
                .plusDays(1)
                .atStartOfDay(ZONE)
                .plusHours(DAY_START_HOUR)
                .toInstant()
                .toEpochMilli()
        return from to until
    }
}
