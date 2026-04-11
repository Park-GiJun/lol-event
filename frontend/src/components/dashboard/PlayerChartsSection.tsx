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
        grid: { color: 'rgba(255,255,255,0.04)' },
        ticks: { color: '#7B8DB5', font: { size: 11 } },
      },
      y: {
        beginAtZero: false,
        grid: { color: 'rgba(255,255,255,0.04)' },
        ticks: { color: '#7B8DB5', font: { size: 11 } },
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
      borderColor: '#10B981',
      pointRadius: 3,
      fill: false,
      segment: {
        borderColor: (ctx: ScriptableLineSegmentContext) =>
          (ctx.p1.parsed.y ?? 0) >= (ctx.p0.parsed.y ?? 0) ? '#10B981' : '#EF4444',
      },
      pointBackgroundColor: eloValues.map((v, i) =>
        i === 0 ? '#10B981' : v >= eloValues[i - 1] ? '#10B981' : '#EF4444'
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
        grid: { color: 'rgba(255,255,255,0.04)' },
        ticks: { color: '#7B8DB5', font: { size: 11 } },
      },
      y: {
        beginAtZero: true,
        max: 100,
        grid: { color: 'rgba(255,255,255,0.04)' },
        ticks: { color: '#7B8DB5', font: { size: 11 } },
      },
    },
  };

  const champData = {
    labels: champLabels,
    datasets: [{
      label: '승률 (%)',
      data: topChamps.map(c => c.winRate),
      backgroundColor: topChamps.map(c =>
        c.winRate >= 50 ? 'rgba(16,185,129,0.6)' : 'rgba(239,68,68,0.6)'
      ),
      borderColor: topChamps.map(c =>
        c.winRate >= 50 ? 'rgba(16,185,129,0.9)' : 'rgba(239,68,68,0.9)'
      ),
      borderWidth: 1,
      borderRadius: 4,
    }],
  };

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <div style={{
        fontWeight: 700, fontSize: 'var(--font-size-sm)',
        marginBottom: 16, color: 'var(--color-text-primary)',
        display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <span style={{ color: 'var(--color-primary)' }}>📈</span>
        통계 차트
      </div>
      <div className="grid-16">
        {hasElo && (
          <div className="col-span-8" style={{ height: '220px' }}>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>
              Elo 추이
            </div>
            <Line data={eloData} options={eloOptions} />
          </div>
        )}
        {hasChamp && (
          <div className="col-span-8" style={{ height: '220px' }}>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>
              챔피언별 승률
            </div>
            <Bar data={champData} options={champOptions} />
          </div>
        )}
      </div>
    </div>
  );
}
