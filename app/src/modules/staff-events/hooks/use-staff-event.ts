import {useEffect, useRef} from 'react';
import {subscribeStaffEvents} from '../staff-event-source';
import {AuthService} from '../../../services/auth-service';
import {isUserActive, onUserActivity} from '../user-activity';

// A burst of events, e.g. while a worker runs an instance, leads to at most one reload per interval.
const THROTTLE_INTERVAL = 1_000;

export interface UseStaffEventOptions {
    type: string;
    /** The ID of the resource. Without it, events for every resource of the type are handled. */
    scope?: string | number | null;
    enabled?: boolean;
    /** Reloads the view. It also runs after a lost connection is restored, because events may have been missed. */
    onEvent: () => void;
}

/**
 * Runs onEvent when the backend reports a matching event. A reload waits while the tab is hidden, and while it would
 * renew the session of a user who has been inactive, because a reload with an expired access token refreshes it.
 */
export function useStaffEvent({type, scope, enabled = true, onEvent}: UseStaffEventOptions): void {
    const onEventRef = useRef(onEvent);
    const normalizedScope = scope == null ? null : String(scope);

    useEffect(() => {
        onEventRef.current = onEvent;
    }, [onEvent]);

    useEffect(() => {
        if (!enabled) {
            return;
        }

        let timeout: number | undefined;
        let deferred = false;

        const canReload = () => document.visibilityState !== 'hidden'
            && AuthService.isAuthenticated()
            && (AuthService.isAccessTokenValid() || isUserActive());

        const schedule = () => {
            if (!canReload()) {
                deferred = true;
                return;
            }
            timeout ??= window.setTimeout(() => {
                timeout = undefined;
                // The tab may have been hidden or the token may have expired during the interval.
                if (!canReload()) {
                    deferred = true;
                    return;
                }
                onEventRef.current();
            }, THROTTLE_INTERVAL);
        };

        const resume = () => {
            if (deferred && canReload()) {
                deferred = false;
                schedule();
            }
        };

        const unsubscribe = subscribeStaffEvents((event) => {
            if (event == null || (event.type === type && (normalizedScope == null || event.scope === normalizedScope))) {
                schedule();
            }
        });
        const stopWaitingForUser = onUserActivity(resume);
        document.addEventListener('visibilitychange', resume);

        return () => {
            unsubscribe();
            stopWaitingForUser();
            window.clearTimeout(timeout);
            document.removeEventListener('visibilitychange', resume);
        };
    }, [enabled, type, normalizedScope]);
}
