import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { lazy, Suspense, useState, useEffect } from 'react';
import { setErrorHandler } from './lib/api/api';
import { DragonProvider } from './context/DragonContext';
import { Layout } from './components/layout/Layout';
import { MobileLayout } from './components/layout/MobileLayout';
import { ErrorModal } from './components/common/ErrorModal';
import { LoadingCenter } from './components/common/Spinner';
import { useIsMobile } from './hooks/useMobile';

// Desktop pages
const HomePage = lazy(() => import('./pages/HomePage').then(m => ({ default: m.HomePage })));
const MemberStatsListPage = lazy(() => import('./pages/MemberStatsListPage').then(m => ({ default: m.MemberStatsListPage })));
const PlayerStatsPage = lazy(() => import('./pages/PlayerStatsPage').then(m => ({ default: m.PlayerStatsPage })));
const ChampionStatsPage = lazy(() => import('./pages/ChampionStatsPage').then(m => ({ default: m.ChampionStatsPage })));
const ChampionListPage = lazy(() => import('./pages/ChampionListPage').then(m => ({ default: m.ChampionListPage })));
const MembersPage = lazy(() => import('./pages/MembersPage').then(m => ({ default: m.MembersPage })));
const MatchesPage = lazy(() => import('./pages/MatchesPage').then(m => ({ default: m.MatchesPage })));
const MatchDetailPage = lazy(() => import('./pages/MatchDetailPage').then(m => ({ default: m.MatchDetailPage })));
const LcuPage = lazy(() => import('./pages/LcuPage').then(m => ({ default: m.LcuPage })));
const SyncPage = lazy(() => import('./pages/SyncPage').then(m => ({ default: m.SyncPage })));
const AdminPage = lazy(() => import('./pages/AdminPage').then(m => ({ default: m.AdminPage })));
const TeamBuilderPage = lazy(() => import('./pages/TeamBuilderPage').then(m => ({ default: m.TeamBuilderPage })));
const StatsPage = lazy(() => import('./pages/StatsPage').then(m => ({ default: m.StatsPage })));
const RankingsPage = lazy(() => import('./pages/RankingsPage').then(m => ({ default: m.RankingsPage })));
const PlayerAnalysisPage = lazy(() => import('./pages/PlayerAnalysisPage').then(m => ({ default: m.PlayerAnalysisPage })));
const ChampionAnalysisPage = lazy(() => import('./pages/ChampionAnalysisPage').then(m => ({ default: m.ChampionAnalysisPage })));
const MatchAnalysisPage = lazy(() => import('./pages/MatchAnalysisPage').then(m => ({ default: m.MatchAnalysisPage })));
const EfficiencyPage = lazy(() => import('./pages/EfficiencyPage').then(m => ({ default: m.EfficiencyPage })));

// Mobile pages
const MobileHomePage = lazy(() => import('./pages/mobile/MobileHomePage').then(m => ({ default: m.MobileHomePage })));
const MobileStatsPage = lazy(() => import('./pages/mobile/MobileStatsPage').then(m => ({ default: m.MobileStatsPage })));
const MobileMatchesPage = lazy(() => import('./pages/mobile/MobileMatchesPage').then(m => ({ default: m.MobileMatchesPage })));
const MobilePlayerListPage = lazy(() => import('./pages/mobile/MobilePlayerListPage').then(m => ({ default: m.MobilePlayerListPage })));
const MobilePlayerDetailPage = lazy(() => import('./pages/mobile/MobilePlayerDetailPage').then(m => ({ default: m.MobilePlayerDetailPage })));
const MobileChampionListPage = lazy(() => import('./pages/mobile/MobileChampionListPage').then(m => ({ default: m.MobileChampionListPage })));
const MobileChampionDetailPage = lazy(() => import('./pages/mobile/MobileChampionDetailPage').then(m => ({ default: m.MobileChampionDetailPage })));
const MobileMorePage = lazy(() => import('./pages/mobile/MobileMorePage').then(m => ({ default: m.MobileMorePage })));
const MobileMembersPage = lazy(() => import('./pages/mobile/MobileMembersPage').then(m => ({ default: m.MobileMembersPage })));
const MobileAdminPage = lazy(() => import('./pages/mobile/MobileAdminPage').then(m => ({ default: m.MobileAdminPage })));
const MobileSyncPage = lazy(() => import('./pages/mobile/MobileSyncPage').then(m => ({ default: m.MobileSyncPage })));
const MobileLcuPage = lazy(() => import('./pages/mobile/MobileLcuPage').then(m => ({ default: m.MobileLcuPage })));
const MobileMatchDetailPage = lazy(() => import('./pages/mobile/MobileMatchDetailPage').then(m => ({ default: m.MobileMatchDetailPage })));

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
      <Suspense fallback={<LoadingCenter />}>
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
            <Route path="rankings" element={<RankingsPage />} />
            <Route path="player-analysis" element={<PlayerAnalysisPage />} />
            <Route path="champion-analysis" element={<ChampionAnalysisPage />} />
            <Route path="match-analysis" element={<MatchAnalysisPage />} />
            <Route path="efficiency" element={<EfficiencyPage />} />
            <Route path="reports" element={<StatsPage />} />
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
      </Suspense>
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
