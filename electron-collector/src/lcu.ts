import axios from 'axios';
import * as https from 'https';
import * as fs from 'fs';
import * as path from 'path';

export interface LcuCredentials {
  port: string;
  password: string;
}

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
  const raw = fs.readFileSync(filePath, 'utf8');
  const [, , port, password] = raw.split(':');
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

export async function getStatus(): Promise<LcuStatus> {
  const lockfilePath = findLockfile();
  if (!lockfilePath) {
    return { connected: false, reason: 'lockfile 없음 — 롤 클라이언트를 실행해주세요' };
  }
  const { port, password } = parseLockfile(lockfilePath);
  try {
    const data = await lcuGet<Record<string, string>>(port, password, '/lol-summoner/v1/current-summoner');
    return {
      connected: true,
      gameName: data['gameName'] ?? data['displayName'],
      tagLine: data['tagLine'] ?? '',
      puuid: data['puuid'],
    };
  } catch {
    return { connected: false, reason: '클라이언트 응답 없음 — 로그인 확인' };
  }
}
