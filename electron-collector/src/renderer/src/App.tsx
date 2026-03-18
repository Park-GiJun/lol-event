import { useEffect, useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/layout/Layout';
import { CollectPage } from './pages/CollectPage';
import { CustomGamePage } from './pages/CustomGamePage';
import { SummonerPage } from './pages/SummonerPage';

type UpdateState = 'checking' | 'downloading' | 'installing' | 'ready';

function UpdateOverlay({ state, progress }: { state: UpdateState; progress: number }) {
  const isChecking = state === 'checking';
  const isDownloading = state === 'downloading';
  const isInstalling = state === 'installing';

  const message = isChecking ? '업데이트 확인 중...'
    : isDownloading ? `업데이트 다운로드 중... ${progress}%`
    : '업데이트 설치 중... 잠시 후 재시작됩니다';

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
      background: '#0A1428',
      display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center',
      gap: 20,
    }}>
      <div style={{ fontSize: 22, fontWeight: 700, color: '#C89B3C', letterSpacing: 1 }}>LoL 수집기</div>
      <div style={{ fontSize: 13, color: '#8A9BB4' }}>{message}</div>
      {(isDownloading || isChecking) && (
        <div style={{ width: 240, height: 4, background: '#1E2D3D', borderRadius: 2, overflow: 'hidden' }}>
          <div style={{
            height: '100%',
            background: '#C89B3C',
            borderRadius: 2,
            transition: 'width 0.3s ease',
            width: isChecking ? '30%' : `${progress}%`,
            animation: isChecking ? 'pulse 1.5s ease-in-out infinite' : 'none',
          }} />
        </div>
      )}
      <style>{`
        @keyframes pulse { 0%,100% { opacity:0.4 } 50% { opacity:1 } }
      `}</style>
    </div>
  );
}

export default function App() {
  const [updateState, setUpdateState] = useState<UpdateState>('checking');
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    // 패키징되지 않은 개발 환경에서는 바로 ready
    if (!window.lol) { setUpdateState('ready'); return; }

    window.lol.onUpdateChecking(() => setUpdateState('checking'));
    window.lol.onUpdateNotAvailable(() => setUpdateState('ready'));
    window.lol.onUpdateAvailable(() => setUpdateState('downloading'));
    window.lol.onUpdateProgress((pct) => { setUpdateState('downloading'); setProgress(pct); });
    window.lol.onUpdateInstalling(() => setUpdateState('installing'));

    // 10초 타임아웃 — 응답 없으면 그냥 진행
    const timeout = setTimeout(() => setUpdateState('ready'), 10_000);
    return () => clearTimeout(timeout);
  }, []);

  return (
    <>
      {updateState !== 'ready' && (
        <UpdateOverlay state={updateState} progress={progress} />
      )}
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<CollectPage />} />
          <Route path="custom" element={<CustomGamePage />} />
          <Route path="summoner" element={<SummonerPage />} />
        </Route>
      </Routes>
    </>
  );
}
