import {Action, ActionsProps} from './actions-props';
import {Box, Button, IconButton, Tooltip} from '@mui/material';
import React from 'react';
import {Link} from 'react-router-dom';

export function Actions(props: ActionsProps) {
    return (
        <Box
            sx={{
                ...props.sx,
                display: 'flex',
                alignItems: 'center',
            }}
        >
            {
                props.actions != null &&
                props.actions
                    .map((action, index) => dispatchToolbarAction(action, index, props.isBusy ?? false))
            }
        </Box>
    );
}

function dispatchToolbarAction(action: Action, index: number, isBusy: boolean) {
    // Check if this action is a separator and render a simple separator div
    if (action === 'separator') {
        return (
            <Box
                key={action + index}
                sx={{
                    ml: 2,
                    width: '1px',
                    height: '2em',
                    backgroundColor: 'black',
                    opacity: '.15',
                }}
            />
        );
    }

    // Check if visible is explicitly set to false. This prevents not rendering, when visible is undefined
    if (action.visible === false) {
        return null;
    }

    // Determine the component to render, either a button or a link
    const component = 'onClick' in action ? 'button' : ('to' in action ? Link : 'a');

    // Determine shared properties
    const href = 'href' in action ? action.href : undefined;
    const to = 'to' in action ? action.to : undefined;
    const target = 'href' in action ? '_blank' : undefined;
    const onClick = 'onClick' in action ? action.onClick : undefined;
    const shouldDisable = action.ignoreBusy ? action.disabled : (action.disabled || isBusy);

    // Build the element for this action which will then be encapsulated in a tooltip
    let element;
    if (action.label != null) {
        element = (
            <Button
                size="small"
                color="primary"
                variant={action.variant}
                sx={{
                    ml: 1,
                }}
                onClick={onClick}
                component={component}
                href={href}
                to={to}
                target={target}
                endIcon={action.icon}
                disabled={shouldDisable}
            >
                {action.label}
            </Button>
        );
    } else {
        element = (
            <IconButton
                size="small"
                color="primary"
                sx={{
                    ml: 1,
                }}
                onClick={onClick}
                component={component}
                href={href}
                to={to}
                target={target}
                disabled={shouldDisable}
            >
                {action.icon}
            </IconButton>
        );
    }

    if (action.tooltip) {
        return (
            <Tooltip key={index} title={action.tooltip} arrow>
                <span>{element}</span>
            </Tooltip>
        );
    }

    return <span key={index}>{element}</span>;
}