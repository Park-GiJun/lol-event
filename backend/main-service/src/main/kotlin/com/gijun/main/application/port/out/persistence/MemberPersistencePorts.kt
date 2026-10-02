package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.member.model.MemberModel

interface MemberQueryPersistencePort {
    fun findAll(): List<MemberModel>

    fun findByPuuid(puuid: String): MemberModel?

    fun existsByPuuid(puuid: String): Boolean

    fun findAllPuuidsByPuuidIn(puuids: Collection<String>): List<String>
}

interface MemberCommandPersistencePort {
    fun save(member: MemberModel): MemberModel

    fun saveAll(members: List<MemberModel>): List<MemberModel>

    fun deleteByPuuid(puuid: String)
}
