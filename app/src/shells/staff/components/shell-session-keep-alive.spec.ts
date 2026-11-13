import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from '../../../services/auth-service';
import {renewSessionOnActivity, SESSION_RENEWAL_INTERVAL} from './shell-session-keep-alive';

describe('renewSessionOnActivity', () => {
    beforeEach(() => {
        vi.spyOn(AuthService, 'refresh').mockResolvedValue(undefined);
        vi.spyOn(AuthService, 'isAuthenticated').mockReturnValue(true);
        vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible');
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('renews the session on input once the renewal interval has passed', () => {
        vi.spyOn(AuthService, 'getRefreshedAt').mockReturnValue(Date.now() - SESSION_RENEWAL_INTERVAL);

        renewSessionOnActivity();

        expect(AuthService.refresh).toHaveBeenCalledTimes(1);
    });

    it('renews a session whose last renewal is unknown', () => {
        vi.spyOn(AuthService, 'getRefreshedAt').mockReturnValue(null);

        renewSessionOnActivity();

        expect(AuthService.refresh).toHaveBeenCalledTimes(1);
    });

    it('does not renew within the renewal interval', () => {
        vi.spyOn(AuthService, 'getRefreshedAt').mockReturnValue(Date.now() - SESSION_RENEWAL_INTERVAL + 1000);

        renewSessionOnActivity();

        expect(AuthService.refresh).not.toHaveBeenCalled();
    });

    it('leaves the renewal to the tab in use', () => {
        vi.spyOn(AuthService, 'getRefreshedAt').mockReturnValue(null);
        vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('hidden');

        renewSessionOnActivity();

        expect(AuthService.refresh).not.toHaveBeenCalled();
    });

    it('does not try to renew an ended session', () => {
        vi.spyOn(AuthService, 'getRefreshedAt').mockReturnValue(null);
        vi.mocked(AuthService.isAuthenticated).mockReturnValue(false);

        renewSessionOnActivity();

        expect(AuthService.refresh).not.toHaveBeenCalled();
    });
});
