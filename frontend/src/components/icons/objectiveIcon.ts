import {
	BaronIcon,
	DragonIcon,
	MinionIcon,
	NexusIcon,
	TurretIcon,
	type IconComponent
} from '@/components/icons/LolIcons';

/**
 * 오브젝트 종류 -> 아이콘. 키는 백엔드 `ObjectiveEntry.kind` 와 같다.
 *
 * 컴포넌트 파일(LolIcons.tsx)이 아니라 여기 있는 이유는 react-refresh 규칙 때문이다 —
 * POSITION_ICON 과 같다.
 *
 * 전령·공허 유충·아타칸 전용 아이콘은 없다. 있는 것을 재사용하는 게 이 저장소 관례다
 * (경기 상세가 이미 전령을 NexusIcon 으로 그리고 있었다).
 */
export const OBJECTIVE_ICON: Record<string, IconComponent> = {
	DRAGON: DragonIcon,
	BARON_NASHOR: BaronIcon,
	RIFTHERALD: NexusIcon,
	HORDE: MinionIcon,
	ATAKHAN: BaronIcon,
	TOWER_BUILDING: TurretIcon,
	INHIBITOR_BUILDING: NexusIcon
};

/** 화면에 띄울 이름. 드래곤은 원소까지 붙인다. */
export function objectiveLabel(
	kind: string,
	subType: string,
	lane: string,
	towerType: string
): string {
	const dragon = DRAGON_LABEL[subType];
	if (kind === 'DRAGON') return dragon ? `${dragon}용` : '드래곤';
	if (kind === 'TOWER_BUILDING')
		return `${LANE_LABEL[lane] ?? ''} ${TOWER_LABEL[towerType] ?? '포탑'}`.trim();
	if (kind === 'INHIBITOR_BUILDING') return `${LANE_LABEL[lane] ?? ''} 억제기`.trim();
	return OBJECTIVE_LABEL[kind] ?? kind;
}

const OBJECTIVE_LABEL: Record<string, string> = {
	BARON_NASHOR: '바론',
	RIFTHERALD: '전령',
	HORDE: '공허 유충',
	ATAKHAN: '아타칸'
};

/** 17판 실측에서 관측된 원소. ELDER_DRAGON 은 아직 없었지만 미리 넣어 둔다. */
const DRAGON_LABEL: Record<string, string> = {
	FIRE_DRAGON: '화염',
	WATER_DRAGON: '바다',
	AIR_DRAGON: '바람',
	EARTH_DRAGON: '대지',
	HEXTECH_DRAGON: '마공학',
	CHEMTECH_DRAGON: '화학공학',
	ELDER_DRAGON: '장로'
};

const LANE_LABEL: Record<string, string> = {
	TOP_LANE: '탑',
	MID_LANE: '미드',
	BOT_LANE: '봇'
};

const TOWER_LABEL: Record<string, string> = {
	OUTER_TURRET: '외곽 포탑',
	INNER_TURRET: '내부 포탑',
	BASE_TURRET: '기지 포탑',
	NEXUS_TURRET: '쌍둥이 포탑'
};
