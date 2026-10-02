package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.DragonSyncResult
import com.gijun.main.application.port.`in`.SyncDataDragonUseCase
import com.gijun.main.application.port.out.cache.DataDragonCachePort
import com.gijun.main.application.port.out.external.DataDragonKtorPort
import com.gijun.main.application.port.out.persistence.DragonCommandPersistencePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SyncDataDragonHandler(
    private val dataDragonKtorPort: DataDragonKtorPort,
    private val dragonCommandPersistencePort: DragonCommandPersistencePort,
    private val cacheStore: DataDragonCachePort,
) : SyncDataDragonUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sync(): DragonSyncResult {
        val version = dataDragonKtorPort.fetchLatestVersion()
        log.info("[DataDragon] 최신 버전: $version, 데이터 동기화 시작")

        val champions = dataDragonKtorPort.fetchChampions(version)
        dragonCommandPersistencePort.saveAllChampions(champions)

        val items = dataDragonKtorPort.fetchItems(version)
        dragonCommandPersistencePort.saveAllItems(items)

        val runes = dataDragonKtorPort.fetchRunes(version)
        dragonCommandPersistencePort.saveAllRunes(runes)

        val spells = dataDragonKtorPort.fetchSummonerSpells(version)
        dragonCommandPersistencePort.saveAllSpells(spells)

        cacheStore.warmUp()

        log.info("[DataDragon] 동기화 완료 - 챔피언: ${champions.size}, 아이템: ${items.size}, 스펠: ${spells.size}, 룬: ${runes.size}")
        return DragonSyncResult(
            version = version,
            champions = champions.size,
            items = items.size,
            spells = spells.size,
            runes = runes.size,
        )
    }
}
