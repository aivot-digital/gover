import {CollapseProps} from './collapse-props';
import {Box, Typography, Collapse as MuiCollapse} from '@mui/material';
import {IconButton} from '../icon-button/icon-button';
import {ExpandLessOutlined, ExpandMoreOutlined} from '@mui/icons-material';
import React, {PropsWithChildren, useReducer} from 'react';

export function Collapse(props: PropsWithChildren<CollapseProps>) {
    const [open, toggle] = useReducer((state) => !state, false);

    return (
        <>
            <Box
                sx={{
                    marginTop: 2,
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                }}
            >
                <Typography
                    variant="subtitle2"
                >
                    {props.label}
                </Typography>

                <IconButton
                    buttonProps={{
                        onClick: toggle,
                    }}
                    tooltipProps={{
                        title: open ? props.closeTooltip : props.openTooltip,
                    }}
                    badgeProps={props.badge ? {
                        variant: 'dot',
                        color: 'primary',
                    } : undefined}
                >
                    {open ? <ExpandMoreOutlined/> : <ExpandLessOutlined/>}
                </IconButton>
            </Box>

            <MuiCollapse
                in={open}
                unmountOnExit
            >
                {props.children}
            </MuiCollapse>
        </>
    );
}