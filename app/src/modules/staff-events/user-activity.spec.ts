import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

async function loadUserActivity() {
    vi.resetModules();
    return await import('./user-activity');
}

describe('user activity', () => {
    beforeEach(() => {
        vi.useFakeTimers({now: new Date('2026-11-02T10:00:00Z')});
    });

    afterEach(() => {
        vi.useRealTimers();
        localStorage.clear();
    });

    it('counts the page load as input and ends the activity after the window without input', async () => {
        const {isUserActive, USER_ACTIVITY_WINDOW} = await loadUserActivity();
        expect(isUserActive()).toBe(true);

        vi.advanceTimersByTime(USER_ACTIVITY_WINDOW);
        expect(isUserActive()).toBe(false);
    });

    it('notifies the listeners when an inactive user returns', async () => {
        const {isUserActive, onUserActivity, USER_ACTIVITY_WINDOW} = await loadUserActivity();
        const listener = vi.fn();
        const unsubscribe = onUserActivity(listener);
        vi.advanceTimersByTime(USER_ACTIVITY_WINDOW);

        window.dispatchEvent(new Event('pointermove'));
        window.dispatchEvent(new Event('keydown'));

        expect(isUserActive()).toBe(true);
        expect(listener).toHaveBeenCalledOnce();
        unsubscribe();
        vi.advanceTimersByTime(USER_ACTIVITY_WINDOW);
        window.dispatchEvent(new Event('pointerdown'));
        expect(listener).toHaveBeenCalledOnce();
    });

    it('counts input in another tab', async () => {
        const {isUserActive, onUserActivity, USER_ACTIVITY_WINDOW} = await loadUserActivity();
        const listener = vi.fn();
        onUserActivity(listener);
        vi.advanceTimersByTime(USER_ACTIVITY_WINDOW);
        expect(isUserActive()).toBe(false);

        localStorage.setItem('staff_last_user_activity', String(Date.now()));
        window.dispatchEvent(new StorageEvent('storage', {key: 'staff_last_user_activity'}));

        expect(isUserActive()).toBe(true);
        expect(listener).toHaveBeenCalledOnce();
    });
});
