import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import { useAppSelector } from '../../../hooks/use-app-selector';
import { selectUser } from '../../../slices/user-slice';
import { Divider, Box, Typography, ListItemIcon } from '@mui/material';
import { useMemo } from 'react';
import { getFullName } from '../../../models/entities/user';
import { Link } from 'react-router-dom';
import { useLogout } from '../../../hooks/use-logout';

import ManageAccountsOutlined from '@mui/icons-material/ManageAccountsOutlined';
import DescriptionOutlined from '@mui/icons-material/DescriptionOutlined';
import HeadsetMicOutlined from '@mui/icons-material/HeadsetMicOutlined';
import InfoOutlined from '@mui/icons-material/InfoOutlined';
import Logout from '@mui/icons-material/Logout';
import OpenInNew from '@mui/icons-material/OpenInNew';

interface ShellUserMenuProps {
    anchorEl: null | HTMLElement;
    onClose: () => void;
}

export function ShellUserMenu({ anchorEl, onClose }: ShellUserMenuProps) {
    const logout = useLogout();
    const user = useAppSelector(selectUser);
    const userName = useMemo(() => getFullName(user), [user]);
    const open = Boolean(anchorEl);

    const handleExternalLink = (url: string) => {
        window.open(url, '_blank');
        onClose();
    };

    return (
        <Menu
            anchorEl={anchorEl}
            open={open}
            onClose={onClose}
            PaperProps={{
                elevation: 6,
                sx: {
                    mt: 1.5,
                    minWidth: 260,
                    overflow: 'visible',
                    '&::before': {
                        // arrow
                        content: '""',
                        display: 'block',
                        position: 'absolute',
                        top: 0,
                        left: 18,
                        width: 10,
                        height: 10,
                        bgcolor: 'rgba(255,255,255,0.98)',
                        transform: 'translateY(-50%) rotate(45deg)',
                        boxShadow: '-1px -1px 2px rgba(0,0,0,0.1)',
                        zIndex: 0,
                    },
                },
            }}
        >
            {/* Header: angemeldet als */}
            <Box
                sx={{
                    px: 2,
                    pt: .25,
                    pb: 0.75,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 0.25,
                }}
            >
                <Typography variant="caption" sx={{ color: 'rgba(0,0,0,0.5)', fontWeight: 500, mb: '-2px' }}>
                    Angemeldet als
                </Typography>
                <Typography
                    variant="body1"
                    sx={{ fontWeight: 600, color: '#111', lineHeight: 1.2 }}
                >
                    {userName || 'Unbekannte Nutzer:in'}
                </Typography>
            </Box>

            <Divider sx={{ my: 1 }} />

            {/* Konto */}
            <MenuItem component={Link} to="/account" onClick={onClose}>
                <ListItemIcon>
                    <ManageAccountsOutlined fontSize="small" />
                </ListItemIcon>
                <Typography variant="body1">Konto verwalten</Typography>
            </MenuItem>

            {/* Handbuch */}
            <MenuItem onClick={() => handleExternalLink('https://docs.gover.digital')}>
                <ListItemIcon>
                    <DescriptionOutlined fontSize="small" />
                </ListItemIcon>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Typography variant="body1">Handbuch</Typography>
                    <OpenInNew fontSize="inherit" sx={{ fontSize: '1rem', opacity: 0.6 }} />
                </Box>
            </MenuItem>

            {/* Support */}
            <MenuItem onClick={() => handleExternalLink('https://support.aivot.de')}>
                <ListItemIcon>
                    <HeadsetMicOutlined fontSize="small" />
                </ListItemIcon>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Typography variant="body1">Supportportal</Typography>
                    <OpenInNew fontSize="inherit" sx={{ fontSize: '1rem', opacity: 0.6 }} />
                </Box>
            </MenuItem>

            {/* Über Gover */}
            <MenuItem disabled>
                <ListItemIcon>
                    <InfoOutlined fontSize="small" />
                </ListItemIcon>
                <Typography variant="body1">Über Gover v5.0.0</Typography>
            </MenuItem>

            <Divider sx={{ my: 1 }} />

            {/* Abmelden */}
            <MenuItem onClick={logout}>
                <ListItemIcon>
                    <Logout fontSize="small" color="error" />
                </ListItemIcon>
                <Typography variant="body1" color="error">
                    Abmelden
                </Typography>
            </MenuItem>
        </Menu>
    );
}
