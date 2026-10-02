package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.GetSessionDetailQuery
import com.gijun.main.application.dto.result.SessionDetailResult
import com.gijun.main.application.dto.result.SessionReportResult
import com.gijun.main.domain.match.enums.GameMode

interface GetSessionReportUseCase {
    fun getSessionReport(mode: GameMode): SessionReportResult
}

interface GetSessionDetailUseCase {
    /**
     * 하루치 내전 상세.
     *
     * 그 세션에 경기가 없으면 null 이다 — 세션 목록에서 넘어오는 화면이라 없는 날짜는 실제
     * 오류이고, 404 가 행동으로 이어진다. (경기 타임라인과 다른 판단이다. 거기는 타임라인이
     * 없는 경기가 정상이라 빈 결과를 준다.)
     */
    fun getSessionDetail(query: GetSessionDetailQuery): SessionDetailResult?
}
