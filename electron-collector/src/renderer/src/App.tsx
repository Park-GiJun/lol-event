import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/layout/Layout';
import { CollectPage } from './pages/CollectPage';
import { PlaceholderPage } from './pages/PlaceholderPage';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<CollectPage />} />
        <Route path="live-game" element={<PlaceholderPage title="현재 게임" sub="진행 중인 게임의 참가자 정보를 표시합니다" />} />
        <Route path="champ-select" element={<PlaceholderPage title="챔피언 선택" sub="챔피언 선택 화면의 실시간 정보를 표시합니다" />} />
        <Route path="summoner" element={<PlaceholderPage title="소환사 검색" sub="소환사 정보 및 전적을 조회합니다" />} />
      </Route>
    </Routes>
  );
}
