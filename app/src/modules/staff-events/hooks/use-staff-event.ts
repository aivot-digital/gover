import {useEffect, useRef} from 'react';
import {subscribeStaffEvents} from '../staff-event-source';

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

/** Runs onEvent when the backend reports a matching event. Events that arrive while the tab is hidden run once it is visible. */
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
        let missedWhileHidden = false;

        const schedule = () => {
            if (document.visibilityState === 'hidden') {
                missedWhileHidden = true;
                return;
            }
            timeout ??= window.setTimeout(() => {
                timeout = undefined;
                onEventRef.current();
            }, THROTTLE_INTERVAL);
        };

        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible' && missedWhileHidden) {
                missedWhileHidden = false;
                schedule();
            }
        };

        const unsubscribe = subscribeStaffEvents((event) => {
            if (event == null || (event.type === type && (normalizedScope == null || event.scope === normalizedScope))) {
                schedule();
            }
        });
        document.addEventListener('visibilitychange', handleVisibilityChange);

        return () => {
            unsubscribe();
            window.clearTimeout(timeout);
            document.removeEventListener('visibilitychange', handleVisibilityChange);
        };
    }, [enabled, type, normalizedScope]);
}
