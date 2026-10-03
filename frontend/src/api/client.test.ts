import { AxiosError, type AxiosAdapter, type AxiosResponse } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, api, client } from '@/api/client';

const originalAdapter = client.defaults.adapter;

/** 네트워크 대신 정해 둔 응답을 돌려주는 어댑터. axios 의 상태코드 판정(validateStatus)은 그대로 탄다. */
function respondWith(status: number, body: unknown) {
	const adapter = vi.fn<AxiosAdapter>((config) => {
		const response: AxiosResponse = {
			data: body,
			status,
			statusText: '',
			headers: {},
			config
		};
		if (status >= 200 && status < 300) return Promise.resolve(response);
		return Promise.reject(
			new AxiosError(`HTTP ${status}`, 'ERR_BAD_RESPONSE', config, null, response)
		);
	});
	client.defaults.adapter = adapter;
	return adapter;
}

describe('api', () => {
	afterEach(() => {
		client.defaults.adapter = originalAdapter;
	});

	it('성공 응답의 data 만 꺼내 준다', async () => {
		respondWith(200, { success: true, data: { hello: 'world' }, message: null });

		await expect(api.get('/api/ping')).resolves.toEqual({ hello: 'world' });
	});

	it('경로를 상대 주소 그대로 부른다 — 백엔드 주소를 박지 않는다', async () => {
		const adapter = respondWith(200, { success: true, data: null, message: null });

		await api.get('/api/stats', { params: { mode: 'normal' } });

		const config = adapter.mock.calls[0][0];
		expect(config.baseURL).toBe('');
		expect(config.url).toBe('/api/stats');
		expect(config.params).toEqual({ mode: 'normal' });
	});

	it('POST 는 본문을 실어 보낸다', async () => {
		const adapter = respondWith(200, { success: true, data: null, message: null });

		await api.post('/api/members/register', { riotId: 'Faker#KR1' });

		const config = adapter.mock.calls[0][0];
		expect(config.method).toBe('post');
		expect(JSON.parse(config.data as string)).toEqual({ riotId: 'Faker#KR1' });
	});

	it('HTTP 실패는 서버 문구·errorCode·상태코드를 실은 ApiError 로 던진다', async () => {
		respondWith(409, {
			success: false,
			data: null,
			message: '이미 등록된 멤버다',
			errorCode: 'DUPLICATE_MEMBER'
		});

		const error = await api.post('/api/members/register', {}).catch((e: unknown) => e);

		expect(error).toBeInstanceOf(ApiError);
		expect(error).toMatchObject({
			message: '이미 등록된 멤버다',
			errorCode: 'DUPLICATE_MEMBER',
			status: 409
		});
	});

	it('200 이어도 success 가 false 면 실패다', async () => {
		respondWith(200, { success: false, data: null, message: '안 된다', errorCode: 'CONFLICT' });

		await expect(api.get('/api/x')).rejects.toMatchObject({
			name: 'ApiError',
			errorCode: 'CONFLICT'
		});
	});

	it('본문이 없는 실패(게이트웨이 502 등)도 ApiError 로 바꾼다', async () => {
		respondWith(502, '<html>Bad Gateway</html>');

		await expect(api.get('/api/x')).rejects.toMatchObject({
			name: 'ApiError',
			message: '요청을 처리하지 못했습니다.',
			status: 502
		});
	});
});
