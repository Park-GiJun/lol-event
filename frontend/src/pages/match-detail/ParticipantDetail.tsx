import type { ReactNode } from 'react';
import type { Participant } from '@/lib/types/match';
import { DragonIcon, TurretIcon, WardIcon, MinionIcon } from '@/components/icons/LolIcons';

function n(v: number) { return v.toLocaleString(); }
function mmss(sec: number) {
  const m = Math.floor(sec / 60);
  return `${m}분 ${sec % 60}초`;
}

function Cell({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="t-detail-cell">
      <span className="t-detail-label">{label}</span>
      <span className="t-detail-value">{value}</span>
      {sub && <span className="t-detail-sub">{sub}</span>}
    </div>
  );
}

function Group({ icon, title, children }: { icon: ReactNode; title: string; children: ReactNode }) {
  return (
    <div className="t-detail-group">
      <div className="t-detail-head">{icon}<span>{title}</span></div>
      <div className="t-detail-cells">{children}</div>
    </div>
  );
}

/**
 * 한 참가자의 세부 기록.
 *
 * 참가자마다 115개 필드를 수집해 저장하는데 표에는 KDA·딜량·CS·시야·아이템 여섯만 나온다.
 * 펜타킬을 몇 번 했는지, 딜이 물리인지 마법인지, 포탑을 얼마나 팼는지가 전부 묻혀 있었다.
 * 표를 넓히는 대신 행을 펼쳐서 보여준다 — 평소에는 방해되지 않고, 궁금할 때만 열린다.
 */
export function ParticipantDetail({ p, duration }: { p: Participant; duration: number }) {
  const dmgTotal = p.physicalDamageDealtToChampions + p.magicDamageDealtToChampions + p.trueDamageDealtToChampions;
  const pct = (v: number) => (dmgTotal > 0 ? Math.round((v / dmgTotal) * 100) : 0);
  const minutes = Math.max(duration / 60, 1);

  return (
    <div className="t-detail">
      <Group icon={<DragonIcon size={14} />} title="가한 피해">
        <Cell label="물리" value={n(p.physicalDamageDealtToChampions)} sub={`${pct(p.physicalDamageDealtToChampions)}%`} />
        <Cell label="마법" value={n(p.magicDamageDealtToChampions)} sub={`${pct(p.magicDamageDealtToChampions)}%`} />
        <Cell label="고정" value={n(p.trueDamageDealtToChampions)} sub={`${pct(p.trueDamageDealtToChampions)}%`} />
        <Cell label="분당 딜" value={n(Math.round(p.damage / minutes))} />
      </Group>

      <Group icon={<TurretIcon size={14} />} title="버틴 양 · 오브젝트">
        <Cell label="받은 피해" value={n(p.totalDamageTaken)} />
        <Cell label="피해 감소" value={n(p.damageSelfMitigated)} />
        <Cell label="오브젝트 딜" value={n(p.damageDealtToObjectives)} />
        <Cell label="포탑 딜" value={n(p.damageDealtToTurrets)} sub={p.turretKills ? `${p.turretKills}킬` : undefined} />
      </Group>

      <Group icon={<WardIcon size={14} />} title="시야 · 보조">
        <Cell label="와드 설치" value={`${p.wardsPlaced}`} />
        <Cell label="와드 제거" value={`${p.wardsKilled}`} />
        <Cell label="회복량" value={n(p.totalHeal)} sub={p.totalUnitsHealed > 1 ? `${p.totalUnitsHealed}명` : undefined} />
        <Cell label="CC 시간" value={`${p.timeCCingOthers}초`} />
      </Group>

      <Group icon={<MinionIcon size={14} />} title="성장 · 생존">
        <Cell label="레벨" value={`${p.champLevel}`} />
        <Cell label="정글 몹" value={`${p.neutralMinionsKilled}`} />
        <Cell label="골드" value={n(p.gold)} sub={`분당 ${n(Math.round(p.gold / minutes))}`} />
        <Cell label="최장 생존" value={mmss(p.longestTimeSpentLiving)} />
      </Group>
    </div>
  );
}

/** 멀티킬·퍼스트 같은 "한 번 있었던 일". 없으면 아무것도 그리지 않는다. */
export function Highlights({ p }: { p: Participant }) {
  const marks: { text: string; strong?: boolean }[] = [];
  if (p.pentaKills)   marks.push({ text: `펜타킬 ${p.pentaKills}`, strong: true });
  if (p.quadraKills)  marks.push({ text: `쿼드라 ${p.quadraKills}`, strong: true });
  if (p.tripleKills)  marks.push({ text: `트리플 ${p.tripleKills}` });
  if (p.doubleKills)  marks.push({ text: `더블 ${p.doubleKills}` });
  if (p.largestKillingSpree >= 5) marks.push({ text: `${p.largestKillingSpree}연속킬` });
  if (p.firstBloodKill) marks.push({ text: '퍼스트블러드', strong: true });
  if (p.firstTowerKill) marks.push({ text: '첫 포탑' });

  if (!marks.length) return null;
  return (
    <span className="t-marks">
      {marks.map((m) => (
        <span key={m.text} className={`t-mark${m.strong ? ' t-mark-strong' : ''}`}>{m.text}</span>
      ))}
    </span>
  );
}
