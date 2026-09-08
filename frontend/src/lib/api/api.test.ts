import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, setErrorHandler } from './api';

// 테스트도 앱과 같은 환경변수 해석을 따른다. .env 값이 바뀌어도 이 테스트는 안 깨진다.
const BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api';

function jsonResponse(body: unknown, init: { ok?: boolean; status?: number } = {}) {
  return {
    ok: init.ok ?? true,
    status: init.status ?? 200,
    json: () => Promise.resolve(body),
  } as Response;
}

describe('api.request', () => {
  const onError = vi.fn();

  beforeEach(() => {
    onError.mockReset();
    setErrorHandler(onError);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('성공 응답의 data 만 꺼내 준다', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve(jsonResponse({ success: true, data: { hello: 'world' }, message: null, errorCode: null })),
    ));
    await expect(api.get('/ping')).resolves.toEqual({ hello: 'world' });
    expect(onError).not.toHaveBeenCalled();
  });

  it('GET 은 베이스 URL 을 붙여 부른다', async () => {
    // 인자를 명시해야 mock.calls 가 [url, init] 튜플로 잡힌다.
    const fetchMock = vi.fn((_url: string, _init?: RequestInit) =>
      Promise.resolve(jsonResponse({ success: true, data: null, message: null, errorCode: null })),
    );
    vi.stubGlobal('fetch', fetchMock);
    await api.get('/matches?page=0');
    expect(fetchMock.mock.calls[0][0]).toBe(`${BASE}/matches?page=0`);
  });

  it('POST 는 본문을 JSON 으로 싣고 Content-Type 을 붙인다', async () => {
    // 인자를 명시해야 mock.calls 가 [url, init] 튜플로 잡힌다.
    const fetchMock = vi.fn((_url: string, _init?: RequestInit) =>
      Promise.resolve(jsonResponse({ success: true, data: null, message: null, errorCode: null })),
    );
    vi.stubGlobal('fetch', fetchMock);
    await api.post('/members', { riotId: 'a#KR1' });
    const [, init] = fetchMock.mock.calls[0];
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe('{"riotId":"a#KR1"}');
    expect(init?.headers).toMatchObject({ 'Content-Type': 'application/json' });
  });

  it('HTTP 에러면 서버 메시지를 그대로 띄우고 throw 한다', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve(jsonResponse({ message: '경기를 찾을 수 없습니다' }, { ok: false, status: 404 })),
    ));
    await expect(api.get('/matches/none')).rejects.toThrow('경기를 찾을 수 없습니다');
    expect(onError).toHaveBeenCalledWith('HTTP 404', '경기를 찾을 수 없습니다');
  });

  it('에러 본문이 JSON 이 아니어도 터지지 않는다', async () => {
    // 게이트웨이가 502 HTML 을 뱉는 경우가 실제로 있다.
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve({
        ok: false,
        status: 502,
        json: () => Promise.reject(new SyntaxError('Unexpected token <')),
      } as unknown as Response),
    ));
    await expect(api.get('/home')).rejects.toThrow('HTTP 502');
    expect(onError).toHaveBeenCalledWith('HTTP 502', '요청 실패');
  });

  it('HTTP 200 이지만 success:false 인 응답도 에러로 다룬다', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve(jsonResponse({ success: false, data: null, message: '집계가 아직 끝나지 않았습니다', errorCode: 'STATS_PENDING' })),
    ));
    await expect(api.get('/stats')).rejects.toThrow('집계가 아직 끝나지 않았습니다');
    expect(onError).toHaveBeenCalledWith('오류', '집계가 아직 끝나지 않았습니다');
  });

  it('메시지 없는 실패에도 기본 문구를 띄운다', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve(jsonResponse({ success: false, data: null, message: null, errorCode: null })),
    ));
    await expect(api.get('/stats')).rejects.toThrow('API Error');
    expect(onError).toHaveBeenCalledWith('오류', '알 수 없는 오류');
  });
});
