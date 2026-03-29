package com.gijun.main.infrastructure.adapter.out.persistence.member.adapter

import com.gijun.main.application.port.out.MemberPersistencePort
import com.gijun.main.domain.model.member.Member
import com.gijun.main.infrastructure.adapter.out.persistence.member.entity.MemberEntity
import com.gijun.main.infrastructure.adapter.out.persistence.member.repository.MemberJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberPersistenceAdapter(private val repo: MemberJpaRepository) : MemberPersistencePort {

    override fun findAll(): List<Member> = repo.findAll().map { it.toDomain() }
    override fun findByPuuid(puuid: String): Member? = repo.findByPuuid(puuid)?.toDomain()
    override fun existsByPuuid(puuid: String): Boolean = repo.existsByPuuid(puuid)
    override fun findAllPuuidsByPuuidIn(puuids: Collection<String>): List<String> =
        repo.findAllPuuidsByPuuidIn(puuids)
    override fun save(member: Member): Member = repo.save(MemberEntity.from(member)).toDomain()
    override fun saveAll(members: List<Member>): List<Member> =
        repo.saveAll(members.map { MemberEntity.from(it) }).map { it.toDomain() }

    @Transactional
    override fun deleteByPuuid(puuid: String) = repo.deleteByPuuid(puuid)
}
