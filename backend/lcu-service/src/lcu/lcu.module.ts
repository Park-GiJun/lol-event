import { Module } from '@nestjs/common';
import { LcuService } from './lcu.service';
import { LcuController } from './lcu.controller';

@Module({
  providers: [LcuService],
  controllers: [LcuController],
  exports: [LcuService],
})
export class LcuModule {}
