import {type ReactNode} from 'react';
import {type SxProps, type TooltipProps} from '@mui/material';

export interface BadgeProps {
    label: ReactNode;
    color: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
    tooltip?: TooltipProps;
    sx?: SxProps;
}

export function isBadgeProps(badge: any): badge is BadgeProps {
    return badge != null &&
        (badge as BadgeProps).label !== undefined &&
        (badge as BadgeProps).color !== undefined;
}
