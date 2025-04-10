import {BadgeProps, IconButtonProps as MuiIconButtonProps, TooltipProps} from '@mui/material';

export interface IconButtonProps {
    buttonProps: MuiIconButtonProps;
    tooltipProps: Omit<TooltipProps, 'children'>;
    badgeProps?: BadgeProps;
}
