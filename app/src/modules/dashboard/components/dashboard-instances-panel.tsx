import {alpha, Box, Button, Divider, List, ListItemButton, Typography, useTheme} from '@mui/material';
import {Link} from 'react-router-dom';
import Inbox from '@aivot/mui-material-symbols-400-n25-outlined/Inbox';
import FolderOpen from '@aivot/mui-material-symbols-400-n25-outlined/FolderOpen';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import Balancer from 'react-wrap-balancer';
import {type DashboardInstance, type DashboardInstanceSummary} from '../models/dashboard-overview';
import {formatInstantInApplicationTimeZone} from '../../../utils/temporal-utils';
import {pluralize} from '../../../utils/humanization-utils';
import {DashboardPanel} from './dashboard-panel';
import {Chip} from '../../../components/chip/chip';
import {
    getProcessInstanceDeadlineProgress,
    ProcessInstanceDeadlineStateColors,
} from '../../process/utils/process-instance-deadline';

// Same scope as the panel: running instances assigned to the current user, most urgent deadline first.
export const DASHBOARD_INSTANCES_LIST_PATH = '/process-instances?filter=active&assignee=mine&sort=deadline&order=asc';

interface DashboardInstancesPanelProps {
    summary: DashboardInstanceSummary;
    previewOnly?: boolean;
}

function DeadlineChip({instance}: {instance: DashboardInstance}) {
    const state = getProcessInstanceDeadlineProgress(instance.started, instance.deadline);
    const deadline = instance.deadline == null ? null : formatInstantInApplicationTimeZone(instance.deadline, 'dd.MM.yyyy');
    if (state == null || deadline == null) {
        return null;
    }

    const labels = {
        open: `Frist · ${deadline}`,
        warning: `Bald fällig · ${deadline}`,
        exceeded: `Überschritten · ${deadline}`,
    };
    const color = ProcessInstanceDeadlineStateColors[state];
    return (
        <Chip
            label={labels[state]}
            size="small"
            mode="soft"
            color={color ?? 'default'}
            sx={{
                flexShrink: 0,
                ...(color == null && {bgcolor: 'transparent'}),
            }}
        />
    );
}

export function DashboardInstancesPanel({summary, previewOnly = false}: DashboardInstancesPanelProps) {
    const theme = useTheme();

    return (
        <DashboardPanel sx={{width: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden'}}>
            <Box sx={{px: 2.5, pt: 2.5, pb: 2, display: 'flex', alignItems: 'center', gap: 1.5}}>
                <Box sx={{width: 40, height: 40, display: 'grid', placeItems: 'center', borderRadius: '50%', bgcolor: alpha(theme.palette.primary.main, 0.1), color: 'primary.main', flexShrink: 0}}>
                    <Inbox/>
                </Box>
                <Box sx={{minWidth: 0}}>
                    <Typography variant="h6" component="h2">Meine Vorgänge</Typography>
                    <Typography variant="body2" sx={{color: 'text.secondary'}}>
                        <Balancer>Laufende Vorgänge, die Ihnen persönlich zugewiesen sind</Balancer>
                    </Typography>
                </Box>
                <Box sx={{ml: 'auto', display: 'flex', flexWrap: 'wrap', justifyContent: 'flex-end', gap: 1}}>
                    {summary.overdue > 0 && (
                        <Chip
                            label={`${summary.overdue} ${pluralize(summary.overdue, 'Frist', 'Fristen')} überschritten`}
                            size="small"
                            mode="soft"
                            color="error"
                        />
                    )}
                    <Chip
                        label={`${summary.total} laufend`}
                        size="small"
                        mode="soft"
                    />
                </Box>
            </Box>
            <Divider/>
            <List disablePadding>
                {summary.items.map((instance, index) => (
                    <Box key={instance.id}>
                        {index > 0 && <Divider component="li"/>}
                        <ListItemButton
                            component={previewOnly ? 'div' : Link}
                            to={previewOnly ? undefined : `/process-instances/${instance.id}`}
                            role={previewOnly ? 'listitem' : undefined}
                            tabIndex={previewOnly ? -1 : undefined}
                            disableRipple={previewOnly}
                            sx={{px: 2.5, py: 1.75, gap: 1.5}}
                        >
                            <FolderOpen sx={{color: 'text.secondary', flexShrink: 0}}/>
                            <Box sx={{flex: 1, minWidth: 0}}>
                                <Typography noWrap sx={{fontWeight: 600}}>{instance.processTitle}</Typography>
                                <Typography
                                    variant="caption"
                                    noWrap
                                    sx={{color: 'text.secondary', display: 'block', lineHeight: 1.4}}
                                >
                                    {instance.caseNumber} · Eingegangen am {formatInstantInApplicationTimeZone(instance.started, 'dd.MM.yyyy')}
                                </Typography>
                            </Box>
                            <DeadlineChip instance={instance}/>
                        </ListItemButton>
                    </Box>
                ))}
            </List>
            <Divider/>
            <Box sx={{px: 2.5, py: 1.25}}>
                <Button component={Link} to={DASHBOARD_INSTANCES_LIST_PATH} endIcon={<ArrowForward/>}>
                    Alle Vorgänge ansehen
                </Button>
            </Box>
        </DashboardPanel>
    );
}
