import {Action, ActionColor, ActionDirection, ActionsProps, ActionTooltipPlacement} from './actions-props';
import {Box, Button, IconButton, Tooltip} from '@mui/material';
import React from 'react';
import {Link} from 'react-router-dom';

export function Actions(props: ActionsProps) {
    return (
        <Box
            sx={{
                ...props.sx,
                display: 'flex',
                flexDirection: props.direction ?? 'row',
                alignItems: 'center',
                height: props.direction == null || props.direction === 'row' || props.direction === 'row-reverse' ? '100%' : undefined,
                width: props.direction === 'column' || props.direction === 'column-reverse' ? '100%' : undefined,
                gap: props.dense ? 1 : 2,
            }}
        >
            {
                props.actions != null &&
                props.actions
                    .map((action, index) => (
                        <ToolbarActionDispatcher
                            key={action === 'separator' ? index : (action.label ?? action.tooltip ?? index)}
                            action={action}
                            color={props.color ?? 'primary'}
                            index={index}
                            isBusy={props.isBusy ?? false}
                            isFirst={index === 0}
                            isLast={index === ((props.actions?.length ?? 0) - 1)}
                            tooltipPlacement={props.tooltipPlacement ?? 'bottom'}
                        />
                    ))
            }
        </Box>
    );
}

interface ToolbarActionDispatcherProps {
    action: Action;
    color: ActionColor;
    index: number;
    isBusy: boolean;
    isFirst: boolean;
    isLast: boolean;
    tooltipPlacement: ActionTooltipPlacement;
}

function ToolbarActionDispatcher(props: ToolbarActionDispatcherProps) {
    const {
        action,
        color,
        index,
        isBusy,
        isFirst,
        isLast,
        tooltipPlacement,
    } = props;

    if (action === 'separator') {
        return (
            <Box
                key={action + index}
                sx={{
                    width: '1px',
                    height: '2em',
                    backgroundColor: 'black',
                    opacity: '.15',
                    m: 0,
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
                color={color}
                sx={{
                    m: 0,
                }}
                variant={action.variant}
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
                color={color}
                sx={{
                    m: 0,
                }}
                onClick={onClick}
                component={component}
                href={href}
                to={to}
                target={target}
                disabled={shouldDisable}
                edge={isFirst ? 'start' : isLast ? 'end' : false}
            >
                {action.icon}
            </IconButton>
        );
    }

    if (action.tooltip) {
        return (
            <Tooltip
                key={index}
                title={action.tooltip}
                arrow
                placement={tooltipPlacement}
            >
                <span>
                    {element}
                </span>
            </Tooltip>
        );
    }

    return <span key={index}>{element}</span>;
}