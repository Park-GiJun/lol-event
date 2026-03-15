import { app, BrowserWindow, Tray, Menu, nativeImage, shell, ipcMain, dialog } from 'electron';
import { autoUpdater } from 'electron-updater';
import path from 'path';
import { getStatus } from './lcu';
import { runCollect } from './collect';

app.setAppUserModelId('net.gijun.lol-collector');
app.on('window-all-closed', () => { /* tray app — 창 닫혀도 유지 */ });

let win: BrowserWindow | null = null;
let tray: Tray | null = null;

function getAssetPath(...parts: string[]): string {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'assets', ...parts)
    : path.join(__dirname, '..', 'assets', ...parts);
}

function getRendererPath(): string {
  return app.isPackaged
    ? path.join(app.getAppPath(), 'renderer', 'index.html')
    : path.join(__dirname, '..', 'renderer', 'index.html');
}

function createWindow() {
  win = new BrowserWindow({
    width: 500,
    height: 620,
    minWidth: 420,
    minHeight: 520,
    resizable: true,
    title: 'LoL 수집기',
    frame: false,
    titleBarStyle: 'hidden',
    backgroundColor: '#0d0f14',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
    icon: getAssetPath('icon.ico'),
  });

  win.loadFile(getRendererPath());

  win.on('closed', () => { win = null; });
}

function setupTray() {
  const iconPath = getAssetPath('icon.ico');
  let icon = nativeImage.createFromPath(iconPath);
  if (icon.isEmpty()) icon = nativeImage.createEmpty();

  tray = new Tray(icon);
  tray.setToolTip('LoL 수집기');

  const menu = Menu.buildFromTemplate([
    { label: 'LoL 수집기', enabled: false },
    { type: 'separator' },
    {
      label: '창 열기',
      click: () => {
        if (win) { win.show(); win.focus(); }
        else createWindow();
      },
    },
    { label: '웹사이트', click: () => shell.openExternal('https://gijun.net/lcu') },
    {
      label: '시작 프로그램 등록',
      type: 'checkbox',
      checked: app.getLoginItemSettings().openAtLogin,
      click: (item) => app.setLoginItemSettings({ openAtLogin: item.checked }),
    },
    { type: 'separator' },
    { label: '종료', click: () => app.quit() },
  ]);

  tray.setContextMenu(menu);
  tray.on('double-click', () => {
    if (win) { win.show(); win.focus(); }
    else createWindow();
  });
}

function setupAutoUpdater() {
  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = true;

  autoUpdater.on('update-available', (info) => {
    win?.webContents.send('update:available', info);
  });

  autoUpdater.on('update-downloaded', (info) => {
    win?.webContents.send('update:downloaded', info);
  });

  autoUpdater.on('error', (err) => {
    console.error('Auto-updater error:', err.message);
  });

  // 앱 시작 10초 후 업데이트 확인
  setTimeout(() => {
    autoUpdater.checkForUpdates().catch(() => { /* 오프라인 등 무시 */ });
  }, 10_000);
}

// IPC 핸들러
function setupIPC() {
  ipcMain.handle('app:version', () => app.getVersion());

  ipcMain.on('win:minimize', () => win?.minimize());
  ipcMain.on('win:close', () => win?.close());

  ipcMain.on('lcu:status-request', async () => {
    const status = await getStatus();
    win?.webContents.send('lcu:status', status);
  });

  ipcMain.on('collect:start', () => {
    runCollect((type, message) => {
      win?.webContents.send('collect:log', type, message);
    }).catch((e) => {
      win?.webContents.send('collect:log', 'error', (e as Error).message);
    });
  });

  ipcMain.on('update:install', () => {
    autoUpdater.quitAndInstall();
  });
}

app.whenReady().then(async () => {
  if (!app.requestSingleInstanceLock()) {
    dialog.showErrorBox('LoL 수집기', '이미 실행 중입니다. 시스템 트레이를 확인하세요.');
    app.quit();
    return;
  }

  setupIPC();
  createWindow();
  setupTray();

  if (app.isPackaged) {
    setupAutoUpdater();
  }
});

app.on('second-instance', () => {
  if (win) { win.show(); win.focus(); }
  else createWindow();
});
