package com.gijun.main.application.port.out.persistence

import com.gijun.main.domain.member.model.MemberModel
import com.gijun.main.shared.domain.vo.Puuid

interface MemberQueryPersistencePort {
    fun findAll(): List<MemberModel>

    fun findByPuuid(puuid: Puuid): MemberModel?

    fun existsByPuuid(puuid: Puuid): Boolean

    fun findAllPuuidsByPuuidIn(puuids: Collection<String>): List<String>
}

interface MemberCommandPersistencePort {
    fun save(member: MemberModel): MemberModel

    fun saveAll(members: List<MemberModel>): List<MemberModel>

    fun deleteByPuuid(puuid: Puuid)
}
