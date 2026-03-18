import { app, BrowserWindow, Tray, Menu, nativeImage, shell, ipcMain, dialog } from 'electron';
import { autoUpdater } from 'electron-updater';
import path, { join } from 'path';
import { getStatus, getLiveGame, getChampSelect, getSummonerHistory, getCustomMostPicks } from './lcu';
import { runCollect } from './collect';

app.setAppUserModelId('net.gijun.lol-collector');
app.on('window-all-closed', () => {});

let win: BrowserWindow | null = null;
let tray: Tray | null = null;

function getAssetPath(...parts: string[]): string {
  return app.isPackaged
    ? path.join(process.resourcesPath, 'assets', ...parts)
    : path.join(__dirname, '../../assets', ...parts);
}

function createWindow(): void {
  win = new BrowserWindow({
    width: 960,
    height: 800,
    minWidth: 760,
    minHeight: 680,
    frame: false,
    backgroundColor: '#0A1428',
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
    icon: getAssetPath('icon.ico'),
  });

  if (process.env['ELECTRON_RENDERER_URL']) {
    win.loadURL(process.env['ELECTRON_RENDERER_URL']);
    win.webContents.openDevTools({ mode: 'detach' });
  } else {
    win.loadFile(join(__dirname, '../renderer/index.html'));
  }

  win.on('closed', () => { win = null; });
}

function setupTray(): void {
  const iconPath = getAssetPath('icon.ico');
  let icon = nativeImage.createFromPath(iconPath);
  if (icon.isEmpty()) icon = nativeImage.createEmpty();

  tray = new Tray(icon);
  tray.setToolTip('LoL 수집기');

  const menu = Menu.buildFromTemplate([
    { label: 'LoL 수집기', enabled: false },
    { type: 'separator' },
    { label: '창 열기', click: () => { if (win) { win.show(); win.focus(); } else createWindow(); } },
    { label: '웹사이트', click: () => shell.openExternal('https://gijun.net') },
    { label: '시작 프로그램 등록', type: 'checkbox', checked: app.getLoginItemSettings().openAtLogin,
      click: (item) => app.setLoginItemSettings({ openAtLogin: item.checked }) },
    { type: 'separator' },
    { label: '종료', click: () => app.quit() },
  ]);
  tray.setContextMenu(menu);
  tray.on('double-click', () => { if (win) { win.show(); win.focus(); } else createWindow(); });
}

function setupAutoUpdater(): void {
  autoUpdater.autoDownload = true;
  autoUpdater.autoInstallOnAppQuit = false;
  autoUpdater.on('update-available', (info) => win?.webContents.send('update:available', info));
  autoUpdater.on('update-downloaded', () => {
    autoUpdater.quitAndInstall(false, true);
  });
  autoUpdater.on('error', (err) => console.error('updater:', err.message));
  autoUpdater.checkForUpdates().catch(() => {});
}

function setupIPC(): void {
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
    }).catch((e) => win?.webContents.send('collect:log', 'error', (e as Error).message));
  });
  ipcMain.handle('lcu:live-game', () => getLiveGame());
  ipcMain.handle('lcu:champ-select', () => getChampSelect());
  ipcMain.handle('lcu:summoner-history', (_e, puuid: string) => getSummonerHistory(puuid));
  ipcMain.handle('lcu:custom-most-picks', () => getCustomMostPicks());
  ipcMain.on('update:install', () => autoUpdater.quitAndInstall());
}

app.whenReady().then(() => {
  if (!app.requestSingleInstanceLock()) {
    dialog.showErrorBox('LoL 수집기', '이미 실행 중입니다.');
    app.quit();
    return;
  }
  setupIPC();
  createWindow();
  setupTray();
  if (app.isPackaged) setupAutoUpdater();
});

app.on('second-instance', () => { if (win) { win.show(); win.focus(); } else createWindow(); });
