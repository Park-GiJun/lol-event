import { Injectable, Logger, OnApplicationShutdown, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Kafka, Producer } from 'kafkajs';

export const MATCH_TOPIC = 'lol.match.events';

@Injectable()
export class KafkaService implements OnModuleInit, OnApplicationShutdown {
  private readonly logger = new Logger(KafkaService.name);
  private readonly producer: Producer;
  private connected = false;

  constructor(private readonly config: ConfigService) {
    const brokers = this.config.get<string>('KAFKA_BROKERS', 'localhost:9093');
    const kafka = new Kafka({
      clientId: 'lcu-service',
      brokers: brokers.split(','),
      retry: { retries: 3 },
    });
    this.producer = kafka.producer();
  }

  async onModuleInit() {
    try {
      await this.producer.connect();
      this.connected = true;
      this.logger.log(`Kafka producer connected — topic: ${MATCH_TOPIC}`);
    } catch (e) {
      this.logger.warn(`Kafka 연결 실패 (수집 후 직접 저장 모드로 동작): ${(e as Error).message}`);
    }
  }

  async onApplicationShutdown() {
    if (this.connected) {
      try {
        await this.producer.disconnect();
      } catch {
        // shutdown 중 에러는 무시
      }
    }
  }

  async publishMatch(matchId: string, matchData: unknown): Promise<void> {
    if (!this.connected) {
      throw new Error('Kafka 미연결 — 메시지 전송 불가');
    }
    await this.producer.send({
      topic: MATCH_TOPIC,
      messages: [{ key: matchId, value: JSON.stringify(matchData) }],
    });
  }

  isConnected(): boolean {
    return this.connected;
  }
}
