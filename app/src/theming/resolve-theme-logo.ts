import type {PaletteMode} from '@mui/material';
import type {ThemeRequestDTO} from '../modules/themes/models/theme';

export function resolveThemeLogoKey(
    theme: Pick<ThemeRequestDTO, 'logoKey' | 'logoKeyDark'>,
    mode: PaletteMode,
): string | null {
    return mode === 'dark' ? theme.logoKeyDark ?? theme.logoKey : theme.logoKey;
}
