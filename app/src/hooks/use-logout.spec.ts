import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from '../services/auth-service';
import {beginExplicitEditorDraftLogout, stopEditorDraftForLogout} from '../services/editor-draft-login';
import {useLogout} from './use-logout';

vi.mock('../services/editor-draft-login', () => ({
    beginExplicitEditorDraftLogout: vi.fn(), stopEditorDraftForLogout: vi.fn(),
}));

describe('explicit staff logout', () => {
    const location = {origin: 'https://staff.example', href: ''};

    beforeEach(() => {
        location.href = '/staff/editor';
        vi.stubGlobal('window', {location, setTimeout, clearTimeout});
        vi.spyOn(AuthService, 'getCsrfToken').mockReturnValue('token');
        vi.spyOn(AuthService, 'logout').mockImplementation(() => {});
        vi.mocked(stopEditorDraftForLogout).mockResolvedValue();
        vi.mocked(beginExplicitEditorDraftLogout).mockResolvedValue();
    });
    afterEach(() => {
        vi.restoreAllMocks();
        vi.unstubAllGlobals();
        vi.useRealTimers();
    });

    it.each([true, false])('invalidates every draft once the logout request settles (success: %s)', async (success) => {
        const response = Promise.withResolvers<Response>();
        const fetch = vi.fn(() => response.promise);
        vi.stubGlobal('fetch', fetch);
        const logout = useLogout()();
        expect(stopEditorDraftForLogout).toHaveBeenCalledOnce();
        expect(fetch).toHaveBeenCalledWith('/api/auth/logout', expect.objectContaining({method: 'POST'}));
        expect(beginExplicitEditorDraftLogout).not.toHaveBeenCalled();
        if (success) {
            response.resolve(new Response(null, {status: 204}));
            await logout;
        } else {
            response.reject(new Error('Offline'));
            await expect(logout).rejects.toThrow('Offline');
        }
        expect(beginExplicitEditorDraftLogout).toHaveBeenCalledOnce();
        expect(AuthService.logout).toHaveBeenCalledOnce();
        expect(location.href).toBe('https://staff.example/staff?logout=true');
    });

    it('finishes logging out even if the local draft database stalls', async () => {
        vi.useFakeTimers();
        Object.assign(window, {setTimeout, clearTimeout});
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, {status: 204})));
        vi.mocked(beginExplicitEditorDraftLogout).mockReturnValue(new Promise(() => {}));
        const logout = useLogout()();
        await vi.runAllTimersAsync();
        await logout;
        expect(location.href).toBe('https://staff.example/staff?logout=true');
    });
});
