import { EloLeaderboard } from '@/components/dashboard/EloLeaderboard';
import { ChampionTierTable } from '@/components/dashboard/ChampionTierTable';
import { BanTrendCard } from '@/components/dashboard/BanTrendCard';

const CURRENT_RIOT_ID_KEY = 'lol-event:currentRiotId';

export function HomePage() {
  const currentRiotId = localStorage.getItem(CURRENT_RIOT_ID_KEY) || undefined;

  return (
    <div className="flex flex-col gap-6">
      <section>
        <h2 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>
          Elo 리더보드
        </h2>
        <EloLeaderboard currentRiotId={currentRiotId} />
      </section>

      <section>
        <h2 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>
          챔피언 티어표
        </h2>
        <ChampionTierTable />
      </section>

      <section>
        <h2 className="page-title" style={{ marginBottom: 'var(--spacing-md)' }}>
          밴픽 트렌드
        </h2>
        <BanTrendCard />
      </section>
    </div>
  );
}
