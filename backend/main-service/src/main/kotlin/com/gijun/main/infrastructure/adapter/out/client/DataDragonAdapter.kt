package com.gijun.main.infrastructure.adapter.out.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.application.port.out.DataDragonFetchPort
import com.gijun.main.domain.model.dragon.DragonChampion
import com.gijun.main.domain.model.dragon.DragonItem
import com.gijun.main.domain.model.dragon.DragonRune
import com.gijun.main.domain.model.dragon.DragonSummonerSpell
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DataDragonAdapter(
    private val client: HttpClient,
    private val objectMapper: ObjectMapper
) : DataDragonFetchPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val base = "https://ddragon.leagueoflegends.com"

    override fun fetchLatestVersion(): String = runBlocking {
        val text = client.get("$base/api/versions.json").bodyAsText()
        objectMapper.readValue(text, Array<String>::class.java).firstOrNull() ?: "15.1.1"
    }

    @Suppress("UNCHECKED_CAST")
    override fun fetchChampions(version: String): List<DragonChampion> = runBlocking {
        val text = client.get("$base/cdn/$version/data/ko_KR/champion.json").bodyAsText()
        val raw = objectMapper.readValue(text, Map::class.java)
        val data = raw["data"] as? Map<String, Any> ?: return@runBlocking emptyList()

        data.values.mapNotNull { entry ->
            runCatching {
                val c = entry as Map<String, Any>
                val imageFull = (c["image"] as? Map<String, Any>)?.get("full") as? String
                DragonChampion(
                    championId = (c["key"] as String).toInt(),
                    championKey = c["id"] as String,
                    nameKo = c["name"] as String,
                    titleKo = c["title"] as? String,
                    imageFull = imageFull,
                    imageUrl = imageFull?.let { "$base/cdn/$version/img/champion/$it" },
                    version = version
                )
            }.onFailure { log.warn("챔피언 파싱 실패: $entry", it) }.getOrNull()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun fetchItems(version: String): List<DragonItem> = runBlocking {
        val text = client.get("$base/cdn/$version/data/ko_KR/item.json").bodyAsText()
        val raw = objectMapper.readValue(text, Map::class.java)
        val data = raw["data"] as? Map<String, Any> ?: return@runBlocking emptyList()

        data.entries.mapNotNull { (key, entry) ->
            runCatching {
                val item = entry as Map<String, Any>
                val imageFull = (item["image"] as? Map<String, Any>)?.get("full") as? String
                val goldTotal = ((item["gold"] as? Map<String, Any>)?.get("total") as? Number)?.toInt() ?: 0
                DragonItem(
                    itemId = key.toInt(),
                    nameKo = item["name"] as String,
                    description = item["description"] as? String,
                    imageFull = imageFull,
                    imageUrl = imageFull?.let { "$base/cdn/$version/img/item/$it" },
                    goldTotal = goldTotal,
                    version = version
                )
            }.onFailure { log.warn("아이템 파싱 실패: key=$key", it) }.getOrNull()
        }
    }

    /**
     * 룬은 runesReforged.json 하나에 계열 → 줄(slot) → 룬 의 3단 구조로 들어 있다.
     * 평평하게 펴서 계열 행과 룬 행을 한 목록으로 만든다.
     *
     * 아이콘 경로만 주의하면 된다. 챔피언·아이템·스펠 이미지는 `cdn/{version}/img/...` 인데
     * 룬 아이콘은 버전이 없는 `cdn/img/{icon}` 이다. 버전을 끼우면 404 가 난다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun fetchRunes(version: String): List<DragonRune> = runBlocking {
        val text = client.get("$base/cdn/$version/data/ko_KR/runesReforged.json").bodyAsText()
        val styles = objectMapper.readValue(text, List::class.java) as? List<Map<String, Any>>
            ?: return@runBlocking emptyList()

        fun iconUrl(icon: String?) = icon?.let { "$base/cdn/img/$it" }

        styles.flatMap { style ->
            runCatching {
                val styleId = (style["id"] as Number).toInt()
                val styleName = style["name"] as String

                // 계열 자체도 한 행으로 남긴다. 화면에서 보조 계열 아이콘을 id 로 찾기 때문이다.
                val styleRow = DragonRune(
                    runeId = styleId,
                    runeKey = style["key"] as? String ?: styleName,
                    nameKo = styleName,
                    description = null,
                    iconPath = style["icon"] as? String,
                    imageUrl = iconUrl(style["icon"] as? String),
                    styleId = styleId,
                    styleNameKo = styleName,
                    slot = DragonRune.STYLE_SLOT,
                    version = version,
                )

                val slots = style["slots"] as? List<Map<String, Any>> ?: emptyList()
                val runeRows = slots.flatMapIndexed { slotIndex, slot ->
                    val runes = slot["runes"] as? List<Map<String, Any>> ?: emptyList()
                    runes.mapNotNull { rune ->
                        runCatching {
                            DragonRune(
                                runeId = (rune["id"] as Number).toInt(),
                                runeKey = rune["key"] as? String ?: "",
                                nameKo = rune["name"] as String,
                                // shortDesc 는 <br> 같은 태그가 섞여 있지만 아이템 description 도
                                // 같은 상태로 저장하고 있어 표기를 맞춘다.
                                description = rune["shortDesc"] as? String ?: rune["longDesc"] as? String,
                                iconPath = rune["icon"] as? String,
                                imageUrl = iconUrl(rune["icon"] as? String),
                                styleId = styleId,
                                styleNameKo = styleName,
                                slot = slotIndex,
                                version = version,
                            )
                        }.onFailure { log.warn("룬 파싱 실패: $rune", it) }.getOrNull()
                    }
                }

                listOf(styleRow) + runeRows
            }.onFailure { log.warn("룬 계열 파싱 실패: $style", it) }.getOrDefault(emptyList())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun fetchSummonerSpells(version: String): List<DragonSummonerSpell> = runBlocking {
        val text = client.get("$base/cdn/$version/data/ko_KR/summoner.json").bodyAsText()
        val raw = objectMapper.readValue(text, Map::class.java)
        val data = raw["data"] as? Map<String, Any> ?: return@runBlocking emptyList()

        data.values.mapNotNull { entry ->
            runCatching {
                val spell = entry as Map<String, Any>
                val imageFull = (spell["image"] as? Map<String, Any>)?.get("full") as? String
                DragonSummonerSpell(
                    spellId = (spell["key"] as String).toInt(),
                    spellKey = spell["id"] as String,
                    nameKo = spell["name"] as String,
                    description = spell["description"] as? String,
                    imageFull = imageFull,
                    imageUrl = imageFull?.let { "$base/cdn/$version/img/spell/$it" },
                    version = version
                )
            }.onFailure { log.warn("스펠 파싱 실패: $entry", it) }.getOrNull()
        }
    }
}
