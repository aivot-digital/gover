import {describe, expect, expectTypeOf, it, vi} from 'vitest';
import {createGenericDetailsPageEventChannel, type GenericDetailsPageEventMap} from './generic-details-page-events';

interface Events {
    changed: {value: number};
    ping: undefined;
}

describe('Generic details page event channel', () => {
    it('delivers repeated events with their payloads only to matching current listeners', () => {
        const channel = createGenericDetailsPageEventChannel<Events>();
        const changed = vi.fn();
        const refresh = vi.fn();
        channel.emitEvent('changed', {value: 0});
        channel.subscribeEvent('changed', changed);
        const unsubscribe = channel.subscribeEvent('refresh', refresh);

        channel.emitEvent('changed', {value: 1});
        channel.emitEvent('changed', {value: 1});
        channel.emitEvent('refresh');
        unsubscribe();
        unsubscribe();
        channel.emitEvent('refresh');

        expect(changed.mock.calls).toEqual([[{value: 1}], [{value: 1}]]);
        expect(refresh).toHaveBeenCalledTimes(1);
    });

    it('keeps subscriptions independent when they share a callback and when channels coexist', () => {
        const first = createGenericDetailsPageEventChannel();
        const second = createGenericDetailsPageEventChannel();
        const listener = vi.fn();
        const other = vi.fn();
        const unsubscribe = first.subscribeEvent('refresh', listener);
        first.subscribeEvent('refresh', listener);
        second.subscribeEvent('refresh', other);

        unsubscribe();
        first.emitEvent('refresh');
        expect(listener).toHaveBeenCalledTimes(1);
        expect(other).not.toHaveBeenCalled();
        second.emitEvent('refresh');
        expect(other).toHaveBeenCalledTimes(1);
    });

    it('does not deliver to listeners added or removed during the current dispatch', () => {
        const channel = createGenericDetailsPageEventChannel();
        const removed = vi.fn();
        const added = vi.fn();
        let unsubscribe = () => {};
        channel.subscribeEvent('refresh', () => {
            unsubscribe();
            channel.subscribeEvent('refresh', added);
        });
        unsubscribe = channel.subscribeEvent('refresh', removed);

        channel.emitEvent('refresh');
        expect(removed).not.toHaveBeenCalled();
        expect(added).not.toHaveBeenCalled();
    });

    it('isolates synchronous errors and rejected promises without stopping other listeners', async () => {
        const log = vi.spyOn(console, 'error').mockImplementation(() => {});
        const channel = createGenericDetailsPageEventChannel();
        const synchronousError = new Error('Synchronous listener failure');
        const asynchronousError = new Error('Asynchronous listener failure');
        const listener = vi.fn();
        channel.subscribeEvent('refresh', () => {throw synchronousError;});
        channel.subscribeEvent('refresh', async () => {throw asynchronousError;});
        channel.subscribeEvent('refresh', listener);

        expect(() => channel.emitEvent('refresh')).not.toThrow();
        await Promise.resolve();
        expect(listener).toHaveBeenCalledTimes(1);
        expect(log).toHaveBeenCalledWith(expect.any(String), synchronousError);
        expect(log).toHaveBeenCalledWith(expect.any(String), asynchronousError);
    });

    it('types event names, payloads and callbacks, including payload-free events', () => {
        const channel = createGenericDetailsPageEventChannel<Events>();
        channel.subscribeEvent('changed', payload => {
            expectTypeOf(payload).toEqualTypeOf<{value: number}>();
        });
        channel.subscribeEvent('refresh', payload => {
            expectTypeOf(payload).toEqualTypeOf<undefined>();
        });
        channel.emitEvent('changed', {value: 1});
        channel.emitEvent('ping');
        channel.emitEvent('refresh');
        expectTypeOf<GenericDetailsPageEventMap<Events>['changed']>().toEqualTypeOf<{value: number}>();

        // These compile-time checks run through npm run typecheck, without dispatching invalid events.
        if (false) {
            // @ts-expect-error Unknown event names are rejected.
            channel.emitEvent('missing');
            // @ts-expect-error Custom event payloads are required.
            channel.emitEvent('changed');
            // @ts-expect-error Payloads must match the event's definition.
            channel.emitEvent('changed', {value: 'wrong'});
            // @ts-expect-error Refresh has no payload.
            channel.emitEvent('refresh', {value: 1});
            // @ts-expect-error Subscriptions must use a known event name.
            channel.subscribeEvent('missing', () => {});
            // @ts-expect-error Callbacks must accept the event's payload type.
            channel.subscribeEvent('changed', (_payload: string) => {});
        }
    });
});
