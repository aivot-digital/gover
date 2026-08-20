import {
    alpha,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    List,
    ListItemButton,
    Typography,
    useTheme,
} from '@mui/material';
import {Link} from 'react-router-dom';
import TaskAlt from '@aivot/mui-material-symbols-400-n25-outlined/TaskAlt';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import CheckCircle from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircle';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import {type DashboardTaskSummary} from '../models/dashboard-overview';
import {formatInstantInApplicationTimeZone} from '../../../utils/temporal-utils';
import {DashboardPanel} from './dashboard-panel';
import Balancer from 'react-wrap-balancer';

interface DashboardTasksPanelProps {
    summary?: DashboardTaskSummary;
    error?: boolean;
}

export function DashboardTasksPanel({summary, error = false}: DashboardTasksPanelProps) {
    const theme = useTheme();

    return (
        <DashboardPanel sx={{width: '100%', height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden'}}>
            <Box sx={{px: 2.5, pt: 2.5, pb: 2, display: 'flex', alignItems: 'center', gap: 1.5}}>
                <Box sx={{width: 40, height: 40, display: 'grid', placeItems: 'center', borderRadius: '50%', bgcolor: alpha(theme.palette.primary.main, 0.1), color: 'primary.main'}}>
                    <TaskAlt/>
                </Box>
                <Box sx={{minWidth: 0}}>
                    <Typography variant="h6" component="h2">Meine Aufgaben</Typography>
                    <Typography variant="body2" color="text.secondary">
                        <Balancer>Aufgaben, die Ihnen aktuell persönlich zugewiesen sind</Balancer>
                    </Typography>
                </Box>
                {summary != null && summary.total > 0 && (
                    <Chip label={`${summary.total} offen`} size="small" color={summary.overdue > 0 ? 'warning' : 'default'} sx={{ml: 'auto'}}/>
                )}
            </Box>
            <Divider/>
            {summary == null && !error && <Box sx={{flex: 1, minHeight: 132, display: 'grid', placeItems: 'center'}}><CircularProgress size={28}/></Box>}
            {error && (
                <Box sx={{flex: 1, minHeight: 132, display: 'grid', placeItems: 'center', px: 3, textAlign: 'center'}}>
                    <Box><Typography fontWeight={600}>Aufgaben konnten nicht geladen werden</Typography><Typography variant="body2" color="text.secondary"><Balancer>Die Aufgabenliste ist weiterhin über die Navigation erreichbar.</Balancer></Typography></Box>
                </Box>
            )}
            {summary != null && summary.items.length === 0 && (
                <Box
                    sx={{
                        flex: 1,
                        minHeight: 180,
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        px: {xs: 2.5, sm: 3.5},
                        py: 3,
                        textAlign: 'center',
                    }}
                >
                    <Box sx={{width: 52, height: 52, display: 'grid', placeItems: 'center', borderRadius: '50%', bgcolor: alpha(theme.palette.success.main, 0.12), color: 'success.main'}}>
                        <CheckCircle sx={{fontSize: 29}}/>
                    </Box>
                    <Typography fontWeight={650} sx={{mt: 1.5}}>Alles erledigt</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{mt: 0.25}}>
                        <Balancer>Aktuell sind Ihnen keine Aufgaben zur Bearbeitung zugewiesen.</Balancer>
                    </Typography>
                </Box>
            )}
            {summary != null && summary.items.length > 0 && (
                <List disablePadding sx={{flex: 1}}>
                    {summary.items.map((task, index) => {
                        const deadline = task.deadline == null ? null : formatInstantInApplicationTimeZone(task.deadline, 'dd.MM.yyyy');
                        const isOverdue = task.deadline != null && Date.parse(task.deadline) < Date.now();
                        return (
                            <Box key={task.id}>
                                {index > 0 && <Divider component="li"/>}
                                <ListItemButton
                                    component={Link}
                                    to={`/tasks/${task.processInstanceId}/${task.id}`}
                                    sx={{px: 2.5, py: 1.75, gap: 1.5}}
                                >
                                    <Task sx={{color: 'text.secondary', flexShrink: 0}}/>
                                    <Box sx={{flex: 1, minWidth: 0}}>
                                        <Typography fontWeight={600} noWrap>{task.taskName}</Typography>
                                        <Typography variant="caption" color="text.secondary" noWrap sx={{display: 'block', lineHeight: 1.4}}>
                                            {task.processTitle} · {task.caseNumber}
                                        </Typography>
                                    </Box>
                                    {deadline != null && <Chip label={isOverdue ? `Überfällig · ${deadline}` : `Frist · ${deadline}`} size="small" color={isOverdue ? 'error' : 'default'} variant={isOverdue ? 'filled' : 'outlined'}/>}
                                </ListItemButton>
                            </Box>
                        );
                    })}
                </List>
            )}
            <Divider/>
            <Box sx={{px: 2.5, py: 1.25}}>
                <Button component={Link} to="/tasks" endIcon={<ArrowForward/>}>
                    Alle Aufgaben ansehen
                </Button>
            </Box>
        </DashboardPanel>
    );
}
