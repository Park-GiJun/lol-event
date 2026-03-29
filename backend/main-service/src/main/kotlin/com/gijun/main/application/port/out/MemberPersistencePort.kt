package com.gijun.main.application.port.out

import com.gijun.main.domain.model.member.Member

interface MemberPersistencePort {
    fun findAll(): List<Member>
    fun findByPuuid(puuid: String): Member?
    fun existsByPuuid(puuid: String): Boolean
    fun findAllPuuidsByPuuidIn(puuids: Collection<String>): List<String>
    fun save(member: Member): Member
    fun saveAll(members: List<Member>): List<Member>
    fun deleteByPuuid(puuid: String)
}
