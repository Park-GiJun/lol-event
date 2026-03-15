import { Injectable, Logger } from '@nestjs/common';
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
  path.join(process.env.LOCALAPPDATA ?? '', 'Riot Games/League of Legends/lockfile'),
];

const lcuAgent = new https.Agent({ rejectUnauthorized: false });

@Injectable()
export class LcuService {
  private readonly logger = new Logger(LcuService.name);

  findLockfile(): string | null {
    for (const p of LOCKFILE_PATHS) {
      if (fs.existsSync(p)) return p;
    }
    return null;
  }

  parseLockfile(filePath: string): LcuCredentials {
    const [, , port, password] = fs.readFileSync(filePath, 'utf8').split(':');
    return { port, password };
  }

  async lcuGet<T>(port: string, password: string, endpoint: string): Promise<T> {
    const token = Buffer.from(`riot:${password}`).toString('base64');
    const res = await axios.get<T>(`https://127.0.0.1:${port}${endpoint}`, {
      headers: { Authorization: `Basic ${token}` },
      httpsAgent: lcuAgent,
    });
    return res.data;
  }

  async getStatus(): Promise<LcuStatus> {
    const lockfilePath = this.findLockfile();
    if (!lockfilePath) {
      return { connected: false, reason: 'lockfile 없음 — 롤 클라이언트를 실행해주세요' };
    }

    const { port, password } = this.parseLockfile(lockfilePath);
    try {
      const data = await this.lcuGet<Record<string, string>>(port, password, '/lol-summoner/v1/current-summoner');
      return {
        connected: true,
        gameName: data.gameName ?? data.displayName,
        tagLine: data.tagLine ?? '',
        puuid: data.puuid,
      };
    } catch {
      return { connected: false, reason: '클라이언트 응답 없음 — 로그인 확인' };
    }
  }

  getDebugInfo() {
    const lockfilePath = this.findLockfile();
    if (!lockfilePath) return { found: false, tried: LOCKFILE_PATHS };
    const raw = fs.readFileSync(lockfilePath, 'utf8');
    const parts = raw.split(':');
    return {
      found: true,
      lockfilePath,
      raw,
      parsed: { name: parts[0], pid: parts[1], port: parts[2], protocol: parts[4]?.trim() },
    };
  }
}
