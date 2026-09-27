import {type BadgeProps} from './badge-props';
import {Chip, Tooltip} from '@mui/material';
import React, {type ReactNode} from 'react';

export function Badge(props: BadgeProps): ReactNode {
    if (props.tooltip != null) {
        return (
            <Tooltip {...props.tooltip}>
                <Chip
                    color={props.color}
                    sx={props.sx}
                    label={props.label}
                    size="small"
                    onDelete={props.onDelete}
                />
            </Tooltip>
        );
    }

    return (
        <Chip
            color={props.color}
            sx={props.sx}
            label={props.label}
            size="small"
            onDelete={props.onDelete}
        />
    );
}
