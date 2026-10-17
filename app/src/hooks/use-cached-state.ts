import {useState} from 'react';

export function useCachedState<T>(localStorageKey: string, initial: T) {
    const [state, setState] = useState<T>(() => {
        const cached = localStorage.getItem(localStorageKey);
        return cached ? JSON.parse(cached) as T : initial;
    });

    const setCachedState = (newState: T) => {
        setState(newState);
        localStorage.setItem(localStorageKey, JSON.stringify(newState));
    };

    return [state, setCachedState] as const;
}