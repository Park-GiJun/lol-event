import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip,
  Legend,
} from 'chart.js';
import type { ChartOptions } from 'chart.js';
import { Bar } from 'react-chartjs-2';
import type { ChampionLaneStat } from '@/lib/types/stats';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Tooltip,
  Legend,
);

const LANE_LABELS: Record<string, string> = {
  TOP:     '탑',
  JUNGLE:  '정글',
  MID:     '미드',
  BOTTOM:  '원딜',
  SUPPORT: '서폿',
};

interface Props {
  laneStats: ChampionLaneStat[];
}

export function ChampionChartsSection({ laneStats }: Props) {
  if (laneStats.length === 0) return null;

  const labels = laneStats.map(l => LANE_LABELS[l.position] ?? l.position);

  const axisStyle = {
    grid: { color: 'rgba(200,170,110,0.06)' },
    ticks: { color: '#A09B8C', font: { size: 11 } },
  };

  const chartOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        enabled: true,
        callbacks: {
          afterLabel: (ctx) => `${laneStats[ctx.dataIndex]?.games ?? 0}판`,
        },
      },
    },
    scales: {
      x: axisStyle,
      y: { ...axisStyle, beginAtZero: true },
    },
  };

  const winRateData = {
    labels,
    datasets: [{
      label: '승률 (%)',
      data: laneStats.map(l => l.winRate),
      backgroundColor: laneStats.map(l =>
        l.winRate >= 60 ? 'rgba(10,200,185,0.65)'
          : l.winRate >= 50 ? 'rgba(200,170,110,0.65)'
          : 'rgba(232,64,87,0.65)'
      ),
      borderColor: laneStats.map(l =>
        l.winRate >= 60 ? 'rgba(10,200,185,0.9)'
          : l.winRate >= 50 ? 'rgba(200,170,110,0.9)'
          : 'rgba(232,64,87,0.9)'
      ),
      borderWidth: 1,
      borderRadius: 5,
    }],
  };

  const kdaData = {
    labels,
    datasets: [{
      label: 'KDA',
      data: laneStats.map(l => l.kda),
      backgroundColor: 'rgba(200,170,110,0.55)',
      borderColor: 'rgba(200,170,110,0.9)',
      borderWidth: 1,
      borderRadius: 5,
    }],
  };

  return (
    <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
      <div className="section-head">
        <span className="icon-chip">📊</span>
        <span className="section-head-title">포지션별 통계 차트</span>
      </div>
      <div className="grid-16">
        <div className="col-span-8" style={{ height: '220px' }}>
          <div style={{
            fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)',
            marginBottom: 'var(--spacing-sm)', textTransform: 'uppercase',
            letterSpacing: 'var(--tracking-wide)', fontWeight: 'var(--font-weight-bold)',
          }}>
            승률
          </div>
          <Bar data={winRateData} options={chartOptions} />
        </div>
        <div className="col-span-8" style={{ height: '220px' }}>
          <div style={{
            fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)',
            marginBottom: 'var(--spacing-sm)', textTransform: 'uppercase',
            letterSpacing: 'var(--tracking-wide)', fontWeight: 'var(--font-weight-bold)',
          }}>
            KDA
          </div>
          <Bar data={kdaData} options={chartOptions} />
        </div>
      </div>
    </div>
  );
}
