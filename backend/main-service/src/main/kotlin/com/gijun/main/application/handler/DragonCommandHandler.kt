package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.DragonSyncResult
import com.gijun.main.application.port.`in`.InitializeDataDragonUseCase
import com.gijun.main.application.port.`in`.SyncDataDragonUseCase
import com.gijun.main.application.port.out.cache.DragonCacheCommandPort
import com.gijun.main.application.port.out.external.DataDragonKtorPort
import com.gijun.main.application.port.out.persistence.DragonCommandPersistencePort
import com.gijun.main.application.port.out.persistence.DragonQueryPersistencePort
import com.gijun.main.domain.dragon.model.DragonChampionModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Data Dragon 정적 데이터 동기화와 캐시 적재.
 */
@Service
class DragonCommandHandler(
    private val dataDragonKtorPort: DataDragonKtorPort,
    private val dragonQueryPersistencePort: DragonQueryPersistencePort,
    private val dragonCommandPersistencePort: DragonCommandPersistencePort,
    private val dragonCacheCommandPort: DragonCacheCommandPort,
) : SyncDataDragonUseCase,
    InitializeDataDragonUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun syncDataDragon(): DragonSyncResult {
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

        warmUpCache()

        log.info("[DataDragon] 동기화 완료 - 챔피언: ${champions.size}, 아이템: ${items.size}, 스펠: ${spells.size}, 룬: ${runes.size}")
        return DragonSyncResult(
            version = version,
            champions = champions.size,
            items = items.size,
            spells = spells.size,
            runes = runes.size,
        )
    }

    override fun initializeDataDragon(): DragonSyncResult? {
        val champions = dragonQueryPersistencePort.findAllChampions()
        if (champions.isEmpty()) return syncDataDragon()

        // DB 에 이미 있으면 Data Dragon 을 다시 받지 않고 캐시만 채운다.
        warmUpCache(champions)
        return null
    }

    /**
     * 캐시는 **방금 받은 목록이 아니라 DB 에서 다시 읽어** 채운다. 저장은 upsert 라 이번 버전에서
     * 빠진 항목도 DB 에는 남아 있고, 옛 경기 기록이 그 id 를 가리킨다.
     */
    private fun warmUpCache(champions: List<DragonChampionModel> = dragonQueryPersistencePort.findAllChampions()) {
        dragonCacheCommandPort.replaceAll(
            champions = champions,
            items = dragonQueryPersistencePort.findAllItems(),
            spells = dragonQueryPersistencePort.findAllSpells(),
            runes = dragonQueryPersistencePort.findAllRunes(),
        )
    }
}
