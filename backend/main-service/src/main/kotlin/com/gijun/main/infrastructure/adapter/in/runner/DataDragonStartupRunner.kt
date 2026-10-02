package com.gijun.main.infrastructure.adapter.`in`.runner

import com.gijun.main.application.port.`in`.InitializeDataDragonUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DataDragonStartupRunner(
    private val initializeDataDragonUseCase: InitializeDataDragonUseCase,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        runCatching { initializeDataDragonUseCase.initializeDataDragon() }
            .onSuccess { result ->
                if (result != null) {
                    log.info(
                        "[DataDragon] 초기 동기화 완료: version={}, 챔피언={}, 아이템={}, 스펠={}",
                        result.version,
                        result.champions,
                        result.items,
                        result.spells,
                    )
                }
            }.onFailure { log.error("[DataDragon] 시작 시 데이터 로드 실패 - 캐시 없이 기동합니다", it) }
    }
}
