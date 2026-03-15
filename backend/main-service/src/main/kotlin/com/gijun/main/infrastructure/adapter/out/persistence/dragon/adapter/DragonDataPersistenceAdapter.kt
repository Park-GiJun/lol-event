package com.gijun.main.infrastructure.adapter.out.persistence.dragon.adapter

import com.gijun.main.application.port.out.DragonDataPort
import com.gijun.main.domain.model.dragon.DragonChampion
import com.gijun.main.domain.model.dragon.DragonItem
import com.gijun.main.domain.model.dragon.DragonSummonerSpell
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.entity.DragonChampionEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.entity.DragonItemEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.entity.DragonSummonerSpellEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.repository.DragonChampionJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.repository.DragonItemJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.repository.DragonSummonerSpellJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class DragonDataPersistenceAdapter(
    private val championRepo: DragonChampionJpaRepository,
    private val itemRepo: DragonItemJpaRepository,
    private val spellRepo: DragonSummonerSpellJpaRepository
) : DragonDataPort {

    @Transactional
    override fun saveAllChampions(champions: List<DragonChampion>) {
        for (domain in champions) {
            val entity = championRepo.findByChampionId(domain.championId)
            if (entity != null) {
                entity.nameKo = domain.nameKo; entity.titleKo = domain.titleKo
                entity.imageFull = domain.imageFull; entity.imageUrl = domain.imageUrl
                entity.version = domain.version; entity.updatedAt = LocalDateTime.now()
            } else {
                championRepo.save(DragonChampionEntity.from(domain))
            }
        }
    }

    @Transactional
    override fun saveAllItems(items: List<DragonItem>) {
        for (domain in items) {
            val entity = itemRepo.findByItemId(domain.itemId)
            if (entity != null) {
                entity.nameKo = domain.nameKo; entity.description = domain.description
                entity.imageFull = domain.imageFull; entity.imageUrl = domain.imageUrl
                entity.goldTotal = domain.goldTotal; entity.version = domain.version
                entity.updatedAt = LocalDateTime.now()
            } else {
                itemRepo.save(DragonItemEntity.from(domain))
            }
        }
    }

    @Transactional
    override fun saveAllSpells(spells: List<DragonSummonerSpell>) {
        for (domain in spells) {
            val entity = spellRepo.findBySpellId(domain.spellId)
            if (entity != null) {
                entity.nameKo = domain.nameKo; entity.description = domain.description
                entity.imageFull = domain.imageFull; entity.imageUrl = domain.imageUrl
                entity.version = domain.version; entity.updatedAt = LocalDateTime.now()
            } else {
                spellRepo.save(DragonSummonerSpellEntity.from(domain))
            }
        }
    }

    override fun findAllChampions(): List<DragonChampion> = championRepo.findAll().map { it.toDomain() }
    override fun findAllItems(): List<DragonItem> = itemRepo.findAll().map { it.toDomain() }
    override fun findAllSpells(): List<DragonSummonerSpell> = spellRepo.findAll().map { it.toDomain() }

    override fun findChampionById(championId: Int): DragonChampion? = championRepo.findByChampionId(championId)?.toDomain()
    override fun findItemById(itemId: Int): DragonItem? = itemRepo.findByItemId(itemId)?.toDomain()
    override fun findSpellById(spellId: Int): DragonSummonerSpell? = spellRepo.findBySpellId(spellId)?.toDomain()
}
