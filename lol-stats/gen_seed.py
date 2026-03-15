import json

with open('C:/Users/tpgj9/IdeaProjects/lol-event/lol-stats/data/matches.json', encoding='utf-8') as f:
    matches = json.load(f)

def b(v): return 'TRUE' if v else 'FALSE'
def i(v): return int(v) if v else 0
def s(v): return str(v).replace("'", "''") if v else ''

lines = []
lines.append('-- Flyway seed: match data (19 matches)')
lines.append('')

# ---- matches ----
lines.append('-- matches')
for m in matches:
    lines.append(
        f"INSERT INTO lol_event.matches (match_id, queue_id, game_creation, game_duration, game_mode, game_type, game_version, map_id, season_id, platform_id)\n"
        f"VALUES ('{s(m['matchId'])}', {m['queueId']}, {m['gameCreation']}, {m['gameDuration']}, "
        f"'{s(m.get('gameMode',''))}', '{s(m.get('gameType',''))}', '{s(m.get('gameVersion',''))}', "
        f"{m.get('mapId',0)}, {m.get('seasonId',0)}, '{s(m.get('platformId',''))}')\n"
        f"ON CONFLICT (match_id) DO NOTHING;"
    )

lines.append('')

# ---- participants ----
lines.append('-- match_participants')
for m in matches:
    mid = s(m['matchId'])
    for p in m['participants']:
        st = p.get('stats', {})
        tl = p.get('timeline', {})

        lines.append(
            f"INSERT INTO lol_event.match_participants (\n"
            f"    match_db_id, puuid, riot_id, champion, champion_id, team, team_id, spell1_id, spell2_id, win,\n"
            f"    kills, deaths, assists, damage, cs, gold, vision_score,\n"
            f"    champ_level, double_kills, triple_kills, quadra_kills, penta_kills, unreal_kills,\n"
            f"    killing_sprees, largest_killing_spree, largest_multi_kill, largest_critical_strike, longest_time_spent_living,\n"
            f"    first_blood_kill, first_blood_assist, first_tower_kill, first_tower_assist, first_inhibitor_kill, first_inhibitor_assist,\n"
            f"    inhibitor_kills, turret_kills,\n"
            f"    wards_killed, wards_placed, sight_wards_bought_in_game, vision_wards_bought_in_game,\n"
            f"    item0, item1, item2, item3, item4, item5, item6,\n"
            f"    perk0, perk0_var1, perk0_var2, perk0_var3,\n"
            f"    perk1, perk1_var1, perk1_var2, perk1_var3,\n"
            f"    perk2, perk2_var1, perk2_var2, perk2_var3,\n"
            f"    perk3, perk3_var1, perk3_var2, perk3_var3,\n"
            f"    perk4, perk4_var1, perk4_var2, perk4_var3,\n"
            f"    perk5, perk5_var1, perk5_var2, perk5_var3,\n"
            f"    perk_primary_style, perk_sub_style,\n"
            f"    magic_damage_dealt, magic_damage_dealt_to_champions, magical_damage_taken,\n"
            f"    physical_damage_dealt, physical_damage_dealt_to_champions, physical_damage_taken,\n"
            f"    true_damage_dealt, true_damage_dealt_to_champions, true_damage_taken,\n"
            f"    total_damage_dealt, total_damage_dealt_to_champions, total_damage_taken,\n"
            f"    damage_dealt_to_objectives, damage_dealt_to_turrets, damage_self_mitigated,\n"
            f"    total_heal, total_units_healed, time_ccing_others, total_time_crowd_control_dealt,\n"
            f"    neutral_minions_killed, neutral_minions_killed_team_jungle, neutral_minions_killed_enemy_jungle,\n"
            f"    combat_player_score, objective_player_score, total_player_score, total_score_rank,\n"
            f"    game_ended_in_surrender, game_ended_in_early_surrender, caused_early_surrender, early_surrender_accomplice, team_early_surrendered,\n"
            f"    player_augment1, player_augment2, player_augment3, player_augment4, player_augment5, player_augment6,\n"
            f"    player_subteam_id, subteam_placement, role_bound_item, lane, role\n"
            f")\n"
            f"SELECT m.id,\n"
            f"    '{s(p.get('puuid',''))}', '{s(p.get('riotId',''))}', '{s(p.get('champion',''))}', {i(p.get('championId'))},\n"
            f"    '{s(p.get('team',''))}', {i(p.get('teamId'))}, {i(p.get('spell1Id'))}, {i(p.get('spell2Id'))}, {b(p.get('win'))},\n"
            f"    {i(p.get('kills'))}, {i(p.get('deaths'))}, {i(p.get('assists'))}, {i(p.get('damage'))}, {i(p.get('cs'))}, {i(p.get('gold'))}, {i(p.get('visionScore'))},\n"
            f"    {i(st.get('champLevel'))}, {i(st.get('doubleKills'))}, {i(st.get('tripleKills'))}, {i(st.get('quadraKills'))}, {i(st.get('pentaKills'))}, {i(st.get('unrealKills'))},\n"
            f"    {i(st.get('killingSprees'))}, {i(st.get('largestKillingSpree'))}, {i(st.get('largestMultiKill'))}, {i(st.get('largestCriticalStrike'))}, {i(st.get('longestTimeSpentLiving'))},\n"
            f"    {b(st.get('firstBloodKill'))}, {b(st.get('firstBloodAssist'))}, {b(st.get('firstTowerKill'))}, {b(st.get('firstTowerAssist'))}, {b(st.get('firstInhibitorKill'))}, {b(st.get('firstInhibitorAssist'))},\n"
            f"    {i(st.get('inhibitorKills'))}, {i(st.get('turretKills'))},\n"
            f"    {i(st.get('wardsKilled'))}, {i(st.get('wardsPlaced'))}, {i(st.get('sightWardsBoughtInGame'))}, {i(st.get('visionWardsBoughtInGame'))},\n"
            f"    {i(st.get('item0'))}, {i(st.get('item1'))}, {i(st.get('item2'))}, {i(st.get('item3'))}, {i(st.get('item4'))}, {i(st.get('item5'))}, {i(st.get('item6'))},\n"
            f"    {i(st.get('perk0'))}, {i(st.get('perk0Var1'))}, {i(st.get('perk0Var2'))}, {i(st.get('perk0Var3'))},\n"
            f"    {i(st.get('perk1'))}, {i(st.get('perk1Var1'))}, {i(st.get('perk1Var2'))}, {i(st.get('perk1Var3'))},\n"
            f"    {i(st.get('perk2'))}, {i(st.get('perk2Var1'))}, {i(st.get('perk2Var2'))}, {i(st.get('perk2Var3'))},\n"
            f"    {i(st.get('perk3'))}, {i(st.get('perk3Var1'))}, {i(st.get('perk3Var2'))}, {i(st.get('perk3Var3'))},\n"
            f"    {i(st.get('perk4'))}, {i(st.get('perk4Var1'))}, {i(st.get('perk4Var2'))}, {i(st.get('perk4Var3'))},\n"
            f"    {i(st.get('perk5'))}, {i(st.get('perk5Var1'))}, {i(st.get('perk5Var2'))}, {i(st.get('perk5Var3'))},\n"
            f"    {i(st.get('perkPrimaryStyle'))}, {i(st.get('perkSubStyle'))},\n"
            f"    {i(st.get('magicDamageDealt'))}, {i(st.get('magicDamageDealtToChampions'))}, {i(st.get('magicalDamageTaken'))},\n"
            f"    {i(st.get('physicalDamageDealt'))}, {i(st.get('physicalDamageDealtToChampions'))}, {i(st.get('physicalDamageTaken'))},\n"
            f"    {i(st.get('trueDamageDealt'))}, {i(st.get('trueDamageDealtToChampions'))}, {i(st.get('trueDamageTaken'))},\n"
            f"    {i(st.get('totalDamageDealt'))}, {i(st.get('totalDamageDealtToChampions'))}, {i(st.get('totalDamageTaken'))},\n"
            f"    {i(st.get('damageDealtToObjectives'))}, {i(st.get('damageDealtToTurrets'))}, {i(st.get('damageSelfMitigated'))},\n"
            f"    {i(st.get('totalHeal'))}, {i(st.get('totalUnitsHealed'))}, {i(st.get('timeCCingOthers'))}, {i(st.get('totalTimeCrowdControlDealt'))},\n"
            f"    {i(st.get('neutralMinionsKilled'))}, {i(st.get('neutralMinionsKilledTeamJungle'))}, {i(st.get('neutralMinionsKilledEnemyJungle'))},\n"
            f"    {i(st.get('combatPlayerScore'))}, {i(st.get('objectivePlayerScore'))}, {i(st.get('totalPlayerScore'))}, {i(st.get('totalScoreRank'))},\n"
            f"    {b(st.get('gameEndedInSurrender'))}, {b(st.get('gameEndedInEarlySurrender'))}, {b(st.get('causedEarlySurrender'))}, {b(st.get('earlySurrenderAccomplice'))}, {b(st.get('teamEarlySurrendered'))},\n"
            f"    {i(st.get('playerAugment1'))}, {i(st.get('playerAugment2'))}, {i(st.get('playerAugment3'))}, {i(st.get('playerAugment4'))}, {i(st.get('playerAugment5'))}, {i(st.get('playerAugment6'))},\n"
            f"    {i(st.get('playerSubteamId'))}, {i(st.get('subteamPlacement'))}, {i(st.get('roleBoundItem'))},\n"
            f"    '{s(tl.get('lane',''))}', '{s(tl.get('role',''))}'\n"
            f"FROM lol_event.matches m WHERE m.match_id = '{mid}';"
        )

lines.append('')

# ---- teams ----
lines.append('-- match_teams')
for m in matches:
    mid = s(m['matchId'])
    for t in m['teams']:
        win = 'TRUE' if t.get('win') == 'Win' else 'FALSE'
        lines.append(
            f"INSERT INTO lol_event.match_teams (match_db_id, team_id, win, baron_kills, dragon_kills, tower_kills, inhibitor_kills, rift_herald_kills, horde_kills, first_blood, first_tower, first_baron, first_inhibitor, first_dragon)\n"
            f"SELECT m.id, {t['teamId']}, {win}, {i(t.get('baronKills'))}, {i(t.get('dragonKills'))}, {i(t.get('towerKills'))}, {i(t.get('inhibitorKills'))}, {i(t.get('riftHeraldKills'))}, {i(t.get('hordeKills'))},\n"
            f"    {b(t.get('firstBlood'))}, {b(t.get('firstTower'))}, {b(t.get('firstBaron'))}, {b(t.get('firstInhibitor'))}, {b(t.get('firstDargon'))}\n"
            f"FROM lol_event.matches m WHERE m.match_id = '{mid}';"
        )

lines.append('')

# ---- members ----
with open('C:/Users/tpgj9/IdeaProjects/lol-event/lol-stats/data/members.json', encoding='utf-8') as f:
    members = json.load(f)

lines.append('-- members')
for mem in members:
    lines.append(
        f"INSERT INTO lol_event.members (riot_id, puuid, registered_at)\n"
        f"VALUES ('{s(mem['riotId'])}', '{s(mem['puuid'])}', '{mem['registeredAt']}')\n"
        f"ON CONFLICT (puuid) DO NOTHING;"
    )

sql = '\n'.join(lines)
out = 'C:/Users/tpgj9/IdeaProjects/lol-event/backend/main-service/src/main/resources/db/migration/V3__seed_matches.sql'
with open(out, 'w', encoding='utf-8') as f:
    f.write(sql)
print('generated:', out)
print('total lines:', len(lines))
