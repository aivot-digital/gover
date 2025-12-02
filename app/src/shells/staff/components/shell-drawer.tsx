import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {Badge, Box, Button, Chip, createTheme, Divider, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Menu, MenuItem, Paper, Snackbar, ThemeProvider, Typography, useTheme} from '@mui/material';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {selectMinimizeDrawer, selectShowAboutGoverDialog, setMinimizeDrawer, setShowAboutGoverDialog, setShowSearchDialog} from '../../../slices/shell-slice';
import {ShellUserMenu} from './shell-user-menu';
import {ModuleIcons} from '../data/module-icons';
import {Actions} from '../../../components/actions/actions';
import {useHotkeys} from 'react-hotkeys-hook';
import {formatShortcut} from '../../../utils/format-shortcut';

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
import {AboutGoverDialog} from './about-gover-dialog';
import {ShellNotificationsMenu} from './shell-notifications-menu';
import Api from '@aivot/mui-material-symbols-400-outlined/dist/api/Api';
import ReadinessScore from '@aivot/mui-material-symbols-400-outlined/dist/readiness-score/ReadinessScore';

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
                chipContent: 26,
            },
            {
                icon: ModuleIcons.submissions,
                label: 'Vorgänge',
                disabled: true,
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
                to: '/data-objects',
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
                    {icon: ModuleIcons.departments, label: 'Fachbereiche (Baum)', to: '/departments-tree'},
                    {icon: ModuleIcons.users, label: 'Mitarbeiter:innen', to: '/users'},
                    {icon: ModuleIcons.teams, label: 'Teams', to: '/teams'},
                    {icon: ModuleIcons.roles, label: 'Rollen', to: '/user-roles'},
                ],
            },
            {icon: ModuleIcons.assets, label: 'Dateien & Medien', to: '/assets'},
            {
                icon: ModuleIcons.dataModels,
                label: 'Datenmodelle',
                to: '/data-models',
            },
            {
                icon: ModuleIcons.settings,
                label: 'Konfiguration',
                children: [
                    {icon: ModuleIcons.settings, label: 'Allgemeine Einstellungen', to: '/settings/app'},
                    {icon: <ReadinessScore />, label: 'Systeminformationen', to: '/settings/status'},
                    {icon: ModuleIcons.themes, label: 'Erscheinungsbild', to: '/themes'},
                    {icon: ModuleIcons.secrets, label: 'Systemvariablen', to: '/secrets'},
                    {
                        icon: <Api />,
                        label: 'Anbindungen',
                        children: [
                            {icon: ModuleIcons.identity, label: 'Identitätsanbieter', to: '/identity-providers'},
                            {icon: ModuleIcons.payment, label: 'Zahlungsanbieter', to: '/payment-providers'},
                        ],
                    },
                    {icon: ModuleIcons.extensions, label: 'Erweiterungen', to: '/settings/extensions'},
                    {icon: <ForwardToInbox />, label: 'SMTP-Test (legacy)', to: '/settings/smtp'},
                    {icon: ModuleIcons.providerLinks, label: 'Links (legacy)', to: '/provider-links'},
                    {icon: ModuleIcons.destinations, label: 'Schnittstellen (legacy)', to: '/destinations'},

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
    const [notificationsAnchorEl, setNotificationsAnchorEl] = useState<null | HTMLElement>(null);
    const [showBlockedMsg, setShowBlockedMsg] = useState(false);
    const showAboutGoverDialog = useAppSelector(selectShowAboutGoverDialog) ?? false;

    // responsive auto-minimize
    useEffect(() => {
        const handleResize = () => {
            if (window.innerWidth < 1480) {
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
                    ...baseTheme.palette,
                    primary: baseTheme.palette.primary,
                    secondary: baseTheme.palette.secondary,
                },
                components: {
                    ...baseTheme.components,
                    MuiTooltip: {
                        styleOverrides: {
                            tooltip: {
                                backgroundColor: 'rgba(255,255,255,1)',
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

    const shortcutLabel = formatShortcut(['meta'], 'k');
    useHotkeys(
        'meta+k, ctrl+k',
        (event) => {
            event.preventDefault();
            dispatch(setShowSearchDialog(true));
        },
        { enableOnFormTags: false }
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
                        width: minimizeDrawer ? '4.25rem' : '16.25rem',
                        backgroundColor: 'primary.dark',
                        color: 'rgba(255, 255, 255, 0.8)',
                    }}
                    elevation={1}
                >
                    <Box sx={{display: 'flex', flexDirection: 'column', px: 1.75,}}>
                        {/* Header */}
                        <Box sx={{display: 'flex', flexDirection: minimizeDrawer ? 'column' : 'row', mb: 3}}>
                            <Link
                                to="/"
                                title="Zurück zur Übersicht"
                                style={{display: 'flex', alignItems: 'center', textDecoration: 'none'}}
                            >
                                <ShellDrawerLogo minimize={minimizeDrawer} />
                            </Link>

                            {!minimizeDrawer && (
                                <ShellDrawerUserActions minimizeDrawer={minimizeDrawer} setUserMenuAnchorEl={setUserMenuAnchorEl} setNotificationsAnchorEl={setNotificationsAnchorEl} />
                            )}
                        </Box>

                        {/* Search */}
                        <Box sx={{mb: minimizeDrawer ? 0 : 2}}>
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
                                    <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center' }}>
                                        <span>Suche</span>
                                        <Box
                                            sx={{
                                                display: 'inline-flex',
                                                alignItems: 'center',
                                                gap: 0.5,
                                                px: 0.5,
                                                py: 0,
                                                borderRadius: 1,
                                                fontSize: '0.75rem',
                                                fontWeight: 600,
                                                background: 'rgba(255,255,255,.15)',
                                                color: 'rgba(255,255,255,0.8)',
                                                transform: 'translateX(7px) translateY(-1px)',
                                            }}
                                            title={"Tastenkürzel zum Öffnen der Suche (" + shortcutLabel + ")"}
                                        >
                                            {shortcutLabel}
                                        </Box>
                                    </Box>
                                </Button>
                            ) : (
                                <Actions
                                    sx={{
                                        '& .MuiIconButton-root': {
                                            borderRadius: 1,
                                            background: 'rgba(255, 255, 255, 0.15)',
                                            border: '1px solid rgba(255, 255, 255, 0.2)',
                                            color: 'rgba(255, 255, 255, 0.8)',
                                        },
                                        '& .MuiIconButton-root:hover': {
                                            background: 'rgba(255, 255, 255, 0.2)',
                                            border: '1px solid rgba(255, 255, 255, 0.25)',
                                            color: 'rgba(255, 255, 255, 1)',

                                        },
                                    }}
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
                        <Box sx={{display: 'flex', flexDirection: minimizeDrawer ? 'column' : 'row', justifyContent: 'space-between', alignItems: 'center'}}>
                            {!minimizeDrawer && (
                                <Button
                                    variant="contained"
                                    size="small"
                                    href="https://docs.gover.digital"
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
                            {minimizeDrawer && (
                                <>
                                    <ShellDrawerUserActions minimizeDrawer={minimizeDrawer} setUserMenuAnchorEl={setUserMenuAnchorEl} setNotificationsAnchorEl={setNotificationsAnchorEl} />
                                    <Box sx={{height: 10}}/>
                                </>
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

            <ShellNotificationsMenu
                minimizeDrawer={minimizeDrawer}
                anchorEl={notificationsAnchorEl}
                onClose={() => setNotificationsAnchorEl(null)}
            />

            <ShellUserMenu
                minimizeDrawer={minimizeDrawer}
                anchorEl={userMenuAnchorEl}
                onClose={() => setUserMenuAnchorEl(null)}
            />

            <Snackbar
                open={showBlockedMsg}
                autoHideDuration={3000}
                onClose={() => setShowBlockedMsg(false)}
                message="Menü kann nicht maximiert werden: Fenster/Bildschirm zu klein."
            />

            <AboutGoverDialog
                open={showAboutGoverDialog}
                onClose={() => dispatch(setShowAboutGoverDialog(false))}
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

    const location = useLocation();
    const pathname = location.pathname;

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
        const actionActiveStyle = {
            color: 'primary.dark',
            backgroundColor: 'secondary.main',
            '&.MuiIconButton-root:hover': {
                color: 'primary.dark',
                backgroundColor: 'secondary.main',
            }
        };

        return (
            <>
                <Actions
                    sx={{
                        height: 'auto',
                        mt: 4,
                        '& .MuiIconButton-root': {
                            borderRadius: 1,
                        },
                        '& .MuiIconButton-root:hover': {
                            color: 'white',
                            backgroundColor: 'rgba(255,255,255,0.1)',

                        },
                        '& .Mui-disabled.MuiIconButton-root': {
                            color: 'rgba(255, 255, 255, 0.8)!important',
                            opacity: '0.5!important',
                        },
                    }}
                    color="inherit"
                    actions={group.items.map((item) =>
                        item.children == null
                            ? {
                                icon: item.icon,
                                tooltip: item.label,
                                to: item.to ?? '',
                                disabled: item.disabled,
                                activeStyle: isDrawerItemActive(item, pathname) ? actionActiveStyle : {},
                            }
                            : {
                                icon: item.icon,
                                tooltip: item.label,
                                onClick: (e: any) => handleOpenMenu(e, item),
                                disabled: item.disabled,
                                activeStyle: isDrawerItemActive(item, pathname) ? actionActiveStyle : {},
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

    const isActive = useMemo(() => isDrawerItemActive(item, pathname), [pathname, item]);

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
                                    width: '14px',
                                    height: '14px',
                                    backgroundColor: 'primary.light',
                                    transform: 'translate(calc(13px * -1), calc(13px * -0.4))',
                                    mask: "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' fill='none' viewBox='0 0 14 14'%3E%3Cpath d='M1 1v4a8 8 0 0 0 8 8h4' stroke='%23efefef' stroke-width='2' stroke-linecap='round'/%3E%3C/svg%3E\") 50% 50% / 100% no-repeat",
                                },
                            }
                            : {}),
                        ...(level > 1
                            ? {
                                // Fix positioning of the nested line connectors
                                transform: 'translateX(-2px)',
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


function ShellDrawerUserActions(props: {minimizeDrawer: boolean, setUserMenuAnchorEl: (el: HTMLElement) => void, setNotificationsAnchorEl: (el: HTMLElement) => void}) {
    const {minimizeDrawer, setUserMenuAnchorEl, setNotificationsAnchorEl} = props;
    return (
        <Actions
            sx={{
                ml: 'auto',
                gap: minimizeDrawer ? 1 : 0.5,
            }}
            color="inherit"
            dense
            actions={[
                {
                    icon: <Badge
                        color="secondary"
                        variant="dot"
                        overlap="circular"
                        badgeContent=" "
                        invisible={false}
                        sx={{'& .MuiBadge-badge': {top: 5, right: 5, borderColor: 'primary.dark', borderWidth: 2, borderStyle: 'solid', transform: 'scale(1.5) translate(50%, -50%)'}}}
                    >
                        <Notifications />
                    </Badge>,
                    tooltip: 'Benachrichtigungen',
                    onClick: (event) => setNotificationsAnchorEl(event.currentTarget as HTMLElement),
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
    )
}

function isDrawerItemActive(item: DrawerItem, pathname: string): boolean {
    if (item.to) {
        if (item.to === '/') return pathname === '/';
        return pathname.startsWith(item.to);
    }

    if (item.children) {
        return item.children.some((c) => isDrawerItemActive(c, pathname));
    }

    return false;
}