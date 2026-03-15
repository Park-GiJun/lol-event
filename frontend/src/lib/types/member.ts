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
