import {IconButtonProps} from './icon-button-props';
import {Badge, IconButton as MuiIconButton, Tooltip} from '@mui/material';
import {PropsWithChildren} from 'react';

export function IconButton(props: PropsWithChildren<IconButtonProps>) {
    const accessibleLabel = props.buttonProps['aria-label'] ?? (
        typeof props.tooltipProps.title === 'string' ? props.tooltipProps.title : undefined
    );

    if (props.badgeProps == null) {
        return (
            <Tooltip arrow {...props.tooltipProps}>
                <span style={{display: 'inline-flex'}}>
                    <MuiIconButton {...props.buttonProps} aria-label={accessibleLabel}>
                        {props.children}
                    </MuiIconButton>
                </span>
            </Tooltip>
        );
    }

    return (
        <Tooltip arrow {...props.tooltipProps}>
            <Badge {...props.badgeProps}>
                <MuiIconButton {...props.buttonProps} aria-label={accessibleLabel}>
                    {props.children}
                </MuiIconButton>
            </Badge>
        </Tooltip>
    );
}
