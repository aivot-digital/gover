import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectUser} from '../../../slices/user-slice';
import {Box, Divider, ListItemIcon, Typography} from '@mui/material';
import {useMemo} from 'react';
import {getFullName} from '../../../models/entities/user';
import {Link} from 'react-router-dom';
import {useLogout} from '../../../hooks/use-logout';

import ManageAccountsOutlined from '@aivot/mui-material-symbols-400-n25-outlined/ManageAccounts';
import DescriptionOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Description';
import HeadsetMicOutlined from '@aivot/mui-material-symbols-400-n25-outlined/HeadsetMic';
import InfoOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import Logout from '@aivot/mui-material-symbols-400-n25-outlined/Logout';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {setShowAboutProsunaDialog} from '../../../slices/shell-slice';
import {StringAvatar} from '../../../components/avatar/string-avatar';
import {getAboutProsunaLabel} from '../../../utils/app-info-utils';

interface ShellUserMenuProps {
    anchorEl: null | HTMLElement;
    onClose: () => void;
    minimizeDrawer?: boolean;
}

export function ShellUserMenu({
                                  anchorEl,
                                  onClose,
                                  minimizeDrawer,
                              }: ShellUserMenuProps) {
    const logout = useLogout();
    const user = useAppSelector(selectUser);
    const userName = useMemo(() => getFullName(user), [user]);
    const open = Boolean(anchorEl);
    const dispatch = useAppDispatch();
    const supportUrl = AppConfig.supportUrl;

    const handleExternalLink = (url: string) => {
        window.open(url, '_blank', 'noopener,noreferrer');
        onClose();
    };

    return (
        <>
            <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={onClose}
                anchorOrigin={{
                    vertical: minimizeDrawer ? 'center' : 'bottom',
                    horizontal: minimizeDrawer ? 'right' : 'left',
                }}
                transformOrigin={{
                    vertical: minimizeDrawer ? 'center' : 'top',
                    horizontal: minimizeDrawer ? 'left' : 'left',
                }}
                PaperProps={{
                    elevation: 6,
                    sx: {
                        mt: 1.5,
                        minWidth: 260,
                        overflow: 'visible',
                        ...(!minimizeDrawer
                            ? {
                                '&::before': {
                                    // arrow
                                    content: '""',
                                    display: 'block',
                                    position: 'absolute',
                                    top: 0,
                                    left: 20,
                                    width: 10,
                                    height: 10,
                                    background: 'inherit',
                                    transform: 'translateY(-50%) translateX(-5px) rotate(45deg)',
                                    boxShadow: '-1px -1px 2px rgba(0,0,0,0.1)',
                                    zIndex: 0,
                                },
                            }
                            : {}),
                    },
                }}
            >
                {/* Header: angemeldet als */}
                <Box
                    sx={{
                        px: 2,
                        pl: 1.125,
                        pt: 0.25,
                        pb: 1,
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1.25,
                    }}
                >
                    <StringAvatar name={userName}
                                  sx={{
                                      width: 34,
                                      height: 34,
                                      fontSize: 14,
                                  }}
                                  backgroundMode={'oklch'}/>

                    <Box sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 0.25,
                        minWidth: 0,
                    }}>
                        <Typography
                            variant="body1"
                            sx={{
                                fontWeight: 600,
                                color: 'text.primary',
                                lineHeight: 1,
                                mt: 0.25,
                            }}
                            noWrap
                            title={userName || 'Unbekannte Nutzer:in'}
                        >
                            {userName || 'Unbekannte Nutzer:in'}
                        </Typography>
                        <Typography
                            variant="caption"
                            sx={{
                                color: 'text.secondary',
                                mt: 0.25,
                                lineHeight: 1,
                            }}
                            noWrap
                            title={user?.email || 'Unbekannte E-Mail-Adresse'}
                        >
                            {user?.email || 'Unbekannte E-Mail-Adresse'}
                        </Typography>
                    </Box>
                </Box>

                <Divider sx={{my: 1}}/>

                {/* Konto */}
                <MenuItem component={Link}
                          to="/account"
                          onClick={onClose}>
                    <ListItemIcon>
                        <ManageAccountsOutlined fontSize="small"/>
                    </ListItemIcon>
                    <Typography variant="body1">Konto verwalten</Typography>
                </MenuItem>

                {/* Handbuch */}
                <MenuItem onClick={() => handleExternalLink('https://docs.prosuna.de')}>
                    <ListItemIcon>
                        <DescriptionOutlined fontSize="small"/>
                    </ListItemIcon>
                    <Box sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 0.5,
                    }}>
                        <Typography variant="body1">Handbuch</Typography>
                        <OpenInNew fontSize="inherit"
                                   sx={{
                                       fontSize: '1rem',
                                       opacity: 0.6,
                                   }}/>
                    </Box>
                </MenuItem>

                {/* Support is deployment-specific and remains hidden when no destination is configured. */}
                {supportUrl && (
                    <MenuItem onClick={() => handleExternalLink(supportUrl)}>
                        <ListItemIcon>
                            <HeadsetMicOutlined fontSize="small"/>
                        </ListItemIcon>
                        <Box sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 0.5,
                        }}>
                            <Typography variant="body1">Support</Typography>
                            <OpenInNew fontSize="inherit"
                                       sx={{
                                           fontSize: '1rem',
                                           opacity: 0.6,
                                       }}/>
                        </Box>
                    </MenuItem>
                )}

                {/* Über Prosuna */}
                <MenuItem
                    onClick={() => {
                        dispatch(setShowAboutProsunaDialog(true));
                        onClose();
                    }}
                >
                    <ListItemIcon>
                        <InfoOutlined fontSize="small"/>
                    </ListItemIcon>
                    <Typography variant="body1">{getAboutProsunaLabel()}</Typography>
                </MenuItem>

                <Divider sx={{my: 1}}/>

                {/* Abmelden */}
                <MenuItem onClick={logout}>
                    <ListItemIcon>
                        <Logout fontSize="small"
                                color="error"/>
                    </ListItemIcon>
                    <Typography variant="body1"
                                color="error">
                        Abmelden
                    </Typography>
                </MenuItem>
            </Menu>
        </>
    );
}
