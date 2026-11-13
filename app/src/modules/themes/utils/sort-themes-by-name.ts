import type {ThemeResponseDTO} from '../models/theme';

export function sortThemesByName(themes: readonly ThemeResponseDTO[]): ThemeResponseDTO[] {
    return [...themes].sort((left, right) => (
        left.name.localeCompare(right.name, 'de', {numeric: true, sensitivity: 'base'}) || left.id - right.id
    ));
}
