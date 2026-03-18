import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/layout/Layout';
import { CollectPage } from './pages/CollectPage';
import { CustomGamePage } from './pages/CustomGamePage';
import { SummonerPage } from './pages/SummonerPage';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<CollectPage />} />
        <Route path="custom" element={<CustomGamePage />} />
        <Route path="summoner" element={<SummonerPage />} />
      </Route>
    </Routes>
  );
}
