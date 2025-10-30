import React, {useEffect, useState} from 'react';
import {Box, Card, CardContent, Divider, List, ListItem, ListItemButton, Skeleton, Typography} from '@mui/material';
import ChevronRight from '@aivot/mui-material-symbols-400-outlined/dist/chevron-right/ChevronRight';
import {Link} from 'react-router-dom';
import {DashboardApiService} from '../dashboard-api-service';
import {DashboardStatItem} from '../models/dashboard-stat-item';
import {isApiError} from '../../../models/api-error';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {withDelay} from '../../../utils/with-delay';

export function DashboardStatsPanel() {
    const dispatch = useAppDispatch();

    const [stats, setStats] = useState<DashboardStatItem[]>();

    useEffect(() => {
        withDelay(
            new DashboardApiService()
                .fetchStats(),
            1000,
        )
            .then(setStats)
            .catch((err) => {
                if (isApiError(err) && err.displayableToUser) {
                    dispatch(showErrorSnackbar(err.message));
                } else {
                    dispatch(showErrorSnackbar('Fehler beim Laden der Statistiken'));
                }
                console.error(err);
            });
    }, []);

    return (
        <Card
            sx={{
                height: '100%',
                borderRadius: 2,
            }}
        >
            <CardContent>
                <Box sx={{ pt: 0.5, px: 1 }}>
                    <Typography
                    variant="h5"
                    component="h3"
                    fontWeight={600}
                fontSize={'1.5rem'}>
                        Zahlen, Daten & Fakten
                    </Typography>

                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{mt: 1, mb: 0.75, maxWidth: '400px'}}
                    >
                        Behalten Sie Ihre Gover-Instanz im Blick und entdecken Sie spannende Metriken.
                    </Typography>
                </Box>

                <List disablePadding>
                    {stats == null
                        ? Array.from({length: 4}).map((_, i) => (
                            <React.Fragment key={i}>
                                <ListItem disablePadding>
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'space-between',
                                            width: '100%',
                                            gap: 2,
                                            py: 2,
                                            px: 1,
                                        }}
                                    >
                                        <Box sx={{flex: 1, minWidth: 0}}>
                                            <Skeleton
                                                variant="text"
                                                height={20}
                                                width="60%"
                                            />
                                            <Skeleton
                                                variant="text"
                                                height={14}
                                                width="40%"
                                                sx={{mt: 0.5}}
                                            />
                                        </Box>

                                        <Box sx={{display: 'flex', alignItems: 'center', minWidth: 80, justifyContent: 'flex-end'}}>
                                            <Skeleton
                                                variant="text"
                                                height={60}
                                                width={70}
                                                sx={{mr: 2}}
                                            />
                                            <Skeleton
                                                variant="circular"
                                                width={40}
                                                height={40}
                                                sx={{opacity: 0.4}}
                                            />
                                        </Box>
                                    </Box>
                                </ListItem>

                                {i < 3 && <Divider component="li" />}
                            </React.Fragment>
                        ))
                        : stats.map((item, index) => (
                            <React.Fragment key={item.id}>
                                <ListItem disablePadding>
                                    <ListItemButton
                                        component={Link}
                                        to={item.href}
                                        sx={{
                                            py: 2,
                                            px: 1,
                                            borderRadius: 1,
                                            '&:hover': {bgcolor: 'action.hover'},
                                            '&.Mui-focusVisible': {outline: '2px solid', outlineColor: 'primary.main'},
                                        }}
                                    >
                                        <Box
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'space-between',
                                                width: '100%',
                                                gap: 2,
                                            }}
                                        >
                                            <Box sx={{flex: 1, minWidth: 0}}>
                                                <Typography
                                                    variant="subtitle1"
                                                    fontWeight={700}
                                                    noWrap
                                                >
                                                    {item.title}
                                                </Typography>
                                                <Typography
                                                    variant="body2"
                                                    color="text.secondary"
                                                    fontSize={'0.875rem'}
                                                    noWrap
                                                >
                                                    {item.subtitle}
                                                </Typography>
                                            </Box>

                                            <Box sx={{display: 'flex', alignItems: 'center', minWidth: 80, justifyContent: 'flex-end'}}>
                                                <Typography
                                                    variant="h5"
                                                    fontWeight={700}
                                                    fontSize={'2.8125rem'}
                                                    sx={{mr: 2, color: 'primary.dark'}}
                                                >
                                                    {item.value}
                                                </Typography>
                                                <ChevronRight
                                                    aria-hidden
                                                    sx={{fontSize: '3rem', color: 'rgba(0,0,0,.2)'}}
                                                />
                                            </Box>
                                        </Box>
                                    </ListItemButton>
                                </ListItem>

                                {index < stats.length - 1 && <Divider component="li" />}
                            </React.Fragment>
                        ))}
                </List>
            </CardContent>
        </Card>
    );
}
