import {useEffect} from 'react';
import {AuthService} from '../../../services/auth-service';
import {onUserActivity} from '../../../modules/staff-events/user-activity';

/**
 * How often input renews the session at most. The session thus ends about its idle timeout after the last input, also
 * while the person works without requests to the server, e.g. while building a form in the form editor.
 */
export const SESSION_RENEWAL_INTERVAL = 60_000;

/** Renews the session on input in the visible tab, unless it was renewed within the renewal interval. */
export function renewSessionOnActivity(): void {
    // Only the tab in use renews, so open background tabs do not renew the session at the same time.
    if (document.visibilityState !== 'visible' || !AuthService.isAuthenticated()) {
        return;
    }

    const refreshedAt = AuthService.getRefreshedAt();
    if (refreshedAt != null && Date.now() - refreshedAt < SESSION_RENEWAL_INTERVAL) {
        return;
    }

    // A failed renewal is handled by the AuthService: an ended session shows the expired session dialog.
    AuthService.refresh().catch(() => undefined);
}

export function ShellSessionKeepAlive(): null {
    useEffect(() => onUserActivity(renewSessionOnActivity), []);
    return null;
}
