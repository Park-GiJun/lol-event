import express from 'express';
import cors from 'cors';
import { Server } from 'http';
import { getStatus } from './lcu';
import { runCollect, getCollectedMatches, getIsCollecting } from './collect';

let httpServer: Server | null = null;

const ALLOWED_ORIGINS = [
  'http://localhost:5173',
  'http://localhost:4173',
  'http://localhost:3000',
  'https://gijun.net',
  'https://www.gijun.net',
];

export function startServer(port: number): Promise<void> {
  const app = express();

  app.use(cors({
    origin: ALLOWED_ORIGINS,
    credentials: true,
  }));

  app.use(express.json());

  // LCU 연결 상태
  app.get('/api/lcu/status', async (_req, res) => {
    const status = await getStatus();
    res.json(status);
  });

  // SSE — 매치 수집
  app.get('/api/lcu/collect', (req, res) => {
    if (getIsCollecting()) {
      res.setHeader('Content-Type', 'text/event-stream');
      res.setHeader('Cache-Control', 'no-cache');
      res.setHeader('Connection', 'keep-alive');
      res.flushHeaders();
      const data = JSON.stringify({ type: 'error', message: '이미 수집 중입니다' });
      res.write(`data: ${data}\n\n`);
      res.end();
      return;
    }

    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders();

    const send = (type: string, message: string) => {
      const data = JSON.stringify({ type, message });
      res.write(`data: ${data}\n\n`);
    };

    // 클라이언트 연결 끊김 처리
    req.on('close', () => res.end());

    runCollect(send).finally(() => {
      try { res.end(); } catch { /* already ended */ }
    });
  });

  // 수집된 매치 목록 반환
  app.get('/api/matches', (_req, res) => {
    res.json(getCollectedMatches());
  });

  return new Promise((resolve, reject) => {
    httpServer = app.listen(port, '127.0.0.1', () => {
      console.log(`✅ LoL 수집기 서버 → http://localhost:${port}`);
      resolve();
    });
    httpServer.on('error', reject);
  });
}

export function stopServer(): void {
  httpServer?.close();
}
