import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

describe('loadMonacoEditor', () => {
    beforeEach(() => { vi.resetModules(); });
    afterEach(() => { vi.doUnmock('./monaco-editor-runtime'); });

    it('defers the runtime import and shares it between concurrent editors', async () => {
        const initialize = vi.fn(() => ({MonacoEditorImplementation: () => null}));
        vi.doMock('./monaco-editor-runtime', initialize);
        const {loadMonacoEditor} = await import('./monaco-editor-loader');
        expect(initialize).not.toHaveBeenCalled();
        const first = loadMonacoEditor();
        const second = loadMonacoEditor();
        expect(second).toBe(first);
        expect(await second).toBe(await first);
        expect(initialize).toHaveBeenCalledTimes(1);
        expect(loadMonacoEditor()).toBe(first);
    });

    it('allows another import attempt after a rejected load', async () => {
        vi.doMock('./monaco-editor-runtime', () => { throw new Error('unavailable'); });
        const {loadMonacoEditor} = await import('./monaco-editor-loader');
        await expect(loadMonacoEditor()).rejects.toThrow();
        const implementation = () => null;
        vi.doMock('./monaco-editor-runtime', () => ({MonacoEditorImplementation: implementation}));
        await expect(loadMonacoEditor()).resolves.toEqual({MonacoEditorImplementation: implementation});
    });

});
