import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/layout/Layout';
import { CollectPage } from './pages/CollectPage';
import { LiveGamePage } from './pages/LiveGamePage';
import { ChampSelectPage } from './pages/ChampSelectPage';
import { SummonerPage } from './pages/SummonerPage';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<CollectPage />} />
        <Route path="live-game" element={<LiveGamePage />} />
        <Route path="champ-select" element={<ChampSelectPage />} />
        <Route path="summoner" element={<SummonerPage />} />
      </Route>
    </Routes>
  );
}
