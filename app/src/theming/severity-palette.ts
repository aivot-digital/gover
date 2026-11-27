import {type Palette, type PaletteMode} from '@mui/material/styles';

type Severity = 'error' | 'warning' | 'info' | 'success';

interface SeverityPalette {
    shades: Record<Severity, Omit<Palette[Severity], 'contrastText'>>;
    standardWarning?: {backgroundColor: string; color: string; iconColor: string};
    filled?: Record<Severity, {backgroundColor: string; color: string; hoverBackgroundColor: string}>;
}

export const APP_SEVERITY_PALETTE: Record<PaletteMode, SeverityPalette> = {
    light: {
        shades: {
            error: {main: '#BD2B39', light: '#DB5360', dark: '#A02130'},
            warning: {main: '#A54C00', light: '#C45D0C', dark: '#853D00'},
            info: {main: '#1E63BE', light: '#4385D0', dark: '#18539D'},
            success: {main: '#26764A', light: '#499266', dark: '#1B603E'},
        },
        // Warning text on neutral surfaces needs a deeper color than a solid warning fill.
        standardWarning: {backgroundColor: '#FFF5E5', color: '#733607', iconColor: '#C65809'},
        filled: {
            error: {backgroundColor: '#C63946', color: '#FFFFFF', hoverBackgroundColor: '#B82E3C'},
            warning: {backgroundColor: '#FFCA79', color: '#2E1803', hoverBackgroundColor: '#F7B966'},
            info: {backgroundColor: '#2B6CC6', color: '#FFFFFF', hoverBackgroundColor: '#1F60B8'},
            success: {backgroundColor: '#347F54', color: '#FFFFFF', hoverBackgroundColor: '#28724B'},
        },
    },
    dark: {
        shades: {
            error: {main: '#F6868D', light: '#FFADB2', dark: '#E27179'},
            warning: {main: '#F0C36A', light: '#F7DB9A', dark: '#DFB354'},
            info: {main: '#83B9F4', light: '#ADD3FA', dark: '#71A7E4'},
            success: {main: '#82CB9D', light: '#AFE4BF', dark: '#68B483'},
        },
    },
};
