import {type AlertColor, type SxProps} from '@mui/material';

export interface AlertComponentProps {
    title?: string;
    text?: string;
    color: AlertColor;
    sx?: SxProps;
    richtext?: boolean;
}
