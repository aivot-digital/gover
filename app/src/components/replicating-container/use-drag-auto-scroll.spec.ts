import {describe, expect, it} from 'vitest';
import {getAutoScrollDelta} from './use-drag-auto-scroll';

const bounds = {top: 100, bottom: 700, left: 0, right: 400};

describe('getAutoScrollDelta', () => {
    it('does not scroll while the pointer is away from the edges', () => {
        expect(getAutoScrollDelta({x: 200, y: 400}, bounds)).toBe(0);
    });

    it('scrolls faster the closer the pointer gets to an edge', () => {
        const nearBottom = getAutoScrollDelta({x: 200, y: 650}, bounds);
        const atBottom = getAutoScrollDelta({x: 200, y: 699}, bounds);
        const nearTop = getAutoScrollDelta({x: 200, y: 150}, bounds);

        expect(nearBottom).toBeGreaterThan(0);
        expect(atBottom).toBeGreaterThan(nearBottom);
        expect(nearTop).toBeLessThan(0);
    });

    it('keeps scrolling at full speed when the pointer overshoots onto a bordering toolbar or footer', () => {
        expect(getAutoScrollDelta({x: 200, y: 760}, bounds)).toBe(20);
        expect(getAutoScrollDelta({x: 200, y: 40}, bounds)).toBe(-20);
    });

    it('does not scroll while the pointer is beside the scroll container', () => {
        expect(getAutoScrollDelta({x: 600, y: 699}, bounds)).toBe(0);
    });
});
