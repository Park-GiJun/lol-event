import { Download, Monitor } from 'lucide-react';

const INSTALLER_URL = 'https://gijun.net/downloads/lol-collector-setup.exe';

const STEPS = [
  '위 버튼으로 수집기(.exe) 설치',
  '수집기 실행 — 시스템 트레이에 아이콘 표시됨',
  'LoL 클라이언트 실행 후 로그인',
  '수집기 창에서 수집 시작 버튼 클릭',
  '수집 완료 시 자동으로 서버에 전송',
];

export function MobileLcuPage() {
  return (
    <div>
      {/* Download */}
      <div className="m-card" style={{ marginBottom: 12 }}>
        <p className="m-section-title">수집기 앱</p>
        <p style={{ fontSize: 13, color: 'var(--color-text-secondary)', lineHeight: 1.7, marginBottom: 14 }}>
          로컬 PC에서 실행되는 프로그램입니다. 설치 후 실행하면 시스템 트레이에 상주하며 내전 데이터를 자동 수집합니다.
        </p>
        <a href={INSTALLER_URL} download style={{ display: 'block' }}>
          <button className="m-admin-btn m-admin-btn-primary" style={{ width: '100%' }}>
            <Download size={16} />
            수집기 다운로드 (.exe)
          </button>
        </a>
      </div>

      {/* Steps */}
      <div className="m-card">
        <p className="m-section-title">
          <Monitor size={13} style={{ marginRight: 6, verticalAlign: 'middle' }} />
          사용 방법
        </p>
        <ol style={{ paddingLeft: 20, display: 'flex', flexDirection: 'column', gap: 10 }}>
          {STEPS.map((step, i) => (
            <li key={i} style={{ fontSize: 13, color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>
              {step}
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}
