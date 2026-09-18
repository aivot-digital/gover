import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('createElkLoader', () => {
    beforeEach(() => { vi.resetModules(); });
    afterEach(() => {
        vi.doUnmock('elkjs/lib/elk.bundled.js');
        vi.useRealTimers();
    });

    it('loads on demand, shares concurrent initialization and keeps diagram instances separate', async () => {
        const createInstance = vi.fn();
        const imported = vi.fn(() => ({default: class {
            constructor() { createInstance(); }
        }}));
        vi.doMock('elkjs/lib/elk.bundled.js', imported);
        const {createElkLoader} = await import('./elk-loader');
        const process = createElkLoader();
        const organization = createElkLoader();
        expect(imported).not.toHaveBeenCalled();
        const first = process();
        expect(process()).toBe(first);
        const instance = await first;
        expect(await process()).toBe(instance);
        expect(await organization()).not.toBe(instance);
        expect(imported).toHaveBeenCalledTimes(1);
        expect(createInstance).toHaveBeenCalledTimes(2);
    });

    it('wraps load errors and permits a later retry', async () => {
        vi.doMock('elkjs/lib/elk.bundled.js', () => { throw new Error('unavailable'); });
        const {createElkLoader, ElkLoadError} = await import('./elk-loader');
        const getElk = createElkLoader();
        await expect(getElk()).rejects.toBeInstanceOf(ElkLoadError);
        vi.doMock('elkjs/lib/elk.bundled.js', () => ({default: class {}}));
        await expect(getElk()).resolves.toBeDefined();
    });

    it('keeps initial loading visible for 500 ms and reuses the ready instance without a further delay', async () => {
        vi.useFakeTimers();
        vi.doMock('elkjs/lib/elk.bundled.js', () => ({default: class {}}));
        const {createElkLoader} = await import('./elk-loader');
        const getElk = createElkLoader();
        const completed = vi.fn();
        const first = getElk();
        void first.then(completed);
        await vi.advanceTimersByTimeAsync(499);
        expect(completed).not.toHaveBeenCalled();
        await vi.advanceTimersByTimeAsync(1);
        const instance = await first;
        expect(completed).toHaveBeenCalledWith(instance);
        expect(await getElk()).toBe(instance);
        expect(vi.getTimerCount()).toBe(0);
    });

    it('also holds a fast load error until the minimum duration has elapsed', async () => {
        vi.useFakeTimers();
        vi.doMock('elkjs/lib/elk.bundled.js', () => { throw new Error('offline'); });
        const {createElkLoader, ElkLoadError} = await import('./elk-loader');
        const failed = vi.fn();
        const first = createElkLoader()().catch(failed);
        await vi.advanceTimersByTimeAsync(499);
        expect(failed).not.toHaveBeenCalled();
        await vi.advanceTimersByTimeAsync(1);
        await first;
        expect(failed).toHaveBeenCalledWith(expect.any(ElkLoadError));
    });
});
