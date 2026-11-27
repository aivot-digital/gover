import {alpha, type Theme} from '@mui/material/styles';

export function getPreviewHighlightStyles(theme: Theme, isHighlighted: boolean) {
    return {
        borderRadius: 1,
        outline: isHighlighted
            ? `2px solid ${alpha(theme.palette.secondary.main, 0.42)}`
            : '2px solid transparent',
        outlineOffset: 3,
        boxShadow: isHighlighted
            ? [
                `0 0 0 4px ${alpha(theme.palette.secondary.main, 0.08)}`,
                `0 8px 22px ${alpha(theme.palette.secondary.main, 0.12)}`,
            ].join(', ')
            : 'none',
        transition: 'outline-color 120ms ease, box-shadow 120ms ease',
    };
}
