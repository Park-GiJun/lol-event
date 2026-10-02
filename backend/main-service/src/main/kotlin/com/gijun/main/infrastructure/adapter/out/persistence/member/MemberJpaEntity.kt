package com.gijun.main.infrastructure.adapter.out.persistence.member

import com.gijun.main.domain.member.model.MemberModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "members", schema = "lol_event")
class MemberJpaEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false) val riotId: String,
    @Column(nullable = false, unique = true) val puuid: String,
    @Column(nullable = false) val registeredAt: LocalDateTime = LocalDateTime.now(),
) {
    fun toModel() = MemberModel(id = id, riotId = riotId, puuid = puuid, registeredAt = registeredAt)

    companion object {
        fun from(domain: MemberModel) =
            MemberJpaEntity(
                id = domain.id,
                riotId = domain.riotId,
                puuid = domain.puuid,
                registeredAt = domain.registeredAt,
            )
    }
}
