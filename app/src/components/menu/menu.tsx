import {MouseEventHandler, ReactNode} from "react";
import {Divider, ListItemIcon, ListItemText, Menu as MuiMenu, MenuItem as MuiMenuItem} from "@mui/material";
import {Link} from "react-router-dom";

type MenuSeparator = 'separator';

type MenuItemBase = {
    label?: string;
    icon: ReactNode;
    disabled?: boolean;
    visible?: boolean;
}

type ClickMenuItem = MenuItemBase & {
    onClick: MouseEventHandler;
}

type LinkMenuItem = MenuItemBase & {
    href: string;
}

type InternalLinkMenuItem = MenuItemBase & {
    to: string;
}

export type MenuItem = MenuSeparator | ClickMenuItem | LinkMenuItem | InternalLinkMenuItem;

interface MenuProps {
    anchorEl: HTMLElement | null;

    open: boolean;
    onClose: () => void;

    disabled?: boolean;

    items: MenuItem[];
}


export function Menu(props: MenuProps) {
    const {
        anchorEl,
        open,
        onClose,
        disabled,
        items,
    } = props;

    return (
        <MuiMenu
            open={open}
            anchorEl={anchorEl}
            onClose={onClose}
        >
            {
                items.map((item, index) => {
                    if (item === 'separator') {
                        return <Divider/>;
                    }

                    if (item.visible === false) {
                        return null;
                    }

                    const key = item.label ?? index;

                    if ('onClick' in item) {
                        return (
                            <MuiMenuItem
                                key={key}
                                onClick={(e) => {
                                    item.onClick(e);
                                    onClose();
                                }}
                                disabled={disabled || item.disabled}
                            >
                                <ListItemIcon>
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText>
                                    {item.label}
                                </ListItemText>
                            </MuiMenuItem>
                        );
                    }

                    if ('href' in item) {
                        return (
                            <MuiMenuItem
                                key={key}
                                component="a"
                                href={item.href}
                                disabled={disabled || item.disabled}
                                onClick={onClose}
                            >
                                <ListItemIcon>
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText>
                                    {item.label}
                                </ListItemText>
                            </MuiMenuItem>
                        );
                    }

                    if ('to' in item) {
                        return (
                            <MuiMenuItem
                                key={key}
                                component={Link}
                                to={item.to}
                                disabled={disabled || item.disabled}
                                onClick={onClose}
                            >
                                <ListItemIcon>
                                    {item.icon}
                                </ListItemIcon>
                                <ListItemText>
                                    {item.label}
                                </ListItemText>
                            </MuiMenuItem>
                        );
                    }

                    return null;
                })
            }
        </MuiMenu>
    );
}