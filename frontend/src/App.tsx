import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { setErrorHandler } from './lib/api/api';
import { DragonProvider } from './context/DragonContext';
import { Layout } from './components/layout/Layout';
import { MobileLayout } from './components/layout/MobileLayout';
import { HomePage } from './pages/HomePage';
import { MemberStatsListPage } from './pages/MemberStatsListPage';
import { PlayerStatsPage } from './pages/PlayerStatsPage';
import { ChampionStatsPage } from './pages/ChampionStatsPage';
import { ChampionListPage } from './pages/ChampionListPage';
import { MembersPage } from './pages/MembersPage';
import { MatchesPage } from './pages/MatchesPage';
import { MatchDetailPage } from './pages/MatchDetailPage';
import { LcuPage } from './pages/LcuPage';
import { SyncPage } from './pages/SyncPage';
import { AdminPage } from './pages/AdminPage';
import { TeamBuilderPage } from './pages/TeamBuilderPage';
import { ErrorModal } from './components/common/ErrorModal';
import { MobileHomePage } from './pages/mobile/MobileHomePage';
import { MobileStatsPage } from './pages/mobile/MobileStatsPage';
import { MobileMatchesPage } from './pages/mobile/MobileMatchesPage';
import { MobilePlayerListPage } from './pages/mobile/MobilePlayerListPage';
import { MobilePlayerDetailPage } from './pages/mobile/MobilePlayerDetailPage';
import { MobileChampionListPage } from './pages/mobile/MobileChampionListPage';
import { MobileChampionDetailPage } from './pages/mobile/MobileChampionDetailPage';
import { MobileMorePage } from './pages/mobile/MobileMorePage';
import { MobileMembersPage } from './pages/mobile/MobileMembersPage';
import { MobileAdminPage } from './pages/mobile/MobileAdminPage';
import { MobileSyncPage } from './pages/mobile/MobileSyncPage';
import { MobileLcuPage } from './pages/mobile/MobileLcuPage';
import { MobileMatchDetailPage } from './pages/mobile/MobileMatchDetailPage';
import { useIsMobile } from './hooks/useMobile';

function MobileRedirect() {
  const isMobile = useIsMobile();
  useEffect(() => {
    if (isMobile && !window.location.pathname.startsWith('/m')) {
      window.location.replace('/m');
    }
  }, [isMobile]);
  return null;
}

function App() {
  const [error, setError] = useState<{ title: string; message: string } | null>(null);

  setErrorHandler((title, message) => setError({ title, message }));

  return (
    <DragonProvider>
    <BrowserRouter>
      <MobileRedirect />
      <Routes>
        {/* Desktop routes */}
        <Route element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="player-stats" element={<MemberStatsListPage />} />
          <Route path="player-stats/:riotId" element={<PlayerStatsPage />} />
          <Route path="stats/player/:riotId" element={<PlayerStatsPage />} />
          <Route path="champions" element={<ChampionListPage />} />
          <Route path="stats/champion/:champion" element={<ChampionStatsPage />} />
          <Route path="members" element={<MembersPage />} />
          <Route path="matches" element={<MatchesPage />} />
          <Route path="matches/:matchId" element={<MatchDetailPage />} />
          <Route path="lcu" element={<LcuPage />} />
          <Route path="sync" element={<SyncPage />} />
          <Route path="admin" element={<AdminPage />} />
          <Route path="team-builder" element={<TeamBuilderPage />} />
        </Route>

        {/* Mobile routes */}
        <Route path="m" element={<MobileLayout />}>
          <Route index element={<MobileHomePage />} />
          <Route path="stats" element={<MobileStatsPage />} />
          <Route path="matches" element={<MobileMatchesPage />} />
          <Route path="match/:matchId" element={<MobileMatchDetailPage />} />
          <Route path="players" element={<MobilePlayerListPage />} />
          <Route path="player/:riotId" element={<MobilePlayerDetailPage />} />
          <Route path="champions" element={<MobileChampionListPage />} />
          <Route path="champion/:champion" element={<MobileChampionDetailPage />} />
          <Route path="more" element={<MobileMorePage />} />
          <Route path="members" element={<MobileMembersPage />} />
          <Route path="admin" element={<MobileAdminPage />} />
          <Route path="sync" element={<MobileSyncPage />} />
          <Route path="lcu" element={<MobileLcuPage />} />
          <Route path="*" element={<Navigate to="/m" replace />} />
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
