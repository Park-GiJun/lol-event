package com.gijun.main.infrastructure.adapter.out.persistence.member

import com.gijun.main.infrastructure.adapter.out.persistence.member.MemberJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MemberJpaRepository : JpaRepository<MemberJpaEntity, Long> {
    fun findByPuuid(puuid: String): MemberJpaEntity?

    fun existsByPuuid(puuid: String): Boolean

    fun deleteByPuuid(puuid: String)

    @Query("SELECT m.puuid FROM MemberJpaEntity m WHERE m.puuid IN :puuids")
    fun findAllPuuidsByPuuidIn(puuids: Collection<String>): List<String>
}
