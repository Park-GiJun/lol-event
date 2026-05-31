import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import type { ChartOptions, ScriptableLineSegmentContext } from 'chart.js';
import { Line, Bar } from 'react-chartjs-2';
import { useDragon } from '@/context/DragonContext';
import type { PlayerEloHistoryResult, ChampionStat } from '@/lib/types/stats';

ChartJS.register(
  CategoryScale, LinearScale, PointElement, LineElement,
  BarElement, Tooltip, Legend, Filler,
);

interface Props {
  eloHistory: PlayerEloHistoryResult | null;
  championStats: ChampionStat[];
}

// Hextech 테마 색 (Chart.js는 리터럴 색이 필요하므로 CSS 변수와 동일한 값을 사용)
const THEME = {
  win: '#0AC8B9',      // var(--color-win)
  loss: '#E84057',     // var(--color-loss)
  grid: 'rgba(200, 170, 110, 0.06)',
  tick: '#A09B8C',     // var(--color-text-secondary)
} as const;

export function PlayerChartsSection({ eloHistory, championStats }: Props) {
  const { champions } = useDragon();

  const hasElo = eloHistory !== null && eloHistory.history.length > 0;
  const hasChamp = championStats.length > 0;
  if (!hasElo && !hasChamp) return null;

  // Elo 차트 데이터 — history는 최신이 앞에 있으므로 역순 처리
  const reversed = hasElo ? [...eloHistory!.history].reverse() : [];
  const eloLabels = reversed.map(h => {
    const d = new Date(h.gameCreation);
    return `${d.getMonth() + 1}/${d.getDate()}`;
  });
  const eloValues = reversed.map(h => h.eloAfter);

  const eloOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { enabled: true },
    },
    scales: {
      x: {
        grid: { color: THEME.grid },
        ticks: { color: THEME.tick, font: { size: 11 } },
      },
      y: {
        beginAtZero: false,
        grid: { color: THEME.grid },
        ticks: { color: THEME.tick, font: { size: 11 } },
      },
    },
    elements: {
      line: { tension: 0.3 },
    },
  };

  const eloData = {
    labels: eloLabels,
    datasets: [{
      label: 'Elo',
      data: eloValues,
      borderWidth: 2,
      borderColor: THEME.win,
      pointRadius: 3,
      fill: false,
      segment: {
        borderColor: (ctx: ScriptableLineSegmentContext) =>
          (ctx.p1.parsed.y ?? 0) >= (ctx.p0.parsed.y ?? 0) ? THEME.win : THEME.loss,
      },
      pointBackgroundColor: eloValues.map((v, i) =>
        i === 0 ? THEME.win : v >= eloValues[i - 1] ? THEME.win : THEME.loss
      ),
    }],
  };

  // 챔피언 차트 데이터 — 판수 기준 상위 8개
  const topChamps = [...championStats].sort((a, b) => b.games - a.games).slice(0, 8);
  const champLabels = topChamps.map(c => champions.get(c.championId)?.nameKo ?? c.champion);

  const champOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        enabled: true,
        callbacks: {
          afterLabel: (ctx) => `${topChamps[ctx.dataIndex]?.games ?? 0}판`,
        },
      },
    },
    scales: {
      x: {
        grid: { color: THEME.grid },
        ticks: { color: THEME.tick, font: { size: 11 } },
      },
      y: {
        beginAtZero: true,
        max: 100,
        grid: { color: THEME.grid },
        ticks: { color: THEME.tick, font: { size: 11 } },
      },
    },
  };

  const champData = {
    labels: champLabels,
    datasets: [{
      label: '승률 (%)',
      data: topChamps.map(c => c.winRate),
      backgroundColor: topChamps.map(c =>
        c.winRate >= 50 ? 'rgba(10,200,185,0.6)' : 'rgba(232,64,87,0.6)'
      ),
      borderColor: topChamps.map(c =>
        c.winRate >= 50 ? 'rgba(10,200,185,0.9)' : 'rgba(232,64,87,0.9)'
      ),
      borderWidth: 1,
      borderRadius: 4,
    }],
  };

  return (
    <div className="card" style={{ marginBottom: 'var(--spacing-lg)' }}>
      <div className="section-head">
        <span className="icon-chip">📈</span>
        <span className="section-head-title">통계 차트</span>
      </div>
      <div className="grid-16">
        {hasElo && (
          <div className="col-span-8" style={{ height: '220px' }}>
            <div className="hero-eyebrow" style={{ marginBottom: 'var(--spacing-sm)' }}>
              Elo 추이
            </div>
            <Line data={eloData} options={eloOptions} />
          </div>
        )}
        {hasChamp && (
          <div className="col-span-8" style={{ height: '220px' }}>
            <div className="hero-eyebrow" style={{ marginBottom: 'var(--spacing-sm)' }}>
              챔피언별 승률
            </div>
            <Bar data={champData} options={champOptions} />
          </div>
        )}
      </div>
    </div>
  );
}
