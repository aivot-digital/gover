import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it} from 'vitest';
import {useSessionStorageState} from './use-session-storage-state';

describe('useSessionStorageState', () => {
    afterEach(() => {
        sessionStorage.clear();
    });

    it('restores the state after a remount', () => {
        const first = renderHook(() => useSessionStorageState<Record<number, boolean>>('test-key', {}));

        act(() => {
            first.result.current[1]((previous) => ({...previous, 7: true}));
        });
        first.unmount();

        const second = renderHook(() => useSessionStorageState<Record<number, boolean>>('test-key', {}));

        expect(second.result.current[0]).toEqual({7: true});
    });

    it('loads the state of a new key without writing the previous state to it', () => {
        const {result, rerender} = renderHook(
            ({storageKey}) => useSessionStorageState<Record<number, boolean>>(storageKey, {}),
            {initialProps: {storageKey: 'version-1'}},
        );

        act(() => {
            result.current[1]({1: true});
        });
        rerender({storageKey: 'version-2'});

        expect(result.current[0]).toEqual({});

        act(() => {
            result.current[1]((previous) => ({...previous, 2: true}));
        });
        rerender({storageKey: 'version-1'});

        expect(result.current[0]).toEqual({1: true});
    });

    it('ignores updates from the setter of a previous key', () => {
        const {result, rerender} = renderHook(
            ({storageKey}) => useSessionStorageState<Record<number, boolean>>(storageKey, {}),
            {initialProps: {storageKey: 'version-1'}},
        );
        const setVersion1State = result.current[1];

        rerender({storageKey: 'version-2'});
        act(() => {
            setVersion1State({1: true});
        });

        expect(result.current[0]).toEqual({});
    });
});
