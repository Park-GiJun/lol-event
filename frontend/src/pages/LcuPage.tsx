import { Download, Monitor, Zap, Shield, RefreshCw, BarChart2, Eye, Gamepad2 } from 'lucide-react';
import { Button } from '../components/common/Button';
import '../styles/pages/stats.css';

const INSTALLER_URL = 'https://gijun.net/downloads/lol-collector.msi';

const FEATURES = [
  { icon: Zap, title: '자동 수집', desc: '게임 종료 감지 → 30초 후 자동 수집 · 서버 전송' },
  { icon: Eye, title: '챔프셀렉트 분석', desc: '상대 모스트픽 · 밴 추천 · 카운터픽 · 팀 전력 비교' },
  { icon: BarChart2, title: '내전 대시보드', desc: 'Elo 리더보드 · 어워즈 · MVP · 멀티킬 하이라이트' },
  { icon: Gamepad2, title: '라이브 게임', desc: '실시간 킬/CS/레벨 · 게임 이벤트 피드' },
  { icon: Shield, title: '로비 캐시', desc: '대기방 10명 데이터 캐싱 → 챔프셀렉트에서 활용' },
  { icon: RefreshCw, title: '자동 업데이트', desc: '새 버전 자동 감지 · 원클릭 업데이트' },
];

export function LcuPage() {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">데스크톱 수집기</h1>
        <p className="page-subtitle">LoL 클라이언트 연동 · 자동 수집 · 실시간 분석</p>
      </div>

      {/* 다운로드 섹션 */}
      <div className="card-glass" style={{ marginBottom: 'var(--spacing-lg)', padding: 'var(--spacing-lg)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <div style={{
            width: 44, height: 44, borderRadius: 12,
            background: 'var(--gradient-primary)', display: 'flex',
            alignItems: 'center', justifyContent: 'center',
            boxShadow: 'var(--shadow-teal-md)',
          }}>
            <Monitor size={22} color="#fff" />
          </div>
          <div>
            <div style={{ fontSize: 'var(--font-size-lg)', fontWeight: 700 }}>
              LoL 내전 수집기 <span style={{ color: 'var(--color-primary)' }}>v1.0.0</span>
            </div>
            <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)' }}>
              Compose Desktop · Windows 전용 · 시스템 트레이 상주
            </div>
          </div>
        </div>

        <p style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-secondary)', lineHeight: 1.7, marginBottom: 'var(--spacing-md)' }}>
          수집기는 로컬 PC에서 실행되며, LoL 클라이언트(LCU)와 직접 연동하여
          내전 데이터를 <strong style={{ color: 'var(--color-text-primary)' }}>자동으로 수집</strong>하고
          서버에 전송합니다. 챔피언 셀렉트 분석, 라이브 게임 모니터링, 내전 대시보드를 제공합니다.
        </p>

        <a href={INSTALLER_URL} download style={{ display: 'inline-block' }}>
          <Button variant="primary" size="md">
            <Download size={15} /> 수집기 다운로드 (.msi)
          </Button>
        </a>
      </div>

      {/* 주요 기능 */}
      <div className="card" style={{ marginBottom: 'var(--spacing-lg)' }}>
        <div className="card-header">
          <span className="card-title">주요 기능</span>
        </div>
        <div className="grid-16">
          {FEATURES.map(({ icon: Icon, title, desc }) => (
            <div key={title} className="col-span-8" style={{
              display: 'flex', gap: 12, padding: '12px 0',
              borderBottom: '1px solid var(--glass-border)',
            }}>
              <div style={{
                width: 36, height: 36, borderRadius: 10, flexShrink: 0,
                background: 'rgba(0, 180, 216, 0.08)',
                border: '1px solid rgba(0, 180, 216, 0.15)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon size={16} color="var(--color-primary)" />
              </div>
              <div>
                <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: 600, marginBottom: 2 }}>{title}</div>
                <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', lineHeight: 1.5 }}>{desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 설치 방법 */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">설치 및 사용</span>
        </div>
        <ol className="lcu-step-list">
          <li>위 버튼으로 <strong className="lcu-step-highlight">.msi 설치 파일</strong> 다운로드</li>
          <li>설치 실행 → Windows 시작 프로그램에 자동 등록됨</li>
          <li>수집기 실행 — 시스템 트레이에 아이콘 표시</li>
          <li>LoL 클라이언트 로그인 → 수집기가 자동으로 LCU 연결</li>
          <li>
            <strong className="lcu-step-highlight">자동 수집 모드</strong>:
            게임 종료 시 자동으로 데이터 수집 및 전송
          </li>
          <li>내전 대기방 진입 시 → 자동으로 팀 분석 · 밴 추천 표시</li>
        </ol>

        <div style={{
          marginTop: 'var(--spacing-md)', padding: 'var(--spacing-sm) var(--spacing-md)',
          background: 'rgba(0, 180, 216, 0.06)', borderRadius: 'var(--radius-md)',
          border: '1px solid rgba(0, 180, 216, 0.12)',
          fontSize: 'var(--font-size-xs)', color: 'var(--color-text-secondary)', lineHeight: 1.6,
        }}>
          💡 <strong style={{ color: 'var(--color-primary)' }}>Tip</strong>: 수집기를 실행한 상태로 LoL을
          플레이하면 모든 내전 데이터가 자동으로 수집됩니다. 별도의 수동 조작이 필요 없습니다.
        </div>
      </div>
    </div>
  );
}
