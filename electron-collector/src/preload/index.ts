import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('lol', {
  getVersion: () => ipcRenderer.invoke('app:version'),
  minimize: () => ipcRenderer.send('win:minimize'),
  close: () => ipcRenderer.send('win:close'),
  startCollect: () => ipcRenderer.send('collect:start'),
  requestStatus: () => ipcRenderer.send('lcu:status-request'),
  installUpdate: () => ipcRenderer.send('update:install'),
  getLiveGame: () => ipcRenderer.invoke('lcu:live-game'),
  getChampSelect: () => ipcRenderer.invoke('lcu:champ-select'),
  getSummonerHistory: (puuid: string) => ipcRenderer.invoke('lcu:summoner-history', puuid),
  getCustomMostPicks: () => ipcRenderer.invoke('lcu:custom-most-picks'),
  openExternal: (url: string) => ipcRenderer.invoke('app:open-external', url),
  onLog: (cb: (type: string, message: string) => void) => {
    ipcRenderer.removeAllListeners('collect:log');
    ipcRenderer.on('collect:log', (_e, type: string, message: string) => cb(type, message));
  },
  onStatus: (cb: (s: Record<string, unknown>) => void) => {
    ipcRenderer.removeAllListeners('lcu:status');
    ipcRenderer.on('lcu:status', (_e, s: Record<string, unknown>) => cb(s));
  },
  onUpdateChecking: (cb: () => void) => {
    ipcRenderer.removeAllListeners('update:checking');
    ipcRenderer.on('update:checking', () => cb());
  },
  onUpdateNotAvailable: (cb: () => void) => {
    ipcRenderer.removeAllListeners('update:not-available');
    ipcRenderer.on('update:not-available', () => cb());
  },
  onUpdateAvailable: (cb: (info: Record<string, unknown>) => void) => {
    ipcRenderer.removeAllListeners('update:available');
    ipcRenderer.on('update:available', (_e, info: Record<string, unknown>) => cb(info));
  },
  onUpdateProgress: (cb: (percent: number) => void) => {
    ipcRenderer.removeAllListeners('update:progress');
    ipcRenderer.on('update:progress', (_e, percent: number) => cb(percent));
  },
  onUpdateInstalling: (cb: () => void) => {
    ipcRenderer.removeAllListeners('update:installing');
    ipcRenderer.on('update:installing', () => cb());
  },
  onUpdateDownloaded: (cb: (info: Record<string, unknown>) => void) => {
    ipcRenderer.removeAllListeners('update:downloaded');
    ipcRenderer.on('update:downloaded', (_e, info: Record<string, unknown>) => cb(info));
  },
});
