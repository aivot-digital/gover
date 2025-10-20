import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {Box, Button, Chip, createTheme, Divider, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Menu, MenuItem, Paper, Snackbar, ThemeProvider, Typography, useTheme} from '@mui/material';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {selectMinimizeDrawer, setMinimizeDrawer, setShowSearchDialog} from '../../../slices/shell-slice';
import {ShellUserMenu} from './shell-user-menu';
import {ModuleIcons} from '../data/module-icons';
import {Actions} from '../../../components/actions/actions';

import KeyboardTabRtl from '@aivot/mui-material-symbols-400-outlined/dist/keyboard-tab-rtl/KeyboardTabRtl';
import SearchFilled from '@aivot/mui-material-symbols-400-outlined/dist/search/SearchFilled';
import ChevronForward from '@aivot/mui-material-symbols-400-outlined/dist/chevron-forward/ChevronForward';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-outlined/dist/keyboard-arrow-down/KeyboardArrowDown';
import Notifications from '@aivot/mui-material-symbols-400-outlined/dist/notifications/Notifications';
import ForwardToInbox from '@aivot/mui-material-symbols-400-outlined/dist/forward-to-inbox/ForwardToInbox';
import PageInfo from '@aivot/mui-material-symbols-400-outlined/dist/page-info/PageInfo';
import Start from '@aivot/mui-material-symbols-400-outlined/dist/start/Start';
import ShellDrawerLogo from './shell-drawer-logo';
import ShellDrawerUserIcon from './shell-drawer-user-icon';
import SimpleBar from 'simplebar-react';
import OpenInNew from '@aivot/mui-material-symbols-400-outlined/dist/open-in-new/OpenInNew';
import Description from '@aivot/mui-material-symbols-400-outlined/dist/description/Description';

/* -----------------------------
 * Types & Navigation Structure
 * ----------------------------- */
interface DrawerGroup {
    title: string | null;
    items: DrawerItem[];
}

export interface DrawerItem {
    icon: ReactNode;
    label: string;
    to?: string;
    children?: DrawerItem[];
    chipContent?: ReactNode;
    disabled?: boolean;
}

const DrawerGroups: DrawerGroup[] = [
    {
        title: null,
        items: [
            {
                icon: ModuleIcons.dashboard,
                label: 'Übersicht',
                to: '/',
            },
        ],
    },
    {
        title: 'Bearbeitung',
        items: [
            {
                icon: ModuleIcons.tasks,
                label: 'Aufgaben',
                disabled: true,
            },
            {
                icon: ModuleIcons.submissions,
                label: 'Vorgänge',
                to: '/submissions',
                chipContent: 26,
            },
            {
                icon: ModuleIcons.forms,
                label: 'Formulare',
                to: '/forms',
            },
            {
                icon: ModuleIcons.processes,
                label: 'Prozesse',
                disabled: true,
            },
            {
                icon: ModuleIcons.dataObjects,
                label: 'Datenobjekte',
                disabled: true,
            },
        ],
    },
    {
        title: 'Nachnutzung',
        items: [
            {
                icon: ModuleIcons.presets,
                label: 'Vorlagen',
                to: '/presets',
            },
            {
                icon: ModuleIcons.marketplace,
                label: 'Marktplatz',
                disabled: true,
                chipContent: 'NEU',
                children: [
                    {icon: ModuleIcons.departments, label: 'Durchsuchen'},
                ],
            },
        ],
    },
    {
        title: 'Verwaltung',
        items: [
            {
                icon: ModuleIcons.organization,
                label: 'Organisation',
                children: [
                    {icon: ModuleIcons.departments, label: 'Fachbereiche', to: '/departments'},
                    {icon: ModuleIcons.users, label: 'Mitarbeiter:innen', to: '/users'},
                ],
            },
            {icon: ModuleIcons.assets, label: 'Dateien & Medien', to: '/assets'},
            {
                icon: ModuleIcons.dataModels,
                label: 'Datenmodelle',
                to: '/data-objects',
            },
            {
                icon: ModuleIcons.settings,
                label: 'Konfiguration',
                children: [
                    {icon: ModuleIcons.settings, label: 'Allgemeine Einstellungen', to: '/settings/app'},
                    {icon: ModuleIcons.providerLinks, label: 'Links', to: '/provider-links'},
                    {icon: ModuleIcons.themes, label: 'Farbschemata', to: '/themes'},
                    {icon: ModuleIcons.payment, label: 'Zahlungsanbieter', to: '/payment-providers'},
                    {icon: ModuleIcons.identity, label: 'Nutzerkontenanbieter', to: '/identity-providers'},
                    {icon: ModuleIcons.secrets, label: 'Geheimnisse', to: '/secrets'},
                    {icon: <ForwardToInbox />, label: 'SMTP-Test', to: '/settings/smtp'},
                    {icon: <PageInfo />, label: 'Systemstatus', to: '/settings/status'},
                    {
                        icon: <PageInfo />,
                        label: 'Dritte Ebene',
                        children: [
                            {icon: ModuleIcons.departments, label: 'Test 1'},
                            {icon: ModuleIcons.users, label: 'Test 2'},
                        ],
                    },
                ],
            },
        ],
    },
];

/* -----------------------------
 * Main Drawer Component
 * ----------------------------- */
export function ShellDrawer() {
    const baseTheme = useTheme();
    const dispatch = useAppDispatch();
    const minimizeDrawer = useAppSelector(selectMinimizeDrawer) ?? false;
    const [userMenuAnchorEl, setUserMenuAnchorEl] = useState<null | HTMLElement>(null);
    const [showBlockedMsg, setShowBlockedMsg] = useState(false);

    // responsive auto-minimize
    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth < 1450) {
                dispatch(setMinimizeDrawer(true));
            }
        };
        handleResize();
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, [dispatch]);

    const handleToggleDrawer = () => {
        if (minimizeDrawer && window.innerWidth < 1450) {
            setShowBlockedMsg(true);
            return;
        }
        dispatch(setMinimizeDrawer(!minimizeDrawer));
    };

    const handleToggleSearchDialog = () => {
        dispatch(setShowSearchDialog(true));
    };

    const drawerTheme = useMemo(
        () =>
            createTheme({
                ...baseTheme,
                palette: {
                    primary: baseTheme.palette.primary,
                    secondary: baseTheme.palette.secondary,
                },
                components: {
                    MuiTooltip: {
                        styleOverrides: {
                            tooltip: {
                                backgroundColor: 'rgba(255,255,255,0.90)',
                                color: '#111',
                                fontWeight: 500,
                                fontSize: '0.8rem',
                                boxShadow:
                                    '0px 2px 6px rgba(0,0,0,0.25), 0px 4px 12px rgba(0,0,0,0.15)',
                            },
                            arrow: {
                                color: 'rgba(255,255,255,90)',
                            },
                        },
                    },
                },
            }),
        [baseTheme],
    );

    return (
        <ThemeProvider theme={drawerTheme}>
            <Box sx={{display: 'block'}}>
                <Paper
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        height: '100vh',
                        overflowY: 'auto',
                        py: 1.5,

                        borderRadius: 0,
                        width: minimizeDrawer ? '4rem' : '18rem',
                        backgroundColor: 'primary.dark',
                        color: 'rgba(255, 255, 255, 0.8)',
                    }}
                    elevation={1}
                >
                    <Box sx={{display: 'flex', flexDirection: 'column', px: 1.75,}}>
                        {/* Header */}
                        <Box sx={{display: 'flex', flexDirection: 'row', mb: 3}}>
                            <Link
                                to="/"
                                title="Zurück zur Übersicht"
                                style={{display: 'flex', alignItems: 'center', textDecoration: 'none'}}
                            >
                                <ShellDrawerLogo minimize={minimizeDrawer} />
                            </Link>

                            <Actions
                                sx={{ml: 'auto'}}
                                color="inherit"
                                dense
                                actions={[
                                    {
                                        icon: <Notifications />, tooltip: 'Benachrichtigungen', onClick: () => {
                                        },
                                    },
                                    {
                                        icon: <ShellDrawerUserIcon />,
                                        tooltip: 'Mein Konto',
                                        onClick: (event) => setUserMenuAnchorEl(event.currentTarget as HTMLElement),
                                    },
                                ]}
                                direction={minimizeDrawer ? 'column' : 'row'}
                                tooltipPlacement={minimizeDrawer ? 'right' : 'bottom'}
                            />
                        </Box>

                        {/* Search */}
                        <Box sx={{mb: 2}}>
                            {!minimizeDrawer ? (
                                <Button
                                    startIcon={<SearchFilled />}
                                    variant="outlined"
                                    fullWidth
                                    onClick={handleToggleSearchDialog}
                                    color="inherit"
                                    sx={{
                                        justifyContent: 'flex-start',
                                        textAlign: 'left',
                                        background: 'rgba(255, 255, 255, 0.15)',
                                        borderColor: 'rgba(255, 255, 255, 0.2)',
                                        fontWeight: 600,
                                        fontSize: '1rem',
                                        color: 'rgba(255, 255, 255, 0.8)',
                                        textTransform: 'none',
                                        '&:hover': {
                                            background: 'rgba(255, 255, 255, 0.2)',
                                            borderColor: 'rgba(255, 255, 255, 0.25)',
                                            color: 'rgba(255, 255, 255, 1)',
                                        },
                                    }}
                                >
                                    Suche
                                </Button>
                            ) : (
                                <Actions
                                    color="inherit"
                                    direction="column"
                                    actions={[
                                        {
                                            icon: <SearchFilled />,
                                            tooltip: 'Suche',
                                            onClick: handleToggleSearchDialog,
                                        },
                                    ]}
                                    tooltipPlacement="right"
                                />
                            )}
                        </Box>
                    </Box>

                    <Box
                        sx={{
                            flexGrow: 1,
                            display: 'flex',
                            flexDirection: 'column',
                            minHeight: 0,
                            '& .simplebar-scrollbar:before': {
                                backgroundColor: 'rgba(255,255,255,0.4)',
                                left: '3px',
                                right: '3px',
                            },
                        }}
                    >
                        <SimpleBar
                            style={{
                                flexGrow: 1,
                                height: '100%',
                                minHeight: 0,
                                overflowX: 'hidden',
                                padding: '0 14px 14px',
                            }}
                        >
                            {/* Navigation */}
                            {DrawerGroups.map((group, index) => (
                                <DrawerGroup
                                    key={group.title || index}
                                    group={group}
                                    minimizeDrawer={minimizeDrawer}
                                />
                            ))}
                        </SimpleBar>
                    </Box>

                    <Box sx={{flexGrow: 1}}></Box>

                    <Box sx={{display: 'flex', flexDirection: 'column', px: 1.75}}>
                        {/* Footer */}
                        <Divider sx={{borderColor: 'rgba(255, 255, 255, 0.1)', mx: -1.75, mb: 1.75}} />
                        <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                            {!minimizeDrawer && (
                                <Button
                                    variant="contained"
                                    size="small"
                                    href="https://docs.gover.app"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    startIcon={<Description fontSize="small" />}
                                    endIcon={<OpenInNew sx={{ fontSize: '1rem!important', opacity: 0.6 }} />}
                                    sx={{
                                        textTransform: 'none',
                                        color: 'white',
                                        backgroundColor: 'transparent',
                                        '&:hover': {
                                            backgroundColor: 'rgba(255,255,255,0.1)',
                                        },
                                    }}
                                >
                                    Dokumentation
                                </Button>
                            )}
                            <Actions
                                sx={{flex: 0, display: 'flex', justifyContent: 'right'}}
                                color="inherit"
                                direction={minimizeDrawer ? 'column' : 'row'}
                                actions={[
                                    {
                                        tooltip: minimizeDrawer ? 'Maximieren' : 'Minimieren',
                                        icon: minimizeDrawer ? <Start /> : <KeyboardTabRtl />,
                                        onClick: handleToggleDrawer,
                                    },
                                ]}
                                tooltipPlacement={minimizeDrawer ? 'right' : 'top'}
                            />
                        </Box>
                    </Box>
                </Paper>
            </Box>

            <ShellUserMenu
                anchorEl={userMenuAnchorEl}
                onClose={() => setUserMenuAnchorEl(null)}
            />

            <Snackbar
                open={showBlockedMsg}
                autoHideDuration={3000}
                onClose={() => setShowBlockedMsg(false)}
                message="Menü kann nicht maximiert werden: Fenster/Bildschirm zu klein."
            />
        </ThemeProvider>
    );
}

/* -----------------------------
 * DrawerGroup
 * ----------------------------- */
interface DrawerGroupProps {
    group: DrawerGroup;
    minimizeDrawer: boolean;
}

function DrawerGroup({group, minimizeDrawer}: DrawerGroupProps) {
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const [activeItem, setActiveItem] = useState<DrawerItem | null>(null);

    const handleOpenMenu = (event: React.MouseEvent<HTMLElement>, item: DrawerItem) => {
        if (!item.children) return;
        setActiveItem(item);
        setAnchorEl(event.currentTarget);
    };

    const handleCloseMenu = () => {
        setActiveItem(null);
        setAnchorEl(null);
    };

    if (minimizeDrawer) {
        return (
            <>
                <Actions
                    sx={{height: 'auto', mt: 4}}
                    color="inherit"
                    actions={group.items.map((item) =>
                        item.children == null
                            ? {
                                icon: item.icon,
                                tooltip: item.label,
                                to: item.to ?? '',
                                disabled: item.disabled,
                            }
                            : {
                                icon: item.icon,
                                tooltip: item.label,
                                onClick: (e: any) => handleOpenMenu(e, item),
                                disabled: item.disabled,
                            },
                    )}
                    dense
                    direction="column"
                    tooltipPlacement="right"
                />
                <NestedMenu
                    anchorEl={anchorEl}
                    rootItem={activeItem}
                    onClose={handleCloseMenu}
                />
            </>
        );
    }

    return (
        <Box>
            {group.title && (
                <Typography
                    sx={{
                        mt: 1.5,
                        px: 1.25,
                        fontWeight: 700,
                        fontSize: '0.75rem',
                        textTransform: 'uppercase',
                        color: 'white',
                    }}
                >
                    {group.title}
                </Typography>
            )}
            <List
                dense
                sx={{my: 0}}
            >
                {group.items.map((item) => (
                    <DrawerListItem
                        key={item.label}
                        item={item}
                        level={0}
                    />
                ))}
            </List>
        </Box>
    );
}

/* -----------------------------
 * DrawerListItem (recursive)
 * ----------------------------- */
function DrawerListItem({item, level = 0}: { item: DrawerItem; level?: number }) {
    const navigate = useNavigate();
    const location = useLocation();
    const pathname = location.pathname;

    const storageKey = `drawer-item-${item.label}-expanded`;
    const [expanded, setExpanded] = useState<boolean>(() => localStorage.getItem(storageKey) != null);

    useEffect(() => {
        if (expanded) localStorage.setItem(storageKey, 'true');
        else localStorage.removeItem(storageKey);
    }, [expanded, storageKey]);

    const isActive = useMemo(() => {
        if (item.to) {
            if (item.to === '/') {
                return pathname === '/';
            }
            return pathname.startsWith(item.to);
        }

        if (item.children) {
            return item.children.some((c) => {
                if (c.to === '/') return pathname === '/';
                return c.to ? pathname.startsWith(c.to) : false;
            });
        }

        return false;
    }, [pathname, item]);


    const handleClick = () => {
        if (item.children) setExpanded((e) => !e);
        else if (item.to) navigate(item.to);
    };

    const labelSizeScale =
        level === 0 ? {fontSize: '1rem', fontWeight: 600} :
            {fontSize: '0.8rem', fontWeight: 600};
    const iconSizeScale =
        level === 0 ? {'& .MuiSvgIcon-root': {width: 24, fontSize: '1.5rem'}} :
            {'& .MuiSvgIcon-root': {width: 20, fontSize: '1.25rem'}};

    const activeStyles =
        level === 0
            ? {
                backgroundColor: expanded && !isActive
                    ? 'rgba(255,255,255,0.1)'
                    : isActive ? 'secondary.main' : undefined,
                '& .MuiListItemText-primary, & .MuiListItemIcon-root, .toggle-icon': {
                    color: isActive ? 'primary.dark' : 'rgba(255,255,255,0.8)',
                },
                '&:hover': {
                    backgroundColor: expanded && !isActive
                        ? 'rgba(255,255,255,0.1)'
                        : isActive ? 'secondary.main' : 'rgba(255, 255, 255, 0.1)',
                    '& .MuiListItemIcon-root, .MuiListItemText-primary, .toggle-icon': {
                        color: isActive ? 'primary.main' : 'rgba(255,255,255,1)',
                    },
                },
            }
            : {
                backgroundColor: isActive ? 'rgba(255,255,255,0.1)' : undefined,
                '& .MuiListItemText-primary, & .MuiListItemIcon-root, .toggle-icon': {
                    color: isActive ? 'secondary.main' : 'rgba(255,255,255,0.75)',
                },
                '&:hover': {
                    backgroundColor: 'rgba(255,255,255,0.1)',
                    '& .MuiListItemIcon-root, .MuiListItemText-primary, .toggle-icon': {
                        color: isActive ? 'secondary.main' : 'rgba(255,255,255,1)',
                    },
                },
            };

    return (
        <>
            <ListItem
                dense
                disableGutters
                sx={{py: 0.3125}}
            >
                <ListItemButton
                    component={item.to && !item.disabled ? Link : 'div'}
                    to={item.to && !item.disabled ? item.to : undefined}
                    onClick={!item.disabled ? handleClick : undefined}
                    disabled={item.disabled}
                    sx={{
                        position: 'relative',
                        px: 1,
                        ml: level * 4,
                        borderRadius: 1,
                        opacity: item.disabled ? '0.5!important' : 1,
                        cursor: item.disabled ? 'not-allowed' : 'pointer',
                        pointerEvents: item.disabled ? 'none' : 'auto',
                        ...activeStyles,
                        '& .MuiListItemIcon-root': {
                            minWidth: 'auto',
                            textAlign: 'center',
                            mr: 1,
                            ...iconSizeScale,
                        },
                        '& .MuiListItemText-primary': {
                            ...labelSizeScale,
                        },
                        ...(level > 0
                            ? {
                                '&::before': {
                                    left: 0,
                                    content: '""',
                                    position: 'absolute',
                                    width: '13px',
                                    height: '13px',
                                    backgroundColor: 'primary.light',
                                    transform: 'translate(calc(13px * -1), calc(13px * -0.4))',
                                    mask: "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' fill='none' viewBox='0 0 14 14'%3E%3Cpath d='M1 1v4a8 8 0 0 0 8 8h4' stroke='%23efefef' stroke-width='2' stroke-linecap='round'/%3E%3C/svg%3E\") 50% 50% / 100% no-repeat",
                                },
                            }
                            : {}),
                    }}
                >
                    <ListItemIcon>
                        {
                            item.icon ?? <PageInfo />
                        }
                    </ListItemIcon>
                    <ListItemText primary={item.label} />
                    {item.chipContent != null && (
                        <Chip
                            label={item.chipContent}
                            size="small"
                            sx={{
                                ml: 'auto',
                                fontWeight: 600,
                                fontSize: '0.75rem',
                                height: 24,
                                borderRadius: '9999px',
                                px: 0.75,
                                color: isActive
                                    ? 'primary.dark'
                                    : 'rgba(255,255,255,0.90)',
                                backgroundColor: isActive
                                    ? 'rgba(0,0,0,0.10)'
                                    : 'rgba(255,255,255,0.10)',
                                '& .MuiChip-label': {
                                    px: 0.75,
                                    lineHeight: '1rem',
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 0.25,
                                },
                            }}
                        />
                    )}
                    <Box className={'toggle-icon'} sx={{display: 'flex', alignItems: 'center'}}>
                        {item.children &&
                            (expanded ? <KeyboardArrowDown sx={{ml: 0.5}}/> : <ChevronForward sx={{ml: 0.5}} />)
                        }
                    </Box>
                </ListItemButton>
            </ListItem>
            {item.children && expanded && (
                <List
                    dense
                    disablePadding
                    sx={{
                        position: 'relative',
                        py: 0,
                        my: 0,
                        '&::before': {
                            top: '-2px',
                            left: level * 30 + 19,
                            width: '2px',
                            content: '""',
                            position: 'absolute',
                            backgroundColor: 'primary.light',
                            bottom: 'calc(36px - 2px - 14px / 2)',
                        },
                    }}
                >
                    {item.children.map((child) => (
                        <DrawerListItem
                            key={child.label}
                            item={child}
                            level={level + 1}
                        />
                    ))}
                </List>
            )}
        </>
    );
}

/* -----------------------------
 * NestedMenu for minimized mode
 * ----------------------------- */
function NestedMenu({
                        anchorEl,
                        rootItem,
                        onClose,
                    }: {
    anchorEl: HTMLElement | null;
    rootItem: DrawerItem | null;
    onClose: () => void;
}) {
    const open = Boolean(anchorEl) && !!rootItem?.children && !rootItem.disabled;
    if (!rootItem) return null;

    return (
        <Menu
            anchorEl={anchorEl}
            open={open}
            onClose={onClose}
            anchorOrigin={{vertical: 'bottom', horizontal: 'right'}}
            transformOrigin={{vertical: 'top', horizontal: 'left'}}
            MenuListProps={{dense: true}}
            disableAutoFocusItem
        >
            {rootItem.children?.map((child) => (
                <NestedMenuItem
                    key={child.label}
                    item={child}
                    onAnyClose={onClose}
                />
            ))}
        </Menu>
    );
}

function NestedMenuItem({
                            item,
                            onAnyClose,
                        }: {
    item: DrawerItem;
    onAnyClose: () => void;
}) {
    const navigate = useNavigate();
    const [submenuAnchor, setSubmenuAnchor] = useState<HTMLElement | null>(null);
    const hasChildren = !!item.children?.length;

    const handleItemClick = (e: React.MouseEvent<HTMLElement>) => {
        e.stopPropagation();

        if (item.disabled) {
            return;
        }

        if (hasChildren) {
            // toggle submenu open/close on click
            if (submenuAnchor) {
                setSubmenuAnchor(null);
            } else {
                setSubmenuAnchor(e.currentTarget);
            }
        } else if (item.to) {
            navigate(item.to);
            onAnyClose(); // close all menus after navigation
        }
    };

    const handleCloseSubmenu: NonNullable<React.ComponentProps<typeof Menu>['onClose']> = () => {
        setSubmenuAnchor(null);
    };

    return (
        <>
            <MenuItem
                onClick={handleItemClick}
                sx={{
                    minWidth: 220,
                    gap: 1,
                    cursor: 'pointer',
                    '&:hover': {backgroundColor: 'rgba(255,255,255,0.08)'},
                }}
            >
                <Box sx={{width: 24, display: 'inline-flex', justifyContent: 'center'}}>
                    {item.icon ?? <PageInfo />}
                </Box>
                <Box sx={{flex: 1}}>{item.label}</Box>
                {hasChildren && <ChevronForward />}
            </MenuItem>

            {hasChildren && (
                <Menu
                    anchorEl={submenuAnchor}
                    open={Boolean(submenuAnchor)}
                    onClose={handleCloseSubmenu}
                    anchorOrigin={{vertical: 'top', horizontal: 'right'}}
                    transformOrigin={{vertical: 'top', horizontal: 'left'}}
                    MenuListProps={{dense: true}}
                    disableAutoFocusItem
                >
                    {item.children!.map((child) => (
                        <NestedMenuItem
                            key={child.label}
                            item={child}
                            onAnyClose={onAnyClose}
                        />
                    ))}
                </Menu>
            )}
        </>
    );
}


