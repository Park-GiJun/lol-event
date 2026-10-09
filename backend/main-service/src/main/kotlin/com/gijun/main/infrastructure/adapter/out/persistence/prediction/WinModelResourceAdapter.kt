package com.gijun.main.infrastructure.adapter.out.persistence.prediction

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.application.port.out.persistence.WinModelQueryPersistencePort
import com.gijun.main.domain.prediction.model.WinModel
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * `lol-ml` 이 학습해 내보낸 승률 예측 모델을 `resources/ml/win-model.json` 에서 읽는다.
 *
 * 파일은 저장소에 같이 커밋한다. 다시 학습하면 `lol-ml` 에서 `./gradlew run --args="export"` 가 이 파일을 새로 쓴다.
 * 뜰 때 한 번 읽는다. 파일이 없거나 값이 빠졌으면 서비스가 뜨지 않는다 — 조용히 0 으로 계산하는 것보다 낫다.
 */
@Component
class WinModelResourceAdapter(
    objectMapper: ObjectMapper,
) : WinModelQueryPersistencePort {
    private val model: WinModel =
        ClassPathResource(PATH).inputStream.use { objectMapper.readTree(it) }.let { json ->
            fun number(field: String) = json.path(field).takeIf { it.isNumber } ?: error("$PATH 에 $field 가 없다")
            WinModel(
                seatLaneWinRateCoefficient = number("seatLaneWinRateCoefficient").asDouble(),
                tierCoefficient = number("tierCoefficient").asDouble(),
                shrinkPrior = number("shrinkPrior").asDouble(),
                tierMean = number("tierMean").asDouble(),
                trainedMatches = number("trainedMatches").asInt(),
                lastGameCreation = number("lastGameCreation").asLong(),
            )
        }

    override fun find(): WinModel = model

    private companion object {
        const val PATH = "ml/win-model.json"
    }
}
