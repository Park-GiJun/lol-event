import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router-dom';
import { lazy, Suspense, useState } from 'react';
import { setErrorHandler } from './lib/api/api';
import { DragonProvider } from './context/DragonContext';
import { AppLayout } from './components/layout/AppLayout';
import { ErrorModal } from './components/common/ErrorModal';
import { LoadingCenter } from './components/common/Spinner';

// 개편된 화면
const HomePage = lazy(() => import('./pages/HomePage').then(m => ({ default: m.HomePage })));
const SummonerPage = lazy(() => import('./pages/SummonerPage').then(m => ({ default: m.SummonerPage })));

// 아직 개편 전인 화면. 새 셸 안에서 그대로 동작한다.
const MemberStatsListPage = lazy(() => import('./pages/MemberStatsListPage').then(m => ({ default: m.MemberStatsListPage })));
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

/** 경로에 낀 파라미터를 유지한 채 옮겨 준다. 예전에 공유한 링크가 죽지 않게. */
function RedirectParam({ to, param }: { to: string; param: string }) {
  const params = useParams();
  const value = params[param];
  return <Navigate to={value ? `${to}/${encodeURIComponent(value)}` : to} replace />;
}

function App() {
  const [error, setError] = useState<{ title: string; message: string } | null>(null);

  setErrorHandler((title, message) => setError({ title, message }));

  return (
    <DragonProvider>
      <BrowserRouter>
        <Suspense fallback={<LoadingCenter />}>
          <Routes>
            <Route element={<AppLayout />}>
              <Route index element={<HomePage />} />

              <Route path="players" element={<MemberStatsListPage />} />
              <Route path="players/:riotId" element={<SummonerPage />} />

              <Route path="champions" element={<ChampionListPage />} />
              <Route path="champions/:champion" element={<ChampionStatsPage />} />

              <Route path="matches" element={<MatchesPage />} />
              <Route path="matches/:matchId" element={<MatchDetailPage />} />

              <Route path="rankings" element={<RankingsPage />} />
              <Route path="player-analysis" element={<PlayerAnalysisPage />} />
              <Route path="champion-analysis" element={<ChampionAnalysisPage />} />
              <Route path="match-analysis" element={<MatchAnalysisPage />} />
              <Route path="efficiency" element={<EfficiencyPage />} />
              <Route path="reports" element={<StatsPage />} />

              <Route path="team-builder" element={<TeamBuilderPage />} />
              <Route path="members" element={<MembersPage />} />
              <Route path="lcu" element={<LcuPage />} />
              <Route path="sync" element={<SyncPage />} />
              <Route path="admin" element={<AdminPage />} />

              {/* 예전 경로. 데스크탑/모바일 두 벌이던 시절 링크를 그대로 살려 둔다. */}
              <Route path="player-stats" element={<Navigate to="/players" replace />} />
              <Route path="player-stats/:riotId" element={<RedirectParam to="/players" param="riotId" />} />
              <Route path="stats/player/:riotId" element={<RedirectParam to="/players" param="riotId" />} />
              <Route path="stats/champion/:champion" element={<RedirectParam to="/champions" param="champion" />} />

              <Route path="m" element={<Navigate to="/" replace />} />
              <Route path="m/stats" element={<Navigate to="/rankings" replace />} />
              <Route path="m/matches" element={<Navigate to="/matches" replace />} />
              <Route path="m/match/:matchId" element={<RedirectParam to="/matches" param="matchId" />} />
              <Route path="m/players" element={<Navigate to="/players" replace />} />
              <Route path="m/player/:riotId" element={<RedirectParam to="/players" param="riotId" />} />
              <Route path="m/champions" element={<Navigate to="/champions" replace />} />
              <Route path="m/champion/:champion" element={<RedirectParam to="/champions" param="champion" />} />
              <Route path="m/members" element={<Navigate to="/members" replace />} />
              <Route path="m/admin" element={<Navigate to="/admin" replace />} />
              <Route path="m/sync" element={<Navigate to="/sync" replace />} />
              <Route path="m/lcu" element={<Navigate to="/lcu" replace />} />
              <Route path="m/more" element={<Navigate to="/" replace />} />

              <Route path="*" element={<Navigate to="/" replace />} />
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
