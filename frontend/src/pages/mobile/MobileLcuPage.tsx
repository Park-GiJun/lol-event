import { Download, Monitor, Zap, Eye, BarChart2, Gamepad2 } from 'lucide-react';

const INSTALLER_URL = 'https://gijun.net/downloads/lol-collector.msi';

const FEATURES = [
  { icon: Zap, title: '자동 수집', desc: '게임 종료 → 자동 수집 · 서버 전송' },
  { icon: Eye, title: '챔프셀렉트 분석', desc: '밴 추천 · 카운터픽 · 팀 전력 비교' },
  { icon: BarChart2, title: '내전 대시보드', desc: 'Elo · 어워즈 · MVP 랭킹' },
  { icon: Gamepad2, title: '라이브 게임', desc: '실시간 스코어 · 이벤트 피드' },
];

const STEPS = [
  '.msi 설치 파일 다운로드',
  '설치 실행 → 시작 프로그램 자동 등록',
  '수집기 실행 → 시스템 트레이 아이콘',
  'LoL 로그인 → 수집기가 LCU 자동 연결',
  '게임 종료 시 자동 수집 · 전송',
];

export function MobileLcuPage() {
  return (
    <div>
      {/* Download */}
      <div className="m-card" style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <div style={{
            width: 36, height: 36, borderRadius: 10,
            background: 'var(--gradient-primary)', display: 'flex',
            alignItems: 'center', justifyContent: 'center',
          }}>
            <Monitor size={18} color="#fff" />
          </div>
          <div>
            <div style={{ fontSize: 15, fontWeight: 700 }}>
              LoL 내전 수집기 <span style={{ color: 'var(--color-primary)' }}>v1.0.0</span>
            </div>
            <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>
              Compose Desktop · Windows
            </div>
          </div>
        </div>
        <p style={{ fontSize: 12, color: 'var(--color-text-secondary)', lineHeight: 1.7, marginBottom: 14 }}>
          LoL 클라이언트와 연동하여 내전 데이터를 자동 수집합니다.
          챔프셀렉트 분석, 라이브 게임 모니터링을 제공합니다.
        </p>
        <a href={INSTALLER_URL} download style={{ display: 'block' }}>
          <button className="m-admin-btn m-admin-btn-primary" style={{ width: '100%' }}>
            <Download size={16} />
            수집기 다운로드 (.msi)
          </button>
        </a>
      </div>

      {/* Features */}
      <div className="m-card" style={{ marginBottom: 12 }}>
        <p className="m-section-title">주요 기능</p>
        {FEATURES.map(({ icon: Icon, title, desc }) => (
          <div key={title} style={{
            display: 'flex', gap: 10, padding: '10px 0',
            borderBottom: '1px solid var(--color-border)',
          }}>
            <div style={{
              width: 32, height: 32, borderRadius: 8, flexShrink: 0,
              background: 'rgba(0,180,216,0.08)', border: '1px solid rgba(0,180,216,0.15)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon size={14} color="var(--color-primary)" />
            </div>
            <div>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 1 }}>{title}</div>
              <div style={{ fontSize: 11, color: 'var(--color-text-secondary)' }}>{desc}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Steps */}
      <div className="m-card">
        <p className="m-section-title">설치 및 사용</p>
        <ol style={{ paddingLeft: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
          {STEPS.map((step, i) => (
            <li key={i} style={{ fontSize: 12, color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
              {step}
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}
