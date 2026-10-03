import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { fileURLToPath, URL } from 'node:url';

/**
 * dev 는 **proxy 로 same-origin** 이다. 프론트 코드는 `/api/...` 상대경로만 쓰고, 여기서 백엔드로
 * 넘긴다. 운영은 nginx(`nginx.conf`)가 같은 일을 하므로 dev 와 운영의 네트워크 모델이 같다.
 *
 * 붙일 백엔드는 `.env.local` 의 `VITE_DEV_BE` 로 바꾼다(`.env.example` 참고).
 */
export default defineConfig(({ mode }) => {
	const env = loadEnv(mode, process.cwd(), '');
	const target = env.VITE_DEV_BE || 'http://localhost:8081';

	return {
		plugins: [react(), tailwindcss()],
		resolve: {
			alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
		},
		server: {
			port: 8080,
			proxy: {
				'/api': {
					target,
					changeOrigin: true,
					configure: (proxy) => {
						// 프록시 구간은 서버 대 서버다. 브라우저가 붙인 Origin 을 그대로 넘기면 백엔드의
						// CORS 필터가 교차 출처로 보고 막을 수 있다 — curl 로는 재현되지 않는다.
						proxy.on('proxyReq', (proxyReq) => {
							proxyReq.removeHeader('origin');
						});
					}
				}
			}
		}
	};
});
