import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {AuthService} from '../../services/auth-service';
import type {StaffEvent} from './staff-event-source';

const activity = vi.hoisted(() => ({active: true, listeners: new Set<() => void>()}));

vi.mock('../../services/auth-service', () => ({
    AUTH_EXPIRATION_LOCAL_STORAGE_KEY: 'auth_expiration',
    AuthService: {
        isAuthenticated: vi.fn(() => true),
        isAccessTokenValid: vi.fn(() => true),
        refresh: vi.fn(() => Promise.resolve()),
    },
}));
vi.mock('./user-activity', () => ({
    isUserActive: () => activity.active,
    onUserActivity: (listener: () => void) => {
        activity.listeners.add(listener);
        return () => activity.listeners.delete(listener);
    },
}));

class FakeEventSource {
    static instances: FakeEventSource[] = [];
    onopen: (() => void) | null = null;
    onmessage: ((message: {data: string}) => void) | null = null;
    onerror: (() => void) | null = null;
    closed = false;

    constructor(public readonly url: string) {
        FakeEventSource.instances.push(this);
    }

    close() {
        this.closed = true;
    }
}

/** Grants the lock like navigator.locks, and lets the test hold it in place of another tab. */
function createLockManager() {
    let held = false;
    const waiting: Array<() => void> = [];
    const release = () => {
        held = false;
        waiting.shift()?.();
    };
    const run = (callback: (lock: unknown) => Promise<void>) => {
        held = true;
        return callback({}).finally(release);
    };

    return {
        request: (_name: string, options: {ifAvailable?: boolean; signal?: AbortSignal}, callback: (lock: unknown) => Promise<void>) => {
            if (!held) {
                return run(callback);
            }
            if (options.ifAvailable) {
                return callback(null);
            }
            return new Promise<void>((resolve, reject) => {
                options.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
                waiting.push(() => run(callback).then(resolve, reject));
            });
        },
        holdForOtherTab: () => {
            held = true;
            return release;
        },
    };
}

const event: StaffEvent = {type: 'process-instance', scope: '7', userId: null};

async function loadSource() {
    vi.resetModules();
    return await import('./staff-event-source');
}

describe('subscribeStaffEvents', () => {
    beforeEach(() => {
        FakeEventSource.instances = [];
        vi.stubGlobal('EventSource', FakeEventSource);
        vi.mocked(AuthService.isAuthenticated).mockReturnValue(true);
        vi.mocked(AuthService.isAccessTokenValid).mockReturnValue(true);
        vi.mocked(AuthService.refresh).mockClear();
        activity.active = true;
        activity.listeners.clear();
    });

    afterEach(() => {
        vi.useRealTimers();
        vi.unstubAllGlobals();
        Reflect.deleteProperty(navigator, 'locks');
    });

    it('opens one stream for all subscribers and closes it with the last one', async () => {
        const {subscribeStaffEvents} = await loadSource();
        const first = vi.fn();
        const second = vi.fn();

        const unsubscribeFirst = subscribeStaffEvents(first);
        const unsubscribeSecond = subscribeStaffEvents(second);

        expect(FakeEventSource.instances).toHaveLength(1);
        const source = FakeEventSource.instances[0];
        expect(source.url).toMatch(/\/api\/staff-events\/$/);

        source.onopen?.();
        source.onmessage?.({data: JSON.stringify(event)});
        expect(first.mock.calls).toEqual([[event]]);
        expect(second.mock.calls).toEqual([[event]]);

        unsubscribeFirst();
        expect(source.closed).toBe(false);
        unsubscribeSecond();
        expect(source.closed).toBe(true);
    });

    it('reconnects with a fresh access token and reports that events may have been missed', async () => {
        vi.useFakeTimers();
        const {subscribeStaffEvents} = await loadSource();
        const listener = vi.fn();
        const unsubscribe = subscribeStaffEvents(listener);
        const first = FakeEventSource.instances[0];
        first.onopen?.();

        // The stream ended after the access token had expired.
        vi.mocked(AuthService.isAccessTokenValid).mockReturnValue(false);
        first.onerror?.();
        expect(first.closed).toBe(true);
        await vi.advanceTimersByTimeAsync(3_000);

        expect(AuthService.refresh).toHaveBeenCalledOnce();
        expect(FakeEventSource.instances).toHaveLength(2);
        FakeEventSource.instances[1].onopen?.();
        expect(listener.mock.calls).toEqual([[null]]);

        unsubscribe();
    });

    it('waits for the next input before it renews the session of an inactive user', async () => {
        vi.useFakeTimers();
        const {subscribeStaffEvents} = await loadSource();
        const listener = vi.fn();
        const unsubscribe = subscribeStaffEvents(listener);
        FakeEventSource.instances[0].onopen?.();

        vi.mocked(AuthService.isAccessTokenValid).mockReturnValue(false);
        activity.active = false;
        FakeEventSource.instances[0].onerror?.();
        await vi.advanceTimersByTimeAsync(60_000);
        expect(AuthService.refresh).not.toHaveBeenCalled();
        expect(FakeEventSource.instances).toHaveLength(1);

        activity.active = true;
        [...activity.listeners].forEach((activityListener) => activityListener());
        await vi.waitFor(() => expect(FakeEventSource.instances).toHaveLength(2));
        expect(AuthService.refresh).toHaveBeenCalledOnce();
        FakeEventSource.instances[1].onopen?.();
        expect(listener.mock.calls).toEqual([[null]]);

        unsubscribe();
        expect(activity.listeners.size).toBe(0);
    });

    it('stops reconnecting once the session has ended', async () => {
        vi.useFakeTimers();
        const {subscribeStaffEvents} = await loadSource();
        const unsubscribe = subscribeStaffEvents(vi.fn());

        vi.mocked(AuthService.isAccessTokenValid).mockReturnValue(false);
        vi.mocked(AuthService.isAuthenticated).mockReturnValue(false);
        FakeEventSource.instances[0].onerror?.();
        await vi.advanceTimersByTimeAsync(60_000);
        [...activity.listeners].forEach((activityListener) => activityListener());
        await vi.advanceTimersByTimeAsync(60_000);

        expect(AuthService.refresh).not.toHaveBeenCalled();
        expect(FakeEventSource.instances).toHaveLength(1);
        unsubscribe();
    });

    it('ends the stream with the session and opens a new one for the next session', async () => {
        vi.useFakeTimers();
        const {subscribeStaffEvents} = await loadSource();
        const listener = vi.fn();
        const unsubscribe = subscribeStaffEvents(listener);
        const previousUser = FakeEventSource.instances[0];
        previousUser.onopen?.();

        // The user logs out in another tab.
        vi.mocked(AuthService.isAuthenticated).mockReturnValue(false);
        vi.mocked(AuthService.isAccessTokenValid).mockReturnValue(false);
        await vi.advanceTimersByTimeAsync(5_000);
        expect(previousUser.closed).toBe(true);
        await vi.advanceTimersByTimeAsync(60_000);
        expect(FakeEventSource.instances).toHaveLength(1);

        // Another user signs in.
        vi.mocked(AuthService.isAuthenticated).mockReturnValue(true);
        vi.mocked(AuthService.isAccessTokenValid).mockReturnValue(true);
        [...activity.listeners].forEach((activityListener) => activityListener());
        await vi.waitFor(() => expect(FakeEventSource.instances).toHaveLength(2));
        FakeEventSource.instances[1].onopen?.();
        expect(listener.mock.calls).toEqual([[null]]);

        unsubscribe();
        expect(FakeEventSource.instances[1].closed).toBe(true);
    });

    it('ends the stream at once when another tab logs out, even if another user signs in before the next check', async () => {
        vi.useFakeTimers();
        const {subscribeStaffEvents} = await loadSource();
        const listener = vi.fn();
        const unsubscribe = subscribeStaffEvents(listener);
        const previousUser = FakeEventSource.instances[0];
        previousUser.onopen?.();

        // The user logs out in another tab, and another user signs in within the same check interval.
        await vi.advanceTimersByTimeAsync(1_000);
        window.dispatchEvent(new StorageEvent('storage', {key: 'auth_expiration', newValue: null}));
        await vi.advanceTimersByTimeAsync(1_000);
        await vi.advanceTimersByTimeAsync(10_000);

        expect(previousUser.closed).toBe(true);
        expect(FakeEventSource.instances).toHaveLength(2);
        FakeEventSource.instances[1].onopen?.();
        expect(listener.mock.calls).toEqual([[null]]);

        // A token refresh in another tab keeps the stream.
        window.dispatchEvent(new StorageEvent('storage', {key: 'auth_expiration', newValue: '{}'}));
        expect(FakeEventSource.instances[1].closed).toBe(false);

        unsubscribe();
        expect(FakeEventSource.instances[1].closed).toBe(true);
    });

    it('lets a tab share the stream of the tab that holds the lock and take it over once that tab closes', async () => {
        const locks = createLockManager();
        Object.defineProperty(navigator, 'locks', {value: locks, configurable: true});
        const releaseOtherTab = locks.holdForOtherTab();
        const otherTab = new BroadcastChannel('prosuna_staff_events');
        const relayedToOtherTab = vi.fn();
        otherTab.onmessage = (message) => relayedToOtherTab(message.data);
        const {subscribeStaffEvents} = await loadSource();
        const listener = vi.fn();

        const unsubscribe = subscribeStaffEvents(listener);
        await vi.waitFor(() => expect(FakeEventSource.instances).toHaveLength(0));
        otherTab.postMessage(event);
        await vi.waitFor(() => expect(listener.mock.calls).toEqual([[event]]));

        releaseOtherTab();
        await vi.waitFor(() => expect(FakeEventSource.instances).toHaveLength(1));
        FakeEventSource.instances[0].onopen?.();
        expect(listener.mock.calls).toEqual([[event], [null]]);
        FakeEventSource.instances[0].onmessage?.({data: JSON.stringify(event)});
        await vi.waitFor(() => expect(relayedToOtherTab.mock.calls).toEqual([[null], [event]]));

        unsubscribe();
        expect(FakeEventSource.instances[0].closed).toBe(true);
        otherTab.close();
    });
});
