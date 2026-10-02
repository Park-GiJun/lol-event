package com.gijun.main.application.handler

import com.gijun.main.application.port.out.cache.DragonCacheQueryPort
import com.gijun.main.domain.dragon.model.DragonChampionModel
import com.gijun.main.domain.dragon.model.DragonRuneModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DragonQueryHandlerTest {
    private val dragonCacheQueryPort = mock<DragonCacheQueryPort>()
    private val handler = DragonQueryHandler(dragonCacheQueryPort)

    private fun champion(
        id: Int,
        name: String,
    ) = DragonChampionModel(
        championId = id,
        championKey = "key$id",
        nameKo = name,
        titleKo = null,
        imageFull = null,
        imageUrl = null,
        version = null,
    )

    private fun rune(
        id: Int,
        styleId: Int,
        slot: Int,
    ) = DragonRuneModel(
        runeId = id,
        runeKey = "rune$id",
        nameKo = "룬$id",
        description = null,
        iconPath = null,
        imageUrl = null,
        styleId = styleId,
        styleNameKo = null,
        slot = slot,
        version = null,
    )

    @Test
    fun `챔피언 목록은 한국어 이름순이다`() {
        whenever(dragonCacheQueryPort.findAllChampions()).doReturn(listOf(champion(1, "제드"), champion(2, "가렌"), champion(3, "아리")))

        assertEquals(listOf("가렌", "아리", "제드"), handler.getDragonChampions().map { it.nameKo })
    }

    @Test
    fun `룬 목록은 계열 - 줄 - 룬 순이다`() {
        whenever(dragonCacheQueryPort.findAllRunes()).doReturn(
            listOf(
                rune(8112, styleId = 8100, slot = 0),
                rune(8005, styleId = 8000, slot = 0),
                rune(8000, styleId = 8000, slot = DragonRuneModel.STYLE_SLOT),
                rune(8009, styleId = 8000, slot = 1),
                rune(8008, styleId = 8000, slot = 0),
            ),
        )

        assertEquals(listOf(8000, 8005, 8008, 8009, 8112), handler.getDragonRunes().map { it.runeId })
    }

    @Test
    fun `없는 id 는 null 이다`() {
        assertNull(handler.getDragonChampion(404))
    }
}
