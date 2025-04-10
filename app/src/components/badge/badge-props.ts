import {ReactNode} from 'react';
import {SxProps, TooltipProps} from '@mui/material';

export interface BadgeProps {
    label: ReactNode;
    color: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
    tooltip?: TooltipProps;
    sx?: SxProps;
}