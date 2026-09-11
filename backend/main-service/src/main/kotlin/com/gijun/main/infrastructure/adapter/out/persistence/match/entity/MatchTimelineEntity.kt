package com.gijun.main.infrastructure.adapter.out.persistence.match.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * `game-timelines` 원본을 담는 곁테이블.
 *
 * `matches` 에 컬럼으로 붙이지 않은 이유가 있다. 경기당 60KB 급이라 목록·통계 쿼리가 매번
 * 끌고 오면 그것만으로 응답이 무거워진다. Hibernate 의 `@Basic(fetch = LAZY)` 는 바이트코드
 * 인핸스먼트가 켜져 있어야 실제로 지연되는데 기본값은 꺼져 있어서, 컬럼으로 두면 사실상 항상
 * 즉시 로딩된다. 테이블을 나누면 조인하지 않는 한 절대 딸려 오지 않는다.
 *
 * 원본은 **가공하지 않고** 그대로 넣는다. 해석은 [com.gijun.main.domain.service.TimelineParser] 담당이다.
 */
@Entity
@Table(name = "match_timelines", schema = "lol_event")
class MatchTimelineEntity(
    @Id
    @Column(name = "match_id", length = 50)
    val matchId: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw", columnDefinition = "jsonb", nullable = false)
    val raw: String = "",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
