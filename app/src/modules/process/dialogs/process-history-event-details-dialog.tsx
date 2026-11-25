import {Alert, Button, Dialog, DialogActions, DialogContent, Stack, Typography} from '@mui/material';
import {useId} from 'react';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {ExpandableCodeBlock} from '../../../components/expandable-code-block/expandable-code-block';
import {ProcessNodeExecutionLogLevel, type ProcessInstanceHistoryEvent} from '../entities/process-instance-event-entity';
import {getHistoryEventActor} from '../utils/process-history-events';
import {formatDateTimeWithRelative} from '../components/process-detail-values';

export function ProcessHistoryEventDetailsDialog({event, onClose}: {event: ProcessInstanceHistoryEvent; onClose: () => void}) {
    const titleId = useId();
    const details = Object.fromEntries(Object.entries(event.details).filter(([key]) => key !== 'manualIntervention'));
    return (
        <Dialog open onClose={onClose} fullWidth maxWidth="md" aria-labelledby={titleId}>
            <DialogTitleWithClose id={titleId} onClose={onClose}>{event.title}</DialogTitleWithClose>
            <DialogContent>
                <Stack spacing={2}>
                    <Typography variant="body2" sx={{color: 'text.secondary'}}>{getHistoryEventActor(event)} · {formatDateTimeWithRelative(event.timestamp)}</Typography>
                    {event.processInstanceTaskId != null && (
                        <Typography variant="body2" sx={{color: 'text.secondary'}}>
                            Aufgabe: {event.processNodeName?.trim() || 'Bezeichnung nicht verfügbar'}
                        </Typography>
                    )}
                    {event.level === ProcessNodeExecutionLogLevel.Error ? (
                        <Alert severity="error" sx={{whiteSpace: 'pre-wrap', overflowWrap: 'anywhere'}}>{event.message}</Alert>
                    ) : <Typography sx={{whiteSpace: 'pre-wrap', overflowWrap: 'anywhere'}}>{event.message}</Typography>}
                    {Object.keys(details).length > 0 && <ExpandableCodeBlock value={JSON.stringify(details, null, 4)} language="json"/>}
                </Stack>
            </DialogContent>
            <DialogActions><Button onClick={onClose}>Schließen</Button></DialogActions>
        </Dialog>
    );
}
