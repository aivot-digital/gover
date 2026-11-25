import {describe, expect, it, vi} from 'vitest';
import {AuthService} from './auth-service';
import {BaseApiService} from './base-api-service';

describe('BaseApiService writes', () => {
    it('does not send a discarded write when the pending authentication refresh completes', async () => {
        const refresh = Promise.withResolvers<void>();
        vi.spyOn(AuthService, 'isAccessTokenValid').mockReturnValue(false);
        const refreshAuth = vi.spyOn(AuthService, 'refresh').mockReturnValue(refresh.promise);
        const fetch = vi.spyOn(globalThis, 'fetch').mockResolvedValue(Response.json({}));
        const controller = new AbortController();

        const save = new BaseApiService().put('/api/test', {}, {abortBeforeSend: controller.signal});
        expect(refreshAuth).toHaveBeenCalledOnce();
        controller.abort();
        refresh.resolve();

        await expect(save).rejects.toBe(controller.signal.reason);
        expect(fetch).not.toHaveBeenCalled();
    });

    it('delivers a confirmed write whose editor was discarded after dispatch', async () => {
        const response = Promise.withResolvers<Response>();
        vi.spyOn(AuthService, 'isAccessTokenValid').mockReturnValue(true);
        vi.spyOn(AuthService, 'getCsrfToken').mockReturnValue('csrf-token');
        const fetch = vi.spyOn(globalThis, 'fetch').mockReturnValue(response.promise);
        const controller = new AbortController();

        const save = new BaseApiService().put('/api/test', {}, {abortBeforeSend: controller.signal});
        expect(fetch).toHaveBeenCalledOnce();
        const networkSignal = fetch.mock.calls[0][1]?.signal;
        controller.abort();
        expect(networkSignal?.aborted).toBe(false);
        response.resolve(Response.json({lockVersion: 3}));

        await expect(save).resolves.toEqual({lockVersion: 3});
    });
});
