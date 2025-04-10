import React, {useEffect, useRef, useState} from 'react';
import {type AppToolbarProps, AppToolbarAction} from './app-toolbar-props';
import {AppBar, Box, Button, IconButton, styled, Toolbar, Tooltip, Typography} from '@mui/material';
import {Link, useNavigate} from 'react-router-dom';
import HomeOutlinedIcon from '@mui/icons-material/HomeOutlined';
import {getPath} from '../../apps/staff-app-routes';
import MenuOutlinedIcon from "@mui/icons-material/MenuOutlined";
import {AppHeaderMenu} from "../app-header-menu/app-header-menu";
import {AppMode} from "../../data/app-mode";

/**
 * Default toolbar of the application.
 * Renders basic navigation as well as a title and actions.
 */
export function AppToolbar(props: AppToolbarProps): JSX.Element {
    const {updateToolbarHeight} = props;
    const Offset = styled('div')(({theme}) => theme.mixins.toolbar);

    const offsetRef = useRef<HTMLDivElement>(null);

    const [menuAnchorEl, setMenuAnchorEl] = useState<HTMLElement>();
    const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
        setMenuAnchorEl(event.currentTarget);
    };
    const handleCloseMenu = () => {
        setMenuAnchorEl(undefined);
    };

    // Create a listener for height changes
    useEffect(() => {
        const updateHeight = () => {
            if (offsetRef.current && updateToolbarHeight) {
                updateToolbarHeight(offsetRef.current.offsetHeight);
            }
        };

        updateHeight();

        window.addEventListener('resize', updateHeight);

        return () => {
            window.removeEventListener('resize', updateHeight);
        };
    }, [updateToolbarHeight]);

    return (
        <>
            <AppBar position="fixed">
                <Toolbar color="primary">
                    <Tooltip title="Zur Startseite">
                        <IconButton
                            size="small"
                            edge="start"
                            component={Link}
                            to={getPath('moduleSelect')}
                            color="inherit"
                        >
                            <HomeOutlinedIcon />
                        </IconButton>
                    </Tooltip>


                    <Tooltip title="Navigationsmenü anzeigen">
                        <IconButton
                            size="small"
                            edge="start"
                            onClick={handleOpenMenu}
                            color="inherit"
                            sx={{
                                ml: 1,
                            }}
                        >
                            <MenuOutlinedIcon />
                        </IconButton>
                    </Tooltip>

                    <Typography
                        variant="h5"
                        component="div"
                        sx={{
                            flexGrow: 1,
                            ml: 2,
                        }}
                    >
                        {props.title}
                    </Typography>

                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        {
                            props.actions != null &&
                            props.actions.map(dispatchToolbarAction)
                        }
                    </Box>
                </Toolbar>
            </AppBar>
            <Offset ref={offsetRef} />
            {
                (menuAnchorEl != null) &&
                <AppHeaderMenu
                    mode={AppMode.Staff}
                    onClose={handleCloseMenu}
                    anchorElement={menuAnchorEl}
                />
            }
        </>
    );
}

function dispatchToolbarAction(action: AppToolbarAction, index: number) {
    // Check if this action is a separator and render a simple separator div
    if (action === 'separator') {
        return (
            <Box
                key={action + index}
                sx={{
                    ml: 2,
                    width: '1px',
                    height: '2em',
                    backgroundColor: 'white',
                    opacity: '.25',
                }}
            />
        );
    }

    // Check if visible is explicitly set to false. This prevents not rendering, when visible is undefined
    if (action.visible === false) {
        return null;
    }

    // Determine the component to render, either a button or a link
    const component = 'onClick' in action ? 'button' : 'a';

    // Determine shared properties
    const href = 'href' in action ? action.href : undefined;
    const target = 'href' in action ? '_blank' : undefined;
    const onClick = 'onClick' in action ? action.onClick : undefined;

    // Build the element for this action which will then be encapsulated in a tooltip
    let element;
    if (action.label != null) {
        element = (
            <Button
                size="small"
                color="inherit"
                sx={{
                    ml: 2,
                }}
                onClick={onClick}
                component={component}
                href={href}
                target={target}
                startIcon={action.icon}
                disabled={action.disabled}
            >
                {action.label}
            </Button>
        );
    } else {
        element = (
            <IconButton
                size="small"
                color="inherit"
                sx={{
                    ml: 2,
                }}
                onClick={onClick}
                component={component}
                href={href}
                target={target}
                disabled={action.disabled}
            >
                {action.icon}
            </IconButton>
        );
    }

    return (
        <Tooltip
            key={index}
            title={action.tooltip}
            arrow
        >
            <span>{element}</span>
        </Tooltip>
    );
}