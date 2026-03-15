import { Controller, Get } from '@nestjs/common';
import { LcuService } from './lcu.service';

@Controller('api/lcu')
export class LcuController {
  constructor(private readonly lcuService: LcuService) {}

  @Get('status')
  getStatus() {
    return this.lcuService.getStatus();
  }

  @Get('debug')
  getDebug() {
    return this.lcuService.getDebugInfo();
  }
}
