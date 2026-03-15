package com.gijun.main.infrastructure.adapter.out.persistence.member.entity

import com.gijun.main.domain.model.member.Member
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "members", schema = "lol_event")
class MemberEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val riotId: String,
    @Column(nullable = false, unique = true) val puuid: String,
    @Column(nullable = false) val registeredAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain() = Member(id = id, riotId = riotId, puuid = puuid, registeredAt = registeredAt)

    companion object {
        fun from(domain: Member) = MemberEntity(
            id = domain.id, riotId = domain.riotId,
            puuid = domain.puuid, registeredAt = domain.registeredAt
        )
    }
}
