// Background work may renew the session only this long after the last input, so an unattended session still ends.
export const USER_ACTIVITY_WINDOW = 5 * 60_000;

// Shared through the local storage, because the tab that holds the event stream is not always the one in use.
const LAST_ACTIVITY_STORAGE_KEY = 'staff_last_user_activity';
const RECORD_INTERVAL = 10_000;
const INPUT_EVENTS = ['pointerdown', 'pointermove', 'keydown', 'wheel', 'touchstart'];

const listeners = new Set<() => void>();
// Opening or reloading the page is input as well.
let lastActivity = Date.now();
let isTracking = false;

/** Whether the user worked with Prosuna in any tab within the activity window. */
export function isUserActive(): boolean {
    startTracking();
    return Date.now() - Math.max(lastActivity, readStoredActivity()) < USER_ACTIVITY_WINDOW;
}

/** Calls the listener on input in any tab, at most once per record interval and tab. */
export function onUserActivity(listener: () => void): () => void {
    startTracking();
    listeners.add(listener);
    return () => listeners.delete(listener);
}

function startTracking(): void {
    if (isTracking) {
        return;
    }
    isTracking = true;

    INPUT_EVENTS.forEach((type) => window.addEventListener(type, recordActivity, {capture: true, passive: true}));
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            recordActivity();
        }
    });
    window.addEventListener('storage', (event) => {
        if (event.key === LAST_ACTIVITY_STORAGE_KEY) {
            notify();
        }
    });
}

function recordActivity(): void {
    const now = Date.now();
    if (now - lastActivity < RECORD_INTERVAL) {
        return;
    }

    lastActivity = now;
    try {
        localStorage.setItem(LAST_ACTIVITY_STORAGE_KEY, String(now));
    } catch {
        // Without the storage, only this tab knows about its input.
    }
    notify();
}

function readStoredActivity(): number {
    try {
        return Number(localStorage.getItem(LAST_ACTIVITY_STORAGE_KEY)) || 0;
    } catch {
        return 0;
    }
}

function notify(): void {
    // A copy, because a listener may subscribe again, e.g. a reconnect that still has to wait.
    [...listeners].forEach((listener) => listener());
}
