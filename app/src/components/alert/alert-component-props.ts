import {type AlertColor, type SxProps} from '@mui/material';

export interface AlertComponentProps {
    title?: string;
    text?: string;
    color: AlertColor;
    colorVariant?: 'default' | 'prominent';
    sx?: SxProps;
    richtext?: boolean;
}
