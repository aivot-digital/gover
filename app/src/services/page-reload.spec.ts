import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {registerPageReloadPreparation, reloadPage} from './page-reload';

describe('reloadPage', () => {
    const originalLocation = window.location;
    const reload = vi.fn();
    let unregister: (() => void) | undefined;

    beforeEach(() => {
        vi.useFakeTimers();
        Object.defineProperty(window, 'location', {configurable: true, value: {...originalLocation, reload}});
    });

    afterEach(() => {
        unregister?.();
        unregister = undefined;
        reload.mockReset();
        vi.useRealTimers();
        Object.defineProperty(window, 'location', {configurable: true, value: originalLocation});
    });

    it('reloads once the registered preparation has finished', async () => {
        const backup = Promise.withResolvers<boolean>();
        unregister = registerPageReloadPreparation(() => backup.promise);

        const reloading = reloadPage();
        await vi.advanceTimersByTimeAsync(1_999);
        expect(reload).not.toHaveBeenCalled();

        backup.resolve(true);
        await reloading;
        expect(reload).toHaveBeenCalledOnce();
    });

    it('reloads after a time limit or a failure of the preparation', async () => {
        unregister = registerPageReloadPreparation(() => new Promise(() => undefined));
        const reloading = reloadPage();
        await vi.advanceTimersByTimeAsync(2_000);
        await reloading;
        expect(reload).toHaveBeenCalledOnce();

        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        unregister = registerPageReloadPreparation(() => Promise.reject(new Error('IndexedDB unavailable')));
        await reloadPage();
        expect(reload).toHaveBeenCalledTimes(2);
    });

    it('no longer waits for a preparation that was unregistered', async () => {
        registerPageReloadPreparation(() => new Promise(() => undefined))();

        await reloadPage();
        expect(reload).toHaveBeenCalledOnce();
    });
});
