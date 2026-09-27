import {useCallback, useLayoutEffect, useRef, useState} from 'react';

type Size = { width: number; height: number };

export function useElementSize<T extends HTMLElement>() {
    const ref = useRef<T | null>(null);
    const [size, setSize] = useState<Size>({ width: 0, height: 0 });
    const rafId = useRef<number | null>(null);

    const measure = useCallback(() => {
        if (!ref.current) return;
        const rect = ref.current.getBoundingClientRect();
        setSize({ width: Math.max(0, Math.round(rect.width)), height: Math.max(0, Math.round(rect.height)) });
    }, []);

    // Measure initially before painting so that there is no 0 value on first render
    useLayoutEffect(() => {
        measure();
    }, [measure]);

    useLayoutEffect(() => {
        if (!ref.current) return;

        const el = ref.current;

        const ro = new ResizeObserver(() => {
            if (rafId.current != null) cancelAnimationFrame(rafId.current);
            rafId.current = requestAnimationFrame(measure);
        });

        ro.observe(el);

        // Fallback: only if the viewport size changes
        const onWinResize = () => {
            if (rafId.current != null) cancelAnimationFrame(rafId.current);
            rafId.current = requestAnimationFrame(measure);
        };
        window.addEventListener('resize', onWinResize);

        // iOS / Mobile: VisualViewport resize (keyboard, safe are, etc.)
        const vv = (window as any).visualViewport as VisualViewport | undefined;
        if (vv) vv.addEventListener('resize', onWinResize);

        // Repeat initial measurement after mounting (if fonts/CSS are reloaded)
        if (rafId.current != null) cancelAnimationFrame(rafId.current);
        rafId.current = requestAnimationFrame(measure);

        return () => {
            ro.disconnect();
            window.removeEventListener('resize', onWinResize);
            if (vv) vv.removeEventListener('resize', onWinResize);
            if (rafId.current != null) cancelAnimationFrame(rafId.current);
        };
    }, [measure]);

    return { ref, size }; // size.height, size.width
}
