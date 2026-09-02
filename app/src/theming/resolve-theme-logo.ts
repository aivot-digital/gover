import type {PaletteMode} from '@mui/material';
import type {ThemeRequestDTO} from '../modules/themes/models/theme';

export function resolveThemeLogoKey(
    theme: Pick<ThemeRequestDTO, 'logoKey' | 'logoKeyDark'>,
    mode: PaletteMode,
): string | null {
    return mode === 'dark' ? theme.logoKeyDark ?? theme.logoKey : theme.logoKey;
}

/**
 * Resolves each theme completely before continuing with its parent. This keeps the light logo of a more specific
 * theme ahead of a parent's dark logo and therefore preserves the existing appearance hierarchy.
 */
export function resolveThemeChainLogoKey(
    themes: Array<Pick<ThemeRequestDTO, 'logoKey' | 'logoKeyDark'>>,
    mode: PaletteMode,
): string | null {
    for (const theme of themes) {
        const logoKey = resolveThemeLogoKey(theme, mode);
        if (logoKey != null) {
            return logoKey;
        }
    }

    return null;
}
