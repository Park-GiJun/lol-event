import axios from 'axios';
import * as https from 'https';
import * as fs from 'fs';
import * as path from 'path';

export interface LcuCredentials { port: string; password: string; }
export interface LcuStatus {
  connected: boolean;
  gameName?: string;
  tagLine?: string;
  puuid?: string;
  reason?: string;
}

const LOCKFILE_PATHS = [
  'C:/Riot Games/League of Legends/lockfile',
  'C:/Program Files/Riot Games/League of Legends/lockfile',
  'C:/Program Files (x86)/Riot Games/League of Legends/lockfile',
  path.join(process.env['LOCALAPPDATA'] ?? '', 'Riot Games/League of Legends/lockfile'),
];

export const lcuAgent = new https.Agent({ rejectUnauthorized: false });

export function findLockfile(): string | null {
  for (const p of LOCKFILE_PATHS) {
    if (fs.existsSync(p)) return p;
  }
  return null;
}

export function parseLockfile(filePath: string): LcuCredentials {
  const [, , port, password] = fs.readFileSync(filePath, 'utf8').split(':');
  return { port: port.trim(), password: password.trim() };
}

export async function lcuGet<T>(port: string, password: string, endpoint: string): Promise<T> {
  const token = Buffer.from(`riot:${password}`).toString('base64');
  const res = await axios.get<T>(`https://127.0.0.1:${port}${endpoint}`, {
    headers: { Authorization: `Basic ${token}` },
    httpsAgent: lcuAgent,
  });
  return res.data;
}

function getCredentials(): LcuCredentials | null {
  const lockfilePath = findLockfile();
  if (!lockfilePath) return null;
  return parseLockfile(lockfilePath);
}

export async function getStatus(): Promise<LcuStatus> {
  const creds = getCredentials();
  if (!creds) return { connected: false, reason: 'lockfile 없음 — 롤 클라이언트를 실행해주세요' };
  const { port, password } = creds;
  try {
    const data = await lcuGet<Record<string, string>>(port, password, '/lol-summoner/v1/current-summoner');
    return { connected: true, gameName: data['gameName'] ?? data['displayName'], tagLine: data['tagLine'] ?? '', puuid: data['puuid'] };
  } catch {
    return { connected: false, reason: '클라이언트 응답 없음 — 로그인 확인' };
  }
}

export async function getLiveGame(): Promise<Record<string, unknown> | null> {
  const creds = getCredentials();
  if (!creds) return null;
  const { port, password } = creds;
  try {
    const phase = await lcuGet<string>(port, password, '/lol-gameflow/v1/gameflow-phase');
    if (phase !== 'InProgress') return { phase };
    const session = await lcuGet<Record<string, unknown>>(port, password, '/lol-gameflow/v1/session');
    return { phase, session };
  } catch {
    return null;
  }
}

export async function getChampSelect(): Promise<Record<string, unknown> | null> {
  const creds = getCredentials();
  if (!creds) return null;
  const { port, password } = creds;
  try {
    const session = await lcuGet<Record<string, unknown>>(port, password, '/lol-champ-select/v1/session');
    return session;
  } catch {
    return null;
  }
}

export async function getSummonerHistory(puuid: string): Promise<Record<string, unknown> | null> {
  const creds = getCredentials();
  if (!creds) return null;
  const { port, password } = creds;
  try {
    const data = await lcuGet<Record<string, unknown>>(
      port, password,
      `/lol-match-history/v1/products/lol/${puuid}/matches?begIndex=0&endIndex=19`
    );
    return data;
  } catch {
    return null;
  }
}

export interface OpponentInfo {
  summonerName: string;
  riotId: string; // gameName#tagLine 형식
}

export interface CustomMostPicksResult {
  isCustom: boolean;
  phase: string;
  opponents: OpponentInfo[];
}

async function summonerIdToInfo(port: string, password: string, summonerId: number): Promise<OpponentInfo | null> {
  try {
    const summoner = await lcuGet<Record<string, unknown>>(port, password, `/lol-summoner/v1/summoners/${summonerId}`);
    const gameName = (summoner['gameName'] as string) ?? '';
    const tagLine = (summoner['tagLine'] as string) ?? '';
    const displayName = (summoner['displayName'] as string) ?? gameName;
    return {
      riotId: gameName ? `${gameName}#${tagLine}` : displayName,
      summonerName: displayName || gameName,
    };
  } catch {
    return null;
  }
}

export async function getCustomMostPicks(): Promise<CustomMostPicksResult | null> {
  const creds = getCredentials();
  if (!creds) return null;
  const { port, password } = creds;

  try {
    const phase = await lcuGet<string>(port, password, '/lol-gameflow/v1/gameflow-phase');
    const cleanPhase = (phase as string).replace(/"/g, '').trim();

    if (cleanPhase === 'Lobby') {
      const lobby = await lcuGet<Record<string, unknown>>(port, password, '/lol-lobby/v2/lobby');

      const localMember = lobby['localMember'] as Record<string, unknown> | undefined;
      const myTeam = localMember?.['team'];
      const mySummonerId = localMember?.['summonerId'];
      const members = (lobby['members'] as unknown[]) ?? [];

      // 팀 정보가 있으면 다른 팀만, 없으면 자신 summonerId 제외
      const opponentMembers = (myTeam != null)
        ? members.filter((m) => (m as Record<string, unknown>)['team'] !== myTeam)
        : members.filter((m) => (m as Record<string, unknown>)['summonerId'] !== mySummonerId);

      const opponents = (await Promise.all(
        opponentMembers.map((m) => {
          const summonerId = (m as Record<string, unknown>)['summonerId'] as number | undefined;
          return summonerId ? summonerIdToInfo(port, password, summonerId) : null;
        })
      )).filter((o): o is OpponentInfo => o !== null);

      return { isCustom: true, phase: cleanPhase, opponents };

    } else if (cleanPhase === 'ChampSelect' || cleanPhase === 'InProgress') {
      const session = await lcuGet<Record<string, unknown>>(port, password, '/lol-gameflow/v1/session');
      const gameData = session['gameData'] as Record<string, unknown> | undefined;
      const queue = gameData?.['queue'] as Record<string, unknown> | undefined;
      const queueId = queue?.['id'] as number | undefined;
      // queueId 0 = 커스텀, null/undefined도 커스텀으로 간주
      if (queueId != null && queueId !== 0) return { isCustom: false, phase: cleanPhase, opponents: [] };

      if (cleanPhase === 'ChampSelect') {
        const champSession = await lcuGet<Record<string, unknown>>(port, password, '/lol-champ-select/v1/session');
        const theirTeam = (champSession['theirTeam'] as unknown[]) ?? [];

        const opponents = (await Promise.all(
          theirTeam.map((s) => {
            const summonerId = (s as Record<string, unknown>)['summonerId'] as number | undefined;
            return summonerId ? summonerIdToInfo(port, password, summonerId) : null;
          })
        )).filter((o): o is OpponentInfo => o !== null);

        return { isCustom: true, phase: cleanPhase, opponents };
      }

      // InProgress: gameflow session에서 팀 정보 추출
      const teamOne = (gameData?.['teamOne'] as unknown[]) ?? [];
      const teamTwo = (gameData?.['teamTwo'] as unknown[]) ?? [];
      const mySummonerId = (session['localPlayer'] as Record<string, unknown> | undefined)?.['summonerId'];

      // 내가 속한 팀 판별
      const inTeamOne = teamOne.some((p) => (p as Record<string, unknown>)['summonerId'] === mySummonerId);
      const opponentTeam = inTeamOne ? teamTwo : teamOne;

      const opponents = (await Promise.all(
        opponentTeam.map((p) => {
          const summonerId = (p as Record<string, unknown>)['summonerId'] as number | undefined;
          return summonerId ? summonerIdToInfo(port, password, summonerId) : null;
        })
      )).filter((o): o is OpponentInfo => o !== null);

      return { isCustom: true, phase: cleanPhase, opponents };

    } else {
      return { isCustom: false, phase: cleanPhase, opponents: [] };
    }
  } catch {
    return null;
  }
}
