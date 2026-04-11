package com.gijun.main.infrastructure.adapter.out.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.gijun.main.application.port.out.DataDragonFetchPort
import com.gijun.main.domain.model.dragon.DragonChampion
import com.gijun.main.domain.model.dragon.DragonItem
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
