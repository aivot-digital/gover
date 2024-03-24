import {styled, Tooltip, tooltipClasses, type TooltipProps} from '@mui/material';
import React from 'react';

export const HintTooltip = styled(({
    className,
    ...props
}: TooltipProps) => (
    <Tooltip {...props} classes={{popper: className}}/>
))(({theme}) => ({
    [`& .${tooltipClasses.tooltip}`]: {
        backgroundColor: '#fff',
        color: '#444',
        maxWidth: 220,
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.05)',
        padding: '10px 12px',
        border: '1px solid #ccc',
    },
    [`& .${tooltipClasses.tooltip} a`]: {
        color: '#444',
        marginTop: '4px',
        display: 'block',
    },
    [`& .${tooltipClasses.arrow}`]: {
        color: '#fff',
    },
    [`& .${tooltipClasses.arrow}:before`]: {
        border: '1px solid #ccc',
    },
}));
