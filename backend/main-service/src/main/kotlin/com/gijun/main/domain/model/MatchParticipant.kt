package com.gijun.main.domain.model

import jakarta.persistence.*

@Entity
@Table(name = "match_participants", schema = "lol_event")
class MatchParticipant(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_db_id", nullable = false)
    val match: Match,

    // 기본 정보
    @Column val puuid: String? = null,
    @Column(nullable = false) val riotId: String,
    @Column(nullable = false) val champion: String,
    @Column(nullable = false) val championId: Int = 0,
    @Column(nullable = false) val team: String,
    @Column(nullable = false) val teamId: Int = 0,
    @Column(nullable = false) val spell1Id: Int = 0,
    @Column(nullable = false) val spell2Id: Int = 0,
    @Column(nullable = false) val win: Boolean,

    // KDA / 기본 스탯
    @Column(nullable = false) val kills: Int = 0,
    @Column(nullable = false) val deaths: Int = 0,
    @Column(nullable = false) val assists: Int = 0,
    @Column(nullable = false) val damage: Int = 0,
    @Column(nullable = false) val cs: Int = 0,
    @Column(nullable = false) val gold: Int = 0,
    @Column(nullable = false) val visionScore: Int = 0,

    // 레벨 / 멀티킬
    @Column(nullable = false) val champLevel: Int = 0,
    @Column(nullable = false) val doubleKills: Int = 0,
    @Column(nullable = false) val tripleKills: Int = 0,
    @Column(nullable = false) val quadraKills: Int = 0,
    @Column(nullable = false) val pentaKills: Int = 0,
    @Column(nullable = false) val unrealKills: Int = 0,
    @Column(nullable = false) val killingSprees: Int = 0,
    @Column(nullable = false) val largestKillingSpree: Int = 0,
    @Column(nullable = false) val largestMultiKill: Int = 0,
    @Column(nullable = false) val largestCriticalStrike: Int = 0,
    @Column(nullable = false) val longestTimeSpentLiving: Int = 0,

    // 퍼스트 블러드 / 타워
    @Column(nullable = false) val firstBloodKill: Boolean = false,
    @Column(nullable = false) val firstBloodAssist: Boolean = false,
    @Column(nullable = false) val firstTowerKill: Boolean = false,
    @Column(nullable = false) val firstTowerAssist: Boolean = false,
    @Column(nullable = false) val firstInhibitorKill: Boolean = false,
    @Column(nullable = false) val firstInhibitorAssist: Boolean = false,
    @Column(nullable = false) val inhibitorKills: Int = 0,
    @Column(nullable = false) val turretKills: Int = 0,

    // 와드
    @Column(nullable = false) val wardsKilled: Int = 0,
    @Column(nullable = false) val wardsPlaced: Int = 0,
    @Column(nullable = false) val sightWardsBoughtInGame: Int = 0,
    @Column(nullable = false) val visionWardsBoughtInGame: Int = 0,

    // 아이템
    @Column(nullable = false) val item0: Int = 0,
    @Column(nullable = false) val item1: Int = 0,
    @Column(nullable = false) val item2: Int = 0,
    @Column(nullable = false) val item3: Int = 0,
    @Column(nullable = false) val item4: Int = 0,
    @Column(nullable = false) val item5: Int = 0,
    @Column(nullable = false) val item6: Int = 0,

    // 룬
    @Column(nullable = false) val perk0: Int = 0,
    @Column(nullable = false) val perk0Var1: Int = 0,
    @Column(nullable = false) val perk0Var2: Int = 0,
    @Column(nullable = false) val perk0Var3: Int = 0,
    @Column(nullable = false) val perk1: Int = 0,
    @Column(nullable = false) val perk1Var1: Int = 0,
    @Column(nullable = false) val perk1Var2: Int = 0,
    @Column(nullable = false) val perk1Var3: Int = 0,
    @Column(nullable = false) val perk2: Int = 0,
    @Column(nullable = false) val perk2Var1: Int = 0,
    @Column(nullable = false) val perk2Var2: Int = 0,
    @Column(nullable = false) val perk2Var3: Int = 0,
    @Column(nullable = false) val perk3: Int = 0,
    @Column(nullable = false) val perk3Var1: Int = 0,
    @Column(nullable = false) val perk3Var2: Int = 0,
    @Column(nullable = false) val perk3Var3: Int = 0,
    @Column(nullable = false) val perk4: Int = 0,
    @Column(nullable = false) val perk4Var1: Int = 0,
    @Column(nullable = false) val perk4Var2: Int = 0,
    @Column(nullable = false) val perk4Var3: Int = 0,
    @Column(nullable = false) val perk5: Int = 0,
    @Column(nullable = false) val perk5Var1: Int = 0,
    @Column(nullable = false) val perk5Var2: Int = 0,
    @Column(nullable = false) val perk5Var3: Int = 0,
    @Column(nullable = false) val perkPrimaryStyle: Int = 0,
    @Column(nullable = false) val perkSubStyle: Int = 0,

    // 데미지 상세
    @Column(nullable = false) val magicDamageDealt: Int = 0,
    @Column(nullable = false) val magicDamageDealtToChampions: Int = 0,
    @Column(nullable = false) val magicalDamageTaken: Int = 0,
    @Column(nullable = false) val physicalDamageDealt: Int = 0,
    @Column(nullable = false) val physicalDamageDealtToChampions: Int = 0,
    @Column(nullable = false) val physicalDamageTaken: Int = 0,
    @Column(nullable = false) val trueDamageDealt: Int = 0,
    @Column(nullable = false) val trueDamageDealtToChampions: Int = 0,
    @Column(nullable = false) val trueDamageTaken: Int = 0,
    @Column(nullable = false) val totalDamageDealt: Int = 0,
    @Column(nullable = false) val totalDamageDealtToChampions: Int = 0,
    @Column(nullable = false) val totalDamageTaken: Int = 0,
    @Column(nullable = false) val damageDealtToObjectives: Int = 0,
    @Column(nullable = false) val damageDealtToTurrets: Int = 0,
    @Column(nullable = false) val damageSelfMitigated: Int = 0,

    // 힐 / CC
    @Column(nullable = false) val totalHeal: Int = 0,
    @Column(nullable = false) val totalUnitsHealed: Int = 0,
    @Column(name = "time_ccing_others", nullable = false) val timeCCingOthers: Int = 0,
    @Column(nullable = false) val totalTimeCrowdControlDealt: Int = 0,

    // 미니언
    @Column(nullable = false) val neutralMinionsKilled: Int = 0,
    @Column(nullable = false) val neutralMinionsKilledTeamJungle: Int = 0,
    @Column(nullable = false) val neutralMinionsKilledEnemyJungle: Int = 0,

    // 스코어
    @Column(nullable = false) val combatPlayerScore: Int = 0,
    @Column(nullable = false) val objectivePlayerScore: Int = 0,
    @Column(nullable = false) val totalPlayerScore: Int = 0,
    @Column(nullable = false) val totalScoreRank: Int = 0,

    // 항복
    @Column(nullable = false) val gameEndedInSurrender: Boolean = false,
    @Column(nullable = false) val gameEndedInEarlySurrender: Boolean = false,
    @Column(nullable = false) val causedEarlySurrender: Boolean = false,
    @Column(nullable = false) val earlySurrenderAccomplice: Boolean = false,
    @Column(nullable = false) val teamEarlySurrendered: Boolean = false,

    // 증강
    @Column(nullable = false) val playerAugment1: Int = 0,
    @Column(nullable = false) val playerAugment2: Int = 0,
    @Column(nullable = false) val playerAugment3: Int = 0,
    @Column(nullable = false) val playerAugment4: Int = 0,
    @Column(nullable = false) val playerAugment5: Int = 0,
    @Column(nullable = false) val playerAugment6: Int = 0,
    @Column(nullable = false) val playerSubteamId: Int = 0,
    @Column(nullable = false) val subteamPlacement: Int = 0,
    @Column(nullable = false) val roleBoundItem: Int = 0,

    // 포지션
    @Column val lane: String? = null,
    @Column val role: String? = null
)
