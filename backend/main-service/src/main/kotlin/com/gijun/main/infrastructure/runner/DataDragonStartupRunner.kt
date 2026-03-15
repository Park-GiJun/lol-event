package com.gijun.main.infrastructure.runner

import com.gijun.main.application.port.`in`.SyncDataDragonUseCase
import com.gijun.main.infrastructure.cache.DataDragonCacheStore
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DataDragonStartupRunner(
    private val syncDataDragonUseCase: SyncDataDragonUseCase,
    private val cacheStore: DataDragonCacheStore
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        runCatching {
            if (cacheStore.isLoaded()) {
                // DB에 이미 데이터가 있으면 캐시 워밍업만
                cacheStore.warmUp()
            } else {
                // 처음 기동이면 DataDragon에서 동기화
                val result = syncDataDragonUseCase.sync()
                log.info("[DataDragon] 초기 동기화 완료: version=${result.version}, 챔피언=${result.champions}, 아이템=${result.items}, 스펠=${result.spells}")
            }
        }.onFailure {
            log.error("[DataDragon] 시작 시 데이터 로드 실패 - 캐시 없이 기동합니다", it)
        }
    }
}
