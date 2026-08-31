import React, {ReactNode, useCallback, useEffect, useMemo, useState} from 'react';
import {
    Badge,
    Box,
    Button,
    Chip,
    createTheme,
    Divider,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Paper,
    Snackbar,
    ThemeProvider,
    Tooltip,
    Typography,
    useTheme,
} from '@mui/material';
import {Link, useLocation} from 'react-router-dom';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {
    selectMinimizeDrawer,
    setMinimizeDrawer,
    setShowSearchDialog,
} from '../../../slices/shell-slice';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {ShellUserMenu} from './shell-user-menu';
import {ModuleIcons, ModuleIconsFilled} from '../data/module-icons';
import {Actions} from '../../../components/actions/actions';
import {useHotkeys} from 'react-hotkeys-hook';
import {formatShortcut} from '../../../utils/format-shortcut';
import LeftPanelClose from '@aivot/mui-material-symbols-400-n25-outlined/LeftPanelClose';
import LeftPanelOpen from '@aivot/mui-material-symbols-400-n25-outlined/LeftPanelOpen';
import SearchFilled from '@aivot/mui-material-symbols-400-n25-outlined/SearchFilled';
import ChevronForward from '@aivot/mui-material-symbols-400-n25-outlined/ChevronForward';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import Notifications from '@aivot/mui-material-symbols-400-n25-outlined/Notifications';
import ForwardToInbox from '@aivot/mui-material-symbols-400-n25-outlined/ForwardToInbox';
import PageInfo from '@aivot/mui-material-symbols-400-n25-outlined/PageInfo';
import ShellDrawerLogo from './shell-drawer-logo';
import ShellDrawerUserIcon from './shell-drawer-user-icon';
import SimpleBar from 'simplebar-react';
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
import {selectPermissions, selectUser} from '../../../slices/user-slice';
import {ProcessInstanceTaskApiService} from '../../../modules/process/services/process-instance-task-api-service';
import {Permission} from '../../../data/permissions/permission';
import {
    hasAnyDepartmentPermission,
    hasAnyTeamPermission,
    hasSystemPermission,
} from '../../../modules/permissions/utils/permission-utils';
import {type PermissionSet} from '../../../modules/permissions/models/permission-set';
import {AssetsApiService} from '../../../modules/assets/assets-api-service';
import {subscribeProcessAssignedTaskCountRefreshEvent} from '../../../modules/process/utils/process-assigned-task-count-events';
import {hasModuleFlag, ModuleFlag} from '../../../utils/module-flags';
import {alpha, type Theme as MuiTheme} from '@mui/material/styles';
import DashboardCustomize from '@aivot/mui-material-symbols-400-n25-outlined/DashboardCustomize';
import {createAppTheme} from '../../../theming/themes';
import {BaseTheme} from '../../../theming/base-theme';
import {ColorModePicker} from '../../../components/color-mode-picker/color-mode-picker';

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
    requiredSystemPermission?: Permission | string;
    isVisible?: (permissions: PermissionSet | undefined) => boolean;
    requiredModuleFlag?: ModuleFlag;
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
                requiredModuleFlag: ModuleFlag.Form,
            },
            {
                ...drawerModuleIcon('dataObjects'),
                label: 'Datenobjekte',
                to: '/data-objects',
                requiredSystemPermission: Permission.OBJECT_ITEM_READ,
                isVisible: (permissions) => hasSystemPermission(permissions, Permission.OBJECT_SCHEMA_READ),
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
                requiredSystemPermission: Permission.PRESET_READ,
            },
            {
                ...drawerModuleIcon('marketplace'),
                label: 'Marktplatz',
                disabled: true,
                children: [
                    {
                        ...drawerModuleIcon('departments'),
                        label: 'Durchsuchen',
                    },
                ],
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
                    {
                        ...drawerModuleIcon('departments'),
                        label: 'Organisationseinheiten',
                        to: '/departments',
                        isVisible: (permissions) => hasAnyDepartmentPermission(permissions, Permission.DEPARTMENT_READ),
                    },
                    {
                        ...drawerModuleIcon('teams'),
                        label: 'Teams',
                        to: '/teams',
                        isVisible: (permissions) => hasAnyTeamPermission(permissions, Permission.TEAM_READ),
                    },
                    {
                        ...drawerModuleIcon('users'),
                        label: 'Mitarbeiter:innen',
                        to: '/users',
                        requiredSystemPermission: Permission.USER_READ,
                    },
                    {
                        ...drawerIcon(<SupervisedUserCircle/>, <SupervisedUserCircleFilled/>),
                        label: 'Rollenverwaltung',
                        children: [
                            {
                                ...drawerModuleIcon('roles'),
                                label: 'Systemrollen',
                                to: '/system-roles',
                                requiredSystemPermission: Permission.SYSTEM_ROLE_READ,
                            },
                            {
                                ...drawerModuleIcon('roles'),
                                label: 'Domänenrollen',
                                to: '/user-roles',
                                requiredSystemPermission: Permission.DOMAIN_ROLE_READ,
                            },
                        ],
                    },
                    {
                        ...drawerIcon(<FamilyHistory/>, <FamilyHistoryFilled/>),
                        label: 'Organigramm',
                        to: '/organization-chart',
                        isVisible: (permissions) => hasAnyDepartmentPermission(permissions, Permission.DEPARTMENT_READ),
                    },
                ],
            },
            {
                ...drawerModuleIcon('assets'),
                label: 'Dateien & Medien',
                to: '/assets',
                requiredSystemPermission: Permission.ASSET_READ,
            },
            {
                ...drawerModuleIcon('dataModels'),
                label: 'Datenmodelle',
                to: '/data-models',
                requiredSystemPermission: Permission.OBJECT_SCHEMA_READ,
            },
            {
                icon: ModuleIcons.codeLists,
                label: 'Codelisten',
                to: '/code-lists',
                requiredSystemPermission: Permission.CODE_LIST_READ,
            },
            {
                ...drawerModuleIcon('settings'),
                label: 'Konfiguration',
                children: [
                    {
                        ...drawerModuleIcon('settings'),
                        label: 'Allgem. Einstellungen',
                        to: '/settings/app',
                        requiredSystemPermission: Permission.SYSTEM_CONFIG_READ,
                    },
                    {
                        ...drawerIcon(<ReadinessScore/>, <ReadinessScoreFilled/>),
                        label: 'Systeminformationen',
                        to: '/settings/status',
                    },
                    {
                        ...drawerModuleIcon('audit'),
                        label: 'Audit-Log',
                        to: '/audit-log',
                        requiredSystemPermission: Permission.AUDIT_LOG_READ,
                    },
                    {
                        ...drawerModuleIcon('themes'),
                        label: 'Erscheinungsbilder',
                        to: '/themes',
                        requiredSystemPermission: Permission.THEME_READ,
                    },
                    {
                        ...drawerModuleIcon('secrets'),
                        label: 'Systemvariablen',
                        to: '/secrets',
                        requiredSystemPermission: Permission.SECRET_READ,
                    },
                    {
                        ...drawerIcon(<Api/>, <ApiFilled/>),
                        label: 'Anbindungen',
                        children: [
                            {
                                ...drawerModuleIcon('identity'),
                                label: 'Identitätsanbieter',
                                to: '/identity-providers',
                                requiredSystemPermission: Permission.IDENTITY_PROVIDER_READ,
                            },
                            {
                                ...drawerModuleIcon('communication'),
                                label: 'Kommunikationsanbieter',
                                to: '/communication-providers',
                                requiredSystemPermission: Permission.COMMUNICATION_PROVIDER_READ,
                            },
                            {
                                ...drawerModuleIcon('payment'),
                                label: 'Zahlungsanbieter',
                                to: '/payment-providers',
                                requiredSystemPermission: Permission.PAYMENT_PROVIDER_READ,
                            },
                            {
                                ...drawerModuleIcon('storage'),
                                label: 'Speicheranbieter',
                                to: '/storage-providers',
                                requiredSystemPermission: Permission.STORAGE_PROVIDER_READ,
                            },
                            {
                                ...drawerIcon(<ForwardToInbox/>, <ForwardToInboxFilled/>),
                                label: 'E-Mail',
                                to: '/mail',
                                requiredSystemPermission: Permission.SYSTEM_CONFIG_UPDATE,
                            },
                        ],
                    },
                    {
                        ...drawerModuleIcon('extensions'),
                        label: 'Erweiterungen',
                        to: '/settings/extensions',
                        requiredSystemPermission: Permission.PLUGIN_READ,
                    },
                    {
                        icon: <DashboardCustomize/>,
                        label: 'Übersicht konfigurieren',
                        to: '/settings/dashboard',
                        requiredSystemPermission: Permission.SYSTEM_CONFIG_READ,
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
    const permissions = useAppSelector(selectPermissions);
    const user = useAppSelector(selectUser);
    const minimizeDrawer = useAppSelector(selectMinimizeDrawer) ?? false;
    const [userMenuAnchorEl, setUserMenuAnchorEl] = useState<null | HTMLElement>(null);
    const [notificationsAnchorEl, setNotificationsAnchorEl] = useState<null | HTMLElement>(null);
    const [showBlockedMsg, setShowBlockedMsg] = useState(false);
    const [assetStorageProviderItems, setAssetStorageProviderItems] = useState<DrawerItem[]>([]);
    const [isLoadingAssetStorageProviders, setIsLoadingAssetStorageProviders] = useState(true);
    const [assignedTaskCount, setAssignedTaskCount] = useState<number | null>(null);
    const hasDrawerSystemPermission = useCallback((permission: Permission | string): boolean => {
        return hasSystemPermission(permissions, permission);
    }, [permissions]);
    const canReadAssets = hasDrawerSystemPermission(Permission.ASSET_READ);

    useEffect(() => {
        if (!canReadAssets) {
            setAssetStorageProviderItems([]);
            setIsLoadingAssetStorageProviders(false);
            return;
        }

        setIsLoadingAssetStorageProviders(true);

        new AssetsApiService()
            .listStorageProviders()
            .then((providers) => {
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
    }, [canReadAssets, dispatch]);

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
        const filterByPermission = (items: DrawerItem[]): DrawerItem[] => {
            return items
                .filter((item) => {
                    if (item.isVisible != null && !item.isVisible(permissions)) {
                        return false;
                    }

                    if (item.requiredModuleFlag != null && !hasModuleFlag(item.requiredModuleFlag)) {
                        return false;
                    }

                    if (item.requiredSystemPermission == null) {
                        return true;
                    }

                    return hasDrawerSystemPermission(item.requiredSystemPermission);
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
    }, [assetStorageProviderItems, assignedTaskCount, hasDrawerSystemPermission, isLoadingAssetStorageProviders, permissions]);

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
        () => {
            // Build from the structural base theme to avoid stacking overrides from the active app theme.
            const darkTheme = createAppTheme(AppConfig.systemTheme, BaseTheme, 'dark');

            return createTheme({
                ...darkTheme,
                components: {
                    ...darkTheme.components,
                    MuiTooltip: {
                        styleOverrides: {
                            tooltip: {
                                backgroundColor: baseTheme.palette.background.paper,
                                color: baseTheme.palette.text.primary,
                                fontWeight: 500,
                                fontSize: '0.8rem',
                                boxShadow:
                                    '0px 2px 6px rgba(0,0,0,0.25), 0px 4px 12px rgba(0,0,0,0.15)',
                            },
                            arrow: {
                                color: baseTheme.palette.background.paper,
                            },
                        },
                    },
                },
            });
        },
        [baseTheme],
    );

    const shortcutLabel = formatShortcut(['meta'], 'k');
    useHotkeys(
        'meta+k, ctrl+k',
        (event) => {
            event.preventDefault();
            dispatch(setShowSearchDialog(true));
        },
        {enableOnFormTags: false},
    );

    return (
        <>
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
                            backgroundColor: 'background.paper',
                            color: 'text.secondary',
                            borderRight: '1px solid',
                            borderColor: 'divider',
                        }}
                        elevation={1}
                    >
                        <Box sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            px: 1.75,
                        }}>
                            {/* Header */}
                            <Box sx={{
                                display: 'flex',
                                flexDirection: minimizeDrawer ? 'column' : 'row',
                                mb: 2.5,
                            }}>
                                <Link
                                    to="/"
                                    title="Zurück zur Übersicht"
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: minimizeDrawer ? 'center' : 'start',
                                        textDecoration: 'none',
                                    }}
                                >
                                    <ShellDrawerLogo
                                        minimize={minimizeDrawer}
                                        style={{color: drawerTheme.palette.text.primary}}
                                    />
                                </Link>

                                {!minimizeDrawer && (
                                    <ShellDrawerUserActions minimizeDrawer={minimizeDrawer}
                                        setUserMenuAnchorEl={setUserMenuAnchorEl}
                                        setNotificationsAnchorEl={setNotificationsAnchorEl}/>
                                )}
                            </Box>

                            {/* Search */}
                            <Box sx={{mb: minimizeDrawer ? 0 : 2}}>
                                {!minimizeDrawer ?
                                    (
                                        <Button
                                            startIcon={<SearchFilled/>}
                                            variant="outlined"
                                            fullWidth
                                            onClick={handleToggleSearchDialog}
                                            color="inherit"
                                            sx={{
                                                justifyContent: 'flex-start',
                                                textAlign: 'left',
                                                backgroundColor: (theme: MuiTheme) => alpha(theme.palette.common.white, 0.14),
                                                borderColor: (theme: MuiTheme) => alpha(theme.palette.common.white, 0.22),
                                                fontWeight: 600,
                                                fontSize: '1rem',
                                                color: 'text.secondary',
                                                textTransform: 'none',
                                                '&:hover': {
                                                    backgroundColor: (theme: MuiTheme) => alpha(theme.palette.common.white, 0.2),
                                                    borderColor: (theme: MuiTheme) => alpha(theme.palette.common.white, 0.3),
                                                    color: 'text.primary',
                                                },
                                            }}
                                        >
                                            <Box sx={{
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                width: '100%',
                                                alignItems: 'center',
                                            }}>
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
                                                        background: 'action.selected',
                                                        color: 'text.secondary',
                                                        transform: 'translateX(7px) translateY(-1px)',
                                                    }}
                                                    title={'Tastenkürzel zum Öffnen der Suche (' + shortcutLabel + ')'}
                                                >
                                                    {shortcutLabel}
                                                </Box>
                                            </Box>
                                        </Button>
                                    ) :
                                    (
                                        <Actions
                                            sx={{
                                                '& .MuiIconButton-root': {
                                                    borderRadius: 1,
                                                    background: (theme: MuiTheme) => theme.palette.action.hover,
                                                    border: (theme: MuiTheme) => `1px solid ${theme.palette.divider}`,
                                                    color: (theme: MuiTheme) => theme.palette.text.secondary,
                                                },
                                                '& .MuiIconButton-root:hover': {
                                                    background: (theme: MuiTheme) => theme.palette.action.selected,
                                                    border: (theme: MuiTheme) => `1px solid ${theme.palette.text.disabled}`,
                                                    color: (theme: MuiTheme) => theme.palette.text.primary,

                                                },
                                            }}
                                            color="inherit"
                                            direction="column"
                                            actions={[
                                                {
                                                    icon: <SearchFilled/>,
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
                                'flexGrow': 1,
                                'display': 'flex',
                                'flexDirection': 'column',
                                'minHeight': 0,
                                '& .simplebar-scrollbar:before': {
                                    backgroundColor: 'text.disabled',
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

                        <Box sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            px: 1.75,
                        }}>
                            {/* Footer */}
                            <Divider sx={{
                                borderColor: 'divider',
                                mx: -1.75,
                                mb: 1.75,
                            }}/>
                            <Box sx={{
                                display: 'flex',
                                flexDirection: minimizeDrawer ? 'column' : 'row',
                                justifyContent: 'flex-start',
                                alignItems: 'center',
                                width: '100%',
                            }}>
                                {minimizeDrawer && (
                                    <>
                                        <ShellDrawerUserActions minimizeDrawer={minimizeDrawer}
                                            setUserMenuAnchorEl={setUserMenuAnchorEl}
                                            setNotificationsAnchorEl={setNotificationsAnchorEl}/>
                                        <Box sx={{height: 10}}/>
                                    </>
                                )}
                                <Box
                                    sx={{
                                        display: 'flex',
                                        flexDirection: minimizeDrawer ? 'column' : 'row',
                                        justifyContent: minimizeDrawer ? 'flex-start' : 'space-between',
                                        alignItems: 'center',
                                        gap: minimizeDrawer ? 1 : 0.25,
                                        width: minimizeDrawer ? 'auto' : '100%',
                                    }}
                                >
                                    <ColorModePicker
                                        showLabel={!minimizeDrawer}
                                        placement={minimizeDrawer ? 'right-end' : 'top-end'}
                                        tooltipPlacement="right"
                                        iconFontSize="small"
                                        size="small"
                                    />
                                    <Actions
                                        sx={{
                                            flex: 0,
                                            display: 'flex',
                                            justifyContent: 'right',
                                        }}
                                        color="inherit"
                                        direction={minimizeDrawer ? 'column' : 'row'}
                                        actions={[
                                            {
                                                tooltip: minimizeDrawer ? 'Seitenleiste maximieren' : 'Seitenleiste minimieren',
                                                icon: minimizeDrawer ? <LeftPanelOpen /> : <LeftPanelClose />,
                                                onClick: handleToggleDrawer,
                                            },
                                        ]}
                                        tooltipPlacement={minimizeDrawer ? 'right' : 'top'}
                                    />
                                </Box>
                            </Box>
                        </Box>
                    </Paper>
                </Box>
            </ThemeProvider>

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
        </>
    );
}

/* -----------------------------
 * DrawerGroup
 * ----------------------------- */
interface DrawerGroupProps {
    group: DrawerGroup;
    minimizeDrawer: boolean;
}

function DrawerGroup({
                         group,
                         minimizeDrawer,
                     }: DrawerGroupProps) {
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
            color: (theme: MuiTheme) => theme.palette.primary.contrastText,
            backgroundColor: (theme: MuiTheme) => theme.palette.primary.main,
            '&.MuiIconButton-root:hover': {
                color: (theme: MuiTheme) => theme.palette.primary.contrastText,
                backgroundColor: (theme: MuiTheme) => theme.palette.primary.main,
            },
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
                            color: (theme: MuiTheme) => theme.palette.text.primary,
                            backgroundColor: (theme: MuiTheme) => theme.palette.action.hover,

                        },
                        '& .Mui-disabled.MuiIconButton-root': {
                            color: (theme: MuiTheme) => `${theme.palette.text.disabled}!important`,
                            opacity: '0.5!important',
                        },
                    }}
                    color="inherit"
                    actions={group.items.map((item) => {
                        const isActive = isDrawerItemActive(item, pathname);

                        return item.children == null
                            ? {
                                icon: <DrawerNavigationIcon item={item}
                                                            active={isActive}/>,
                                tooltip: item.label,
                                to: item.to ?? '',
                                disabled: item.disabled,
                                activeStyle: isActive ? actionActiveStyle : {},
                            }
                            : {
                                icon: <DrawerNavigationIcon item={item}
                                                            active={isActive}/>,
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
                        color: 'text.secondary',
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
function DrawerListItem({
                            item,
                            level = 0,
                            isLastSibling = true,
                        }: { item: DrawerItem; level?: number; isLastSibling?: boolean }) {
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
        level === 0 ? {
                fontSize: '1rem',
                fontWeight: 600,
            } :
            {
                fontSize: '0.8rem',
                fontWeight: 600,
            };
    const iconSizeScale =
        level === 0 ? {
                '& .MuiSvgIcon-root': {
                    width: 24,
                    fontSize: '1.5rem',
                },
            } :
            {
                '& .MuiSvgIcon-root': {
                    width: 20,
                    fontSize: '1.25rem',
                },
            };

    const activeStyles =
        level === 0
            ? {
                backgroundColor: expanded && !isActive
                    ? (theme: MuiTheme) => theme.palette.action.selected
                    : isActive
                        ? (theme: MuiTheme) => theme.palette.primary.main
                        : undefined,
                '& .MuiListItemText-primary, & .MuiListItemIcon-root, .toggle-icon': {
                    color: isActive
                        ? (theme: MuiTheme) => theme.palette.primary.contrastText
                        : (theme: MuiTheme) => theme.palette.text.secondary,
                },
                '&:hover': {
                    backgroundColor: expanded && !isActive
                        ? (theme: MuiTheme) => theme.palette.action.selected
                        : isActive
                            ? (theme: MuiTheme) => theme.palette.primary.main
                            : (theme: MuiTheme) => theme.palette.action.hover,
                    '& .MuiListItemIcon-root, .MuiListItemText-primary, .toggle-icon': {
                        color: isActive
                            ? (theme: MuiTheme) => theme.palette.primary.contrastText
                            : (theme: MuiTheme) => theme.palette.text.primary,
                    },
                },
            }
            : {
                backgroundColor: isActive
                    ? (theme: MuiTheme) => theme.palette.primary.main
                    : undefined,
                '& .MuiListItemText-primary, & .MuiListItemIcon-root, .toggle-icon': {
                    color: isActive
                        ? (theme: MuiTheme) => theme.palette.primary.contrastText
                        : (theme: MuiTheme) => theme.palette.text.secondary,
                },
                '&:hover': {
                    backgroundColor: isActive
                        ? (theme: MuiTheme) => theme.palette.primary.main
                        : (theme: MuiTheme) => theme.palette.action.hover,
                    '& .MuiListItemIcon-root, .MuiListItemText-primary, .toggle-icon': {
                        color: isActive
                            ? (theme: MuiTheme) => theme.palette.primary.contrastText
                            : (theme: MuiTheme) => theme.palette.text.primary,
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
                                    backgroundColor: (theme: MuiTheme) => theme.palette.mode === 'dark'
                                        ? theme.palette.grey[700]
                                        : theme.palette.grey[300],
                                    transform: 'translate(calc(13px * -1), calc(13px * -0.4))',
                                    mask: 'url("data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'14\' height=\'14\' fill=\'none\' viewBox=\'0 0 14 14\'%3E%3Cpath d=\'M1 1v4a8 8 0 0 0 8 8h4\' stroke=\'%23efefef\' stroke-width=\'2\' stroke-linecap=\'round\'/%3E%3C/svg%3E") 50% 50% / 100% no-repeat',
                                },
                            }
                            : {}),
                        ...(level > 0 && isLastSibling && item.children != null && expanded
                            ? {
                                '&::after': {
                                    // Cover the part of the parent-level connector that still runs through
                                    // this last visible branch item before the nested child list starts.
                                    left: -17,
                                    top: 'calc(50% - 3px)',
                                    bottom: -12,
                                    width: '6px',
                                    content: '""',
                                    position: 'absolute',
                                    backgroundColor: 'background.paper',
                                    pointerEvents: 'none',
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
                        <DrawerNavigationIcon item={item}
                                              active={isActive}/>
                    </ListItemIcon>
                    <ListItemText primary={item.label}/>
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
                                    ? (theme) => theme.palette.primary.contrastText
                                    : (theme) => theme.palette.text.secondary,
                                backgroundColor: isActive
                                    ? (theme) => alpha(theme.palette.primary.contrastText, 0.14)
                                    : (theme) => theme.palette.action.selected,
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
                    <Box className={'toggle-icon'}
                         sx={{
                             display: 'flex',
                             alignItems: 'center',
                         }}>
                        {item.children &&
                            (expanded ? <KeyboardArrowDown sx={{ml: 0.5}}/> : <ChevronForward sx={{ml: 0.5}}/>)
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
                            backgroundColor: (theme: MuiTheme) => theme.palette.mode === 'dark'
                                ? theme.palette.grey[700]
                                : theme.palette.grey[300],
                            bottom: 'calc(36px - 2px - 14px / 2)',
                        },
                        ...(level > 0 && isLastSibling
                            ? {
                                '&::after': {
                                    // The parent-level connector is drawn by the surrounding list. When this
                                    // expanded branch is the last visible sibling, mask that connector inside
                                    // the nested branch so it stops at the current item instead of running down.
                                    top: -8,
                                    left: (level - 1) * 30 + 17,
                                    width: '6px',
                                    content: '""',
                                    position: 'absolute',
                                    backgroundColor: 'background.paper',
                                    bottom: 0,
                                    pointerEvents: 'none',
                                },
                            }
                            : {}),
                    }}
                >
                    {item.children.map((child, index) => (
                        <DrawerListItem
                            key={child.label}
                            item={child}
                            level={level + 1}
                            isLastSibling={index === item.children!.length - 1}
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
            anchorOrigin={{
                vertical: 'bottom',
                horizontal: 'right',
            }}
            transformOrigin={{
                vertical: 'top',
                horizontal: 'left',
            }}
            disableAutoFocusItem
            slotProps={{
                list: {dense: true}
            }}
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
                <Box sx={{
                    width: 24,
                    display: 'inline-flex',
                    justifyContent: 'center',
                }}>
                    <DrawerNavigationIcon item={item}
                                          active={isActive}/>
                </Box>
                <Box sx={{flex: 1}}>{item.label}</Box>
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
                <Box sx={{
                    width: 24,
                    display: 'inline-flex',
                    justifyContent: 'center',
                }}>
                    <DrawerNavigationIcon item={item}
                                          active={isActive}/>
                </Box>
                <Box sx={{flex: 1}}>{item.label}</Box>
                {hasChildren && <ChevronForward/>}
            </MenuItem>

            {hasChildren && (
                <Menu
                    anchorEl={submenuAnchor}
                    open={Boolean(submenuAnchor)}
                    onClose={() => setSubmenuAnchor(null)}
                    anchorOrigin={{
                        vertical: 'top',
                        horizontal: 'right',
                    }}
                    transformOrigin={{
                        vertical: 'top',
                        horizontal: 'left',
                    }}
                    disableAutoFocusItem
                    slotProps={{
                        list: {dense: true}
                    }}
                >
                    {item.children!.map((child) => (
                        <NestedMenuItem key={child.label}
                                        item={child}
                                        onAnyClose={onAnyClose}/>
                    ))}
                </Menu>
            )}
        </>
    );
}


function DrawerNavigationIcon({
                                  item,
                                  active,
                              }: { item: DrawerItem; active: boolean }) {
    const defaultIcon = item.icon ?? <PageInfo/>;

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


function ShellDrawerUserActions(props: {
    minimizeDrawer: boolean,
    setUserMenuAnchorEl: (el: HTMLElement) => void,
    setNotificationsAnchorEl: (el: HTMLElement) => void
}) {
    const {
        minimizeDrawer,
        setUserMenuAnchorEl,
        setNotificationsAnchorEl,
    } = props;
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
                        variant="dot"
                        overlap="circular"
                        badgeContent=" "
                        invisible={false}
                        sx={{
                            '& .MuiBadge-badge': {
                                top: 5,
                                right: 5,
                                borderColor: 'background.paper',
                                backgroundColor: 'secondary.main',
                                borderWidth: 2,
                                borderStyle: 'solid',
                                transform: 'scale(1.5) translate(50%, -50%)',
                            },
                        }}
                    >
                        <Notifications/>
                    </Badge>,
                    tooltip: 'Benachrichtigungen',
                    onClick: (event) => setNotificationsAnchorEl(event.currentTarget as HTMLElement),
                },
                {
                    icon: <ShellDrawerUserIcon/>,
                    tooltip: 'Mein Konto',
                    onClick: (event) => setUserMenuAnchorEl(event.currentTarget as HTMLElement),
                },
            ]}
            direction={minimizeDrawer ? 'column' : 'row'}
            tooltipPlacement={minimizeDrawer ? 'right' : 'bottom'}
        />
    );
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
