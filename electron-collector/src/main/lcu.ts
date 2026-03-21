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

export interface ChampSelectSlot {
  cellId: number;
  summonerId?: number;
  championId: number;
  assignedPosition: string;
  riotId: string;
  summonerName: string;
  isMe: boolean;
}

export interface ChampSelectFull {
  myTeam: ChampSelectSlot[];
  theirTeam: ChampSelectSlot[];
  bans: { championId: number; team: 'blue' | 'red' }[];
  phase: string;
  timer: number;
}

export async function getChampSelectFull(): Promise<ChampSelectFull | null> {
  const creds = getCredentials();
  if (!creds) return null;
  const { port, password } = creds;
  try {
    const session = await lcuGet<Record<string, unknown>>(port, password, '/lol-champ-select/v1/session');
    const myTeamRaw    = (session['myTeam']    as unknown[]) ?? [];
    const theirTeamRaw = (session['theirTeam'] as unknown[]) ?? [];
    const localCellId  = session['localPlayerCellId'] as number | undefined;
    const timerRaw     = session['timer'] as Record<string, unknown> | undefined;
    const phase        = String(timerRaw?.['phase'] ?? '');
    const remaining    = (timerRaw?.['adjustedTimeLeftInPhase'] ?? 0) as number;

    const resolveSlot = async (s: unknown): Promise<ChampSelectSlot> => {
      const slot        = s as Record<string, unknown>;
      const summonerId  = slot['summonerId'] as number | undefined;
      const championId  = (slot['championId'] as number) || 0;
      const cellId      = slot['cellId'] as number ?? 0;
      const assignedPosition = String(slot['assignedPosition'] ?? '');
      const isMe = cellId === localCellId;
      let riotId = '';
      let summonerName = '';
      if (summonerId) {
        const info = await summonerIdToInfo(port, password, summonerId);
        riotId       = info?.riotId ?? '';
        summonerName = info?.summonerName ?? '';
      }
      return { cellId, summonerId, championId, assignedPosition, riotId, summonerName, isMe };
    };

    let banIdx = 0;
    const bans: { championId: number; team: 'blue' | 'red' }[] = [];
    for (const actionGroup of ((session['actions'] as unknown[][]) ?? [])) {
      for (const a of actionGroup) {
        const action = a as { type: string; completed: boolean; championId: number };
        if (action.type === 'ban' && action.completed && action.championId) {
          bans.push({ championId: action.championId, team: banIdx < 5 ? 'blue' : 'red' });
          banIdx++;
        }
      }
    }

    const [myTeam, theirTeam] = await Promise.all([
      Promise.all(myTeamRaw.map(resolveSlot)),
      Promise.all(theirTeamRaw.map(resolveSlot)),
    ]);

    return { myTeam, theirTeam, bans, phase, timer: Math.ceil(remaining / 1000) };
  } catch {
    return null;
  }
}

export interface TeamMemberInfo {
  summonerName: string;
  riotId: string;
  isMe: boolean;
}

export interface CustomTeamsResult {
  phase: string;
  blueTeam: TeamMemberInfo[];
  redTeam: TeamMemberInfo[];
}

async function summonerIdToInfo(port: string, password: string, summonerId: number): Promise<{ summonerName: string; riotId: string } | null> {
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

export async function getCustomMostPicks(): Promise<CustomTeamsResult | null> {
  const creds = getCredentials();
  if (!creds) return null;
  const { port, password } = creds;

  try {
    const phase = await lcuGet<string>(port, password, '/lol-gameflow/v1/gameflow-phase');
    const cleanPhase = (phase as string).replace(/"/g, '').trim();

    if (cleanPhase === 'Lobby') {
      const lobby = await lcuGet<Record<string, unknown>>(port, password, '/lol-lobby/v2/lobby');
      const gameConfig = lobby['gameConfig'] as Record<string, unknown> | undefined;
      const localMember = lobby['localMember'] as Record<string, unknown> | undefined;
      const mySummonerId = localMember?.['summonerId'] as number | undefined;

      // customTeam100 = 블루팀, customTeam200 = 레드팀
      const team100 = (gameConfig?.['customTeam100'] as unknown[]) ?? [];
      const team200 = (gameConfig?.['customTeam200'] as unknown[]) ?? [];

      const toInfo = async (m: unknown): Promise<TeamMemberInfo | null> => {
        const member = m as Record<string, unknown>;
        const sid = member['summonerId'] as number | undefined;
        if (!sid) return null;
        const info = await summonerIdToInfo(port, password, sid);
        if (!info) return null;
        return { ...info, isMe: sid === mySummonerId };
      };

      const [blueTeam, redTeam] = await Promise.all([
        Promise.all(team100.map(toInfo)).then(r => r.filter((o): o is TeamMemberInfo => o !== null)),
        Promise.all(team200.map(toInfo)).then(r => r.filter((o): o is TeamMemberInfo => o !== null)),
      ]);

      return { phase: cleanPhase, blueTeam, redTeam };

    } else if (cleanPhase === 'ChampSelect') {
      const session = await lcuGet<Record<string, unknown>>(port, password, '/lol-gameflow/v1/session');
      const gameData = session['gameData'] as Record<string, unknown> | undefined;
      const queue = gameData?.['queue'] as Record<string, unknown> | undefined;
      const queueId = queue?.['id'] as number | undefined;
      if (queueId != null && queueId !== 0) return { phase: cleanPhase, blueTeam: [], redTeam: [] };

      const champSession = await lcuGet<Record<string, unknown>>(port, password, '/lol-champ-select/v1/session');
      const myTeamRaw = (champSession['myTeam'] as unknown[]) ?? [];
      const theirTeamRaw = (champSession['theirTeam'] as unknown[]) ?? [];
      const localCellId = champSession['localPlayerCellId'] as number | undefined;
      // cellId 0-4 = 블루팀, 5-9 = 레드팀
      const iAmBlue = localCellId == null || localCellId < 5;

      const toInfo = async (s: unknown): Promise<TeamMemberInfo | null> => {
        const member = s as Record<string, unknown>;
        const sid = member['summonerId'] as number | undefined;
        if (!sid) return null;
        const info = await summonerIdToInfo(port, password, sid);
        if (!info) return null;
        return { ...info, isMe: member['cellId'] === localCellId };
      };

      const myInfos = (await Promise.all(myTeamRaw.map(toInfo))).filter((o): o is TeamMemberInfo => o !== null);
      const theirInfos = (await Promise.all(theirTeamRaw.map(toInfo))).filter((o): o is TeamMemberInfo => o !== null);
      const [blueTeam, redTeam] = iAmBlue ? [myInfos, theirInfos] : [theirInfos, myInfos];

      return { phase: cleanPhase, blueTeam, redTeam };

    } else if (cleanPhase === 'InProgress') {
      const session = await lcuGet<Record<string, unknown>>(port, password, '/lol-gameflow/v1/session');
      const gameData = session['gameData'] as Record<string, unknown> | undefined;
      const queue = gameData?.['queue'] as Record<string, unknown> | undefined;
      const queueId = queue?.['id'] as number | undefined;
      if (queueId != null && queueId !== 0) return { phase: cleanPhase, blueTeam: [], redTeam: [] };

      const teamOne = (gameData?.['teamOne'] as unknown[]) ?? [];
      const teamTwo = (gameData?.['teamTwo'] as unknown[]) ?? [];
      const mySummonerId = (session['localPlayer'] as Record<string, unknown> | undefined)?.['summonerId'] as number | undefined;

      const toInfo = async (p: unknown): Promise<TeamMemberInfo | null> => {
        const player = p as Record<string, unknown>;
        const sid = player['summonerId'] as number | undefined;
        if (!sid) return null;
        const info = await summonerIdToInfo(port, password, sid);
        if (!info) return null;
        return { ...info, isMe: sid === mySummonerId };
      };

      const [blueTeam, redTeam] = await Promise.all([
        Promise.all(teamOne.map(toInfo)).then(r => r.filter((o): o is TeamMemberInfo => o !== null)),
        Promise.all(teamTwo.map(toInfo)).then(r => r.filter((o): o is TeamMemberInfo => o !== null)),
      ]);

      return { phase: cleanPhase, blueTeam, redTeam };

    } else {
      return { phase: cleanPhase, blueTeam: [], redTeam: [] };
    }
  } catch {
    return null;
  }
}
