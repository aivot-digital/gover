import {Box, Divider, ListItemIcon, ListItemText, Menu, MenuItem, Switch} from '@mui/material';
import React, {type ReactNode, useMemo} from 'react';

type FormDetailsPageMoreMenuActionItem = {
    label: string;
    icon: ReactNode;
    endIcon?: ReactNode;
    onClick: () => void;
    disabled?: boolean;
    visible?: boolean;
    isDangerous?: boolean;
    type?: 'action';
};

type FormDetailsPageMoreMenuToggleItem = {
    label: string;
    icon: ReactNode;
    checked: boolean;
    onToggle: () => void;
    disabled?: boolean;
    visible?: boolean;
    type: 'toggle';
};

export type FormDetailsPageMoreMenuItem =
    | FormDetailsPageMoreMenuActionItem
    | FormDetailsPageMoreMenuToggleItem
    | 'separator';

interface FormDetailsPageMoreMenuProps {
    anchorEl: HTMLElement | null;
    onClose: () => void;
    items: FormDetailsPageMoreMenuItem[];
}

function normalizeFormDetailsPageMoreMenuItems(items: FormDetailsPageMoreMenuItem[]): FormDetailsPageMoreMenuItem[] {
    const visibleItems = items.filter((item) => item === 'separator' || item.visible !== false);
    const normalizedItems: FormDetailsPageMoreMenuItem[] = [];

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

export function FormDetailsPageMoreMenu(props: FormDetailsPageMoreMenuProps): ReactNode {
    const {
        anchorEl,
        onClose,
        items,
    } = props;

    const normalizedItems = useMemo(() => normalizeFormDetailsPageMoreMenuItems(items), [items]);

    return (
        <Menu
            open={anchorEl != null}
            anchorEl={anchorEl}
            onClose={onClose}
            anchorOrigin={{
                horizontal: 'right',
                vertical: 'top',
            }}
            transformOrigin={{
                horizontal: 'left',
                vertical: 'top',
            }}
            PaperProps={{
                elevation: 6,
                sx: {
                    mt: -0.875,
                    ml: 0.5,
                    minWidth: 400,
                    width: 'max-content',
                    maxWidth: 'calc(100vw - 32px)',
                    overflow: 'visible',
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
                },
            }}
            MenuListProps={{
                sx: {
                    py: 1,
                },
            }}
        >
            {
                normalizedItems.map((item, index) => {
                    if (item === 'separator') {
                        return <Divider key={`separator-${index}`}/>;
                    }

                    if (item.type === 'toggle') {
                        const handleToggle = () => {
                            item.onToggle();
                        };

                        return (
                            <MenuItem
                                key={`${item.label}-${index}`}
                                onClick={handleToggle}
                                disabled={item.disabled}
                                sx={{
                                    minHeight: 42,
                                    px: 1.5,
                                    py: 0,
                                    gap: 1,
                                }}
                            >
                                <ListItemIcon
                                    sx={{
                                        minWidth: 32,
                                        color: 'text.secondary',
                                    }}
                                >
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText
                                    primary={item.label}
                                    primaryTypographyProps={{
                                        noWrap: true,
                                    }}
                                />
                                <Switch
                                    edge="end"
                                    checked={item.checked}
                                    onChange={() => {
                                        handleToggle();
                                    }}
                                    onClick={(event) => {
                                        event.stopPropagation();
                                    }}
                                    sx={{
                                        ml: 1,
                                        flexShrink: 0,
                                    }}
                                />
                            </MenuItem>
                        );
                    }

                    return (
                        <MenuItem
                            key={`${item.label}-${index}`}
                            onClick={() => {
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
                                    noWrap: true,
                                    color: item.isDangerous ? 'error.main' : 'text.primary',
                                }}
                            />
                            {
                                item.endIcon != null &&
                                <Box
                                    sx={{
                                        ml: 1,
                                        color: item.isDangerous ? 'error.main' : 'text.secondary',
                                        display: 'flex',
                                        alignItems: 'center',
                                        flexShrink: 0,
                                    }}
                                >
                                    {item.endIcon}
                                </Box>
                            }
                        </MenuItem>
                    );
                })
            }
        </Menu>
    );
}
