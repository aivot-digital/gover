import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {Badge, Box, Button, Chip, createTheme, Divider, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Menu, MenuItem, Paper, Snackbar, ThemeProvider, Typography, useTheme} from '@mui/material';
import {Link, useLocation} from 'react-router-dom';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {selectMinimizeDrawer, selectShowAboutGoverDialog, setMinimizeDrawer, setShowAboutGoverDialog, setShowSearchDialog} from '../../../slices/shell-slice';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {ShellUserMenu} from './shell-user-menu';
import {ModuleIcons, ModuleIconsFilled} from '../data/module-icons';
import {Actions} from '../../../components/actions/actions';
import {useHotkeys} from 'react-hotkeys-hook';
import {formatShortcut} from '../../../utils/format-shortcut';

import KeyboardTabRtl from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardTabRtl';
import SearchFilled from '@aivot/mui-material-symbols-400-n25-outlined/SearchFilled';
import ChevronForward from '@aivot/mui-material-symbols-400-n25-outlined/ChevronForward';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import Notifications from '@aivot/mui-material-symbols-400-n25-outlined/Notifications';
import ForwardToInbox from '@aivot/mui-material-symbols-400-n25-outlined/ForwardToInbox';
import PageInfo from '@aivot/mui-material-symbols-400-n25-outlined/PageInfo';
import Start from '@aivot/mui-material-symbols-400-n25-outlined/Start';
import ShellDrawerLogo from './shell-drawer-logo';
import ShellDrawerUserIcon from './shell-drawer-user-icon';
import SimpleBar from 'simplebar-react';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import Description from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import {AboutGoverDialog} from './about-gover-dialog';
import {ShellNotificationsMenu} from './shell-notifications-menu';
import Api from '@aivot/mui-material-symbols-400-n25-outlined/Api';
import ApiFilled from '@aivot/mui-material-symbols-400-n25-outlined/ApiFilled';
import ReadinessScore from '@aivot/mui-material-symbols-400-n25-outlined/ReadinessScore';
import ReadinessScoreFilled from '@aivot/mui-material-symbols-400-n25-outlined/ReadinessScoreFilled';
import FamilyHistory from '@aivot/mui-material-symbols-400-n25-outlined/FamilyHistory';
import FamilyHistoryFilled from '@aivot/mui-material-symbols-400-n25-outlined/FamilyHistoryFilled';
import SupervisedUserCircle from '@aivot/mui-material-symbols-400-n25-outlined/SupervisedUserCircle';
import SupervisedUserCircleFilled from '@aivot/mui-material-symbols-400-n25-outlined/SupervisedUserCircleFilled';
import ForwardToInboxFilled from '@aivot/mui-material-symbols-400-n25-outlined/ForwardToInboxFilled';
import {StorageProvidersApiService} from '../../../modules/storage/storage-providers-api-service';
import {StorageProviderType} from '../../../modules/storage/enums/storage-provider-type';
import {selectPermissions, selectUser} from '../../../slices/user-slice';
import {AUDIT_LOG_READ_PERMISSION} from '../../../modules/audit/constants/audit-permissions';
import {ProcessInstanceTaskApiService} from '../../../modules/process/services/process-instance-task-api-service';
import {subscribeProcessAssignedTaskCountRefreshEvent} from '../../../modules/process/utils/process-assigned-task-count-events';

export const COLLAPSED_DRAWER_WIDTH_REM = '4.25rem';
export const EXPANDED_DRAWER_WIDTH_REM = '16.25rem';

/* -----------------------------
 * Types & Navigation Structure
 * ----------------------------- */
interface DrawerGroup {
    title: string | null;
    items: DrawerItem[];
}

export interface DrawerItem {
    icon: ReactNode;
    activeIcon?: ReactNode;
    label: string;
    to?: string;
    children?: DrawerItem[];
    chipContent?: ReactNode;
    disabled?: boolean;
    requiredSystemPermission?: string;
}

const drawerModuleIcon = (name: keyof typeof ModuleIcons): Pick<DrawerItem, 'icon' | 'activeIcon'> => ({
    icon: ModuleIcons[name],
    activeIcon: ModuleIconsFilled[name],
});

const drawerIcon = (icon: ReactNode, activeIcon: ReactNode): Pick<DrawerItem, 'icon' | 'activeIcon'> => ({
    icon,
    activeIcon,
});

const BaseDrawerGroups: DrawerGroup[] = [
    {
        title: null,
        items: [
            {
                ...drawerModuleIcon('dashboard'),
                label: 'Übersicht',
                to: '/',
            },
        ],
    },
    {
        title: 'Bearbeitung',
        items: [
            {
                ...drawerModuleIcon('tasks'),
                label: 'Aufgaben',
                to: '/tasks',
            },
            {
                ...drawerModuleIcon('submissions'),
                label: 'Vorgänge',
                to: '/process-instances',
            },
            {
                ...drawerModuleIcon('processes'),
                label: 'Prozesse',
                to: '/processes',
            },
            {
                ...drawerModuleIcon('forms'),
                label: 'Formulare',
                to: '/forms',
            },
            {
                ...drawerModuleIcon('dataObjects'),
                label: 'Datenobjekte',
                to: '/data-objects',
            },
        ],
    },
    {
        title: 'Nachnutzung',
        items: [
            {
                ...drawerModuleIcon('presets'),
                label: 'Vorlagen',
                to: '/presets',
                disabled: true,
            },
            {
                ...drawerModuleIcon('marketplace'),
                label: 'Marktplatz',
                disabled: true,
                children: [
                    {...drawerModuleIcon('departments'), label: 'Durchsuchen'},
                ],
            },
            {
                icon: ModuleIcons.codeLists,
                label: 'Codelisten',
                to: '/code-lists',
            },
        ],
    },
    {
        title: 'Verwaltung',
        items: [
            {
                ...drawerModuleIcon('organization'),
                label: 'Organisation',
                children: [
                    {...drawerModuleIcon('departments'), label: 'Organisationseinheiten', to: '/departments'},
                    {...drawerModuleIcon('teams'), label: 'Teams', to: '/teams'},
                    {...drawerModuleIcon('users'), label: 'Mitarbeiter:innen', to: '/users'},
                    {
                        ...drawerIcon(<SupervisedUserCircle />, <SupervisedUserCircleFilled />),
                        label: 'Rollenverwaltung',
                        children: [
                            {...drawerModuleIcon('roles'), label: 'Domänenrollen', to: '/user-roles'},
                            {...drawerModuleIcon('roles'), label: 'Systemrollen', to: '/system-roles'},
                        ],
                    },
                    {...drawerIcon(<FamilyHistory/>, <FamilyHistoryFilled/>), label: 'Organigramm', to: '/organization-chart'},
                ],
            },
            {...drawerModuleIcon('assets'), label: 'Dateien & Medien', to: '/assets'},
            {
                ...drawerModuleIcon('dataModels'),
                label: 'Datenmodelle',
                to: '/data-models',
            },
            {
                ...drawerModuleIcon('settings'),
                label: 'Konfiguration',
                children: [
                    {...drawerModuleIcon('settings'), label: 'Allgemeine Einstellungen', to: '/settings/app'},
                    {...drawerIcon(<ReadinessScore />, <ReadinessScoreFilled />), label: 'Systeminformationen', to: '/settings/status'},
                    {
                        ...drawerModuleIcon('audit'),
                        label: 'Audit-Log',
                        to: '/audit-log',
                        requiredSystemPermission: AUDIT_LOG_READ_PERMISSION,
                    },
                    {...drawerModuleIcon('themes'), label: 'Erscheinungsbild', to: '/themes'},
                    {...drawerModuleIcon('secrets'), label: 'Systemvariablen', to: '/secrets'},
                    {
                        ...drawerIcon(<Api />, <ApiFilled />),
                        label: 'Anbindungen',
                        children: [
                            {...drawerModuleIcon('identity'), label: 'Identitätsanbieter', to: '/identity-providers'},
                            {...drawerModuleIcon('payment'), label: 'Zahlungsanbieter', to: '/payment-providers'},
                            {...drawerModuleIcon('storage'), label: 'Speicheranbieter', to: '/storage-providers'},
                            // {...drawerModuleIcon('destinations'), label: 'Schnittstellen', to: '/destinations'},
                        ],
                    },
                    {...drawerModuleIcon('extensions'), label: 'Erweiterungen', to: '/settings/extensions'},
                    {...drawerIcon(<ForwardToInbox />, <ForwardToInboxFilled />), label: 'SMTP-Test (legacy)', to: '/settings/smtp'},
                    {...drawerModuleIcon('providerLinks'), label: 'Links (legacy)', to: '/provider-links'},
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
    const permissions = useAppSelector(selectPermissions);
    const user = useAppSelector(selectUser);
    const minimizeDrawer = useAppSelector(selectMinimizeDrawer) ?? false;
    const [userMenuAnchorEl, setUserMenuAnchorEl] = useState<null | HTMLElement>(null);
    const [notificationsAnchorEl, setNotificationsAnchorEl] = useState<null | HTMLElement>(null);
    const [showBlockedMsg, setShowBlockedMsg] = useState(false);
    const showAboutGoverDialog = useAppSelector(selectShowAboutGoverDialog) ?? false;
    const [assetStorageProviderItems, setAssetStorageProviderItems] = useState<DrawerItem[]>([]);
    const [isLoadingAssetStorageProviders, setIsLoadingAssetStorageProviders] = useState(true);
    const [assignedTaskCount, setAssignedTaskCount] = useState<number | null>(null);

    useEffect(() => {
        setIsLoadingAssetStorageProviders(true);

        new StorageProvidersApiService()
            .listAll({
                type: StorageProviderType.Assets,
            })
            .then(({content: providers}) => {
                const providerItems = providers
                    .slice()
                    .sort((a, b) => a.name.localeCompare(b.name, 'de'))
                    .map((provider) => ({
                        ...drawerModuleIcon('storage'),
                        label: provider.name,
                        to: `/assets/providers/${provider.id}`,
                    }));

                setAssetStorageProviderItems(providerItems);
            })
            .catch((err) => {
                setAssetStorageProviderItems([]);
                dispatch(showApiErrorSnackbar(err, 'Die Liste der Asset-Speicheranbieter konnte nicht geladen werden.'));
            })
            .finally(() => {
                setIsLoadingAssetStorageProviders(false);
            });
    }, [dispatch]);

    useEffect(() => {
        if (user?.id == null) {
            setAssignedTaskCount(null);
            return;
        }

        let isMounted = true;

        const refreshAssignedTaskCount = () => {
            if (document.visibilityState === 'hidden') {
                return;
            }

            new ProcessInstanceTaskApiService()
                .getAssignedTaskCount()
                .then((count) => {
                    if (isMounted) {
                        setAssignedTaskCount(count);
                    }
                })
                .catch(() => {
                    // Keep the last known value to avoid noisy UI errors for a purely decorative counter.
                });
        };

        refreshAssignedTaskCount();
        const unsubscribeRefreshListener = subscribeProcessAssignedTaskCountRefreshEvent(refreshAssignedTaskCount);

        return () => {
            isMounted = false;
            unsubscribeRefreshListener();
        };
    }, [user?.id]);

    const drawerGroups = useMemo(() => {
        const hasSystemPermission = (permission: string): boolean => {
            return permissions?.systemPermissions
                ?.some((entry) => entry.permissions.includes(permission)) ?? false;
        };

        const filterByPermission = (items: DrawerItem[]): DrawerItem[] => {
            return items
                .filter((item) => {
                    if (item.requiredSystemPermission == null) {
                        return true;
                    }

                    return hasSystemPermission(item.requiredSystemPermission);
                })
                .map((item) => {
                    if (item.children == null) {
                        return item;
                    }

                    return {
                        ...item,
                        children: filterByPermission(item.children),
                    };
                })
                .filter((item) => item.children == null || item.children.length > 0);
        };

        return BaseDrawerGroups.map((group) => ({
            ...group,
            items: group.items.map((item) => {
                if (item.label === 'Aufgaben') {
                    return {
                        ...item,
                        chipContent: assignedTaskCount ?? undefined,
                    };
                }

                if (item.label !== 'Dateien & Medien') {
                    return item;
                }

                const providerChildren: DrawerItem[] = isLoadingAssetStorageProviders
                    ? [{
                        ...drawerModuleIcon('storage'),
                        label: 'Speicheranbieter laden...',
                        disabled: true,
                    }]
                    : assetStorageProviderItems.length > 0
                        ? assetStorageProviderItems
                        : [{
                            ...drawerModuleIcon('storage'),
                            label: 'Keine Asset-Speicheranbieter',
                            disabled: true,
                        }];

                return {
                    ...item,
                    to: undefined,
                    children: providerChildren,
                };
            }),
        }))
            .map((group) => ({
                ...group,
                items: filterByPermission(group.items),
            }))
            .filter((group) => group.items.length > 0);
    }, [assetStorageProviderItems, assignedTaskCount, isLoadingAssetStorageProviders, permissions]);

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
                        width: minimizeDrawer ? COLLAPSED_DRAWER_WIDTH_REM : EXPANDED_DRAWER_WIDTH_REM,
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
                            {drawerGroups.map((group, index) => (
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
                    actions={group.items.map((item) => {
                        const isActive = isDrawerItemActive(item, pathname);

                        return item.children == null
                            ? {
                                icon: <DrawerNavigationIcon item={item} active={isActive} />,
                                tooltip: item.label,
                                to: item.to ?? '',
                                disabled: item.disabled,
                                activeStyle: isActive ? actionActiveStyle : {},
                            }
                            : {
                                icon: <DrawerNavigationIcon item={item} active={isActive} />,
                                tooltip: item.label,
                                onClick: (e: any) => handleOpenMenu(e, item),
                                disabled: item.disabled,
                                activeStyle: isActive ? actionActiveStyle : {},
                            };
                    })}
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
                        <DrawerNavigationIcon item={item} active={isActive} />
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
    const [submenuAnchor, setSubmenuAnchor] = useState<HTMLElement | null>(null);
    const hasChildren = !!item.children?.length;
    const location = useLocation();
    const pathname = location.pathname;
    const isActive = useMemo(() => isDrawerItemActive(item, pathname), [item, pathname]);
    const menuItemSx = {
        minWidth: 220,
        gap: 1,
        '&.Mui-selected': {
            color: 'primary.main',
            backgroundColor: 'action.selected',
        },
        '&.Mui-selected:hover': {
            backgroundColor: 'action.hover',
        },
    };

    const handleToggleSubmenu = (e: React.MouseEvent<HTMLElement>) => {
        e.stopPropagation();
        if (item.disabled) return;
        setSubmenuAnchor((prev) => (prev ? null : e.currentTarget));
    };

    // Regular menu item
    if (!hasChildren && item.to) {
        return (
            <MenuItem
                component={Link}
                to={item.to}
                onClick={onAnyClose}
                disabled={item.disabled}
                selected={isActive}
                sx={menuItemSx}
            >
                <Box sx={{ width: 24, display: 'inline-flex', justifyContent: 'center' }}>
                    <DrawerNavigationIcon item={item} active={isActive} />
                </Box>
                <Box sx={{ flex: 1 }}>{item.label}</Box>
            </MenuItem>
        );
    }

    // Submenu item
    return (
        <>
            <MenuItem
                onClick={handleToggleSubmenu}
                disabled={item.disabled}
                selected={isActive}
                sx={menuItemSx}
            >
                <Box sx={{ width: 24, display: 'inline-flex', justifyContent: 'center' }}>
                    <DrawerNavigationIcon item={item} active={isActive} />
                </Box>
                <Box sx={{ flex: 1 }}>{item.label}</Box>
                {hasChildren && <ChevronForward />}
            </MenuItem>

            {hasChildren && (
                <Menu
                    anchorEl={submenuAnchor}
                    open={Boolean(submenuAnchor)}
                    onClose={() => setSubmenuAnchor(null)}
                    anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
                    transformOrigin={{ vertical: 'top', horizontal: 'left' }}
                    MenuListProps={{ dense: true }}
                    disableAutoFocusItem
                >
                    {item.children!.map((child) => (
                        <NestedMenuItem key={child.label} item={child} onAnyClose={onAnyClose} />
                    ))}
                </Menu>
            )}
        </>
    );
}


function DrawerNavigationIcon({item, active}: {item: DrawerItem; active: boolean}) {
    const defaultIcon = item.icon ?? <PageInfo />;

    if (item.activeIcon == null) {
        return defaultIcon;
    }

    return (
        <Box
            component="span"
            className="drawer-navigation-icon"
            sx={{
                display: 'inline-grid',
                alignItems: 'center',
                justifyContent: 'center',
                '& .drawer-navigation-icon-slot': {
                    gridArea: '1 / 1',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    transition: 'opacity 120ms ease',
                },
                '& .drawer-navigation-icon-default': {
                    opacity: active ? 0 : 1,
                },
                '& .drawer-navigation-icon-active': {
                    opacity: active ? 1 : 0,
                },
            }}
        >
            <Box
                component="span"
                className="drawer-navigation-icon-slot drawer-navigation-icon-default"
            >
                {defaultIcon}
            </Box>
            <Box
                component="span"
                className="drawer-navigation-icon-slot drawer-navigation-icon-active"
            >
                {item.activeIcon}
            </Box>
        </Box>
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
