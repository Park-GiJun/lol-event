import { contextBridge, ipcRenderer } from 'electron';

contextBridge.exposeInMainWorld('lol', {
  getVersion: () => ipcRenderer.invoke('app:version'),
  minimize: () => ipcRenderer.send('win:minimize'),
  close: () => ipcRenderer.send('win:close'),
  startCollect: () => ipcRenderer.send('collect:start'),
  requestStatus: () => ipcRenderer.send('lcu:status-request'),
  installUpdate: () => ipcRenderer.send('update:install'),
  onLog: (cb: (type: string, message: string) => void) => {
    ipcRenderer.removeAllListeners('collect:log');
    ipcRenderer.on('collect:log', (_e, type: string, message: string) => cb(type, message));
  },
  onStatus: (cb: (s: Record<string, unknown>) => void) => {
    ipcRenderer.removeAllListeners('lcu:status');
    ipcRenderer.on('lcu:status', (_e, s: Record<string, unknown>) => cb(s));
  },
  onUpdateAvailable: (cb: (info: Record<string, unknown>) => void) => {
    ipcRenderer.removeAllListeners('update:available');
    ipcRenderer.on('update:available', (_e, info: Record<string, unknown>) => cb(info));
  },
  onUpdateDownloaded: (cb: (info: Record<string, unknown>) => void) => {
    ipcRenderer.removeAllListeners('update:downloaded');
    ipcRenderer.on('update:downloaded', (_e, info: Record<string, unknown>) => cb(info));
  },
});
