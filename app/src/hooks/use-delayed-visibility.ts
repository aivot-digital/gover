import {useEffect, useState} from 'react';

export function useDelayedVisibility(isVisible: boolean, delay: number): boolean {
    const [show, setShow] = useState(false);

    useEffect(() => {
        if (!isVisible) {
            setShow(false);
            return;
        }

        const timeoutId = window.setTimeout(() => {
            setShow(true);
        }, delay);

        return () => {
            window.clearTimeout(timeoutId);
        };
    }, [delay, isVisible]);

    return show;
}
