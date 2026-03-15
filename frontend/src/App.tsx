import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useState } from 'react';
import { setErrorHandler } from './lib/api/api';
import { DragonProvider } from './context/DragonContext';
import { Layout } from './components/layout/Layout';
import { StatsPage } from './pages/StatsPage';
import { MemberStatsListPage } from './pages/MemberStatsListPage';
import { PlayerStatsPage } from './pages/PlayerStatsPage';
import { ChampionStatsPage } from './pages/ChampionStatsPage';
import { MembersPage } from './pages/MembersPage';
import { MatchesPage } from './pages/MatchesPage';
import { LcuPage } from './pages/LcuPage';
import { SyncPage } from './pages/SyncPage';
import { AdminPage } from './pages/AdminPage';
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
          <Route path="player-stats" element={<MemberStatsListPage />} />
          <Route path="player-stats/:riotId" element={<PlayerStatsPage />} />
          <Route path="stats/player/:riotId" element={<PlayerStatsPage />} />
          <Route path="stats/champion/:champion" element={<ChampionStatsPage />} />
          <Route path="members" element={<MembersPage />} />
          <Route path="matches" element={<MatchesPage />} />
          <Route path="lcu" element={<LcuPage />} />
          <Route path="sync" element={<SyncPage />} />
          <Route path="admin" element={<AdminPage />} />
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
