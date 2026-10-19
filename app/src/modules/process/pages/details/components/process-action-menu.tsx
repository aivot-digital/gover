import {Divider, ListItemIcon, ListItemText, Menu, MenuItem, Switch, type PopoverOrigin} from '@mui/material';
import React, {type ReactNode, useMemo} from 'react';
import {Link} from 'react-router-dom';

export type ProcessActionMenuItem = {
    label: string;
    icon: ReactNode;
    onClick: () => void;
    disabled?: boolean;
    visible?: boolean;
    isDangerous?: boolean;
    type?: 'action';
} | {
    label: string;
    icon: ReactNode;
    checked: boolean;
    onToggle: () => void;
    disabled?: boolean;
    visible?: boolean;
    type: 'toggle';
} | {
    label: string;
    icon: ReactNode;
    to: string;
    newTab?: boolean;
    disabled?: boolean;
    visible?: boolean;
    isDangerous?: boolean;
} | {
    label: string;
    icon: ReactNode;
    href: string;
    newTab?: boolean;
    disabled?: boolean;
    visible?: boolean;
    isDangerous?: boolean;
} | 'separator';

interface ProcessActionMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    items: ProcessActionMenuItem[];
    minWidth?: number;
    anchorOrigin?: PopoverOrigin;
    transformOrigin?: PopoverOrigin;
    showArrow?: boolean;
}

function normalizeProcessActionMenuItems(items: ProcessActionMenuItem[]): ProcessActionMenuItem[] {
    const visibleItems = items.filter((item) => item === 'separator' || item.visible !== false);
    const normalizedItems: ProcessActionMenuItem[] = [];

    for (const item of visibleItems) {
        if (item === 'separator') {
            if (normalizedItems.length === 0 || normalizedItems[normalizedItems.length - 1] === 'separator') {
                continue;
            }
        }

        normalizedItems.push(item);
    }

    while (normalizedItems[normalizedItems.length - 1] === 'separator') {
        normalizedItems.pop();
    }

    return normalizedItems;
}

export function ProcessActionMenu(props: ProcessActionMenuProps): ReactNode {
    const {
        anchorEl,
        onClose,
        items,
        minWidth = 180,
        anchorOrigin = {
            horizontal: 'right',
            vertical: 'top',
        },
        transformOrigin = {
            horizontal: 'left',
            vertical: 'top',
        },
        showArrow = true,
    } = props;

    const normalizedItems = useMemo(() => normalizeProcessActionMenuItems(items), [items]);

    return (
        <Menu
            open={anchorEl != null}
            anchorEl={anchorEl}
            onClose={onClose}
            anchorOrigin={anchorOrigin}
            transformOrigin={transformOrigin}
            slotProps={{
                paper: {
                    elevation: 6,
                    sx: {
                        mt: showArrow ? -0.875 : 0.5,
                        ml: showArrow ? 0.5 : 0,
                        minWidth,
                        overflow: 'visible',
                        ...(showArrow ? {
                            '&::before': {
                                content: '""',
                                position: 'absolute',
                                top: 19,
                                left: 0,
                                width: 10,
                                height: 10,
                                // Inherit the Paper surface including MUI's dark-mode elevation overlay.
                                background: 'inherit',
                                transform: 'translateX(-50%) rotate(45deg)',
                                boxShadow: '-2px 2px 6px rgba(15, 23, 42, 0.08)',
                                zIndex: 0,
                            },
                        } : {}),
                    },
                },
                list: {
                    sx: {
                        py: 1,
                    },
                }
            }}
        >
            {
                normalizedItems.map((item, index) => {
                    if (item === 'separator') {
                        return <Divider key={`separator-${index}`}/>;
                    }

                    if ('type' in item && item.type === 'toggle') {
                        const handleToggle = () => {
                            if (!item.disabled) {
                                item.onToggle();
                            }
                        };

                        return (
                            <MenuItem
                                key={`${item.label}-${index}`}
                                aria-label={item.label}
                                onClick={handleToggle}
                                disabled={item.disabled}
                                sx={{minHeight: 42, px: 1.5, gap: 1}}
                            >
                                <ListItemIcon sx={{minWidth: 32, color: 'text.secondary'}}>
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText primary={item.label}/>
                                <Switch
                                    edge="end"
                                    checked={item.checked}
                                    disabled={item.disabled}
                                    onChange={handleToggle}
                                    onClick={(event) => event.stopPropagation()}
                                    slotProps={{input: {'aria-label': item.label}}}
                                    sx={{ml: 1, flexShrink: 0}}
                                />
                            </MenuItem>
                        );
                    }

                    const content = (
                        <>
                            <ListItemIcon sx={{minWidth: 32, color: item.isDangerous ? 'error.main' : 'text.secondary'}}>
                                {item.icon}
                            </ListItemIcon>
                            <ListItemText
                                primary={item.label}
                                slotProps={{primary: {color: item.isDangerous ? 'error.main' : 'text.primary'}}}
                            />
                        </>
                    );

                    if ('to' in item) {
                        return (
                            <MenuItem
                                component={Link}
                                key={`${item.label}-${index}`}
                                to={item.to}
                                target={item.newTab ? '_blank' : '_self'}
                                disabled={item.disabled}
                                sx={{minHeight: 42, px: 1.5, gap: 1}}
                            >
                                {content}
                            </MenuItem>
                        );
                    }

                    if ('href' in item) {
                        return (
                            <MenuItem
                                component="a"
                                key={`${item.label}-${index}`}
                                href={item.href}
                                target={item.newTab ? '_blank' : '_self'}
                                disabled={item.disabled}
                                sx={{minHeight: 42, px: 1.5, gap: 1}}
                            >
                                {content}
                            </MenuItem>
                        );
                    }

                    return (
                        <MenuItem
                            key={`${item.label}-${index}`}
                            onClick={(event) => {
                                event.stopPropagation();
                                event.preventDefault();
                                item.onClick();
                                onClose();
                            }}
                            disabled={item.disabled}
                            sx={{minHeight: 42, px: 1.5, gap: 1}}
                        >
                            {content}
                        </MenuItem>
                    );
                })
            }
        </Menu>
    );
}
