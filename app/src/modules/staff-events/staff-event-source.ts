import {AUTH_EXPIRATION_LOCAL_STORAGE_KEY, AuthService} from '../../services/auth-service';
import {createApiPath} from '../../utils/url-path-utils';
import {isUserActive, onUserActivity} from './user-activity';

/** An event from the backend. It only names what changed; views load the current state through the regular API. */
export interface StaffEvent {
    type: string;
    scope: string | null;
    userId: string | null;
}

/** Receives each event, or null once a lost connection is restored, because events may have been missed meanwhile. */
export type StaffEventListener = (event: StaffEvent | null) => void;

const SHARED_CONNECTION_NAME = 'prosuna_staff_events';
const RECONNECT_DELAY = 3_000;
const SESSION_CHECK_INTERVAL = 5_000;

const listeners = new Set<StaffEventListener>();
let stopConnection: (() => void) | null = null;

/** Subscribes to the staff events. The first subscription opens the connection and the last one closes it. */
export function subscribeStaffEvents(listener: StaffEventListener): () => void {
    listeners.add(listener);
    stopConnection ??= startConnection();

    return () => {
        listeners.delete(listener);
        if (listeners.size === 0) {
            stopConnection?.();
            stopConnection = null;
        }
    };
}

function notify(event: StaffEvent | null): void {
    listeners.forEach((listener) => listener(event));
}

/**
 * Over HTTP/1.1, browsers open only six connections per server, so the tabs share one stream: the tab that holds the
 * lock keeps it open and relays its events to the other tabs.
 */
function startConnection(): () => void {
    if (typeof EventSource === 'undefined') {
        return () => undefined;
    }

    if (typeof navigator.locks === 'undefined' || typeof BroadcastChannel === 'undefined') {
        return connect(notify, false);
    }

    const channel = new BroadcastChannel(SHARED_CONNECTION_NAME);
    channel.onmessage = (message: MessageEvent<StaffEvent | null>) => notify(message.data);
    const relay: StaffEventListener = (event) => {
        notify(event);
        channel.postMessage(event);
    };

    const stopped = new AbortController();
    let stopStream: (() => void) | undefined;
    const lead = (missedEvents: boolean) => stopped.signal.aborted ? Promise.resolve() : new Promise<void>((resolve) => {
        stopStream = connect(relay, missedEvents);
        stopped.signal.addEventListener('abort', () => resolve());
    });

    // A tab that has to wait takes over from a tab that closed, so events may have been missed in between.
    navigator.locks
        .request(SHARED_CONNECTION_NAME, {ifAvailable: true}, (lock) => lock != null
            ? lead(false)
            : navigator.locks.request(SHARED_CONNECTION_NAME, {signal: stopped.signal}, () => lead(true)))
        // The waiting request is aborted when the tab stops listening.
        .catch(() => undefined);

    return () => {
        stopped.abort();
        stopStream?.();
        channel.close();
    };
}

function connect(onEvent: StaffEventListener, missedEvents: boolean): () => void {
    let source: EventSource | undefined;
    let reconnectTimeout: number | undefined;
    let stopWaitingForUser: (() => void) | undefined;
    let stopped = false;
    let missed = missedEvents;

    const open = async () => {
        // The stream outlives the access token, so a reconnect usually needs a fresh one.
        if (!AuthService.isAccessTokenValid()) {
            // A refresh renews the session, so it waits for the user: otherwise an open tab would keep an unattended
            // session alive. An ended session cannot be refreshed, and retrying it would only repeat failed requests.
            if (!AuthService.isAuthenticated() || !isUserActive()) {
                stopWaitingForUser = onUserActivity(() => {
                    stopWaitingForUser?.();
                    void open();
                });
                return;
            }
            await AuthService.refresh().catch(() => undefined);
        }
        if (stopped) {
            return;
        }

        source = new EventSource(createApiPath('/api/staff-events/'));
        source.onopen = () => {
            if (missed) {
                onEvent(null);
            }
            missed = false;
        };
        source.onmessage = (message: MessageEvent<string>) => onEvent(JSON.parse(message.data) as StaffEvent);
        source.onerror = () => {
            // Reconnect manually: the browser gives up after a rejected request, e.g. once the token has expired.
            source?.close();
            source = undefined;
            missed = true;
            reconnectTimeout = window.setTimeout(() => void open(), RECONNECT_DELAY);
        };
    };

    // The stream belongs to the user who opened it, and the other tabs receive its events. It ends with the session
    // in this browser, e.g. after a logout in another tab, so the next user who signs in gets a stream of their own.
    const endStream = () => {
        if (source == null) {
            return;
        }
        source.close();
        source = undefined;
        missed = true;
        void open();
    };
    // The interval notices sessions that expire without a logout.
    const sessionCheck = window.setInterval(() => {
        if (!AuthService.isAuthenticated()) {
            endStream();
        }
    }, SESSION_CHECK_INTERVAL);
    // A logout in another tab ends the stream at once, even if another user has signed in again before the next check.
    const onStorage = (event: StorageEvent) => {
        if (event.key == null || (event.key === AUTH_EXPIRATION_LOCAL_STORAGE_KEY && event.newValue == null)) {
            endStream();
        }
    };
    window.addEventListener('storage', onStorage);

    void open();

    return () => {
        stopped = true;
        window.clearTimeout(reconnectTimeout);
        window.clearInterval(sessionCheck);
        window.removeEventListener('storage', onStorage);
        stopWaitingForUser?.();
        source?.close();
    };
}
