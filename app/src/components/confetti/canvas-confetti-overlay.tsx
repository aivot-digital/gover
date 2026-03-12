import React, {useCallback, useEffect, useRef, useState} from 'react';
import confetti from 'canvas-confetti';

const defaultStartDelay = 200;
const defaultDuration = 2000;
const defaultIntervalMs = 16;
const staffShellConfettiContainerSelector = '[data-confetti-container="staff-shell-content"]';

interface CanvasBounds {
    top: number;
    left: number;
    width: number;
    height: number;
}

function resolveCanvasBounds(): CanvasBounds {
    const shellContentElement = document.querySelector<HTMLElement>(staffShellConfettiContainerSelector);
    if (shellContentElement == null) {
        return {
            top: 0,
            left: 0,
            width: window.innerWidth,
            height: window.innerHeight,
        };
    }

    const rect = shellContentElement.getBoundingClientRect();
    return {
        top: rect.top,
        left: rect.left,
        width: shellContentElement.clientWidth,
        height: shellContentElement.clientHeight,
    };
}

export interface CanvasConfettiOverlayProps {
    playKey: number | null;
    colors: string[];
    startDelay?: number;
    duration?: number;
}

export function CanvasConfettiOverlay(props: CanvasConfettiOverlayProps) {
    const {
        playKey,
        colors,
        startDelay = defaultStartDelay,
        duration = defaultDuration,
    } = props;

    const [isVisible, setIsVisible] = useState(false);
    const [canvasBounds, setCanvasBounds] = useState<CanvasBounds>(() => ({
        top: 0,
        left: 0,
        width: 0,
        height: 0,
    }));
    const canvasRef = useRef<HTMLCanvasElement | null>(null);
    const animationInstance = useRef<ReturnType<typeof confetti.create> | null>(null);
    const intervalId = useRef<ReturnType<typeof setInterval> | null>(null);
    const startTimeoutId = useRef<ReturnType<typeof setTimeout> | null>(null);
    const stopTimeoutId = useRef<ReturnType<typeof setTimeout> | null>(null);
    const lastBurstPromise = useRef<Promise<null[]> | null>(null);
    const runIdRef = useRef(0);

    const clearTimers = useCallback(() => {
        if (startTimeoutId.current) {
            clearTimeout(startTimeoutId.current);
            startTimeoutId.current = null;
        }
        if (stopTimeoutId.current) {
            clearTimeout(stopTimeoutId.current);
            stopTimeoutId.current = null;
        }
        if (intervalId.current) {
            clearInterval(intervalId.current);
            intervalId.current = null;
        }
    }, []);

    const hideCanvas = useCallback(() => {
        animationInstance.current = null;
        lastBurstPromise.current = null;
        setIsVisible(false);
    }, []);

    const stopAnimation = useCallback(() => {
        clearTimers();
        hideCanvas();
    }, [clearTimers, hideCanvas]);

    const pauseAnimation = useCallback(() => {
        if (intervalId.current) {
            clearInterval(intervalId.current);
            intervalId.current = null;
        }
    }, []);

    const nextTickAnimation = useCallback(() => {
        const leftBurst = animationInstance.current?.({
            particleCount: 2,
            startVelocity: 40,
            spread: 80,
            angle: 60,
            origin: {x: 0},
            colors,
            disableForReducedMotion: true,
        });
        const rightBurst = animationInstance.current?.({
            particleCount: 2,
            startVelocity: 40,
            spread: 80,
            angle: 120,
            origin: {x: 1},
            colors,
            disableForReducedMotion: true,
        });

        const bursts = [leftBurst, rightBurst].filter((value): value is Promise<null> => value != null);
        if (bursts.length > 0) {
            lastBurstPromise.current = Promise.all(bursts);
        }
    }, [colors]);

    const startAnimation = useCallback(() => {
        if (!animationInstance.current && canvasRef.current) {
            animationInstance.current = confetti.create(canvasRef.current, {
                resize: true,
                useWorker: true,
            });
        }

        if (!animationInstance.current || intervalId.current) {
            return;
        }

        nextTickAnimation();
        intervalId.current = setInterval(nextTickAnimation, defaultIntervalMs);
    }, [nextTickAnimation]);

    useEffect(() => {
        if (!isVisible) {
            if (intervalId.current) {
                clearInterval(intervalId.current);
                intervalId.current = null;
            }
            animationInstance.current = null;
            return;
        }

        if (canvasRef.current && !animationInstance.current) {
            animationInstance.current = confetti.create(canvasRef.current, {
                resize: true,
                useWorker: true,
            });
        }
    }, [isVisible]);

    useEffect(() => {
        if (!isVisible) {
            return;
        }

        const updateBounds = () => {
            setCanvasBounds(resolveCanvasBounds());
        };

        updateBounds();

        const shellContentElement = document.querySelector<HTMLElement>(staffShellConfettiContainerSelector);
        const resizeObserver = typeof ResizeObserver !== 'undefined'
            ? new ResizeObserver(() => {
                updateBounds();
            })
            : null;

        if (shellContentElement != null) {
            resizeObserver?.observe(shellContentElement);
        }

        window.addEventListener('resize', updateBounds);

        return () => {
            window.removeEventListener('resize', updateBounds);
            resizeObserver?.disconnect();
        };
    }, [isVisible]);

    useEffect(() => {
        if (playKey == null) {
            stopAnimation();
            return;
        }

        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            return;
        }

        stopAnimation();
        setIsVisible(true);
        runIdRef.current += 1;
        const currentRunId = runIdRef.current;

        startTimeoutId.current = setTimeout(() => {
            startAnimation();
        }, startDelay);
        stopTimeoutId.current = setTimeout(() => {
            pauseAnimation();

            const finalBurstPromise = lastBurstPromise.current;
            if (finalBurstPromise == null) {
                if (runIdRef.current === currentRunId) {
                    hideCanvas();
                }
                return;
            }

            void finalBurstPromise.finally(() => {
                if (runIdRef.current === currentRunId) {
                    hideCanvas();
                }
            });
        }, startDelay + duration);

        return () => {
            stopAnimation();
        };
    }, [duration, hideCanvas, pauseAnimation, playKey, startAnimation, startDelay, stopAnimation]);

    useEffect(() => {
        return () => {
            stopAnimation();
        };
    }, [stopAnimation]);

    if (!isVisible) {
        return null;
    }

    return (
        <canvas
            ref={canvasRef}
            style={{
                position: 'fixed',
                pointerEvents: 'none',
                width: `${canvasBounds.width}px`,
                height: `${canvasBounds.height}px`,
                top: `${canvasBounds.top}px`,
                left: `${canvasBounds.left}px`,
                zIndex: 9999,
                display: 'block',
                background: 'transparent',
            }}
            aria-hidden="true"
        />
    );
}
