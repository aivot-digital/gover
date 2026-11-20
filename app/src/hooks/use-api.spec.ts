import {renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {useApi} from './use-api';
import {AuthService, AUTH_EXPIRATION_LOCAL_STORAGE_KEY} from '../services/auth-service';
import {NoCodeApiService} from '../services/no-code-api-service';

vi.mock('./use-app-selector', () => ({useAppSelector: () => null}));
vi.mock('./use-app-dispatch', () => ({useAppDispatch: () => vi.fn()}));

const request = {
    expression: {
        type: 'NoCodeStaticValue' as const,
        value: 'true',
    },
    elementData: {
        effectiveValues: {},
        elementStates: {},
    },
    processData: null,
    instanceData: null,
    nodeData: null,
};
const result = {
    status: 'Evaluated',
    value: true,
    operands: [],
};
const session = (accessExpires = Date.now() - 1_000, csrfToken: string | undefined = 'old-csrf') => ({
    accessExpires,
    refreshExpires: Date.now() + 60_000,
    csrfToken,
});

afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
});

describe('useApi authenticated POST', () => {
    it.each(['expired-access', 'missing-csrf'])('refreshes before a No-Code dry run with %s', async (state) => {
        vi.stubGlobal('AppConfig', {apiHostname: ''});
        localStorage.setItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY, JSON.stringify(state === 'expired-access'
            ? session() : {
                ...session(Date.now() + 30_000),
                csrfToken: undefined,
            }));
        let refreshed = false;
        const fetch = vi.fn(async (url: string) => {
            if (url === '/api/auth/refresh') {
                refreshed = true;
                return Response.json(session(Date.now() + 30_000), {headers: {'X-CSRF-TOKEN': 'new-csrf'}});
            }
            return refreshed ? Response.json(result) : Response.json({message: 'Expired access token'}, {status: 401});
        });
        vi.stubGlobal('fetch', fetch);
        const {result: hook} = renderHook(useApi);
        const controller = new AbortController();
        await expect(new NoCodeApiService(hook.current).dryRun(request, controller)).resolves.toEqual(result);
        expect(fetch.mock.calls.map(([url]) => url)).toEqual(['/api/auth/refresh', '/api/no-code/dry-run/']);
        expect(fetch).toHaveBeenLastCalledWith('/api/no-code/dry-run/', expect.objectContaining({
            method: 'POST',
            body: JSON.stringify(request),
            signal: controller.signal,
            headers: expect.objectContaining({'X-CSRF-TOKEN': 'new-csrf'}),
        }));
        expect(AuthService.isAuthenticated()).toBe(true);
    });

    it('still clears the local session on a genuine unauthorized response', async () => {
        vi.stubGlobal('AppConfig', {apiHostname: ''});
        localStorage.setItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY, JSON.stringify(session(Date.now() + 30_000)));
        const fetch = vi.fn().mockResolvedValue(Response.json({message: 'Unauthorized'}, {status: 401}));
        vi.stubGlobal('fetch', fetch);
        const {result: hook} = renderHook(useApi);
        await expect(new NoCodeApiService(hook.current).dryRun(request)).rejects.toMatchObject({status: 401});
        expect(fetch).toHaveBeenCalledOnce();
        expect(AuthService.isAuthenticated()).toBe(false);
    });

    it('keeps the session when a dry run is aborted', async () => {
        vi.stubGlobal('AppConfig', {apiHostname: ''});
        localStorage.setItem(AUTH_EXPIRATION_LOCAL_STORAGE_KEY, JSON.stringify(session(Date.now() + 30_000)));
        const controller = new AbortController();
        controller.abort();
        vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new DOMException('Aborted', 'AbortError')));
        const {result: hook} = renderHook(useApi);
        await expect(new NoCodeApiService(hook.current).dryRun(request, controller)).rejects.toMatchObject({name: 'AbortError'});
        expect(AuthService.isAuthenticated()).toBe(true);
    });
});
