package com.gijun.main.infrastructure.adapter.out.persistence.dragon

import com.gijun.main.application.port.out.persistence.DragonCommandPersistencePort
import com.gijun.main.application.port.out.persistence.DragonQueryPersistencePort
import com.gijun.main.domain.dragon.model.DragonChampionModel
import com.gijun.main.domain.dragon.model.DragonItemModel
import com.gijun.main.domain.dragon.model.DragonRuneModel
import com.gijun.main.domain.dragon.model.DragonSummonerSpellModel
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonChampionJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonChampionJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonItemJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonItemJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonRuneJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonRuneJpaRepository
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonSummonerSpellJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.dragon.DragonSummonerSpellJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class DragonQueryPersistenceAdapter(
    private val championRepo: DragonChampionJpaRepository,
    private val itemRepo: DragonItemJpaRepository,
    private val spellRepo: DragonSummonerSpellJpaRepository,
    private val runeRepo: DragonRuneJpaRepository,
) : DragonQueryPersistencePort {
    override fun findAllChampions(): List<DragonChampionModel> = championRepo.findAll().map { it.toModel() }

    override fun findAllItems(): List<DragonItemModel> = itemRepo.findAll().map { it.toModel() }

    override fun findAllSpells(): List<DragonSummonerSpellModel> = spellRepo.findAll().map { it.toModel() }

    override fun findAllRunes(): List<DragonRuneModel> = runeRepo.findAll().map { it.toModel() }

    override fun findChampionById(championId: Int): DragonChampionModel? = championRepo.findByChampionId(championId)?.toModel()

    override fun findItemById(itemId: Int): DragonItemModel? = itemRepo.findByItemId(itemId)?.toModel()

    override fun findSpellById(spellId: Int): DragonSummonerSpellModel? = spellRepo.findBySpellId(spellId)?.toModel()

    override fun findRuneById(runeId: Int): DragonRuneModel? = runeRepo.findByRuneId(runeId)?.toModel()
}

@Component
class DragonCommandPersistenceAdapter(
    private val championRepo: DragonChampionJpaRepository,
    private val itemRepo: DragonItemJpaRepository,
    private val spellRepo: DragonSummonerSpellJpaRepository,
    private val runeRepo: DragonRuneJpaRepository,
) : DragonCommandPersistencePort {
    @Transactional
    override fun saveAllChampions(champions: List<DragonChampionModel>) {
        for (domain in champions) {
            val entity = championRepo.findByChampionId(domain.championId)
            if (entity != null) {
                entity.nameKo = domain.nameKo
                entity.titleKo = domain.titleKo
                entity.imageFull = domain.imageFull
                entity.imageUrl = domain.imageUrl
                entity.version = domain.version
                entity.updatedAt = LocalDateTime.now()
            } else {
                championRepo.save(DragonChampionJpaEntity.from(domain))
            }
        }
    }

    @Transactional
    override fun saveAllItems(items: List<DragonItemModel>) {
        for (domain in items) {
            val entity = itemRepo.findByItemId(domain.itemId)
            if (entity != null) {
                entity.nameKo = domain.nameKo
                entity.description = domain.description
                entity.imageFull = domain.imageFull
                entity.imageUrl = domain.imageUrl
                entity.goldTotal = domain.goldTotal
                entity.version = domain.version
                entity.updatedAt = LocalDateTime.now()
            } else {
                itemRepo.save(DragonItemJpaEntity.from(domain))
            }
        }
    }

    @Transactional
    override fun saveAllSpells(spells: List<DragonSummonerSpellModel>) {
        for (domain in spells) {
            val entity = spellRepo.findBySpellId(domain.spellId)
            if (entity != null) {
                entity.nameKo = domain.nameKo
                entity.description = domain.description
                entity.imageFull = domain.imageFull
                entity.imageUrl = domain.imageUrl
                entity.version = domain.version
                entity.updatedAt = LocalDateTime.now()
            } else {
                spellRepo.save(DragonSummonerSpellJpaEntity.from(domain))
            }
        }
    }

    @Transactional
    override fun saveAllRunes(runes: List<DragonRuneModel>) {
        for (domain in runes) {
            val entity = runeRepo.findByRuneId(domain.runeId)
            if (entity != null) {
                entity.runeKey = domain.runeKey
                entity.nameKo = domain.nameKo
                entity.description = domain.description
                entity.iconPath = domain.iconPath
                entity.imageUrl = domain.imageUrl
                entity.styleId = domain.styleId
                entity.styleNameKo = domain.styleNameKo
                entity.slot = domain.slot
                entity.version = domain.version
                entity.updatedAt = LocalDateTime.now()
            } else {
                runeRepo.save(DragonRuneJpaEntity.from(domain))
            }
        }
    }
}
