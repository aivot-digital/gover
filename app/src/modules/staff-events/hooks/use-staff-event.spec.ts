import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {renderHook} from '@testing-library/react';
import {useStaffEvent} from './use-staff-event';
import {type StaffEventListener, subscribeStaffEvents} from '../staff-event-source';

vi.mock('../staff-event-source', () => ({
    subscribeStaffEvents: vi.fn(),
}));

describe('useStaffEvent', () => {
    let emit: StaffEventListener;
    const unsubscribe = vi.fn();

    beforeEach(() => {
        vi.useFakeTimers();
        unsubscribe.mockClear();
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
