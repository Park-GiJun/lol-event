interface Window {
  lol: {
    getVersion: () => Promise<string>;
    minimize: () => void;
    close: () => void;
    startCollect: () => void;
    requestStatus: () => void;
    installUpdate: () => void;
    onLog: (cb: (type: string, message: string) => void) => void;
    onStatus: (cb: (s: Record<string, unknown>) => void) => void;
    onUpdateAvailable: (cb: (info: Record<string, unknown>) => void) => void;
    onUpdateDownloaded: (cb: (info: Record<string, unknown>) => void) => void;
  };
}
