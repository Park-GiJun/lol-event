package com.gijun.main.application.handler

import com.gijun.main.application.dto.result.DragonSyncResult
import com.gijun.main.application.port.out.cache.DragonCacheCommandPort
import com.gijun.main.application.port.out.external.DataDragonKtorPort
import com.gijun.main.application.port.out.persistence.DragonCommandPersistencePort
import com.gijun.main.application.port.out.persistence.DragonQueryPersistencePort
import com.gijun.main.domain.dragon.model.DragonChampionModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DragonCommandHandlerTest {
    private val dataDragonKtorPort = mock<DataDragonKtorPort>()
    private val dragonQueryPersistencePort = mock<DragonQueryPersistencePort>()
    private val dragonCommandPersistencePort = mock<DragonCommandPersistencePort>()
    private val dragonCacheCommandPort = mock<DragonCacheCommandPort>()
    private val handler =
        DragonCommandHandler(dataDragonKtorPort, dragonQueryPersistencePort, dragonCommandPersistencePort, dragonCacheCommandPort)

    private fun champion(
        id: Int,
        name: String,
    ) = DragonChampionModel(
        championId = id,
        championKey = name,
        nameKo = name,
        titleKo = null,
        imageFull = null,
        imageUrl = null,
        version = "15.1.1",
    )

    @Test
    fun `기동 - DB 가 비어 있으면 동기화하고 결과를 돌려준다`() {
        val fetched = listOf(champion(103, "아리"))
        whenever(dragonQueryPersistencePort.findAllChampions()).doReturn(emptyList(), fetched)
        whenever(dataDragonKtorPort.fetchLatestVersion()).doReturn("15.1.1")
        whenever(dataDragonKtorPort.fetchChampions("15.1.1")).doReturn(fetched)

        val result = handler.initializeDataDragon()

        assertEquals(DragonSyncResult(version = "15.1.1", champions = 1, items = 0, spells = 0, runes = 0), result)
        verify(dragonCommandPersistencePort).saveAllChampions(fetched)
    }

    @Test
    fun `기동 - DB 에 이미 있으면 Data Dragon 을 부르지 않고 캐시만 채운다`() {
        val stored = listOf(champion(103, "아리"))
        whenever(dragonQueryPersistencePort.findAllChampions()).doReturn(stored)

        val result = handler.initializeDataDragon()

        assertNull(result)
        verify(dataDragonKtorPort, never()).fetchLatestVersion()
        verify(dragonCommandPersistencePort, never()).saveAllChampions(any())
        verify(dragonCacheCommandPort).replaceAll(champions = stored, items = emptyList(), spells = emptyList(), runes = emptyList())
    }

    @Test
    fun `동기화 - 캐시는 저장이 끝난 뒤 DB 에서 다시 읽어 채운다`() {
        // 이번 버전에서 빠진 챔피언도 DB 에는 남아 있고 옛 경기가 그 id 를 가리킨다.
        val fetched = listOf(champion(103, "아리"))
        val storedAfter = listOf(champion(103, "아리"), champion(999, "삭제된챔피언"))
        whenever(dataDragonKtorPort.fetchLatestVersion()).doReturn("15.1.1")
        whenever(dataDragonKtorPort.fetchChampions("15.1.1")).doReturn(fetched)
        whenever(dragonQueryPersistencePort.findAllChampions()).doReturn(storedAfter)

        handler.syncDataDragon()

        inOrder(dragonCommandPersistencePort, dragonCacheCommandPort) {
            verify(dragonCommandPersistencePort).saveAllSpells(any())
            verify(
                dragonCacheCommandPort,
            ).replaceAll(champions = storedAfter, items = emptyList(), spells = emptyList(), runes = emptyList())
        }
    }
}
