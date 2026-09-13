import { useQuery } from '@tanstack/react-query';
import { BarChartIcon, DownloadIcon, EyeIcon, GamepadIcon, MonitorIcon, RefreshIcon, ShieldIcon, ZapIcon } from '@/components/icons/LolIcons';
import { Button } from '../components/common/Button';

/*
 * 수집기는 GitHub Releases 에서 바로 받는다.
 *
 * 예전에는 배포 때 런처 MSI 를 사이트의 /downloads 로 복사해 두고 그걸 링크했는데,
 * 본체가 Rust 단일 exe 로 바뀐 뒤로 MSI 는 필요 없어졌고 페이지 문구도 옛 버전에 멈춰 있었다.
 * `releases/latest/download/<자산>` 은 GitHub 가 항상 --latest 릴리즈로 돌려 주므로
 * 새 버전을 올려도 이 파일은 손대지 않아도 된다. (--latest 는 본체 릴리즈에만 붙인다.)
 */
const REPO = 'Park-GiJun/lol-event';
const LATEST = `https://github.com/${REPO}/releases/latest`;
const EXE_URL = `${LATEST}/download/LoL-Collector.exe`;
const CERT_URL = `${LATEST}/download/LoL-Collector.cer`;
const TRUST_BAT_URL = `${LATEST}/download/trust-cert.bat`;

/** 버전 표시용. 실패해도 다운로드 링크는 그대로 동작하므로 조용히 숨긴다. */
function useLatestVersion() {
  return useQuery({
    queryKey: ['collector-latest-version'],
    queryFn: async () => {
      const res = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`);
      if (!res.ok) throw new Error(`GitHub ${res.status}`);
      const body: { tag_name: string } = await res.json();
      return body.tag_name.replace(/^desktop-/, '');
    },
    staleTime: 10 * 60 * 1000,
    retry: false,
  });
}

const FEATURES = [
  { icon: ZapIcon, title: '자동 수집', desc: '게임 종료 감지 후 자동 수집 · 서버 전송 (타임라인 포함)' },
  { icon: EyeIcon, title: '챔프셀렉트 분석', desc: '상대 모스트픽 · 밴 추천 · 카운터픽 · 팀 전력 비교' },
  { icon: BarChartIcon, title: '내전 대시보드', desc: 'Elo 리더보드 · 어워즈 · MVP · 멀티킬 하이라이트' },
  { icon: GamepadIcon, title: '라이브 게임', desc: '실시간 킬/CS/레벨 · 게임 이벤트 피드' },
  { icon: ShieldIcon, title: '로비 캐시', desc: '대기방 10명 데이터 캐싱 챔프셀렉트에서 활용' },
  { icon: RefreshIcon, title: '자동 업데이트', desc: '실행 시 새 버전 자동 감지 · 관리자 권한 없이 교체' },
];

export function LcuPage() {
  const { data: version } = useLatestVersion();

  return (
    <div className="t-page">
      <div className="hero-banner" style={{ marginBottom: 'var(--spacing-lg)' }}>
        <div className="hero-eyebrow">Desktop Collector</div>
        <h1 className="hero-title">데스크톱 수집기</h1>
        <p className="hero-subtitle">LoL 클라이언트 연동 · 자동 수집 · 실시간 분석</p>
      </div>

      {/* 다운로드 섹션 */}
      <div className="card" style={{ marginBottom: 'var(--spacing-lg)', padding: 'var(--spacing-lg)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-sm)', marginBottom: 'var(--spacing-md)' }}>
          <div style={{
            width: 44, height: 44, borderRadius: 'var(--radius-lg)',
            background: 'var(--gradient-primary)', display: 'flex',
            alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            boxShadow: 'var(--shadow-teal-md)',
          }}>
            <MonitorIcon size={22} color="#fff" />
          </div>
          <div>
            <div style={{ fontSize: 'var(--font-size-lg)', fontWeight: 'var(--font-weight-bold)', color: 'var(--color-text-primary)' }}>
              LoL 내전 수집기{version && <> <span style={{ color: 'var(--color-primary)' }}>{version}</span></>}
            </div>
            <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
              Windows 전용 · 설치 없이 실행 파일 하나 · 시스템 트레이 상주
            </div>
          </div>
        </div>

        <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', lineHeight: 1.7, marginBottom: 'var(--spacing-md)' }}>
          수집기는 로컬 PC에서 실행되며, LoL 클라이언트(LCU)와 직접 연동하여
          내전 데이터를 <strong style={{ color: 'var(--color-text-primary)' }}>자동으로 수집</strong>하고
          서버에 전송합니다. 챔피언 셀렉트 분석, 라이브 게임 모니터링, 내전 대시보드를 제공합니다.
        </p>

        <a href={EXE_URL} style={{ display: 'inline-block' }}>
          <Button variant="primary" size="md">
            <DownloadIcon size={15} /> 수집기 다운로드 (.exe)
          </Button>
        </a>
        <div style={{ marginTop: 'var(--spacing-xs)', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)' }}>
          <a href={LATEST} target="_blank" rel="noreferrer">릴리즈 노트 · 이전 버전</a>
        </div>
      </div>

      {/* 주요 기능 */}
      <div className="card" style={{ marginBottom: 'var(--spacing-lg)' }}>
        <div className="section-head">
          <span className="icon-chip"><ZapIcon size={15} /></span>
          <span className="section-head-title">주요 기능</span>
        </div>
        <div className="grid-16">
          {FEATURES.map(({ icon: Icon, title, desc }) => (
            <div key={title} className="col-span-8" style={{
              display: 'flex', gap: 'var(--spacing-sm)', padding: 'var(--spacing-sm) 0',
              borderBottom: '1px solid var(--color-border)',
            }}>
              <span className="icon-chip"><Icon size={15} /></span>
              <div>
                <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 'var(--font-weight-semibold)', color: 'var(--color-text-primary)', marginBottom: 2 }}>{title}</div>
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', lineHeight: 1.5 }}>{desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 설치 방법 */}
      <div className="card">
        <div className="section-head">
          <span className="icon-chip"><DownloadIcon size={15} /></span>
          <span className="section-head-title">설치 및 사용</span>
        </div>
        <ol className="lcu-step-list">
          <li>위 버튼으로 <strong className="lcu-step-highlight">LoL-Collector.exe</strong> 다운로드</li>
          <li>
            실행 — "Windows의 PC 보호" 창이 뜨면 <strong className="lcu-step-highlight">추가 정보 → 실행</strong>
            (처음 한 번만. 이후 자동 업데이트는 경고 없이 진행됩니다)
          </li>
          <li>첫 실행 때 스스로 설치되고 바탕화면 · 시작 메뉴에 바로가기가 생깁니다. 받은 exe 는 지워도 됩니다</li>
          <li>LoL 클라이언트 로그인 → 수집기가 자동으로 LCU 연결</li>
          <li>
            <strong className="lcu-step-highlight">자동 수집 모드</strong>:
            게임 종료 시 자동으로 데이터 수집 및 전송
          </li>
          <li>내전 대기방 진입 시 → 자동으로 팀 분석 · 밴 추천 표시</li>
        </ol>

        <div style={{
          marginTop: 'var(--spacing-md)', padding: 'var(--spacing-sm) var(--spacing-md)',
          background: 'rgba(200, 170, 110, 0.06)', borderRadius: 'var(--radius-md)',
          border: '1px solid rgba(200, 170, 110, 0.18)',
          fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', lineHeight: 1.6,
        }}><strong style={{ color: 'var(--color-primary)' }}>선택</strong>: "알 수 없는 게시자" 표시를 없애려면{' '}
          <a href={CERT_URL}>LoL-Collector.cer</a> 와 <a href={TRUST_BAT_URL}>trust-cert.bat</a> 을
          같은 폴더에 받은 뒤, trust-cert.bat 을 관리자 권한으로 한 번 실행하세요.
        </div>
      </div>
    </div>
  );
}
