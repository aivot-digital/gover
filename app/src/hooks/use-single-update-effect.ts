import {useEffect, useRef} from 'react';

/**
 * This hook is used to run an effect only after the first render and on updates.
 * @param func The effect to run after the first render and on updated.
 * @param deps The dependencies of the effect.
 */
export function useSingleUpdateEffect(func: () => void, deps: any[]) {
    const didMount = useRef(false);

    useEffect(() => {
        if (!didMount.current) {
            didMount.current = true;
            return func();
        }
    }, deps);
}