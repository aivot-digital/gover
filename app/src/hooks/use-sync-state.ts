import {Dispatch, RefObject, SetStateAction, useCallback, useEffect, useRef, useState} from 'react';

export function useSyncState<S>(key: string, initialState: S | (() => S)): [S, Dispatch<SetStateAction<S>>] {
    const [value, internalSetter] = useState<S>(initialState);

    const broadcastChannelRef: RefObject<BroadcastChannel | null> = useRef<BroadcastChannel | null>(null);

    useEffect(() => {
        if (broadcastChannelRef.current != null) {
            broadcastChannelRef.current.close();
            broadcastChannelRef.current = null;
        }

        broadcastChannelRef.current = new BroadcastChannel('sync_state__' + key);

        broadcastChannelRef.current.onmessage = (event: MessageEvent<S>) => {
            internalSetter(event.data);
        };

        return () => {
            if (broadcastChannelRef.current) {
                broadcastChannelRef.current.close();
                broadcastChannelRef.current = null;
            }
        };
    }, [key]);

    const setter: Dispatch<SetStateAction<S>> = useCallback((a: SetStateAction<S>) => {
        if (typeof a === 'function') {
            internalSetter((prev) => {
                const newValue = (a as (prevState: S) => S)(prev);
                if (broadcastChannelRef.current) {
                    broadcastChannelRef.current.postMessage(newValue);
                }
                return newValue;
            });
        } else {
            internalSetter(a);
            if (broadcastChannelRef.current) {
                broadcastChannelRef.current.postMessage(a);
            }
        }
    }, [internalSetter]);

    return [value, setter];
}