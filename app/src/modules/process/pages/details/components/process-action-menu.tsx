import {Divider, ListItemIcon, ListItemText, Menu, MenuItem, type PopoverOrigin} from '@mui/material';
import React, {type ReactNode, useMemo} from 'react';

export type ProcessActionMenuItem = {
    label: string;
    icon: ReactNode;
    onClick: () => void;
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
            PaperProps={{
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
                            bgcolor: 'background.paper',
                            transform: 'translateX(-50%) rotate(45deg)',
                            boxShadow: '-2px 2px 6px rgba(15, 23, 42, 0.08)',
                            zIndex: 0,
                        },
                    } : {}),
                },
            }}
            MenuListProps={{
                sx: {
                    py: 1,
                },
            }}
        >
            {
                normalizedItems.map((item, index) => item === 'separator' ? (
                    <Divider key={`separator-${index}`}/>
                ) : (
                    <MenuItem
                        key={`${item.label}-${index}`}
                        onClick={(event) => {
                            event.stopPropagation();
                            event.preventDefault();

                            item.onClick();
                            onClose();
                        }}
                        disabled={item.disabled}
                        sx={{
                            minHeight: 42,
                            px: 1.5,
                            gap: 1,
                        }}
                    >
                        <ListItemIcon
                            sx={{
                                minWidth: 32,
                                color: item.isDangerous ? 'error.main' : 'text.secondary',
                            }}
                        >
                            {item.icon}
                        </ListItemIcon>
                        <ListItemText
                            primary={item.label}
                            primaryTypographyProps={{
                                color: item.isDangerous ? 'error.main' : 'text.primary',
                            }}
                        />
                    </MenuItem>
                ))
            }
        </Menu>
    );
}
