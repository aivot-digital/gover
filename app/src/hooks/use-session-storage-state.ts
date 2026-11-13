import {type Dispatch, type SetStateAction, useCallback, useEffect, useState} from 'react';
import {StorageScope, StorageService} from '../services/storage-service';

/**
 * State that survives remounts within the same browser tab, e.g. when navigating to another page and back.
 * The state is reloaded from the session storage whenever the key changes.
 */
export function useSessionStorageState<T>(key: string, initialState: T): [T, Dispatch<SetStateAction<T>>] {
    const [entry, setEntry] = useState(() => ({
        key,
        value: loadSessionStorageState(key, initialState),
    }));

    // Keep the key next to the value, so a value is never written to the storage slot of another key.
    let current = entry;
    if (entry.key !== key) {
        current = {
            key,
            value: loadSessionStorageState(key, initialState),
        };
        setEntry(current);
    }

    useEffect(() => {
        try {
            StorageService.storeObject_unsafe(entry.key, entry.value, StorageScope.Session);
        } catch (error) {
            // The storage can be unavailable or full. The state keeps working in memory.
            console.warn(error);
        }
    }, [entry]);

    const setValue: Dispatch<SetStateAction<T>> = useCallback((action) => {
        setEntry((previous) => {
            // Drop updates of a previous key, e.g. from a request that resolves after the key changed.
            if (previous.key !== key) {
                return previous;
            }

            return {
                key,
                value: typeof action === 'function' ?
                    (action as (previousValue: T) => T)(previous.value) :
                    action,
            };
        });
    }, [key]);

    return [current.value, setValue];
}

function loadSessionStorageState<T>(key: string, initialState: T): T {
    try {
        return StorageService.loadObject_unsafe<T>(key) ?? initialState;
    } catch (error) {
        console.warn(error);
        return initialState;
    }
}
