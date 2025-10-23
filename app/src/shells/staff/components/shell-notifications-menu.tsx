import React from 'react';
import {
    Box,
    Divider,
    List,
    ListItem,
    Menu,
    Skeleton,
    Typography,
} from '@mui/material';

interface ShellNotificationsMenuProps {
    anchorEl: null | HTMLElement;
    onClose: () => void;
    minimizeDrawer?: boolean;
}

const fetchSize = 4;

export function ShellNotificationsMenu({ anchorEl, onClose, minimizeDrawer }: ShellNotificationsMenuProps) {
    const open = Boolean(anchorEl);

    return (
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
                    minWidth: 400,
                    overflow: 'visible',
                    ...(!minimizeDrawer
                        ? {
                            '&::before': {
                                content: '""',
                                display: 'block',
                                position: 'absolute',
                                top: 0,
                                left: 16,
                                width: 10,
                                height: 10,
                                bgcolor: '#f4f4f4',
                                transform: 'translateY(-50%) translateX(-5px) rotate(45deg)',
                                boxShadow: '-1px -1px 2px rgba(0,0,0,0.1)',
                                zIndex: 0,
                            },
                        }
                        : {}),
                },
            }}
            sx={{ '& .MuiMenu-list': {p: 0} }}
        >
            <Box sx={{ px: 2, py: 2, backgroundColor: '#f4f4f4', borderTopLeftRadius: 4, borderTopRightRadius: 4 }}>
                <Typography variant="h6" component="h3" fontWeight={600} sx={{ lineHeight: 1, m: 0 }}>
                    Benachrichtigungen
                </Typography>
            </Box>

            <Divider />

            <Box sx={{ position: 'relative', width: 400, maxWidth: '100%' }}>
                <List disablePadding>
                    {Array.from({ length: fetchSize }).map((_, i) => (
                        <React.Fragment key={i}>
                            <ListItem disablePadding>
                                <Box
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        width: '100%',
                                        gap: 2,
                                        py: 2.25,
                                        px: 2,
                                    }}
                                >
                                    <Box sx={{ flex: 1, minWidth: 0 }}>
                                        <Skeleton variant="text" height={20} width="70%" animation={false} />
                                        <Skeleton variant="text" height={14} width="50%" sx={{ mt: 0.5 }} animation={false} />
                                    </Box>
                                    <Skeleton
                                        variant="circular"
                                        width={40}
                                        height={40}
                                        animation={false}
                                    />
                                </Box>
                            </ListItem>
                            {i < fetchSize - 1 && <Divider component="li" />}
                        </React.Fragment>
                    ))}
                </List>

                <Box
                    sx={{
                        position: 'absolute',
                        inset: 0,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        bgcolor: 'rgba(255,255,255,0.6)',
                        textAlign: 'center',
                        px: 2,
                        borderBottomLeftRadius: 4,
                        borderBottomRightRadius: 4
                    }}
                >
                    <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 320, transform: 'translateY(-13%)', }}>
                        Die Funktionalitäten für Benachrichtigungen werden derzeit entwickelt und stehen in Kürze zur Verfügung.
                    </Typography>
                </Box>
            </Box>
        </Menu>
    );
}