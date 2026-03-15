import { Module } from '@nestjs/common';
import { CollectService } from './collect.service';
import { CollectController } from './collect.controller';
import { LcuModule } from '../lcu/lcu.module';

@Module({
  imports: [LcuModule],
  providers: [CollectService],
  controllers: [CollectController],
})
export class CollectModule {}
