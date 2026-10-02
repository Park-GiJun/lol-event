package com.gijun.main.application.port.`in`

import com.gijun.main.application.dto.query.ValidateRatingQuery
import com.gijun.main.application.dto.result.RatingValidationResult

interface ValidateRatingUseCase {
    /**
     * 워크포워드 평가. 앞 예열 경기로 레이팅을 데운 뒤, 매 경기를 **그 시점 레이팅으로만**
     * 예측하고 나서 반영한다. 저장된 레이팅은 건드리지 않는다.
     *
     * 팀 반복 경기를 왜 빼는지는 [com.gijun.main.application.handler.RatingValidationQueryHandler] 참고.
     */
    fun validateRating(query: ValidateRatingQuery): RatingValidationResult
}
