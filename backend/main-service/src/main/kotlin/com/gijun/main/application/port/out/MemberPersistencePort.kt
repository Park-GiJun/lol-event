package com.gijun.main.application.port.out

import com.gijun.main.domain.model.member.Member

interface MemberPersistencePort {
    fun findAll(): List<Member>
    fun findByPuuid(puuid: String): Member?
    fun existsByPuuid(puuid: String): Boolean
    fun save(member: Member): Member
    fun deleteByPuuid(puuid: String)
}
