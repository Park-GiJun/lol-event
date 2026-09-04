import { Fragment, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMatch } from '@/hooks/useMatches';
import { ChampionIcon, ItemIcons, PersonLink } from '@/components/ds/Champion';
import { InlineError } from '@/components/common/InlineError';
import { calcMvp, fmt } from '@/lib/lol';
import type { Match, Participant } from '@/lib/types/match';
import { byPosition, positionLabel } from '@/lib/position';
import { ParticipantDetail, Highlights } from './match-detail/ParticipantDetail';
import { DragonIcon, BaronIcon, TurretIcon, NexusIcon } from '@/components/icons/LolIcons';


function kdaRatio(p: Participant) {
  return p.deaths === 0 ? p.kills + p.assists : (p.kills + p.assists) / p.deaths;
}

function TeamTable({
  match,
  team,
  aceId,
  maxDamage,
  openId,
  onToggle,
}: {
  match: Match;
  team: 'blue' | 'red';
  aceId: string;
  maxDamage: number;
  openId: string | null;
  onToggle: (riotId: string) => void;
}) {
  const players = match.participants.filter((p) => p.team === team).sort(byPosition);
  const won = players[0]?.win ?? false;
  const kills = players.reduce((s, p) => s + p.kills, 0);
  const gold = players.reduce((s, p) => s + p.gold, 0);
  const teamInfo = match.teams.find((t) => t.teamId === (team === 'blue' ? 100 : 200));
  const minutes = Math.max(match.gameDuration / 60, 1);

  return (
    <section className="t-card">
      <div className="t-card-head">
        <h2 className="t-card-title" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className={`t-chip ${won ? 't-chip-win' : 't-chip-loss'}`}>{won ? '승리' : '패배'}</span>
          {team === 'blue' ? '블루팀' : '레드팀'}
        </h2>
        <span className="t-objectives">
          <span className="t-stat-sample">{kills}킬 · {gold.toLocaleString()}골드</span>
          {teamInfo && (
            <>
              <span className="t-obj" title="드래곤"><DragonIcon size={14} />{teamInfo.dragonKills}</span>
              <span className="t-obj" title="바론"><BaronIcon size={14} />{teamInfo.baronKills}</span>
              {teamInfo.riftHeraldKills > 0 && (
                <span className="t-obj" title="전령"><NexusIcon size={14} />{teamInfo.riftHeraldKills}</span>
              )}
              <span className="t-obj" title="포탑"><TurretIcon size={14} />{teamInfo.towerKills}</span>
              <span className="t-obj" title="억제기"><NexusIcon size={14} />{teamInfo.inhibitorKills}</span>
              {teamInfo.firstBlood && <span className="t-mark t-mark-strong">퍼블</span>}
            </>
          )}
        </span>
      </div>

      <div className="t-tablewrap">
        <table className="t-table">
          <thead>
            <tr>
              <th style={{ width: 44 }}>포지션</th>
              <th>플레이어</th>
              <th className="t-num">KDA</th>
              <th style={{ width: 120 }}>딜량</th>
              <th className="t-num">CS</th>
              <th className="t-num">시야</th>
              <th>아이템</th>
            </tr>
          </thead>
          <tbody>
            {players.map((p) => (
              <Fragment key={p.riotId}>
              <tr
                className="t-row-expandable"
                onClick={() => onToggle(p.riotId)}
                aria-expanded={openId === p.riotId}
              >
                <td className="t-stat-sample">
                  {positionLabel(p.assignedPosition)}
                </td>
                <td>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <ChampionIcon championId={p.championId} champion={p.champion} />
                    <span style={{ minWidth: 0 }}>
                      <PersonLink riotId={p.riotId} />
                      {p.riotId === aceId && (
                        <span className="t-chip t-chip-blue" style={{ marginLeft: 6 }}>MVP</span>
                      )}
                      <Highlights p={p} />
                    </span>
                  </span>
                </td>
                <td className="t-num">
                  <b>{p.kills} / {p.deaths} / {p.assists}</b>
                  <br />
                  <span className="t-stat-sample">{kdaRatio(p).toFixed(2)}</span>
                </td>
                <td>
                  {/* 딜량은 절대값보다 이 경기 안에서의 비중이 읽힌다.
                      가장 많이 넣은 사람을 100%로 두고 막대로 비교한다. */}
                  <div className="t-bar" style={{ marginBottom: 3 }}>
                    <div className="t-bar-fill" style={{ width: `${(p.damage / maxDamage) * 100}%` }} />
                  </div>
                  <span className="t-stat-sample">{p.damage.toLocaleString()}</span>
                </td>
                <td className="t-num">
                  {p.cs}
                  <br />
                  <span className="t-stat-sample">{(p.cs / minutes).toFixed(1)}/분</span>
                </td>
                <td className="t-num">{p.visionScore}</td>
                <td>
                  <ItemIcons items={[p.item0, p.item1, p.item2, p.item3, p.item4, p.item5, p.item6]} />
                </td>
              </tr>
              {openId === p.riotId && (
                <tr>
                  <td colSpan={7} style={{ padding: 0 }}>
                    <ParticipantDetail p={p} duration={match.gameDuration} />
                  </td>
                </tr>
              )}
              </Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export function MatchDetailPage() {
  // 한 번에 한 명만 펼친다. 여러 개가 열려 있으면 표가 길어져 비교가 안 된다.
  const [openId, setOpenId] = useState<string | null>(null);
  const toggle = (riotId: string) => setOpenId((cur) => (cur === riotId ? null : riotId));
  const { matchId = '' } = useParams();
  const { data: match, isPending, error, refetch } = useMatch(matchId);

  if (isPending) {
    return (
      <div className="t-page">
        <div className="t-skel" style={{ height: 90, borderRadius: 16 }} />
        <div className="t-skel" style={{ height: 320, borderRadius: 16 }} />
        <div className="t-skel" style={{ height: 320, borderRadius: 16 }} />
      </div>
    );
  }

  if (error || !match) {
    return (
      <div className="t-page">
        <InlineError message="이 경기를 불러오지 못했습니다." onRetry={() => refetch()} />
      </div>
    );
  }

  const { aceId } = calcMvp(match);
  const maxDamage = Math.max(...match.participants.map((p) => p.damage), 1);
  const played = new Date(match.gameCreation);

  return (
    <div className="t-page">
      <section className="t-card">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 28, alignItems: 'flex-end' }}>
          <div className="t-stat">
            <span className="t-stat-label">
              {played.toLocaleDateString('ko-KR')} {played.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
            </span>
            <span className="t-stat-value t-stat-hero" style={{ fontSize: 32 }}>{fmt(match.gameDuration)}</span>
          </div>
          <div className="t-stat">
            <span className="t-stat-label">총 킬</span>
            <span className="t-stat-value">
              {match.participants.reduce((s, p) => s + p.kills, 0)}
            </span>
          </div>
          <div className="t-stat">
            <span className="t-stat-label">경기 ID</span>
            <span className="t-stat-sample">{match.matchId}</span>
          </div>
        </div>
      </section>

      <TeamTable match={match} team="blue" aceId={aceId} maxDamage={maxDamage} openId={openId} onToggle={toggle} />
      <TeamTable match={match} team="red" aceId={aceId} maxDamage={maxDamage} openId={openId} onToggle={toggle} />
    </div>
  );
}
