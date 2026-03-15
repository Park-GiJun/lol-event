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
