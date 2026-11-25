import {useCallback, useRef, useState} from 'react';

/** An async editor operation must see edits even if React is still batching their render. */
export function useCurrentState<T>(initial: T) {
    const [value, setValue] = useState(() => initial);
    const current = useRef(initial);
    const update = useCallback((next: T) => {
        current.current = next;
        setValue(() => next);
    }, []);
    return [value, update, current] as const;
}
