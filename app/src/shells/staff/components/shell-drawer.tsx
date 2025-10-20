import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectMaximizeDrawer, setMaximizeDrawer, setShowSearchDialog} from '../../../slices/shell-slice';
import KeyboardTabRtl from '@aivot/mui-material-symbols-400-outlined/dist/keyboard-tab-rtl/KeyboardTabRtl';
import SearchFilled from '@aivot/mui-material-symbols-400-outlined/dist/search/SearchFilled';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ShellUserMenu} from './shell-user-menu';
import {Link, useLocation} from 'react-router-dom';
import ChevronForward from '@aivot/mui-material-symbols-400-outlined/dist/chevron-forward/ChevronForward';
import KeyboardArrowDown from '@aivot/mui-material-symbols-400-outlined/dist/keyboard-arrow-down/KeyboardArrowDown';
import {createTheme, Dialog, DialogContent, Paper, ThemeProvider, useTheme} from '@mui/material';
import {ModuleIcons} from '../data/module-icons';
import {Actions} from '../../../components/actions/actions';
import Notifications from '@aivot/mui-material-symbols-400-outlined/dist/notifications/Notifications';
import ForwardToInbox from '@aivot/mui-material-symbols-400-outlined/dist/forward-to-inbox/ForwardToInbox';
import PageInfo from '@aivot/mui-material-symbols-400-outlined/dist/page-info/PageInfo';
import Start from '@aivot/mui-material-symbols-400-outlined/dist/start/Start';
import ShellDrawerLogo from './shell-drawer-logo';
import ShellDrawerUserIcon from './shell-drawer-user-icon';

interface DrawerGroup {
    title: string | null;
    items: DrawerItem[];
}

interface DrawerItem {
    icon: ReactNode;
    label: string;
    to?: string;
    children?: DrawerItem[];
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
                icon: ModuleIcons.forms,
                label: 'Formulare',
                to: '/forms',
            },
            {
                icon: ModuleIcons.submissions,
                label: 'Anträge',
                to: '/submissions',
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
        ],
    },
    {
        title: 'Verwaltung',
        items: [
            {
                icon: ModuleIcons.organization,
                label: 'Organisation',
                children: [
                    {
                        icon: ModuleIcons.departments,
                        label: 'Fachbereiche',
                        to: '/departments',
                    },
                    {
                        icon: ModuleIcons.users,
                        label: 'Mitarbeiter:innen',
                        to: '/users',
                    },
                ],
            },
            {
                icon: ModuleIcons.dataObjects,
                label: 'Datenobjekte',
                to: '/data-objects',
            },
            {
                icon: ModuleIcons.assets,
                label: 'Dateien & Medien',
                to: '/assets',
            },
            {
                icon: ModuleIcons.settings,
                label: 'Konfiguration',
                children: [
                    {
                        icon: ModuleIcons.settings,
                        label: 'Allgemeine Einstellungen',
                        to: '/settings/app',
                    },
                    {
                        icon: ModuleIcons.providerLinks,
                        label: 'Links',
                        to: '/provider-links',
                    },
                    {
                        icon: ModuleIcons.themes,
                        label: 'Farbschemata',
                        to: '/themes',
                    },
                    {
                        icon: ModuleIcons.payment,
                        label: 'Zahlungsanbieter',
                        to: '/payment-providers',
                    },
                    {
                        icon: ModuleIcons.identity,
                        label: 'Nutzerkontenanbieter',
                        to: '/identity-providers',
                    },
                    {
                        icon: ModuleIcons.secrets,
                        label: 'Geheimnisse',
                        to: '/secrets',
                    },
                    {
                        icon: <ForwardToInbox />,
                        label: 'SMTP-Test',
                        to: '/settings/smtp',
                    },
                    {
                        icon: <PageInfo />,
                        label: 'Systemstatus',
                        to: '/settings/status',
                    },
                    {
                        icon: <PageInfo />,
                        label: 'Dritte Ebene',
                        children: [
                            {
                                icon: ModuleIcons.departments,
                                label: 'Fachbereiche',
                                to: '/departments',
                            },
                            {
                                icon: ModuleIcons.users,
                                label: 'Mitarbeiter:innen',
                                to: '/users',
                            },
                        ],
                    },
                ],
            },
        ],
    },
];

export function ShellDrawer() {
    const baseTheme = useTheme();
    const dispatch = useAppDispatch();
    const maximizeDrawer = useAppSelector(selectMaximizeDrawer);

    const [userMenuAnchorEl, setUserMenuAnchorEl] = useState<null | HTMLElement>(null);

    const handleToggleDrawer = () => {
        dispatch(setMaximizeDrawer(!maximizeDrawer));
    };

    const handleToggleSearchDialog = () => {
        dispatch(setShowSearchDialog(true));
    };

    const drawerTheme = useMemo(() => {
        return createTheme({
            ...baseTheme,
            palette: {
                primary: baseTheme.palette.primary,
                secondary: baseTheme.palette.secondary,
                error: baseTheme.palette.error,
                warning: baseTheme.palette.warning,
                info: baseTheme.palette.info,
                success: baseTheme.palette.success,
            },
        });
    }, [baseTheme]);

    return (
        <ThemeProvider theme={drawerTheme}>
            <Box
                sx={{
                    display: 'block',
                }}
            >
                <Paper
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        height: '100vh',
                        overflowY: 'auto',
                        py: 1.5,
                        px: 1.75,
                        borderRadius: 0,
                        width: maximizeDrawer ? '18rem' : '4rem',
                        backgroundColor: 'primary.dark',
                        color: 'rgba(255, 255, 255, 0.8)',
                    }}
                    elevation={1}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'row',
                            mb: 3,
                        }}
                    >
                        <Link to={'/'} title={'Zurück zur Übersicht'} style={{display: 'flex', alignItems: 'center', textDecoration: 'none'}}>
                            <ShellDrawerLogo minimize={!maximizeDrawer} />
                        </Link>

                        <Actions
                            sx={{
                                ml: 'auto',
                            }}
                            color="inherit"
                            dense={true}
                            actions={[
                                {
                                    icon: <Notifications />,
                                    tooltip: 'Benachrichtigungen',
                                    onClick: event => {
                                    },
                                },
                                {
                                    icon: <ShellDrawerUserIcon />,
                                    tooltip: 'Benutzerkonto',
                                    onClick: event => {
                                        setUserMenuAnchorEl(event.currentTarget as HTMLElement);
                                    },
                                },
                            ]}
                            direction={maximizeDrawer ? 'row' : 'column'}
                            tooltipPlacement={maximizeDrawer ? 'bottom' : 'right'}
                        />
                    </Box>

                    <Box
                        sx={{
                            mb: 2,
                        }}
                    >
                        {
                            maximizeDrawer ?
                                <Button
                                    startIcon={<SearchFilled />}
                                    variant="outlined"
                                    fullWidth={true}
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
                                    }}
                                >
                                    Suche
                                </Button> :
                                <Actions
                                    color="inherit"
                                    direction="column"
                                    actions={[{
                                        icon: <SearchFilled />,
                                        tooltip: 'Suche',
                                        onClick: handleToggleSearchDialog,
                                    }]}
                                    tooltipPlacement="right"
                                />
                        }
                    </Box>

                    <Box sx={{flex: 1}}>
                        {
                            DrawerGroups.map((group, index) => (
                                <DrawerGroup
                                    key={group.title || index}
                                    group={group}
                                    maximizeDrawer={maximizeDrawer}
                                />
                            ))
                        }
                    </Box>

                    <Actions
                        sx={{
                            flex: 0,
                            display: 'flex',
                            justifyContent: 'right',
                        }}
                        color="inherit"
                        direction={maximizeDrawer ? 'row' : 'column'}
                        actions={[{
                            tooltip: maximizeDrawer ? 'Minimieren' : 'Maximieren',
                            icon: maximizeDrawer ? <KeyboardTabRtl /> : <Start />,
                            onClick: handleToggleDrawer,
                        }]}
                        tooltipPlacement={maximizeDrawer ? 'top' : 'right'}
                    />
                </Paper>
            </Box>

            <ShellUserMenu
                anchorEl={userMenuAnchorEl}
                onClose={() => {
                    setUserMenuAnchorEl(null);
                }}
            />
        </ThemeProvider>
    );
}

interface DrawerGroupProps {
    group: DrawerGroup;
    maximizeDrawer: boolean;
}

function DrawerGroup(props: DrawerGroupProps) {
    const {
        group,
        maximizeDrawer,
    } = props;

    const [showDrawerItems, setShowDrawerItems] = useState<DrawerItem[] | null>(null);

    if (!maximizeDrawer) {
        return (
            <>
                <Actions
                    sx={{
                        height: 'auto',
                        mt: 4,
                    }}
                    color="inherit"
                    actions={group.items.map((item) => item.children == null ? ({
                        icon: item.icon,
                        tooltip: item.label,
                        to: item.to ?? '',
                    }) : ({
                        icon: item.icon,
                        tooltip: item.label,
                        onClick: () => {
                            setShowDrawerItems(item.children ?? []);
                        },
                    }))}
                    dense={true}
                    direction="column"
                    tooltipPlacement="right"
                />

                <Dialog
                    open={showDrawerItems != null}
                    onClose={() => {
                        setShowDrawerItems(null);
                    }}
                >
                    <DialogContent>
                        <List
                            dense={true}
                            sx={{
                                py: 0,
                                my: 0,
                            }}
                        >
                            {
                                (showDrawerItems ?? [])
                                    .map((child) => (
                                        <DrawerListItem
                                            item={child}
                                            key={child.label}
                                            showChildren={true}
                                            showIcon={true}
                                            onClick={() => {
                                                setShowDrawerItems(null);
                                            }}
                                        />
                                    ))
                            }
                        </List>
                    </DialogContent>
                </Dialog>
            </>
        );
    }

    return (
        <Box>
            {
                maximizeDrawer &&
                group.title != null &&
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
            }

            {
                !maximizeDrawer &&
                <Box
                    sx={{
                        height: '1rem',
                    }}
                />
            }

            <List
                dense={true}
                sx={{
                    my: 0,
                }}
            >
                {
                    group.items.map((item, itemIndex) => (
                        <DrawerListItem
                            item={item}
                            key={item.label}
                            showChildren={maximizeDrawer}
                            showIcon={true}
                        />
                    ))
                }
            </List>
        </Box>
    );
}

interface DrawerListItemProps {
    item: DrawerItem;
    showChildren: boolean;
    showIcon: boolean;
    onClick?: () => void;
}

function DrawerListItem({item, showChildren, showIcon, onClick}: DrawerListItemProps) {
    const {
        to,
        children,
    } = item;

    const [expanded, setExpanded] = useState(localStorage.getItem(`drawer-item-${item.label}-expanded`) != null);
    const location = useLocation();
    const {
        pathname,
    } = location;

    useEffect(() => {
        if (expanded) {
            localStorage.setItem(`drawer-item-${item.label}-expanded`, 'true');
        } else {
            localStorage.removeItem(`drawer-item-${item.label}-expanded`);
        }
    }, [expanded]);

    const isActive = useMemo(() => {
        if (to != null) {
            if (to === '/') {
                return pathname === '/';
            }
            return pathname.startsWith(to);
        }

        if (children != null) {
            return children.some((child) => {
                if (child.to != null) {
                    if (child.to === '/') {
                        return pathname === '/';
                    }
                    return pathname.startsWith(child.to);
                }
                return false;
            });
        }

        return false;
    }, [pathname, to, children]);

    if (to != null) {
        return (
            <ListItem
                dense={true}
                disableGutters={true}
            >
                <ListItemButton
                    component={Link}
                    to={to}
                    onClick={() => {
                        if (onClick) {
                            onClick();
                        }
                    }}
                    sx={{
                        px: 1,
                        borderRadius: 1,
                        backgroundColor: isActive ? 'secondary.main' : undefined,
                        '& .MuiListItemIcon-root': {
                            minWidth: 'auto',
                            width: '24px',
                            textAlign: 'center',
                            marginRight: 1,
                        },
                        '& .MuiListItemText-primary': {
                            fontWeight: 600,
                            fontSize: '1rem',
                        },
                        '& .MuiListItemIcon-root, .MuiListItemText-primary': {
                            color: isActive ? 'primary.dark' : 'rgba(255, 255, 255, 0.8)',
                        },
                        '&:hover': {
                            backgroundColor: isActive ? 'secondary.main' : 'rgba(255, 255, 255, 0.1)',
                            '& .MuiListItemIcon-root, .MuiListItemText-primary': {
                                color: isActive ? 'primary.main' : 'rgba(255,255,255,1)',
                            },
                        }
                    }}
                >
                    {
                        showIcon &&
                        <ListItemIcon>
                            {item.icon}
                        </ListItemIcon>
                    }
                    {
                        showChildren &&
                        <ListItemText
                            primary={item.label}
                        />
                    }
                </ListItemButton>
            </ListItem>
        );
    } else if (children != null) {

        return (
            <>
                <ListItem
                    dense={true}
                    disableGutters={true}
                >
                    <ListItemButton
                        onClick={() => {
                            setExpanded(!expanded);
                            if (onClick) {
                                onClick();
                            }
                        }}
                        sx={{
                            px: 1,
                            borderRadius: 1,
                            backgroundColor: isActive ? 'secondary.main' : undefined,
                            '& .MuiListItemIcon-root': {
                                minWidth: 'auto',
                                width: '24px',
                                textAlign: 'center',
                                marginRight: 1,
                            },
                            '& .MuiListItemText-primary': {
                                fontWeight: 600,
                                fontSize: '1rem',
                            },
                            '& .toggle-icon': {
                                fontSize: '1rem',
                                fontWeight: 600,
                            },
                            '& .MuiListItemIcon-root, .MuiListItemText-primary, .toggle-icon': {
                                color: isActive ? 'primary.dark' : 'rgba(255, 255, 255, 0.8)',
                            },
                            '&:hover': {
                                backgroundColor: isActive ? 'secondary.main' : 'rgba(255, 255, 255, 0.1)',
                                '& .MuiListItemIcon-root, .MuiListItemText-primary, .toggle-icon': {
                                    color: isActive ? 'primary.main' : 'rgba(255,255,255,1)',
                                },
                            }
                        }}
                    >
                        {
                            showIcon &&
                            <ListItemIcon>
                                <PageInfo />
                            </ListItemIcon>
                        }
                        {
                            showChildren &&
                            <ListItemText
                                primary={item.label}
                            />
                        }
                        {
                            showChildren &&
                            <Box className={'toggle-icon'} sx={{display: 'flex', alignItems: 'center'}}>
                                {
                                    expanded ? <KeyboardArrowDown /> : <ChevronForward />
                                }
                            </Box>
                        }
                    </ListItemButton>
                </ListItem>
                {
                    showChildren &&
                    expanded &&
                    <List
                        dense={true}
                        sx={{
                            py: 0,
                            my: 0,
                            pl: 4,
                        }}
                    >
                        {
                            children.map((child) => (
                                <DrawerListItem
                                    item={child}
                                    key={child.label}
                                    showChildren={showChildren}
                                    showIcon={false}
                                />
                            ))
                        }
                    </List>
                }
            </>
        );
    } else {
        return null;
    }
}