import { Controller, Get, Query, Sse } from '@nestjs/common';
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
}
