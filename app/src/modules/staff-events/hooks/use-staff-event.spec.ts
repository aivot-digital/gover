import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {renderHook} from '@testing-library/react';
import {useStaffEvent} from './use-staff-event';
import {type StaffEventListener, subscribeStaffEvents} from '../staff-event-source';
import {AuthService} from '../../../services/auth-service';

const activity = vi.hoisted(() => ({active: true, listeners: new Set<() => void>()}));

vi.mock('../staff-event-source', () => ({
    subscribeStaffEvents: vi.fn(),
}));
vi.mock('../../../services/auth-service', () => ({
    AuthService: {
        isAuthenticated: vi.fn(() => true),
        isAccessTokenValid: vi.fn(() => true),
    },
}));
vi.mock('../user-activity', () => ({
    isUserActive: () => activity.active,
    onUserActivity: (listener: () => void) => {
        activity.listeners.add(listener);
        return () => activity.listeners.delete(listener);
    },
}));

describe('useStaffEvent', () => {
    let emit: StaffEventListener;
    const unsubscribe = vi.fn();

    beforeEach(() => {
        vi.useFakeTimers();
        unsubscribe.mockClear();
        activity.active = true;
        vi.mocked(AuthService.isAccessTokenValid).mockReturnValue(true);
        vi.mocked(subscribeStaffEvents).mockReset().mockImplementation((listener) => {
            emit = listener;
            return unsubscribe;
        });
    });

    afterEach(() => {
        vi.useRealTimers();
        setVisibility('visible');
    });

    it('reloads once for a burst of events about the resource', () => {
        const onEvent = vi.fn();
        renderHook(() => useStaffEvent({type: 'process-instance', scope: 7, onEvent}));

        emit({type: 'process-instance', scope: '7', userId: null});
        emit({type: 'process-instance', scope: '7', userId: null});
        emit({type: 'process-instance', scope: '8', userId: null});
        emit({type: 'notification', scope: '7', userId: null});
        vi.advanceTimersByTime(1_000);

        expect(onEvent).toHaveBeenCalledOnce();
    });

    it('ignores events about other resources', () => {
        const onEvent = vi.fn();
        renderHook(() => useStaffEvent({type: 'process-instance', scope: 7, onEvent}));

        emit({type: 'process-instance', scope: '8', userId: null});
        vi.advanceTimersByTime(1_000);

        expect(onEvent).not.toHaveBeenCalled();
    });

    it('reloads for every resource of the type without a scope and after a restored connection', () => {
        const onEvent = vi.fn();
        renderHook(() => useStaffEvent({type: 'process-instance', onEvent}));

        emit({type: 'process-instance', scope: '8', userId: null});
        vi.advanceTimersByTime(1_000);
        emit(null);
        vi.advanceTimersByTime(1_000);

        expect(onEvent).toHaveBeenCalledTimes(2);
    });

    it('defers the reload while the tab is hidden', () => {
        const onEvent = vi.fn();
        renderHook(() => useStaffEvent({type: 'process-instance', scope: 7, onEvent}));

        setVisibility('hidden');
        emit({type: 'process-instance', scope: '7', userId: null});
        vi.advanceTimersByTime(1_000);
        expect(onEvent).not.toHaveBeenCalled();

        setVisibility('visible');
        vi.advanceTimersByTime(1_000);
        expect(onEvent).toHaveBeenCalledOnce();
    });

    it('defers the reload when the tab is hidden before it runs', () => {
        const onEvent = vi.fn();
        renderHook(() => useStaffEvent({type: 'process-instance', scope: 7, onEvent}));

        emit({type: 'process-instance', scope: '7', userId: null});
        setVisibility('hidden');
        vi.advanceTimersByTime(1_000);
        expect(onEvent).not.toHaveBeenCalled();

        setVisibility('visible');
        vi.advanceTimersByTime(1_000);
        expect(onEvent).toHaveBeenCalledOnce();
    });

    it('waits for the next input before a reload would renew the session of an inactive user', () => {
        const onEvent = vi.fn();
        renderHook(() => useStaffEvent({type: 'process-instance', scope: 7, onEvent}));
        vi.mocked(AuthService.isAccessTokenValid).mockReturnValue(false);
        activity.active = false;

        emit({type: 'process-instance', scope: '7', userId: null});
        vi.advanceTimersByTime(1_000);
        expect(onEvent).not.toHaveBeenCalled();

        activity.active = true;
        [...activity.listeners].forEach((listener) => listener());
        vi.advanceTimersByTime(1_000);
        expect(onEvent).toHaveBeenCalledOnce();
    });

    it('subscribes only while enabled and mounted', () => {
        const {rerender, unmount} = renderHook(
            ({enabled}) => useStaffEvent({type: 'process-instance', enabled, onEvent: vi.fn()}),
            {initialProps: {enabled: false}},
        );
        expect(subscribeStaffEvents).not.toHaveBeenCalled();

        rerender({enabled: true});
        expect(subscribeStaffEvents).toHaveBeenCalledOnce();

        unmount();
        expect(unsubscribe).toHaveBeenCalledOnce();
    });
});

function setVisibility(visibilityState: DocumentVisibilityState) {
    Object.defineProperty(document, 'visibilityState', {value: visibilityState, configurable: true});
    document.dispatchEvent(new Event('visibilitychange'));
}
