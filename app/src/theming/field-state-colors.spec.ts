import {createTheme} from '@mui/material/styles';
import {describe, expect, it} from 'vitest';
import {getDisabledFieldBackground} from './field-state-colors';

describe('getDisabledFieldBackground', () => {
    it.each([
        ['light', 'rgba(0, 0, 0, 0.03)'],
        ['dark', 'rgba(255, 255, 255, 0.06)'],
    ] as const)('returns a subtle %s-mode overlay', (mode, expectedColor) => {
        const theme = createTheme({palette: {mode}});

        expect(getDisabledFieldBackground(theme)).toBe(expectedColor);
    });
});
