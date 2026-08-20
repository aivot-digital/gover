import {describe, expect, it} from 'vitest';
import {resolveThemeChainLogoKey, resolveThemeLogoKey} from './resolve-theme-logo';

describe('resolveThemeLogoKey', () => {
    it('uses the dedicated dark logo when available', () => {
        expect(resolveThemeLogoKey({logoKey: 'light', logoKeyDark: 'dark'}, 'dark')).toBe('dark');
    });

    it('falls back to the light logo in dark mode', () => {
        expect(resolveThemeLogoKey({logoKey: 'light', logoKeyDark: null}, 'dark')).toBe('light');
    });

    it('resolves both variants within the most specific theme before continuing the chain', () => {
        expect(resolveThemeChainLogoKey([
            {logoKey: 'specific-light', logoKeyDark: null},
            {logoKey: 'parent-light', logoKeyDark: 'parent-dark'},
        ], 'dark')).toBe('specific-light');
    });
});
