import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableCors({
    origin: [
      'http://localhost:8080', 'http://localhost:5173',
      'http://localhost:3000', 'http://localhost:4173',
      'https://gijun.net', 'https://www.gijun.net',
    ],
    credentials: true,
  });
  const port = process.env.PORT ?? 3001;
  await app.listen(port);
  console.log(`✅ lcu-service → http://localhost:${port}`);
}
bootstrap();
