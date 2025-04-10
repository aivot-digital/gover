import {IconButtonProps} from './icon-button-props';
import {Tooltip, IconButton as MuiIconButton, Badge} from '@mui/material';
import {PropsWithChildren} from 'react';

export function IconButton(props: PropsWithChildren<IconButtonProps>) {
    if (props.badgeProps == null) {
        return (
            <Tooltip {...props.tooltipProps}>
                <MuiIconButton {...props.buttonProps}>
                    {props.children}
                </MuiIconButton>
            </Tooltip>
        );
    }

    return (
        <Tooltip {...props.tooltipProps}>
            <Badge {...props.badgeProps}>
                <MuiIconButton {...props.buttonProps}>
                    {props.children}
                </MuiIconButton>
            </Badge>
        </Tooltip>
    );
}