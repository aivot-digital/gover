import {useEffect, useId, useState} from 'react';
import {Alert, AlertTitle, Box, Button, Paper, Skeleton, Stack, Typography} from '@mui/material';
import Person from '@aivot/mui-material-symbols-400-n25-outlined/Person';
import {type Page} from '../../../models/dtos/page';
import {type ProcessInstanceHistoryEvent} from '../entities/process-instance-event-entity';
import {ProcessHistoryEventScope} from '../enums/process-history-event-scope';
import {ProcessInstanceEventApiService} from '../services/process-instance-event-api-service';
import {ProcessHistoryEventDetailsDialog} from '../dialogs/process-history-event-details-dialog';
import {getHistoryEventActor} from '../utils/process-history-events';
import {formatDateTimeWithRelative} from './process-detail-values';
import {ProcessHistoryPagination} from './process-history-pagination';

export function ProcessHistoryEventSection({
                                               instanceId,
                                               scope,
                                               refreshCounter,
                                           }: {
    instanceId: number;
    scope: ProcessHistoryEventScope.ManualInterventions | ProcessHistoryEventScope.InstanceErrors;
    refreshCounter: number;
}) {
    const id = useId();
    const [page, setPage] = useState(0);
    const [result, setResult] = useState<Page<ProcessInstanceHistoryEvent>>();
    const [loading, setLoading] = useState(true);
    const [failed, setFailed] = useState(false);
    const [retry, setRetry] = useState(0);
    const [selectedEvent, setSelectedEvent] = useState<ProcessInstanceHistoryEvent>();
    const manual = scope === ProcessHistoryEventScope.ManualInterventions;
    const title = manual ? 'Manuelle Eingriffe' : 'Fehler auf Vorgangsebene';

    useEffect(() => {
        let active = true;
        setLoading(true);
        setFailed(false);
        new ProcessInstanceEventApiService().listHistory(instanceId, {
            scope,
            page,
            size: 20,
        })
            .then(value => {
                if (!active) return;
                if (page > 0 && value.content.length === 0) setPage(Math.max(0, value.page.totalPages - 1));
                else setResult(value);
            })
            .catch(() => {
                if (active) setFailed(true);
            })
            .finally(() => {
                if (active) setLoading(false);
            });
        return () => {
            active = false;
        };
    }, [instanceId, scope, page, refreshCounter, retry]);

    if (!manual && !failed && result?.page.totalElements === 0) return null;
    return <Box component="section"
                aria-labelledby={id}
                aria-busy={loading}>
        <Typography variant={manual ? 'h5' : 'h6'}
                    id={id}
                    sx={{mb: 1}}>{title}</Typography>
        {manual && <Typography sx={{
            mt: 1,
            mb: 2,
            maxWidth: 820,
            color: 'text.secondary',
        }}>
            Hier finden Sie manuelle Aktionen außerhalb des modellierten Ablaufs, zum Beispiel Neuzuweisungen und
            erneute Ausführungen. Eingriffe in eine Aufgabe erscheinen zusätzlich bei deren Ereignissen.
        </Typography>}
        {failed && <Alert severity="error"
                          action={<Button color="inherit"
                                          onClick={() => setRetry(value => value + 1)}
                                          loading={loading}>Erneut versuchen</Button>}>
            {manual ? 'Die manuellen Eingriffe konnten nicht geladen werden.' : 'Die Fehler auf Vorgangsebene konnten nicht geladen werden.'}
        </Alert>}
        {result == null && loading && <Skeleton variant="rounded"
                                                height={64}/>}
        {manual && result?.content.length === 0 && <Box sx={{
            border: '1px dashed',
            borderColor: 'divider',
            borderRadius: 1,
            p: 3,
            textAlign: 'center',
        }}>
            <Typography sx={{color: 'text.secondary'}}>Keine manuellen Eingriffe protokolliert.</Typography>
        </Box>}
        {result != null && result.content.length > 0 && (manual ? (
            <Paper component="ol"
                   aria-label={title}
                   variant="outlined"
                   sx={{
                       listStyle: 'none',
                       m: 0,
                       p: 0,
                       overflow: 'hidden',
                   }}>
                {result.content.map(event => <Box key={event.id}
                                                  component="li"
                                                  sx={{
                                                      display: 'flex',
                                                      alignItems: 'center',
                                                      gap: 2,
                                                      px: 2,
                                                      py: 1.5,
                                                      minWidth: 0,
                                                      '& + li': {
                                                          borderTop: '1px solid',
                                                          borderColor: 'divider',
                                                      },
                                                  }}>
                    <Person color="action"
                            sx={{flexShrink: 0}}/>
                    <Box sx={{
                        flex: 1,
                        minWidth: 0,
                    }}>
                        <Typography title={event.title}
                                    sx={{
                                        fontWeight: 600,
                                        whiteSpace: 'nowrap',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                    }}>{event.title}</Typography>
                        <Typography variant="caption"
                                    component="div"
                                    sx={{color: 'text.secondary'}}>{getHistoryEventActor(event)} · {formatDateTimeWithRelative(event.timestamp)}</Typography>
                    </Box>
                    <Button size="small"
                            sx={{flexShrink: 0}}
                            onClick={() => setSelectedEvent(event)}>Details anzeigen</Button>
                </Box>)}
            </Paper>
        ) : <Stack spacing={1}>
            {result.content.map(event => <Alert key={event.id}
                                                severity="error"
                                                action={<Button color="inherit"
                                                                size="small"
                                                                onClick={() => setSelectedEvent(event)}>Details
                                                                                                        anzeigen</Button>}>
                <AlertTitle>{event.title}</AlertTitle>
                <Typography sx={{
                    whiteSpace: 'pre-wrap',
                    overflowWrap: 'anywhere',
                }}>{event.message}</Typography>
                <Typography component="time"
                            dateTime={event.timestamp}
                            variant="caption"
                            sx={{
                                display: 'block',
                                mt: 0.5,
                            }}>{formatDateTimeWithRelative(event.timestamp)}</Typography>
            </Alert>)}
        </Stack>)}
        {result != null && <ProcessHistoryPagination page={result.page}
                                                     label={`Seiten: ${title}`}
                                                     disabled={loading}
                                                     onChange={setPage}/>}
        {selectedEvent != null && <ProcessHistoryEventDetailsDialog event={selectedEvent}
                                                                    onClose={() => setSelectedEvent(undefined)}/>}
    </Box>;
}
