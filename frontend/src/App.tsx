import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useState } from 'react';
import { setErrorHandler } from './lib/api/api';
import { DragonProvider } from './context/DragonContext';
import { Layout } from './components/layout/Layout';
import { StatsPage } from './pages/StatsPage';
import { PlayerStatsPage } from './pages/PlayerStatsPage';
import { MembersPage } from './pages/MembersPage';
import { MatchesPage } from './pages/MatchesPage';
import { LcuPage } from './pages/LcuPage';
import { SyncPage } from './pages/SyncPage';
import { MonitoringPage } from './pages/MonitoringPage';
import { ErrorModal } from './components/common/ErrorModal';

function App() {
  const [error, setError] = useState<{ title: string; message: string } | null>(null);

  setErrorHandler((title, message) => setError({ title, message }));

  return (
    <DragonProvider>
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<StatsPage />} />
          <Route path="stats/player/:riotId" element={<PlayerStatsPage />} />
          <Route path="members" element={<MembersPage />} />
          <Route path="matches" element={<MatchesPage />} />
          <Route path="lcu" element={<LcuPage />} />
          <Route path="sync" element={<SyncPage />} />
          <Route path="monitoring" element={<MonitoringPage />} />
        </Route>
      </Routes>
      <ErrorModal
        isOpen={!!error}
        title={error?.title ?? ''}
        message={error?.message ?? ''}
        onClose={() => setError(null)}
      />
    </BrowserRouter>
    </DragonProvider>
  );
}

export default App;
