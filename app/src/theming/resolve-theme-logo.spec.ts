import {describe, expect, it} from 'vitest';
import {resolveThemeLogoKey} from './resolve-theme-logo';

describe('resolveThemeLogoKey', () => {
    it('uses the dedicated dark logo when available', () => {
        expect(resolveThemeLogoKey({logoKey: 'light', logoKeyDark: 'dark'}, 'dark')).toBe('dark');
    });

    it('falls back to the light logo in dark mode', () => {
        expect(resolveThemeLogoKey({logoKey: 'light', logoKeyDark: null}, 'dark')).toBe('light');
    });
    it('returns no logo when the selected theme has none', () => {
        expect(resolveThemeLogoKey({logoKey: null, logoKeyDark: null}, 'dark')).toBeNull();
    });
});
