import {alpha, type Theme} from '@mui/material/styles';

const REACT_FLOW_BACKGROUND_DOT_OPACITY = {
    light: 0.4,
    dark: 0.22,
} as const;

export function getReactFlowBackgroundDotColor(theme: Theme): string {
    return alpha(
        theme.palette.text.primary,
        REACT_FLOW_BACKGROUND_DOT_OPACITY[theme.palette.mode],
    );
}
