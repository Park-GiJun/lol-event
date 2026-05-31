export interface Member {
  riotId: string;
  puuid: string;
  registeredAt: string;
}

export interface RegisterMemberRequest {
  riotId: string;
}

export interface RegisterBulkRequest {
  riotIds: string[];
}

export interface BulkRegisterResult {
  riotId: string;
  status: 'ok' | 'skip' | 'error';
  reason?: string;
}

export interface BulkRegisterResponse {
  results: BulkRegisterResult[];
  total: number;
}

// 포지션 재배정 백필 (POST /admin/positions/reassign)
export interface ReassignPositionsResult {
  matchesScanned: number;
  matchesSkippedAram: number;
  teamsScanned: number;
  teamsAlreadyValid: number;
  teamsFixed: number;
  participantsUpdated: number;
}
