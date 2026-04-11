import { Download, Monitor } from 'lucide-react';
import { Button } from '../components/common/Button';
import '../styles/pages/stats.css';

const INSTALLER_URL = 'https://gijun.net/downloads/lol-collector-setup.exe';

export function LcuPage() {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">LCU 수집</h1>
        <p className="page-subtitle">LoL 클라이언트에서 내전 데이터를 수집하고 서버에 저장합니다</p>
      </div>

      {/* 수집기 다운로드 */}
      <div className="card" style={{ marginBottom: 'var(--spacing-md)' }}>
        <div className="card-header">
          <span className="card-title">수집기 앱 다운로드</span>
        </div>
        <p className="text-secondary" style={{ fontSize: 'var(--font-size-sm)', marginBottom: 'var(--spacing-md)', lineHeight: 1.7 }}>
          수집기는 <strong style={{ color: 'var(--color-text-primary)' }}>로컬 PC</strong>에서 실행되는 프로그램입니다.
          설치 후 실행하면 시스템 트레이에 상주하며, 앱 창에서 수집 버튼을 눌러 데이터를 서버로 전송할 수 있습니다.
        </p>
        <a href={INSTALLER_URL} download style={{ display: 'inline-block' }}>
          <Button>
            <Download size={14} /> 수집기 다운로드 (.exe)
          </Button>
        </a>
      </div>

      {/* 사용 방법 */}
      <div className="card">
        <div className="card-header">
          <span className="card-title"><Monitor size={14} style={{ marginRight: 6 }} />사용 방법</span>
        </div>
        <ol className="lcu-step-list">
          <li>위 버튼으로 수집기 설치</li>
          <li>수집기 실행 — 시스템 트레이에 아이콘 표시됨</li>
          <li>LoL 클라이언트 실행 후 로그인</li>
          <li>수집기 창에서 <strong className="lcu-step-highlight">수집 시작</strong> 버튼 클릭</li>
          <li>수집 완료 시 자동으로 서버에 전송 → 통계 페이지에서 확인</li>
        </ol>
      </div>
    </div>
  );
}
