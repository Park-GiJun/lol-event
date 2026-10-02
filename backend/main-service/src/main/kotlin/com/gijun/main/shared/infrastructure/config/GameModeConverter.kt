package com.gijun.main.shared.infrastructure.config

import com.gijun.main.domain.match.enums.GameMode
import com.gijun.main.shared.domain.exception.ErrorCode
import com.gijun.main.shared.domain.exception.InvalidRequestException
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

/**
 * `?mode=normal` 을 [GameMode] 로 바꾼다.
 *
 * Spring 의 기본 enum 변환은 **상수 이름**(`NORMAL`)에만 맞는다. 화면과 수집기는 소문자 키
 * (`normal`)를 보내므로 그대로 두면 전부 400 이 된다. 대소문자를 가리지 않고 키로 찾는다.
 *
 * 모르는 값은 400 이다. 예전에는 조용히 협곡 통계로 떨어졌다.
 */
@Component
class GameModeConverter : Converter<String, GameMode> {
    override fun convert(source: String): GameMode =
        GameMode.parse(source)
            ?: throw InvalidRequestException(
                "mode 는 ${GameMode.entries.joinToString(", ") { it.key }} 중 하나여야 한다.",
                ErrorCode.INVALID_GAME_MODE,
            )
}
