package com.gijun.main.infrastructure.adapter.out.persistence.member.repository

import com.gijun.main.infrastructure.adapter.out.persistence.member.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<MemberEntity, Long> {
    fun findByPuuid(puuid: String): MemberEntity?
    fun existsByPuuid(puuid: String): Boolean
    fun deleteByPuuid(puuid: String)
}
