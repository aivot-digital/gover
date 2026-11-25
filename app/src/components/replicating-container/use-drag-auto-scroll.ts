import {type RefObject, useEffect} from 'react';
import {useDragLayer} from 'react-dnd';

// Distance from the visible top or bottom edge of the scroll container within which dragging scrolls it.
const EDGE_SIZE_PX = 72;
// Scroll distance per animation frame once the pointer reaches the edge or moves beyond it.
const MAX_STEP_PX = 20;

interface Bounds {
    top: number;
    bottom: number;
    left: number;
    right: number;
}

/**
 * Scrolls the nearest scroll container of the given element while an item of the given drag type is dragged near
 * or beyond its visible top or bottom edge. The browsers' own drag scrolling only reacts within a few pixels of the
 * edge, which is easily missed when toolbars or footers directly border the scroll container, and some platforms
 * do not scroll with the mouse wheel during a drag.
 */
export function useDragAutoScroll(elementRef: RefObject<HTMLElement | null>, dragType: string): void {
    const isActive = useDragLayer((monitor) => monitor.isDragging() && monitor.getItemType() === dragType);

    useEffect(() => {
        const element = elementRef.current;
        if (!isActive || element == null) {
            return;
        }

        const scrollContainer = findScrollContainer(element);
        let pointer: {x: number; y: number} | null = null;
        let frame = 0;

        // The HTML5 drag suppresses mouse events, so dragover is the only source of the pointer position.
        const handleDragOver = (event: DragEvent) => {
            pointer = {x: event.clientX, y: event.clientY};
        };
        // Stops scrolling once the pointer leaves the window, where no further dragover events report its position.
        const handleDragLeave = (event: DragEvent) => {
            if (event.relatedTarget == null) {
                pointer = null;
            }
        };

        const step = () => {
            if (pointer != null) {
                const delta = getAutoScrollDelta(pointer, getVisibleBounds(scrollContainer));
                if (delta !== 0) {
                    scrollContainer.scrollTop += delta;
                }
            }
            frame = requestAnimationFrame(step);
        };

        window.addEventListener('dragover', handleDragOver, true);
        window.addEventListener('dragleave', handleDragLeave, true);
        frame = requestAnimationFrame(step);

        return () => {
            window.removeEventListener('dragover', handleDragOver, true);
            window.removeEventListener('dragleave', handleDragLeave, true);
            cancelAnimationFrame(frame);
        };
    }, [elementRef, isActive]);
}

/**
 * Returns the vertical scroll step for the pointer position. It grows towards the edge and stays at its maximum
 * beyond the edge, so overshooting onto a bordering toolbar or footer keeps scrolling.
 */
export function getAutoScrollDelta(pointer: {x: number; y: number}, bounds: Bounds): number {
    if (pointer.x < bounds.left || pointer.x > bounds.right) {
        return 0;
    }

    const edgeSize = Math.min(EDGE_SIZE_PX, (bounds.bottom - bounds.top) / 3);
    if (pointer.y < bounds.top + edgeSize) {
        return -Math.ceil(MAX_STEP_PX * Math.min(1, (bounds.top + edgeSize - pointer.y) / edgeSize));
    }
    if (pointer.y > bounds.bottom - edgeSize) {
        return Math.ceil(MAX_STEP_PX * Math.min(1, (pointer.y - bounds.bottom + edgeSize) / edgeSize));
    }
    return 0;
}

function findScrollContainer(element: HTMLElement): HTMLElement {
    for (let parent = element.parentElement; parent != null; parent = parent.parentElement) {
        const {overflowY} = getComputedStyle(parent);
        if ((overflowY === 'auto' || overflowY === 'scroll' || overflowY === 'overlay') && parent.scrollHeight > parent.clientHeight) {
            return parent;
        }
    }
    return (document.scrollingElement ?? document.documentElement) as HTMLElement;
}

function getVisibleBounds(scrollContainer: HTMLElement): Bounds {
    const viewport = {top: 0, bottom: window.innerHeight, left: 0, right: window.innerWidth};
    if (scrollContainer === document.scrollingElement || scrollContainer === document.documentElement) {
        return viewport;
    }

    // Only the part of the scroll container inside the viewport can be reached by the pointer.
    const rect = scrollContainer.getBoundingClientRect();
    return {
        top: Math.max(rect.top, viewport.top),
        bottom: Math.min(rect.bottom, viewport.bottom),
        left: Math.max(rect.left, viewport.left),
        right: Math.min(rect.right, viewport.right),
    };
}
