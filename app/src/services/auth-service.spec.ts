import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from './auth-service';

const STORED_AUTH = JSON.stringify({
    accessExpires: Date.now() - 1_000,
    refreshExpires: Date.now() + 60_000,
    csrfToken: 'csrf-token',
});

describe('AuthService.refresh', () => {
    beforeEach(() => {
        localStorage.setItem('auth_expiration', STORED_AUTH);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        localStorage.clear();
    });

    it.each([502, 503, 504])('keeps the session when the backend cannot be reached (%i)', async (status) => {
        vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, {status}))));

        await expect(AuthService.refresh()).rejects.toMatchObject({status});

        expect(localStorage.getItem('auth_expiration')).toBe(STORED_AUTH);
        expect(AuthService.isAuthenticated()).toBe(true);
    });

    it.each([400, 401, 500])('logs out when the refresh is rejected (%i)', async (status) => {
        vi.stubGlobal('fetch', vi.fn(() => Promise.resolve(new Response(null, {status}))));

        await expect(AuthService.refresh()).rejects.toMatchObject({status});

        expect(localStorage.getItem('auth_expiration')).toBeNull();
    });
});
