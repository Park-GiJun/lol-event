package com.gijun.main.infrastructure.adapter.out.persistence.dragon.repository

import com.gijun.main.infrastructure.adapter.out.persistence.dragon.entity.DragonChampionEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.entity.DragonItemEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.entity.DragonSummonerSpellEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DragonChampionJpaRepository : JpaRepository<DragonChampionEntity, Long> {
    fun findByChampionId(championId: Int): DragonChampionEntity?
}

interface DragonItemJpaRepository : JpaRepository<DragonItemEntity, Long> {
    fun findByItemId(itemId: Int): DragonItemEntity?
}

interface DragonSummonerSpellJpaRepository : JpaRepository<DragonSummonerSpellEntity, Long> {
    fun findBySpellId(spellId: Int): DragonSummonerSpellEntity?
}
