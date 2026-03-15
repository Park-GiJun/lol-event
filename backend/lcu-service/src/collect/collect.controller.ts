import { Body, Controller, Get, Post, Query, Sse } from '@nestjs/common';
import { Observable } from 'rxjs';
import { CollectService, SseEvent } from './collect.service';

@Controller('api/lcu')
export class CollectController {
  constructor(private readonly collectService: CollectService) {}

  @Sse('collect')
  collect(@Query('mainServiceUrl') mainServiceUrl?: string): Observable<SseEvent> {
    const url = mainServiceUrl ?? process.env.MAIN_SERVICE_URL ?? 'http://localhost:8080';
    return this.collectService.collect(url);
  }

  @Post('ingest')
  async ingest(@Body() body: { matches: Record<string, unknown>[] }): Promise<{ published: number }> {
    return this.collectService.ingestMatches(body.matches ?? []);
  }
}
