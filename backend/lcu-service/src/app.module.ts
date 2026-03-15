import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { KafkaModule } from './kafka/kafka.module';
import { LcuModule } from './lcu/lcu.module';
import { CollectModule } from './collect/collect.module';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    KafkaModule,
    LcuModule,
    CollectModule,
  ],
})
export class AppModule {}
