interface Window {
  lol: {
    getVersion: () => Promise<string>;
    minimize: () => void;
    close: () => void;
    startCollect: () => void;
    requestStatus: () => void;
    installUpdate: () => void;
    getLiveGame: () => Promise<Record<string, unknown> | null>;
    getChampSelect: () => Promise<Record<string, unknown> | null>;
    getSummonerHistory: (puuid: string) => Promise<Record<string, unknown> | null>;
    getCustomMostPicks: () => Promise<{
      isCustom: boolean;
      phase: string;
      opponents: { summonerName: string; riotId: string }[];
    } | null>;
    onLog: (cb: (type: string, message: string) => void) => void;
    onStatus: (cb: (s: Record<string, unknown>) => void) => void;
    onUpdateAvailable: (cb: (info: Record<string, unknown>) => void) => void;
    onUpdateDownloaded: (cb: (info: Record<string, unknown>) => void) => void;
  };
}
