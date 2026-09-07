import {afterEach, describe, expect, it, vi} from 'vitest';
import {BaseApiService} from '../../../services/base-api-service';
import {
    CustomerTaskViewApiService,
    isRequiredIdentityAuthenticationError,
} from './customer-task-view-api-service';

describe('CustomerTaskViewApiService', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('loads customer task views without invoking staff authentication handling', async () => {
        const get = vi
            .spyOn(BaseApiService.prototype, 'get')
            .mockResolvedValue({layout: {}, data: {}, events: []});

        await new CustomerTaskViewApiService().getTaskView('instance', 'task');

        expect(get).toHaveBeenCalledWith('/api/public/processes/instance/tasks/task/', {
            skipAuthCheck: true,
            doNotHandleStatusCodes: true,
        });
    });

    it('creates a login link with the cleaned origin and existing task query parameters', () => {
        const link = new CustomerTaskViewApiService().createRequiredIdentityAuthenticationStartLink(
            'instance/key',
            'task key',
            'https://prosuna.example.test/process/instance/tasks/task?foo=bar&identity-state=0&error=failed',
        );
        const linkUrl = new URL(link);
        const origin = new URL(linkUrl.searchParams.get('origin') ?? '');

        expect(linkUrl.pathname).toBe('/api/public/processes/instance%2Fkey/tasks/task%20key/identity/start/');
        expect(linkUrl.searchParams.get('foo')).toBe('bar');
        expect(origin.searchParams.get('foo')).toBe('bar');
        expect(origin.searchParams.has('identity-state')).toBe(false);
        expect(origin.searchParams.has('error')).toBe(false);
    });

    it('recognizes only the structured required-identity authentication error', () => {
        expect(isRequiredIdentityAuthenticationError({
            status: 401,
            message: 'Erneute Anmeldung erforderlich.',
            displayableToUser: true,
            details: {reason: 'required_identity_authentication'},
        })).toBe(true);
        expect(isRequiredIdentityAuthenticationError({
            status: 401,
            message: 'Nicht angemeldet.',
            displayableToUser: true,
            details: null,
        })).toBe(false);
    });
});
