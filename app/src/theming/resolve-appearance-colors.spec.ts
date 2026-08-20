import {describe, expect, it} from 'vitest';
import {
    DEFAULT_APPEARANCE_COLORS,
    getColorContrastRatio,
    MINIMUM_THEME_CONTRAST,
    mixHexColors,
    resolveAppearanceColors,
} from './resolve-appearance-colors';

describe('resolveAppearanceColors', () => {
    it('keeps configured MUI main colors unchanged', () => {
        const result = resolveAppearanceColors({
            primaryColor: '#FF613A',
            secondaryColor: '#5F6368',
        });

        expect(result.primary).toBe('#FF613A');
        expect(result.secondary).toBe('#5F6368');
        expect(result.onPrimary).toBe('#000000');
        expect(result.onSecondary).toBe('#FFFFFF');
    });

    it('derives accessible foreground tones without changing the main colors', () => {
        const result = resolveAppearanceColors({
            primaryColor: '#FFD600',
            secondaryColor: '#BCE9FF',
        });

        expect(result.primaryForeground).not.toBe(result.primary);
        expect(result.secondaryForeground).not.toBe(result.secondary);
        expect(getColorContrastRatio(result.primaryForeground, '#FFFFFF'))
            .toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        expect(getColorContrastRatio(result.secondaryForeground, '#FFFFFF'))
            .toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
    });

    it('provides contrasting text for both configured fill colors', () => {
        const result = resolveAppearanceColors({
            primaryColor: '#FF613A',
            secondaryColor: '#173F5F',
        });

        expect(getColorContrastRatio(result.onPrimary, result.primary))
            .toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        expect(getColorContrastRatio(result.onSecondary, result.secondary))
            .toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
    });

    it('derives accessible foreground tones for a dark paper surface', () => {
        const paper = '#1C1C1C';
        const result = resolveAppearanceColors({
            primaryColor: '#253B5B',
            secondaryColor: '#5F6368',
        }, paper);

        expect(result.primary).toBe('#253B5B');
        expect(result.secondary).toBe('#5F6368');
        expect(result.primaryForeground).not.toBe(result.primary);
        expect(getColorContrastRatio(result.primaryForeground, paper))
            .toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
        expect(getColorContrastRatio(result.secondaryForeground, paper))
            .toBeGreaterThanOrEqual(MINIMUM_THEME_CONTRAST);
    });

    it('uses the standard appearance when no input is given', () => {
        const result = resolveAppearanceColors();

        expect(result.primary).toBe(DEFAULT_APPEARANCE_COLORS.primaryColor);
        expect(result.secondary).toBe(DEFAULT_APPEARANCE_COLORS.secondaryColor);
    });

    it('mixes opaque colors with a bounded weight', () => {
        expect(mixHexColors('#808080', '#FF0000', 0.25)).toBe('#A06060');
        expect(() => mixHexColors('#808080', '#FF0000', 1.01)).toThrow('Invalid color weight');
    });
});
