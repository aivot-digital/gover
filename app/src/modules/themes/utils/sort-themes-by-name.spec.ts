import {describe, expect, it} from 'vitest';
import type {ThemeResponseDTO} from '../models/theme';
import {sortThemesByName} from './sort-themes-by-name';

describe('sortThemesByName', () => {
    it('uses German alphabetical and numeric ordering without changing the source list', () => {
        const themes = ['Zebra', 'Thema 10', 'Ämter', 'Thema 2', 'alpha'].map((name, id) => ({id, name}) as ThemeResponseDTO);

        expect(sortThemesByName(themes).map((theme) => theme.name)).toEqual(['alpha', 'Ämter', 'Thema 2', 'Thema 10', 'Zebra']);
        expect(themes[0].name).toBe('Zebra');
    });
});
