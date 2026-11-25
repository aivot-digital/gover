import {styled, Tooltip, tooltipClasses, type TooltipProps} from '@mui/material';
import React from 'react';

export const HintTooltip = styled(({
                                       className,
                                       ...props
                                   }: TooltipProps) => (
    <Tooltip {...props} classes={{popper: className}}/>
))({
    [`& .${tooltipClasses.tooltip}`]: {
        maxWidth: 260,
        padding: '12px',
    },
    [`& .${tooltipClasses.tooltip} a, & .${tooltipClasses.tooltip} .MuiLink-root`]: {
        color: 'inherit',
        marginTop: '4px',
        display: 'block',
    },
});
