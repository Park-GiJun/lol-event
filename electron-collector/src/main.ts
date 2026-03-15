import { app, Tray, Menu, nativeImage, shell, dialog } from 'electron';
import path from 'path';
import { startServer, stopServer } from './server';

// Windows 알림 센터 앱 ID
app.setAppUserModelId('net.gijun.lol-collector');

// 창이 없어도 앱 유지
app.on('window-all-closed', () => { /* tray app — 종료하지 않음 */ });

let tray: Tray | null = null;

app.whenReady().then(async () => {
  // 중복 실행 방지
  const gotLock = app.requestSingleInstanceLock();
  if (!gotLock) {
    dialog.showErrorBox('LoL 수집기', '이미 실행 중입니다. 시스템 트레이를 확인하세요.');
    app.quit();
    return;
  }

  // Express 서버 시작
  try {
    await startServer(3001);
  } catch {
    dialog.showErrorBox(
      'LoL 수집기 — 포트 오류',
      'localhost:3001 포트가 이미 사용 중입니다.\n기존 수집기를 종료 후 다시 실행하세요.'
    );
    app.quit();
    return;
  }

  // 트레이 아이콘
  const iconPath = app.isPackaged
    ? path.join(process.resourcesPath, 'assets', 'icon.ico')
    : path.join(__dirname, '..', 'assets', 'icon.ico');

  let icon = nativeImage.createFromPath(iconPath);
  if (icon.isEmpty()) {
    // 아이콘 파일이 없으면 빈 이미지 (개발 환경 fallback)
    icon = nativeImage.createEmpty();
  }

  tray = new Tray(icon);
  tray.setToolTip('LoL 수집기 — localhost:3001 실행 중');

  const updateMenu = () => Menu.buildFromTemplate([
    { label: 'LoL 수집기', enabled: false },
    { label: 'localhost:3001 실행 중', enabled: false },
    { type: 'separator' },
    {
      label: '웹사이트 열기',
      click: () => shell.openExternal('https://gijun.net/lcu'),
    },
    {
      label: '시작 프로그램에 등록',
      type: 'checkbox',
      checked: app.getLoginItemSettings().openAtLogin,
      click: (item) => {
        app.setLoginItemSettings({ openAtLogin: item.checked });
      },
    },
    { type: 'separator' },
    {
      label: '종료',
      click: () => {
        stopServer();
        app.quit();
      },
    },
  ]);

  tray.setContextMenu(updateMenu());

  // 트레이 클릭 시 메뉴 갱신
  tray.on('click', () => tray?.setContextMenu(updateMenu()));
});
