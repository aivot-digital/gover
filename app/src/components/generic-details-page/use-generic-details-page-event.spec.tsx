import {StrictMode} from 'react';
import {act, render} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {GenericDetailsPageProvider} from './generic-details-page-context';
import {createGenericDetailsPageEventChannel} from './generic-details-page-events';
import {useGenericDetailsPageEvent} from './use-generic-details-page-event';

interface Events {
    changed: {value: number};
}

describe('useGenericDetailsPageEvent', () => {
    it('keeps one active subscription in StrictMode and uses the latest callback without resubscribing', () => {
        const channel = createGenericDetailsPageEventChannel<Events>();
        const subscribeEvent = vi.spyOn(channel, 'subscribeEvent');
        const listener = vi.fn();
        function Subscriber({label}: {label: string}) {
            useGenericDetailsPageEvent<Events, 'changed'>('changed', payload => {
                listener(`${label}:${payload.value}`);
            });
            return null;
        }
        const value = {
            setItem: vi.fn(), setAdditionalData: vi.fn(), isBusy: false,
            setIsBusy: vi.fn(), refresh: vi.fn(), isEditable: true, subscribeEvent: channel.subscribeEvent,
        };
        const tree = (label: string) => (
            <StrictMode>
                <GenericDetailsPageProvider value={value}>
                    <Subscriber label={label} />
                </GenericDetailsPageProvider>
            </StrictMode>
        );

        channel.emitEvent('changed', {value: 0});
        const rendered = render(tree('first'));
        expect(listener).not.toHaveBeenCalled();
        act(() => channel.emitEvent('changed', {value: 1}));
        expect(listener.mock.calls).toEqual([['first:1']]);
        const subscriptionCount = subscribeEvent.mock.calls.length;

        rendered.rerender(tree('second'));
        expect(subscribeEvent).toHaveBeenCalledTimes(subscriptionCount);
        act(() => channel.emitEvent('changed', {value: 2}));
        expect(listener.mock.calls).toEqual([['first:1'], ['second:2']]);

        rendered.unmount();
        channel.emitEvent('changed', {value: 3});
        expect(listener).toHaveBeenCalledTimes(2);
    });

    it('replaces subscriptions when the provider changes', () => {
        const first = createGenericDetailsPageEventChannel();
        const second = createGenericDetailsPageEventChannel();
        const listener = vi.fn();
        function Subscriber() {
            useGenericDetailsPageEvent('refresh', listener);
            return null;
        }
        const tree = (channel: typeof first) => (
            <GenericDetailsPageProvider value={{
                setItem: vi.fn(), setAdditionalData: vi.fn(), isBusy: false,
                setIsBusy: vi.fn(), refresh: vi.fn(), isEditable: true,
                subscribeEvent: channel.subscribeEvent,
            }}>
                <Subscriber />
            </GenericDetailsPageProvider>
        );
        const rendered = render(tree(first));
        rendered.rerender(tree(second));
        first.emitEvent('refresh');
        second.emitEvent('refresh');
        expect(listener).toHaveBeenCalledTimes(1);
    });

    it('replaces subscriptions when the event name changes', () => {
        const channel = createGenericDetailsPageEventChannel<{ping: undefined}>();
        const listener = vi.fn();
        function Subscriber({name}: {name: 'refresh' | 'ping'}) {
            useGenericDetailsPageEvent<{ping: undefined}, 'refresh' | 'ping'>(name, listener);
            return null;
        }
        const tree = (name: 'refresh' | 'ping') => (
            <GenericDetailsPageProvider value={{
                setItem: vi.fn(), setAdditionalData: vi.fn(), isBusy: false,
                setIsBusy: vi.fn(), refresh: vi.fn(), isEditable: true,
                subscribeEvent: channel.subscribeEvent,
            }}>
                <Subscriber name={name} />
            </GenericDetailsPageProvider>
        );
        const rendered = render(tree('refresh'));
        rendered.rerender(tree('ping'));
        channel.emitEvent('refresh');
        expect(listener).not.toHaveBeenCalled();
        channel.emitEvent('ping');
        expect(listener).toHaveBeenCalledTimes(1);
    });
});
