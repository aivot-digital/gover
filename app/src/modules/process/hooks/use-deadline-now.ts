import {useEffect, useReducer} from 'react';

/** Refresh visible deadlines locally; advancing the clock does not require another API request. */
export function useDeadlineNow(active: boolean): number {
    const [, refresh] = useReducer((value: number) => value + 1, 0);
    useEffect(() => {
        if (!active) return;
        const interval = window.setInterval(refresh, 60_000);
        return () => window.clearInterval(interval);
    }, [active]);
    return Date.now();
}
