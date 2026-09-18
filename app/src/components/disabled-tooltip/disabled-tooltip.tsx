import {Box, Tooltip, type SxProps, type Theme, type TooltipProps} from '@mui/material';
import React, {type ReactNode} from 'react';

interface DisabledTooltipProps {
    disabled?: boolean;
    title?: ReactNode;
    children: ReactNode;
    placement?: TooltipProps['placement'];
    wrapperSx?: SxProps<Theme>;
}

export function DisabledTooltip(props: DisabledTooltipProps): ReactNode {
    if (props.disabled !== true || props.title == null || props.title === false || props.title === '') {
        if (props.wrapperSx == null) {
            return props.children;
        }

        return (
            <Box
                component="span"
                sx={props.wrapperSx}
            >
                {props.children}
            </Box>
        );
    }

    return (
        <Tooltip
            title={props.title}
            arrow
            placement={props.placement}
        >
            <Box
                component="span"
                sx={props.wrapperSx}
            >
                {props.children}
            </Box>
        </Tooltip>
    );
}
