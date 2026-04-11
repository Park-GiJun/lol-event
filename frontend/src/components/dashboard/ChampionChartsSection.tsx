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
    grid: { color: 'rgba(255,255,255,0.04)' },
    ticks: { color: '#7B8DB5', font: { size: 11 } },
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
        l.winRate >= 60 ? 'rgba(16,185,129,0.65)'
          : l.winRate >= 50 ? 'rgba(0,180,216,0.65)'
          : 'rgba(239,68,68,0.65)'
      ),
      borderColor: laneStats.map(l =>
        l.winRate >= 60 ? 'rgba(16,185,129,0.9)'
          : l.winRate >= 50 ? 'rgba(0,180,216,0.9)'
          : 'rgba(239,68,68,0.9)'
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
      backgroundColor: 'rgba(0,180,216,0.55)',
      borderColor: 'rgba(0,180,216,0.9)',
      borderWidth: 1,
      borderRadius: 5,
    }],
  };

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <div style={{
        fontWeight: 700, fontSize: 'var(--font-size-sm)',
        marginBottom: 16, color: 'var(--color-text-primary)',
        display: 'flex', alignItems: 'center', gap: 8,
      }}>
        <span style={{ color: 'var(--color-primary)' }}>📊</span>
        포지션별 통계 차트
      </div>
      <div className="grid-16">
        <div className="col-span-8" style={{ height: '220px' }}>
          <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>
            승률
          </div>
          <Bar data={winRateData} options={chartOptions} />
        </div>
        <div className="col-span-8" style={{ height: '220px' }}>
          <div style={{ fontSize: 11, color: 'var(--color-text-secondary)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 600 }}>
            KDA
          </div>
          <Bar data={kdaData} options={chartOptions} />
        </div>
      </div>
    </div>
  );
}
