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
      phase: string;
      blueTeam: { summonerName: string; riotId: string; isMe: boolean }[];
      redTeam: { summonerName: string; riotId: string; isMe: boolean }[];
    } | null>;
    getChampSelectFull: () => Promise<{
      myTeam: { cellId: number; summonerId?: number; championId: number; assignedPosition: string; riotId: string; summonerName: string; isMe: boolean }[];
      theirTeam: { cellId: number; summonerId?: number; championId: number; assignedPosition: string; riotId: string; summonerName: string; isMe: boolean }[];
      bans: { championId: number; team: 'blue' | 'red' }[];
      phase: string;
      timer: number;
    } | null>;
    onLog: (cb: (type: string, message: string) => void) => void;
    onStatus: (cb: (s: Record<string, unknown>) => void) => void;
    onAutoStatus: (cb: (message: string) => void) => void;
    onUpdateAvailable: (cb: (info: Record<string, unknown>) => void) => void;
    onUpdateDownloaded: (cb: (info: Record<string, unknown>) => void) => void;
  };
}
