import { Injectable, Logger, OnApplicationShutdown, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Kafka, Producer } from 'kafkajs';

export const MATCH_TOPIC = 'lol.match.events';

@Injectable()
export class KafkaService implements OnModuleInit, OnApplicationShutdown {
  private readonly logger = new Logger(KafkaService.name);
  private readonly producer: Producer;

  constructor(private readonly config: ConfigService) {
    const brokers = this.config.get<string>('KAFKA_BROKERS', 'localhost:9093');
    const kafka = new Kafka({
      clientId: 'lcu-service',
      brokers: brokers.split(','),
    });
    this.producer = kafka.producer();
  }

  async onModuleInit() {
    await this.producer.connect();
    this.logger.log(`Kafka producer connected — topic: ${MATCH_TOPIC}`);
  }

  async onApplicationShutdown() {
    await this.producer.disconnect();
  }

  async publishMatch(matchId: string, matchData: unknown): Promise<void> {
    await this.producer.send({
      topic: MATCH_TOPIC,
      messages: [{ key: matchId, value: JSON.stringify(matchData) }],
    });
  }
}
