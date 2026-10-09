package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.prediction.model.WinModel

interface WinModelQueryPersistencePort {
    /** `lol-ml` 이 학습해 내보낸 승률 예측 모델. */
    fun find(): WinModel
}
