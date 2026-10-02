package com.gijun.main.infrastructure.adapter.out.persistence.member

import com.gijun.main.application.port.out.persistence.MemberCommandPersistencePort
import com.gijun.main.application.port.out.persistence.MemberQueryPersistencePort
import com.gijun.main.domain.member.model.MemberModel
import com.gijun.main.infrastructure.adapter.out.persistence.member.MemberJpaEntity
import com.gijun.main.infrastructure.adapter.out.persistence.member.MemberJpaRepository
import com.gijun.main.shared.domain.vo.Puuid
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberQueryPersistenceAdapter(
    private val repo: MemberJpaRepository,
) : MemberQueryPersistencePort {
    override fun findAll(): List<MemberModel> = repo.findAll().map { it.toModel() }

    override fun findByPuuid(puuid: Puuid): MemberModel? = repo.findByPuuid(puuid.value)?.toModel()

    override fun existsByPuuid(puuid: Puuid): Boolean = repo.existsByPuuid(puuid.value)

    override fun findAllPuuidsByPuuidIn(puuids: Collection<String>): List<String> = repo.findAllPuuidsByPuuidIn(puuids)
}

@Component
class MemberCommandPersistenceAdapter(
    private val repo: MemberJpaRepository,
) : MemberCommandPersistencePort {
    override fun save(member: MemberModel): MemberModel = repo.save(MemberJpaEntity.from(member)).toModel()

    override fun saveAll(members: List<MemberModel>): List<MemberModel> =
        repo
            .saveAll(
                members.map {
                    MemberJpaEntity.from(it)
                },
            ).map { it.toModel() }

    @Transactional
    override fun deleteByPuuid(puuid: Puuid) = repo.deleteByPuuid(puuid.value)
}
