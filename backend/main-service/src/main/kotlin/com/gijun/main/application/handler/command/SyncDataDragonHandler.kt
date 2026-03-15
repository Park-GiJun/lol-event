package com.gijun.main.application.handler.command

import com.gijun.main.application.dto.dragon.result.DragonSyncResult
import com.gijun.main.application.port.`in`.SyncDataDragonUseCase
import com.gijun.main.application.port.out.DragonDataPort
import com.gijun.main.infrastructure.adapter.out.client.DataDragonAdapter
import com.gijun.main.infrastructure.adapter.out.cache.DataDragonCacheStore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SyncDataDragonHandler(
    private val dataDragonAdapter: DataDragonAdapter,
    private val dragonDataPort: DragonDataPort,
    private val cacheStore: DataDragonCacheStore
) : SyncDataDragonUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sync(): DragonSyncResult {
        val version = dataDragonAdapter.fetchLatestVersion()
        log.info("[DataDragon] 최신 버전: $version, 데이터 동기화 시작")

        val champions = dataDragonAdapter.fetchChampions(version)
        dragonDataPort.saveAllChampions(champions)

        val items = dataDragonAdapter.fetchItems(version)
        dragonDataPort.saveAllItems(items)

        val spells = dataDragonAdapter.fetchSummonerSpells(version)
        dragonDataPort.saveAllSpells(spells)

        cacheStore.warmUp()

        log.info("[DataDragon] 동기화 완료 - 챔피언: ${champions.size}, 아이템: ${items.size}, 스펠: ${spells.size}")
        return DragonSyncResult(
            version = version,
            champions = champions.size,
            items = items.size,
            spells = spells.size
        )
    }
}
