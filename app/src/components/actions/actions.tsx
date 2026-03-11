import {type Action, type ActionColor, type ActionsProps, type ActionTooltipPlacement} from './actions-props';
import {Box, Button, IconButton, Tooltip} from '@mui/material';
import React, {type ReactNode} from 'react';
import {Link} from 'react-router-dom';

export function Actions(props: ActionsProps): ReactNode {
    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: props.direction ?? 'row',
                alignItems: 'center',
                height: props.direction == null || props.direction === 'row' || props.direction === 'row-reverse' ? '100%' : undefined,
                width: props.direction === 'column' || props.direction === 'column-reverse' ? '100%' : undefined,
                gap: (props.dense === true) ? 1 : 2,
                ...props.sx,
            }}
        >
            {
                props
                    .actions
                    ?.map((action, index) => (
                        <ToolbarActionDispatcher
                            key={action === 'separator' ? index : (action.label ?? action.tooltip ?? index)}
                            action={action}
                            color={props.color ?? 'primary'}
                            index={index}
                            isBusy={props.isBusy ?? false}
                            isFirst={index === 0}
                            isLast={index === ((props.actions?.length ?? 0) - 1)}
                            tooltipPlacement={props.tooltipPlacement ?? 'bottom'}
                            size={props.size ?? 'small'}
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
    size: 'small' | 'medium' | 'large';
}

function ToolbarActionDispatcher(props: ToolbarActionDispatcherProps): ReactNode {
    const {
        action,
        color,
        index,
        isBusy,
        isFirst,
        isLast,
        tooltipPlacement,
        size,
    } = props;

    if (action === 'separator') {
        return (
            <Box
                key={action + index.toString()}
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
    const isHashLink = 'to' in action && action.to.startsWith('#');
    const component = 'onClick' in action ? 'button' : ('to' in action ? (isHashLink ? 'a' : Link) : 'a');

    // Determine shared properties
    const href = 'href' in action ? action.href : undefined;
    const to = 'to' in action ? action.to : undefined;
    const resolvedHref = isHashLink ? to : href;
    const target = 'href' in action ? '_blank' : undefined;
    const onClick = 'onClick' in action ? action.onClick : undefined;
    const shouldDisable = action.ignoreBusy ? action.disabled : (action.disabled || isBusy);
    const activeStyle = 'activeStyle' in action ? action.activeStyle : undefined;

    // Build the element for this action which will then be encapsulated in a tooltip
    let element;
    if (action.label != null) {
        element = (
            <Button
                size={size}
                color={color}
                sx={{
                    m: 0,
                    ...(activeStyle != null ? activeStyle : {}),
                }}
                variant={action.variant}
                onClick={onClick}
                component={component}
                href={resolvedHref}
                to={isHashLink ? undefined : to}
                target={target}
                aria-label={action.ariaLabel}
                endIcon={action.icon}
                disabled={shouldDisable}
            >
                {action.label}
            </Button>
        );
    } else {
        element = (
            <IconButton
                size={size}
                color={color}
                sx={{
                    m: 0,
                    ...(activeStyle != null ? activeStyle : {}),
                }}
                onClick={onClick}
                component={component}
                href={resolvedHref}
                to={isHashLink ? undefined : to}
                target={target}
                aria-label={action.ariaLabel}
                disabled={shouldDisable}
                edge={isFirst ? 'start' : isLast ? 'end' : false}
            >
                {action.icon}
            </IconButton>
        );
    }

    if (action.tooltip != null || (shouldDisable === true && action.disabledTooltip != null)) {
        return (
            <Tooltip
                key={index}
                title={shouldDisable === true && action.disabledTooltip != null ? action.disabledTooltip : action.tooltip}
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
