---
stepsCompleted: [1, 2, '2b', '2c', 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
classification:
  projectType: Web App + Desktop App (Electron) + Mobile Web
  domain: Gaming / Internal Community Tool
  complexity: medium
  projectContext: brownfield
inputDocuments: ['_bmad-output/planning-artifacts/product-brief-lol-event-2026-03-21.md', '_bmad-output/brainstorming/brainstorming-session-2026-03-21-1430.md', '_bmad-output/planning-artifacts/ux-design-specification.md']
workflowType: 'prd'
briefCount: 1
researchCount: 0
brainstormingCount: 1
uxCount: 1
projectDocsCount: 0
---

# Product Requirements Document - lol-event

**Author:** 박기준
**Date:** 2026-03-21

---

## Executive Summary

lol-event는 고정 멤버(50-60명)의 LoL 내전을 위한 전용 플랫폼이다. 기존 데이터 모델을 유지하면서 Dashboard(웹), Electron 대기방, 모바일 웹 세 영역의 UI/UX를 전면 재설계한다. 핵심 문제는 "데이터는 있는데 불편하다" — 탐색성 부족, 가독성 부족, 모바일 경험 열악이다.

- **일반 참가자:** Elo/통계 확인, 밴픽 결정, 리더보드 탐색
- **관리자:** 멤버 데이터 기반 팀 구성, 리그 운영

### What Makes This Special

OP.GG 등 외부 서비스는 서버 전체 데이터 기반으로 내전 전용 맥락을 제공할 수 없다. lol-event는 이 포지션을 독점한다:

- **내전 전용 메타:** 우리 멤버들 사이의 실제 강자 — 서버 전체 통계와 무관
- **감정적 의미를 가진 데이터:** 서로 아는 멤버들의 기록이므로 숫자에 맥락이 존재
- **Electron 자동화:** 대기방 진입 시 분석이 유저를 찾아옴 — 검색 행동 제거
- **숫자 우선 원칙:** 이모지 없이 정확한 수치 중심

## Project Classification

- **Project Type:** Web App + Desktop App (Electron) + Mobile Web
- **Domain:** Gaming / Internal Community Tool
- **Complexity:** Medium (복합 플랫폼, 내부 전용, 50-60명, 규제 없음)
- **Project Context:** Brownfield — 기존 앱 존재, 데이터 모델 유지 + UI/UX 전면 재설계

## Core Design Principles

- **시즌 없음, 전체 누적 데이터:** 모든 통계(승률, Elo, 챔피언 티어)는 시즌 구분 없이 전체 누적 기준으로 산정한다. 기간 필터 불필요.
- **숫자 우선:** 모든 지표는 수치로 표현. 이모지/아이콘으로 대체하지 않는다.
- **플랫폼 완전 분리:** Dashboard(데스크톱), 모바일 웹(`/m/*`), Electron은 각자 독립적으로 최적화된 UX를 가진다.
- **데이터 레이어 유지:** 기존 API, 데이터 모델, LCU 동기화 기능은 변경하지 않는다.

## Success Criteria

### User Success

- **가독성:** 멤버들이 원하는 데이터를 추가 탐색 없이 즉시 확인 가능 (홈→상세 최대 2클릭)
- **모바일 사용성:** 모바일 환경에서 주요 화면(리더보드, 개인 통계, 챔피언 티어표) 데스크톱과 동등한 수준의 사용 경험
- **대기방 활용도:** 내전 당일 밴픽 결정 시 Electron 앱을 실제로 참고

### Business Success

- Dashboard UI/UX 전면 재설계 완료 (탐색성 + 가독성 개선)
- Electron 대기방 UI 개선 완료 (개인 통계 기반 밴픽 추천)
- 모바일 웹 최적화 완료 (데스크톱과 분리된 모바일 전용 구현)

### Technical Success

- Hover 위젯 (PlayerLink/ChampionLink): 마우스 진입 후 300ms 이내 통계 표시
- Electron 대기방: 진입 2초 이내 양팀 카드 렌더링
- 기존 데이터 모델 및 API 유지 (신규 데이터 작업 최소화)

### Measurable Outcomes

- 관리자가 팀 구성 시 한 화면에서 전체 멤버 데이터 비교 가능
- 대기방 진입 시 Electron 자동 트리거 — 별도 조작 불필요
- 모바일에서 주요 화면 불편함 없이 사용 가능 (멤버 직접 확인)

## User Journeys

### Journey 1: 민준 (일반 참가자) — 대기방 밴픽 결정

민준은 내전에 매주 참가하는 멤버다. 오늘 대기방에 입장하는 순간, Electron이 자동으로 열리며 양팀 카드가 펼쳐진다. 상대팀에 야스오 승률 68%의 철수가 있고, 밴 추천 1순위로 표시된다. 별도로 앱을 켜거나 검색할 필요 없이 10초 만에 밴픽을 결정한다.

**드러나는 요구사항:** Electron 자동 트리거, PlayerCard, 밴 추천, 개인 승률 기반 위협도 산정

### Journey 2: 지수 (일반 참가자) — 모바일 통계 확인

지수는 버스에서 스마트폰으로 lol-event를 연다. 하단 탭에서 리더보드를 탭하자 Elo 순위가 나타나고 자신의 이름이 하이라이트된다. 챔피언 탭에서 이번 리그 야스오가 S티어임을 확인한다.

**드러나는 요구사항:** 모바일 전용 라우트(`/m/*`), 하단 탭바, EloLeaderboard 모바일 변형, ChampionTierTable 모바일 최적화

### Journey 3: 지수 (일반 참가자) — 챔피언 상세 탐색

PC에서 홈의 챔피언 티어표에서 야스오를 클릭(1클릭). 상세 페이지에서 승률 62%, 픽률 35%, KDA 3.2 카드 확인 후 Chart.js 승률 추이 차트로 최근 상승세를 파악한다. Breadcrumb으로 홈에 복귀.

**드러나는 요구사항:** 계층 탐색(홈→챔피언), Breadcrumb, ChampionTierTable, Chart.js 상세 페이지, StatHoverWidget

### Journey 4: 박기준 (관리자) — 팀 구성

홈 Elo 리더보드에서 참가자 8명을 확인. 각 플레이어 이름 hover → Hover 위젯에 승률/주력 챔피언/Elo 즉시 표시. 클릭 없이 8명 데이터를 스캔하여 균형 잡힌 팀 2개 구성. 관리자 페이지에서 저장.

**드러나는 요구사항:** EloLeaderboard, StatHoverWidget(PlayerLink), 관리자 페이지 유지

## Domain-Specific Requirements

### Technical Constraints

- **LCU API 의존성:** Electron이 LoL 클라이언트 로컬 API(LCU)를 통해 대기방 진입 감지. 클라이언트 패치 시 API 변경 대응 필요.
- **오프라인 처리:** LoL 클라이언트 미실행 시 Electron graceful degradation — 수동 모드 또는 안내 메시지 표시.

### Integration Requirements

- Riot LCU API (로컬): 대기방 감지, 현재 게임 정보
- 내부 lol-event API: 플레이어/챔피언/매치 통계 조회

## Innovation & Novel Patterns

1. **Zero-friction 자동화 (Electron):** LCU API로 대기방 진입 감지 → 유저 조작 없이 분석 화면 자동 표시. 기존 통계 툴은 모두 수동 조작 필요 — "데이터가 유저를 찾아오는" 패턴이 핵심 차별화.

2. **커뮤니티 전용 메타:** 전체 서버가 아닌 특정 그룹(50-60명)의 누적 데이터로 챔피언 티어 산정. 외부 서비스에서 제공 불가능한 내전 전용 인사이트.

**Risk:** LCU API 변경 시 기존 동기화 로직 유지 + 수동 모드 fallback으로 대응.

## Platform-Specific Requirements

lol-event는 세 플랫폼이 같은 데이터 소스를 공유하지만 각자 독립적인 UX를 가지는 구조다. 데이터 레이어(API)는 공유, UI 레이어는 완전 분리.

### Technical Architecture

**공통:** 기존 REST API 유지. React Query로 클라이언트 캐싱.

**Dashboard (웹/데스크톱):** React + TypeScript + Tailwind CSS. 좌측 사이드바 레이아웃 전역 적용. Chart.js는 챔피언/플레이어 상세 페이지 한정 동적 임포트(`React.lazy`).

**모바일 웹:** `/m/*` 전용 라우트. 공통 API 훅 재사용, UI 컴포넌트만 별도. 하단 탭바 고정(`fixed bottom-0`, `safe-area-inset-bottom`).

**Electron:** 기존 앱 구조 유지 — LCU API 연동 로직 보존. 대기방 뷰 `grid-cols-2` 좌우 분할.

### Navigation & Routing

| 플랫폼 | 라우트 패턴 | 네비게이션 |
|--------|-------------|------------|
| Dashboard | `/`, `/champions/*`, `/players/*`, `/matches/*` | 좌측 사이드바 |
| 모바일 | `/m/`, `/m/champions`, `/m/players` | 하단 탭바 |
| Electron | 단일 뷰 (대기방) | 없음 |

### Implementation Constraints

- PlayerLink/ChampionLink: 기존 구현 인터페이스 유지, 스타일만 업데이트
- 관리자 페이지 (AdminPage, SyncPage, LcuPage, MonitoringPage): 변경 없음
- Chart.js: 상세 페이지에만 적용, 홈/목록 페이지 미사용

## Product Scope & Phased Development

**MVP Approach:** Experience MVP — 기존 데이터/API/LCU 동기화를 유지하면서 UI/UX를 전면 교체.

### Phase 1 — MVP

1. **Dashboard 홈:** EloLeaderboard + ChampionTierTable + 밴픽 트렌드
2. **Dashboard 계층 탐색:** 챔피언/플레이어/매치 상세 + Chart.js 시각화
3. **Dashboard Hover 위젯:** PlayerLink/ChampionLink 기존 구현 유지 + 스타일 업데이트
4. **Electron 대기방:** 자동 트리거 + 좌우 분할 PlayerCard + 밴 추천
5. **모바일 웹:** `/m/*` 전용 라우트, 하단 탭바, 리더보드/개인통계/챔피언 티어표

### Phase 2 — Growth (Post-MVP)

- 밴 가치 점수 고도화 (상대 승률 × 우리팀 상대 승률 결합)
- 픽 추천 (팀 시너지 분석 포함)

### Phase 3 — Vision

- 명예의 전당 (역대 기록 자동 감지, Dashboard 고정 섹션)
- 게임 종료 후 Electron 자동 요약 (Elo 변동 + KDA)

## Functional Requirements

### Dashboard 홈 & 탐색

- FR1: 유저는 홈 화면에서 전체 멤버 Elo 리더보드를 즉시 확인할 수 있다
- FR2: 유저는 홈 화면에서 내전 기반 챔피언 티어표(S/A/B/C)를 즉시 확인할 수 있다
- FR3: 유저는 홈 화면에서 챔피언 밴픽 트렌드를 확인할 수 있다
- FR4: 유저는 챔피언 목록에서 챔피언을 선택하여 상세 페이지로 이동할 수 있다 (1클릭)
- FR5: 유저는 플레이어 목록에서 플레이어를 선택하여 상세 페이지로 이동할 수 있다 (1클릭)
- FR6: 유저는 매치 목록에서 매치를 선택하여 상세 내역을 확인할 수 있다
- FR7: 유저는 Breadcrumb을 통해 상위 계층으로 돌아갈 수 있다

### Hover 통계 위젯

- FR8: 유저는 플레이어 이름에 마우스를 올려 해당 플레이어의 핵심 통계를 즉시 확인할 수 있다 (PlayerLink)
- FR9: 유저는 챔피언 이름에 마우스를 올려 해당 챔피언의 핵심 통계를 즉시 확인할 수 있다 (ChampionLink)

### 데이터 시각화

- FR10: 유저는 챔피언 상세 페이지에서 승률/픽률/KDA 수치 카드를 확인할 수 있다
- FR11: 유저는 챔피언 상세 페이지에서 Chart.js 기반 시계열 시각화(승률 추이, 게임별 퍼포먼스)를 확인할 수 있다
- FR12: 유저는 플레이어 상세 페이지에서 개인 통계 수치 카드를 확인할 수 있다
- FR13: 유저는 플레이어 상세 페이지에서 Chart.js 기반 Elo 추이 및 챔피언별 성적 시각화를 확인할 수 있다
- FR14: 유저는 Elo 리더보드에서 자신의 순위가 자동으로 하이라이트된 상태로 확인할 수 있다

### Electron 대기방

- FR15: 시스템은 LoL 클라이언트 대기방 진입을 자동으로 감지할 수 있다
- FR16: 시스템은 대기방 진입 감지 시 양팀 분석 화면을 자동으로 표시할 수 있다
- FR17: 유저는 좌우 분할 카드로 우리팀과 상대팀 플레이어를 동시에 확인할 수 있다
- FR18: 유저는 각 플레이어 카드에서 닉네임, Elo, 고승률 챔피언 TOP 3를 확인할 수 있다
- FR19: 유저는 각 플레이어 카드에서 밴 추천 챔피언을 확인할 수 있다
- FR20: 유저는 LoL 클라이언트 미실행 시 Electron 안내 메시지를 확인할 수 있다

### 모바일 웹

- FR21: 유저는 모바일 전용 경로(`/m/*`)로 접근하여 모바일 최적화 화면을 사용할 수 있다
- FR22: 유저는 모바일 하단 탭바를 통해 홈(리더보드)/챔피언/플레이어 간 이동할 수 있다
- FR23: 유저는 모바일에서 Elo 리더보드를 확인하고 자신의 순위를 파악할 수 있다
- FR24: 유저는 모바일에서 챔피언 티어표를 확인할 수 있다
- FR25: 유저는 모바일에서 개인 통계 페이지를 확인할 수 있다

### 관리자

- FR26: 관리자는 멤버 Elo 리더보드를 기반으로 팀 구성에 필요한 데이터를 한 화면에서 비교할 수 있다
- FR27: 관리자는 기존 관리 페이지(AdminPage, SyncPage, LcuPage, MonitoringPage)를 통해 리그를 운영할 수 있다
- FR28: 시스템은 LCU 동기화를 통해 게임 데이터를 자동으로 수집할 수 있다

## Non-Functional Requirements

### Performance

- NFR1: Hover 위젯(PlayerLink/ChampionLink) — 마우스 진입 후 300ms 이내 통계 표시
- NFR2: Electron 대기방 — 대기방 진입 감지 후 2초 이내 양팀 카드 렌더링
- NFR3: Dashboard 페이지 초기 로딩 — 첫 화면 3초 이내 표시 (LCP 기준)
- NFR4: Chart.js 시각화 — 상세 페이지 진입 후 2초 이내 차트 렌더링
- NFR5: 홈/목록 페이지 — Chart.js 미포함 (동적 임포트로 번들 분리)

### Accessibility

- NFR6: 텍스트/배경 색상 대비 WCAG AA 기준 4.5:1 이상 준수
- NFR7: 승/패 색상 표시 시 색상 + 텍스트 병행 (색맹 대응)
- NFR8: 모바일 최소 터치 타겟 44px × 44px 이상

### Integration

- NFR9: LCU API — LoL 클라이언트 미실행 시 Electron graceful degradation (안내 메시지 표시)
- NFR10: LCU 동기화 — 클라이언트 패치 후 API 변경 감지 및 오류 로깅

### Reliability

- NFR11: API 응답 실패 시 인라인 에러 메시지 + 재시도 버튼 표시 (앱 전체 중단 없음)
- NFR12: Skeleton UI로 로딩 중에도 레이아웃 구조 유지 (CLS 방지)
