import {useEffect, useLayoutEffect, useRef} from 'react';
import {useGenericDetailsPageContext} from './generic-details-page-context';
import {type GenericDetailsPageEventMap, type GenericDetailsPageEventName} from './generic-details-page-events';

/**
 * Subscribes without replaying past events or requiring a memoized listener.
 * For custom payloads, specify both types: useGenericDetailsPageEvent<Events, 'changed'>('changed', listener).
 */
export function useGenericDetailsPageEvent<
    Events extends object = {},
    Name extends GenericDetailsPageEventName<Events> = 'refresh',
>(
    name: Name,
    listener: (payload: GenericDetailsPageEventMap<Events>[Name]) => void | Promise<void>,
): void {
    const {subscribeEvent} = useGenericDetailsPageContext<unknown, unknown, Events>();
    const listenerRef = useRef(listener);

    // Refresh is emitted after the context commit; update the callback before passive effects run.
    useLayoutEffect(() => {
        listenerRef.current = listener;
    });

    useEffect(() => {
        return subscribeEvent(name, payload => listenerRef.current(payload));
    }, [name, subscribeEvent]);
}
