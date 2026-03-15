import { Injectable, Logger } from '@nestjs/common';
import { Observable, Subject } from 'rxjs';
import axios from 'axios';
import { LcuService } from '../lcu/lcu.service';
import { KafkaService } from '../kafka/kafka.service';

export interface SseEvent {
  data: string;
}

export interface CollectedMatch {
  matchId: string;
  [key: string]: unknown;
}

const CUSTOM_QUEUE_IDS = new Set([0, 3130, 3270]);
const MAX_GAMES = 500;
const PAGE = 20;
const SLEEP_MS = 200;

const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));
const n = (v: unknown, def = 0): number => (typeof v === 'number' ? v : def);
const b = (v: unknown): boolean => v === true;
const s = (v: unknown): string | null => (typeof v === 'string' ? v : null);

@Injectable()
export class CollectService {
  private readonly logger = new Logger(CollectService.name);
  private champCache: Record<string, string> | null = null;

  constructor(
    private readonly lcuService: LcuService,
    private readonly kafkaService: KafkaService,
  ) {}

  private async getChampMap(): Promise<Record<string, string>> {
    if (this.champCache) return this.champCache;
    const versions = await axios.get<string[]>('https://ddragon.leagueoflegends.com/api/versions.json');
    const latest = versions.data[0];
    const res = await axios.get<{ data: Record<string, { key: string; id: string }> }>(
      `https://ddragon.leagueoflegends.com/cdn/${latest}/data/ko_KR/champion.json`
    );
    this.champCache = {};
    for (const c of Object.values(res.data.data)) {
      this.champCache[c.key] = c.id;
    }
    return this.champCache;
  }

  collect(mainServiceUrl: string): Observable<SseEvent> {
    const subject = new Subject<SseEvent>();
    const send = (type: string, message: string) => {
      subject.next({ data: JSON.stringify({ type, message }) });
    };
    this.runCollect(send, mainServiceUrl).finally(() => subject.complete());
    return subject.asObservable();
  }

  private async runCollect(
    send: (type: string, message: string) => void,
    mainServiceUrl: string
  ): Promise<void> {
    const lockfilePath = this.lcuService.findLockfile();
    if (!lockfilePath) {
      send('error', 'lockfile 없음 — 롤 클라이언트를 실행해주세요');
      return;
    }

    const { port, password } = this.lcuService.parseLockfile(lockfilePath);

    let summoner: Record<string, unknown>;
    try {
      summoner = await this.lcuService.lcuGet(port, password, '/lol-summoner/v1/current-summoner');
      send('info', `클라이언트 연결 — ${summoner.gameName}#${summoner.tagLine}`);
    } catch (e) {
      send('error', `LCU 연결 실패: ${(e as Error).message}`);
      return;
    }

    const champMap = await this.getChampMap();
    const newMatches: CollectedMatch[] = [];
    const seenGameIds = new Set<number>();
    let begIndex = 0;

    while (begIndex < MAX_GAMES) {
      try {
        await sleep(SLEEP_MS);
        const endpoint = `/lol-match-history/v1/products/lol/current-summoner/matches?begIndex=${begIndex}&endIndex=${begIndex + PAGE - 1}`;
        const data = await this.lcuService.lcuGet<{
          games?: { games?: Array<Record<string, unknown>> };
        }>(port, password, endpoint);

        const games = data?.games?.games ?? [];
        if (games.length === 0) break;

        if (seenGameIds.has(games[0].gameId as number)) {
          send('info', `매치 히스토리 끝 (${begIndex}번째에서 중복 감지)`);
          break;
        }
        for (const g of games) seenGameIds.add(g.gameId as number);
        send('progress', `${begIndex}~${begIndex + games.length - 1}번 조회 — ${games.length}건`);

        for (const game of games) {
          if (!CUSTOM_QUEUE_IDS.has(game.queueId as number)) continue;

          const matchId = `KR_${game.gameId}`;
          await sleep(SLEEP_MS);

          let detail: Record<string, unknown>;
          try {
            detail = await this.lcuService.lcuGet(port, password, `/lol-match-history/v1/games/${game.gameId}`);
          } catch (e) {
            send('warn', `${matchId} 상세 조회 실패 — ${(e as Error).message}`);
            continue;
          }

          const identityMap: Record<number, Record<string, unknown>> = {};
          for (const identity of ((detail.participantIdentities as Array<{ participantId: number; player: Record<string, unknown> }>) ?? [])) {
            identityMap[identity.participantId] = identity.player ?? {};
          }

          const participants = ((detail.participants as Array<Record<string, unknown>>) ?? []).map(p => {
            const champId = String(p.championId);
            const identity = identityMap[p.participantId as number] ?? {};
            const st = (p.stats ?? {}) as Record<string, unknown>;
            const tl = (p.timeline ?? {}) as Record<string, unknown>;
            const pPuuid = s(identity.puuid) ?? '';
            const gameName = identity.gameName;
            const tagLine = identity.tagLine;

            return {
              puuid: pPuuid,
              riotId: gameName ? `${gameName}#${tagLine ?? ''}` : (s(identity.summonerName) ?? '???'),
              champion: champMap[champId] ?? `Champion_${champId}`,
              championId: n(p.championId),
              team: p.teamId === 100 ? 'blue' : 'red',
              teamId: n(p.teamId),
              spell1Id: n(p.spell1Id),
              spell2Id: n(p.spell2Id),
              win: b(st.win),
              kills: n(st.kills),
              deaths: n(st.deaths),
              assists: n(st.assists),
              damage: n(st.totalDamageDealtToChampions),
              cs: n(st.totalMinionsKilled) + n(st.neutralMinionsKilled),
              gold: n(st.goldEarned),
              visionScore: n(st.visionScore),
              champLevel: n(st.champLevel),
              doubleKills: n(st.doubleKills),
              tripleKills: n(st.tripleKills),
              quadraKills: n(st.quadraKills),
              pentaKills: n(st.pentaKills),
              unrealKills: n(st.unrealKills),
              killingSprees: n(st.killingSprees),
              largestKillingSpree: n(st.largestKillingSpree),
              largestMultiKill: n(st.largestMultiKill),
              largestCriticalStrike: n(st.largestCriticalStrike),
              longestTimeSpentLiving: n(st.longestTimeSpentLiving),
              firstBloodKill: b(st.firstBloodKill),
              firstBloodAssist: b(st.firstBloodAssist),
              firstTowerKill: b(st.firstTowerKill),
              firstTowerAssist: b(st.firstTowerAssist),
              firstInhibitorKill: b(st.firstInhibitorKill),
              firstInhibitorAssist: b(st.firstInhibitorAssist),
              inhibitorKills: n(st.inhibitorKills),
              turretKills: n(st.turretKills),
              wardsKilled: n(st.wardsKilled),
              wardsPlaced: n(st.wardsPlaced),
              sightWardsBoughtInGame: n(st.sightWardsBoughtInGame),
              visionWardsBoughtInGame: n(st.visionWardsBoughtInGame),
              item0: n(st.item0), item1: n(st.item1), item2: n(st.item2), item3: n(st.item3),
              item4: n(st.item4), item5: n(st.item5), item6: n(st.item6),
              perk0: n(st.perk0), perk0Var1: n(st.perk0Var1), perk0Var2: n(st.perk0Var2), perk0Var3: n(st.perk0Var3),
              perk1: n(st.perk1), perk1Var1: n(st.perk1Var1), perk1Var2: n(st.perk1Var2), perk1Var3: n(st.perk1Var3),
              perk2: n(st.perk2), perk2Var1: n(st.perk2Var1), perk2Var2: n(st.perk2Var2), perk2Var3: n(st.perk2Var3),
              perk3: n(st.perk3), perk3Var1: n(st.perk3Var1), perk3Var2: n(st.perk3Var2), perk3Var3: n(st.perk3Var3),
              perk4: n(st.perk4), perk4Var1: n(st.perk4Var1), perk4Var2: n(st.perk4Var2), perk4Var3: n(st.perk4Var3),
              perk5: n(st.perk5), perk5Var1: n(st.perk5Var1), perk5Var2: n(st.perk5Var2), perk5Var3: n(st.perk5Var3),
              perkPrimaryStyle: n(st.perkPrimaryStyle),
              perkSubStyle: n(st.perkSubStyle),
              magicDamageDealt: n(st.magicDamageDealt),
              magicDamageDealtToChampions: n(st.magicDamageDealtToChampions),
              magicalDamageTaken: n(st.magicalDamageTaken),
              physicalDamageDealt: n(st.physicalDamageDealt),
              physicalDamageDealtToChampions: n(st.physicalDamageDealtToChampions),
              physicalDamageTaken: n(st.physicalDamageTaken),
              trueDamageDealt: n(st.trueDamageDealt),
              trueDamageDealtToChampions: n(st.trueDamageDealtToChampions),
              trueDamageTaken: n(st.trueDamageTaken),
              totalDamageDealt: n(st.totalDamageDealt),
              totalDamageDealtToChampions: n(st.totalDamageDealtToChampions),
              totalDamageTaken: n(st.totalDamageTaken),
              damageDealtToObjectives: n(st.damageDealtToObjectives),
              damageDealtToTurrets: n(st.damageDealtToTurrets),
              damageSelfMitigated: n(st.damageSelfMitigated),
              totalHeal: n(st.totalHeal),
              totalUnitsHealed: n(st.totalUnitsHealed),
              timeCCingOthers: n(st.timeCCingOthers),
              totalTimeCrowdControlDealt: n(st.totalTimeCrowdControlDealt),
              neutralMinionsKilled: n(st.neutralMinionsKilled),
              neutralMinionsKilledTeamJungle: n(st.neutralMinionsKilledTeamJungle),
              neutralMinionsKilledEnemyJungle: n(st.neutralMinionsKilledEnemyJungle),
              combatPlayerScore: n(st.combatPlayerScore),
              objectivePlayerScore: n(st.objectivePlayerScore),
              totalPlayerScore: n(st.totalPlayerScore),
              totalScoreRank: n(st.totalScoreRank),
              gameEndedInSurrender: b(st.gameEndedInSurrender),
              gameEndedInEarlySurrender: b(st.gameEndedInEarlySurrender),
              causedEarlySurrender: b(st.causedEarlySurrender),
              earlySurrenderAccomplice: b(st.earlySurrenderAccomplice),
              teamEarlySurrendered: b(st.teamEarlySurrendered),
              playerAugment1: n(st.playerAugment1),
              playerAugment2: n(st.playerAugment2),
              playerAugment3: n(st.playerAugment3),
              playerAugment4: n(st.playerAugment4),
              playerAugment5: n(st.playerAugment5),
              playerAugment6: n(st.playerAugment6),
              playerSubteamId: n(st.playerSubteamId),
              subteamPlacement: n(st.subteamPlacement),
              roleBoundItem: n(st.roleBoundItem),
              lane: s(tl.lane),
              role: s(tl.role),
            };
          });

          const teams = ((detail.teams as Array<Record<string, unknown>>) ?? []).map(t => ({
            teamId: n(t.teamId),
            win: t.win === 'Win',
            baronKills: n(t.baronKills),
            dragonKills: n(t.dragonKills),
            towerKills: n(t.towerKills),
            inhibitorKills: n(t.inhibitorKills),
            riftHeraldKills: n(t.riftHeraldKills),
            hordeKills: n(t.hordeKills),
            firstBlood: b(t.firstBlood),
            firstTower: b(t.firstTower),
            firstBaron: b(t.firstBaron),
            firstInhibitor: b(t.firstInhibitor),
            firstDragon: b(t.firstDargon), // LCU 오탈자 그대로
          }));

          newMatches.push({
            matchId,
            queueId: n(game.queueId),
            gameCreation: n(game.gameCreation),
            gameDuration: n(game.gameDuration),
            gameMode: s(detail.gameMode),
            gameType: s(detail.gameType),
            gameVersion: s(detail.gameVersion),
            mapId: n(detail.mapId),
            seasonId: n(detail.seasonId),
            platformId: s(detail.platformId),
            participants,
            teams,
          });

          send('info', `✅ ${matchId} 저장 (${new Date(n(game.gameCreation)).toLocaleDateString('ko-KR')})`);
        }

        if (games.length < PAGE) break;
        begIndex += PAGE;
      } catch (e) {
        send('warn', `페이지 ${begIndex} 실패: ${(e as Error).message}`);
        break;
      }
    }

    if (newMatches.length === 0) {
      send('done', `수집 완료 — 신규 0건`);
      return;
    }

    if (this.kafkaService.isConnected()) {
      send('info', `Kafka로 ${newMatches.length}건 전송 중...`);
      let published = 0;
      for (const match of newMatches) {
        try {
          await this.kafkaService.publishMatch(match.matchId, match);
          published++;
        } catch (e) {
          send('warn', `Kafka 전송 실패 (${match.matchId}): ${(e as Error).message}`);
        }
      }
      send('done', `완료 — Kafka ${published}/${newMatches.length}건 전송`);
    } else {
      send('info', `Kafka 미연결 — main-service로 직접 저장 중 (${newMatches.length}건)...`);
      try {
        const res = await axios.post(`${mainServiceUrl}/api/matches/bulk`, { matches: newMatches });
        const result = res.data?.data as { saved?: number; total?: number } | undefined;
        send('done', `완료 — 직접 저장 ${result?.saved ?? newMatches.length}건 (누적 ${result?.total ?? '?'}건)`);
      } catch (e) {
        send('error', `직접 저장 실패: ${(e as Error).message}`);
      }
    }
  }
}
