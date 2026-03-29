import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../../lib/api/api';
import type {
  SurvivalIndexResult,
  JungleDominanceResult,
  SupportImpactResult,
  PlaystyleDnaResult,
  GameLengthTendencyResult,
  GoldEfficiencyResult,
  GrowthCurveResult,
  KillParticipationResult,
  PositionChampionPoolResult,
  PlayerPositionEntry,
} from '../../../lib/types/stats';
import { useDragon } from '../../../context/DragonContext';
import { LoadingCenter } from '../../../components/common/Spinner';

const MODES = [
  { value: 'normal', label: '5v5' },
  { value: 'aram', label: '칼바람' },
  { value: 'all', label: '전체' },
];

type PersonalSubTab = '생존지수' | '정글점령' | '서폿기여' | 'DNA' | '게임성향' | '골드효율' | '성장곡선' | 'KP랭킹' | '포지션풀';
const PERSONAL_SUB_TABS: PersonalSubTab[] = ['생존지수', '정글점령', '서폿기여', 'DNA', '게임성향', '골드효율', '성장곡선', 'KP랭킹', '포지션풀'];

function SurvivalTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-survival', mode],
    queryFn: () => api.get<SurvivalIndexResult>(`/stats/survival-index?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">탱킹/생존 지수 순위</p>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
              </div>
              <span style={{ fontSize: 16, fontWeight: 800, color: '#2196F3' }}>{p.survivalIndex.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip">받은딜 {Math.round(p.avgDamageTaken).toLocaleString()}</span>
              <span className="m-stat-chip">경감률 {(p.avgMitigationRatio * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">생존율 {(p.avgSurvivalRatio * 100).toFixed(0)}%</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

function JungleTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-jungle', mode],
    queryFn: () => api.get<JungleDominanceResult>(`/stats/jungle-dominance?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">정글 점령 지수 순위</p>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        const c = p.topChampionId ? champions.get(p.topChampionId) : null;
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1 }}>
                {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={28} height={28} style={{ borderRadius: 4 }} />}
                <div>
                  <span className="m-player-name">{name}</span>
                  {tag && <span className="m-player-tag"> #{tag}</span>}
                  <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{p.playStyleTag}</div>
                </div>
              </div>
              <span style={{ fontSize: 16, fontWeight: 800, color: '#4CAF50' }}>{p.avgJungleDominance.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip">침입 {(p.avgInvadeRatio * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">오브젝트 {(p.avgObjShare * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">KP {(p.avgKp * 100).toFixed(0)}%</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

function SupportTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-support', mode],
    queryFn: () => api.get<SupportImpactResult>(`/stats/support-impact?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      <p className="m-section-title">서폿/힐러 기여도 순위</p>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        const c = p.topChampionId ? champions.get(p.topChampionId) : null;
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1 }}>
                {c?.imageUrl && <img src={c.imageUrl} alt={c.nameKo} width={28} height={28} style={{ borderRadius: 4 }} />}
                <div>
                  <span className="m-player-name">{name}</span>
                  {tag && <span className="m-player-tag"> #{tag}</span>}
                  <div style={{ fontSize: 10, color: 'var(--color-text-secondary)' }}>{p.roleTag}</div>
                </div>
              </div>
              <span style={{ fontSize: 16, fontWeight: 800, color: '#E91E8A' }}>{p.supportImpact.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip">힐 {(p.avgHealShare * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">CC {(p.avgCcShare * 100).toFixed(0)}%</span>
              <span className="m-stat-chip">시야 {(p.avgVisionShare * 100).toFixed(0)}%</span>
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

function DnaBar({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ marginBottom: 4 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, marginBottom: 2 }}>
        <span style={{ color: 'var(--color-text-secondary)' }}>{label}</span>
        <span style={{ color }}>{value.toFixed(0)}</span>
      </div>
      <div style={{ height: 4, borderRadius: 2, background: 'var(--color-bg-hover)' }}>
        <div style={{ height: '100%', borderRadius: 2, background: color, width: `${Math.min(value, 100)}%` }} />
      </div>
    </div>
  );
}

function DnaTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-dna', mode],
    queryFn: () => api.get<PlaystyleDnaResult>(`/stats/playstyle-dna?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.players.map(p => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
                <span className="m-stat-chip" style={{ marginLeft: 8 }}>{p.styleTag}</span>
              </div>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{p.games}게임</span>
            </div>
            <div style={{ marginTop: 8, padding: '0 4px' }}>
              <DnaBar label="공격성" value={p.aggression} color="#FF6B2B" />
              <DnaBar label="생존력" value={p.durability} color="#2196F3" />
              <DnaBar label="팀플레이" value={p.teamPlay} color="#4CAF50" />
              <DnaBar label="오브젝트" value={p.objectiveFocus} color="#FFD700" />
              <DnaBar label="골드효율" value={p.economy} color="#E91E8A" />
              <DnaBar label="시야장악" value={p.visionControl} color="#9C27B0" />
            </div>
          </div>
        );
      })}
      {data.players.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

function GameLengthTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-gamelength', mode],
    queryFn: () => api.get<GameLengthTendencyResult>(`/stats/game-length-tendency?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const LENGTH_LABEL: Record<string, string> = {
    SHORT_GAME: '단기전 강자', MID_GAME: '중반전 강자', LONG_GAME: '장기전 강자', BALANCED: '밸런스형',
  };

  return (
    <div>
      <p className="m-section-title">플레이어별 게임 길이 성향</p>
      {data.players.map(p => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
                <div style={{ fontSize: 11, color: 'var(--color-primary)', marginTop: 1 }}>
                  {LENGTH_LABEL[p.tendency] ?? p.tendency}
                </div>
              </div>
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{p.totalGames}게임</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">단기 {p.shortGame.winRate.toFixed(0)}% ({p.shortGame.games})</span>
              <span className="m-stat-chip">중반 {p.midGame.winRate.toFixed(0)}% ({p.midGame.games})</span>
              <span className="m-stat-chip">장기 {p.longGame.winRate.toFixed(0)}% ({p.longGame.games})</span>
            </div>
          </div>
        );
      })}
      {data.players.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

function GoldEffTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['mobile-goldeff', mode],
    queryFn: () => api.get<GoldEfficiencyResult>(`/stats/gold-efficiency?mode=${mode}`),
  });

  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {(data.dmgEfficiencyKing || data.visionEfficiencyKing || data.csEfficiencyKing) && (
        <>
          <p className="m-section-title">효율 왕</p>
          <div className="m-card" style={{ marginBottom: 12 }}>
            {data.dmgEfficiencyKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.dmgEfficiencyKing!)}`)}>
                <span style={{ fontSize: 14, width: 28 }}>⚔️</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>딜효율왕</span>
                <span style={{ fontWeight: 700 }}>{data.dmgEfficiencyKing.split('#')[0]}</span>
              </div>
            )}
            {data.visionEfficiencyKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.visionEfficiencyKing!)}`)}>
                <span style={{ fontSize: 14, width: 28 }}>👁️</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>시야효율왕</span>
                <span style={{ fontWeight: 700 }}>{data.visionEfficiencyKing.split('#')[0]}</span>
              </div>
            )}
            {data.csEfficiencyKing && (
              <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.csEfficiencyKing!)}`)}>
                <span style={{ fontSize: 14, width: 28 }}>🌾</span>
                <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 70 }}>CS효율왕</span>
                <span style={{ fontWeight: 700 }}>{data.csEfficiencyKing.split('#')[0]}</span>
              </div>
            )}
          </div>
        </>
      )}
      <p className="m-section-title">골드 효율 순위</p>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
              </div>
              <span style={{ fontSize: 16, fontWeight: 800, color: '#FFD700' }}>{p.goldEfficiencyScore.toFixed(1)}</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip">딜/골드 {p.avgDmgPerGold.toFixed(2)}</span>
              <span className="m-stat-chip">CS/골드 {p.avgCsPerGold.toFixed(2)}</span>
              {p.tags.map(t => <span key={t} className="m-stat-chip" style={{ color: '#FFD700', fontSize: 10 }}>{t}</span>)}
            </div>
          </div>
        );
      })}
      {data.rankings.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

function GrowthTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const [riotId, setRiotId] = useState('');
  const [query, setQuery] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['mobile-growth', query, mode],
    queryFn: () => query ? api.get<GrowthCurveResult>(`/stats/player/${encodeURIComponent(query)}/growth-curve?mode=${mode}`) : null,
    enabled: !!query,
  });

  const TREND_COLOR = { IMPROVING: '#4CAF50', DECLINING: '#FF4757', STABLE: '#9E9E9E' };
  const TREND_LABEL = { IMPROVING: '상승 중', DECLINING: '하락 중', STABLE: '안정적' };

  return (
    <div>
      <div className="m-card" style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <input value={riotId} onChange={e => setRiotId(e.target.value)} placeholder="닉네임#태그"
            style={{ flex: 1, padding: '8px 10px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--color-bg-hover)', color: 'var(--color-text-primary)', fontSize: 12 }} />
          <button onClick={() => riotId && setQuery(riotId)}
            style={{ padding: '8px 16px', borderRadius: 8, background: 'var(--color-primary)', color: '#fff', border: 'none', fontWeight: 700, cursor: 'pointer' }}>
            조회
          </button>
        </div>
      </div>

      {isLoading && <LoadingCenter />}
      {data && (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <button onClick={() => navigate(`/m/player/${encodeURIComponent(data.riotId)}`)}
              style={{ fontWeight: 700, fontSize: 15, background: 'none', border: 'none', color: 'var(--color-text-primary)', cursor: 'pointer', padding: 0 }}>
              {data.riotId.split('#')[0]}
            </button>
            <span style={{ fontSize: 13, fontWeight: 700, color: TREND_COLOR[data.trend] }}>
              {TREND_LABEL[data.trend]}
            </span>
          </div>
          <div className="m-card" style={{ marginBottom: 12 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, textAlign: 'center' }}>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700 }}>{data.totalGames}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>총 게임</div>
              </div>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--color-primary)' }}>{data.recentAvgKda.toFixed(2)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>최근 KDA</div>
              </div>
              <div>
                <div style={{ fontSize: 18, fontWeight: 700 }}>{data.overallAvgKda.toFixed(2)}</div>
                <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>전체 KDA</div>
              </div>
            </div>
          </div>
          <p className="m-section-title">최근 경기 기록</p>
          {data.entries.slice(-15).reverse().map((e, i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 0', borderBottom: '1px solid var(--color-border)' }}>
              <div style={{ width: 6, height: 6, borderRadius: '50%', background: e.win ? 'var(--color-win)' : 'var(--color-loss)', flexShrink: 0 }} />
              <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 40 }}>
                {new Date(e.gameCreation).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' })}
              </span>
              <span style={{ fontSize: 12, flex: 1 }}>{e.champion}</span>
              <span className="m-stat-chip" style={{ color: e.rollingKda >= data.overallAvgKda ? 'var(--color-win)' : 'inherit' }}>
                KDA {e.kda.toFixed(2)}
              </span>
              <span className="m-stat-chip">추세 {e.rollingKda.toFixed(2)}</span>
            </div>
          ))}
        </div>
      )}
      {!data && !isLoading && query && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

function MobileKpTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { data, isLoading } = useQuery({
    queryKey: ['m-kp', mode],
    queryFn: () => api.get<KillParticipationResult>(`/stats/kill-participation?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  return (
    <div>
      {data.kpKing && (
        <div className="m-card" style={{ marginBottom: 12 }}>
          <div className="m-leader-row" onClick={() => navigate(`/m/player/${encodeURIComponent(data.kpKing!)}`)}>
            <span style={{ fontSize: 16, width: 28 }}>⚡</span>
            <span style={{ fontSize: 12, color: 'var(--color-text-secondary)', width: 50 }}>KP왕</span>
            <span style={{ fontWeight: 700 }}>{data.kpKing.split('#')[0]}</span>
          </div>
        </div>
      )}
      <p className="m-section-title">킬 가담률 순위</p>
      {data.rankings.map((p, i) => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
              </div>
              <span style={{ fontSize: 16, fontWeight: 800, color: p.avgKp >= 70 ? '#4CAF50' : 'var(--color-primary)' }}>{p.avgKp.toFixed(1)}%</span>
            </div>
            <div className="m-stat-chips">
              <span className="m-stat-chip">{p.games}게임</span>
              <span className="m-stat-chip" style={{ color: 'var(--color-win)' }}>승 {p.avgKpWin.toFixed(0)}%</span>
              <span className="m-stat-chip" style={{ color: 'var(--color-loss)' }}>패 {p.avgKpLoss.toFixed(0)}%</span>
              <span className="m-stat-chip">{p.avgKills.toFixed(1)}킬 {p.avgAssists.toFixed(1)}어시</span>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function MobilePosPoolTab({ mode }: { mode: string }) {
  const navigate = useNavigate();
  const { champions } = useDragon();
  const [selectedPos, setSelectedPos] = useState('TOP');
  const { data, isLoading } = useQuery({
    queryKey: ['m-pospool', mode],
    queryFn: () => api.get<PositionChampionPoolResult>(`/stats/position-champion-pool?mode=${mode}`),
  });
  if (isLoading) return <LoadingCenter />;
  if (!data) return <div className="m-empty">데이터 없음</div>;

  const POSITIONS = ['TOP', 'JUNGLE', 'MID', 'BOTTOM', 'SUPPORT'];
  const POS_LABEL: Record<string, string> = { TOP: '탑', JUNGLE: 'JGL', MID: '미드', BOTTOM: '원딜', SUPPORT: '서폿' };
  const posPlayers = data.allPlayers.filter((p: PlayerPositionEntry) => p.position === selectedPos).sort((a: PlayerPositionEntry, b: PlayerPositionEntry) => b.games - a.games);

  return (
    <div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 12, flexWrap: 'wrap' }}>
        {POSITIONS.map(pos => (
          <button key={pos} className={`m-lane-tab${selectedPos === pos ? ' active' : ''}`} onClick={() => setSelectedPos(pos)}>
            {POS_LABEL[pos]}
          </button>
        ))}
      </div>
      {posPlayers.map((p: PlayerPositionEntry, i: number) => {
        const [name, tag] = p.riotId.split('#');
        return (
          <div key={p.riotId} className="m-player-card"
            onClick={() => navigate(`/m/player/${encodeURIComponent(p.riotId)}`)}>
            <div className="m-player-card-header">
              <div className={`m-player-rank${i < 3 ? ` rank-${i + 1}` : ''}`}>{i + 1}</div>
              <div style={{ flex: 1 }}>
                <span className="m-player-name">{name}</span>
                {tag && <span className="m-player-tag"> #{tag}</span>}
              </div>
              <span className="m-player-games">{p.games}게임</span>
            </div>
            <div style={{ display: 'flex', gap: 6, overflowX: 'auto', paddingBottom: 4, scrollbarWidth: 'none' }}>
              {p.champions.slice(0, 6).map(ce => {
                const c = champions.get(ce.championId);
                return (
                  <div key={ce.champion} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, flexShrink: 0 }}>
                    {c?.imageUrl ? (
                      <img src={c.imageUrl} alt={c.nameKo} width={32} height={32} style={{ borderRadius: 6 }} />
                    ) : (
                      <div style={{ width: 32, height: 32, borderRadius: 6, background: 'var(--color-bg-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 9 }}>{ce.champion.slice(0, 2)}</div>
                    )}
                    <span style={{ fontSize: 9, color: ce.winRate >= 60 ? 'var(--color-win)' : 'inherit' }}>{ce.winRate.toFixed(0)}%</span>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
      {posPlayers.length === 0 && <div className="m-empty">데이터가 없습니다</div>}
    </div>
  );
}

export default function MobilePersonalAnalysisGroup() {
  const [sub, setSub] = useState<PersonalSubTab>('생존지수');
  const [mode, setMode] = useState('normal');

  return (
    <div>
      <div className="m-sort-chips" style={{ marginBottom: 0 }}>
        {MODES.map(m => (
          <button key={m.value} className={`m-sort-chip${mode === m.value ? ' active' : ''}`} onClick={() => setMode(m.value)}>
            {m.label}
          </button>
        ))}
      </div>
      <div className="m-tab-bar" style={{ overflowX: 'auto', scrollbarWidth: 'none', flexWrap: 'nowrap' }}>
        {PERSONAL_SUB_TABS.map(t => (
          <button key={t} className={`m-tab${sub === t ? ' active' : ''}`} onClick={() => setSub(t)} style={{ flexShrink: 0, fontSize: 12 }}>
            {t}
          </button>
        ))}
      </div>
      {sub === '생존지수'  && <SurvivalTab mode={mode} />}
      {sub === '정글점령'  && <JungleTab mode={mode} />}
      {sub === '서폿기여'  && <SupportTab mode={mode} />}
      {sub === 'DNA'       && <DnaTab mode={mode} />}
      {sub === '게임성향'  && <GameLengthTab mode={mode} />}
      {sub === '골드효율'  && <GoldEffTab mode={mode} />}
      {sub === '성장곡선'  && <GrowthTab mode={mode} />}
      {sub === 'KP랭킹'   && <MobileKpTab mode={mode} />}
      {sub === '포지션풀'  && <MobilePosPoolTab mode={mode} />}
    </div>
  );
}
