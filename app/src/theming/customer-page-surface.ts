import {type Theme} from '@mui/material';
import {APP_BACKGROUND_COLORS} from './themes';

// Customer content and logos need a neutral surface, independent of the brand tint and editor shell.
export function getCustomerPageSurfaceColor(theme: Theme): string {
    return APP_BACKGROUND_COLORS[theme.palette.mode].paper;
}
