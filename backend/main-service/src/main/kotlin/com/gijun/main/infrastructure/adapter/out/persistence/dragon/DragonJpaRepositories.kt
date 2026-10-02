package com.gijun.main.infrastructure.adapter.out.persistence.dragon

import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonChampionJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonItemJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonRuneJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonSummonerSpellJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DragonChampionJpaRepository : JpaRepository<DragonChampionJpaEntity, Long> {
    fun findByChampionId(championId: Int): DragonChampionJpaEntity?
}

interface DragonItemJpaRepository : JpaRepository<DragonItemJpaEntity, Long> {
    fun findByItemId(itemId: Int): DragonItemJpaEntity?
}

interface DragonSummonerSpellJpaRepository : JpaRepository<DragonSummonerSpellJpaEntity, Long> {
    fun findBySpellId(spellId: Int): DragonSummonerSpellJpaEntity?
}

interface DragonRuneJpaRepository : JpaRepository<DragonRuneJpaEntity, Long> {
    fun findByRuneId(runeId: Int): DragonRuneJpaEntity?
}
