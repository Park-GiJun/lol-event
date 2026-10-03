import { useQuery } from '@tanstack/react-query';
import { getEloLeaderboard } from '@/api/rating/ratingApi';

export function useLeaderboard() {
	return useQuery({
		queryKey: ['leaderboard'],
		queryFn: ({ signal }) => getEloLeaderboard({ signal })
	});
}
