import {Box, Button, Divider, List, ListItemButton, Typography} from '@mui/material';
import {Link} from 'react-router-dom';
import Route from '@aivot/mui-material-symbols-400-n25-outlined/Route';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import {type DashboardRecentProcess} from '../models/dashboard-overview';
import {formatInstantInApplicationTimeZone} from '../../../utils/temporal-utils';
import {DashboardPanel} from './dashboard-panel';
import Balancer from 'react-wrap-balancer';

function getProcessLink(process: DashboardRecentProcess): string {
    const version = process.draftedVersion ?? process.publishedVersion;
    return version == null ? '/processes' : `/processes/${process.id}/versions/${version}`;
}

export function DashboardRecentProcessesPanel({processes}: {processes: DashboardRecentProcess[]}) {
    return (
        <DashboardPanel sx={{width: '100%', height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden'}}>
            <Box sx={{px: 2.25, pt: 2.5, pb: 2}}>
                <Typography variant="h6" component="h2">Zuletzt geänderte Prozesse</Typography>
                <Typography variant="body2" color="text.secondary">
                    <Balancer>Prozesse, die Sie bearbeiten dürfen</Balancer>
                </Typography>
            </Box>
            <Divider/>
            <List disablePadding sx={{flex: 1}}>
                {processes.map((process, index) => (
                    <Box key={process.id}>
                        {index > 0 && <Divider component="li"/>}
                        <ListItemButton component={Link} to={getProcessLink(process)} sx={{px: 2.25, py: 1.75, gap: 1.5}}>
                            <Route sx={{color: 'text.secondary', flexShrink: 0}}/>
                            <Box sx={{minWidth: 0}}>
                                <Typography fontWeight={600} variant="body2" noWrap>{process.title}</Typography>
                                <Typography variant="caption" color="text.secondary" sx={{display: 'block', lineHeight: 1.4}}>
                                    Bearbeitet am {formatInstantInApplicationTimeZone(process.updated, 'dd.MM.yyyy')}
                                </Typography>
                            </Box>
                        </ListItemButton>
                    </Box>
                ))}
            </List>
            <Divider/>
            <Box sx={{px: 2.25, py: 1.25}}><Button component={Link} to="/processes" endIcon={<ArrowForward/>}>Alle Prozesse ansehen</Button></Box>
        </DashboardPanel>
    );
}
