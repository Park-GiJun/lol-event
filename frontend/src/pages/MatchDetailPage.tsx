import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Clock, Calendar } from 'lucide-react';
import { api } from '../lib/api/api';
import type { Match } from '../lib/types/match';
import { LoadingCenter } from '../components/common/Spinner';
import { Scoreboard, StatsTab, TeamInfoTab, TABS, QUEUE_LABEL, fmt } from './MatchesPage';
import type { Tab } from './MatchesPage';
import '../styles/pages/matches.css';

export function MatchDetailPage() {
  const { matchId } = useParams<{ matchId: string }>();
  const navigate = useNavigate();
  const [data, setData] = useState<Match | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<Tab>('요약');

  useEffect(() => {
    if (!matchId) {
      navigate('/matches', { replace: true });
      return;
    }
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    api.get<Match>(`/matches/${encodeURIComponent(matchId)}`)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [matchId, navigate]);

  if (!matchId) return null;

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button className="back-btn" onClick={() => navigate('/matches')}>
            <ChevronLeft size={18} />
          </button>
          <div>
            <h1 className="page-title">
              {data ? (QUEUE_LABEL[data.queueId] ?? data.queueId) : '경기 상세'}
            </h1>
            {data && (
              <p className="page-subtitle" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Clock size={11} />{fmt(data.gameDuration)}
                <Calendar size={11} />{new Date(data.gameCreation).toLocaleDateString('ko-KR')}
                {data.gameVersion && <span>v{data.gameVersion.split('.').slice(0, 2).join('.')}</span>}
              </p>
            )}
          </div>
        </div>
      </div>

      {loading ? (
        <LoadingCenter />
      ) : !data ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--color-text-secondary)' }}>
          경기 데이터를 불러올 수 없습니다.
        </div>
      ) : (
        <div className="card">
          <div className="match-tabs">
            {TABS.map(t => (
              <button key={t} className={`match-tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
                {t}
              </button>
            ))}
          </div>
          {tab === '요약'     && <Scoreboard match={data} />}
          {tab === '팀 정보' && <TeamInfoTab teams={data.teams ?? []} />}
          {(tab === '딜/피해' || tab === '경제' || tab === '시야/오브젝트' || tab === '멀티킬') && (
            <StatsTab match={data} tab={tab} />
          )}
        </div>
      )}
    </div>
  );
}
