import {styled, Tooltip, tooltipClasses, type TooltipProps} from '@mui/material';
import React from 'react';

export const HintTooltip = styled(({
    className,
    ...props
}: TooltipProps) => (
    <Tooltip {...props} classes={{popper: className}}/>
))(({theme}) => ({
    [`& .${tooltipClasses.tooltip}`]: {
        backgroundColor: theme.palette.background.paper,
        color: theme.palette.text.primary,
        maxWidth: 220,
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
        padding: '10px 12px',
        border: `1px solid ${theme.palette.divider}`,
    },
    [`& .${tooltipClasses.tooltip} a`]: {
        color: theme.palette.text.primary,
        marginTop: '4px',
        display: 'block',
    },
    [`& .${tooltipClasses.arrow}`]: {
        color: theme.palette.background.paper,
    },
    [`& .${tooltipClasses.arrow}:before`]: {
        border: `1px solid ${theme.palette.divider}`,
    },
}));
