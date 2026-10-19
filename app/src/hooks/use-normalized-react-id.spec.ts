import {describe, expect, it} from 'vitest';
import {renderHook} from '@testing-library/react';
import {useNormalizedReactId} from './use-normalized-react-id';

describe('useNormalizedReactId', () => {
    it('returns a stable ID containing only selector-friendly characters', () => {
        const {result, rerender} = renderHook(() => useNormalizedReactId());
        const initialId = result.current;

        expect(initialId).not.toBe('');
        expect(initialId).toMatch(/^[A-Za-z0-9_-]+$/);

        rerender();
        expect(result.current).toBe(initialId);
    });
});
