import {alpha, type Theme} from '@mui/material/styles';

const DISABLED_FIELD_BACKGROUND_OPACITY = {
    light: 0.03,
    dark: 0.06,
} as const;

export function getDisabledFieldBackground(theme: Theme): string {
    // Keep disabled fields recognizable without making them look like selected or filled surfaces.
    return alpha(
        theme.palette.text.primary,
        DISABLED_FIELD_BACKGROUND_OPACITY[theme.palette.mode],
    );
}
