import {
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogContent,
    Divider,
    List,
    ListItemButton,
    Skeleton,
    Stack,
    ToggleButton,
    ToggleButtonGroup,
    Tooltip,
    Typography,
} from '@mui/material';
import {alpha, type PaletteColor, type Theme} from '@mui/material/styles';
import {type ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import AccountBox from '@aivot/mui-material-symbols-400-n25-outlined/AccountBox';
import ArrowDownward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowDownward';
import ArrowUpward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowUpward';
import BugReport from '@aivot/mui-material-symbols-400-n25-outlined/BugReport';
import Info from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import Memory from '@aivot/mui-material-symbols-400-n25-outlined/Memory';
import Refresh from '@aivot/mui-material-symbols-400-n25-outlined/Refresh';
import Report from '@aivot/mui-material-symbols-400-n25-outlined/Report';
import Warning from '@aivot/mui-material-symbols-400-n25-outlined/Warning';
import {AlertComponent} from '../../../components/alert/alert-component';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {EmptyDataListPlaceholder} from '../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {ExpandableCodeBlock} from '../../../components/expandable-code-block/expandable-code-block';
import {SearchInput} from '../../../components/search-input/search-input';
import {humanizeMillisecondsDuration} from '../../../utils/duration-utils';
import {formatInstantInApplicationTimeZone} from '../../../utils/temporal-utils';
import {ProcessNodeExecutionLogLevel} from '../entities/process-instance-event-entity';
import {
    type ProcessInstanceEventLog,
    type ProcessInstanceEventLogEntry,
    type ProcessInstanceEventLogFilter,
    type ProcessInstanceEventLogSortOrder,
} from '../models/process-instance-event-log';
import {ProcessInstanceEventApiService} from '../services/process-instance-event-api-service';

const EVENT_PAGE_SIZE = 50;
const eventApiService = new ProcessInstanceEventApiService();

interface ProcessInstanceEventDialogProps {
    open: boolean;
    onClose: () => void;
    instanceId: number;
    taskId: number | null;
}

interface EventLevelPresentation {
    label: string;
    renderIcon: (fontSize: 'small' | 'medium') => ReactNode;
    palette: 'info' | 'warning' | 'error' | null;
}

// The symbol package and MUI can resolve different module entry points in IDEs. Rendering here avoids
// comparing their deeply overloaded component types while preserving the icon's supported font sizes.
const EVENT_LEVEL_PRESENTATION: Record<ProcessNodeExecutionLogLevel, EventLevelPresentation> = {
    [ProcessNodeExecutionLogLevel.Debug]: {
        label: 'Debug',
        renderIcon: fontSize => <BugReport fontSize={fontSize}/>,
        palette: null,
    },
    [ProcessNodeExecutionLogLevel.Info]: {
        label: 'Information',
        renderIcon: fontSize => <Info fontSize={fontSize}/>,
        palette: 'info',
    },
    [ProcessNodeExecutionLogLevel.Warn]: {
        label: 'Warnung',
        renderIcon: fontSize => <Warning fontSize={fontSize}/>,
        palette: 'warning',
    },
    [ProcessNodeExecutionLogLevel.Error]: {
        label: 'Fehler',
        renderIcon: fontSize => <Report fontSize={fontSize}/>,
        palette: 'error',
    },
};

function formatEventTimestamp(value: unknown): string {
    const formatted = formatInstantInApplicationTimeZone(value, 'dd.MM.yyyy, HH:mm:ss');
    return formatted != null ? `${formatted} Uhr` : '-';
}

function formatRuntime(runtime: number | null): string {
    return runtime == null ? '-' : humanizeMillisecondsDuration(runtime);
}

function getEventSource(event: ProcessInstanceEventLogEntry): string {
    if (event.triggeringUserName != null && event.triggeringUserName.trim().length > 0) {
        return event.triggeringUserName;
    }
    return event.triggeringUserId == null ? 'System' : 'Unbekannte Nutzer:in';
}

export function ProcessInstanceEventDialog(props: ProcessInstanceEventDialogProps) {
    const {open, onClose, instanceId, taskId} = props;
    const [eventLog, setEventLog] = useState<ProcessInstanceEventLog | null>(null);
    const [selectedEventId, setSelectedEventId] = useState<number | null>(null);
    const [search, setSearch] = useState('');
    const [filter, setFilter] = useState<ProcessInstanceEventLogFilter>('all');
    const [sortOrder, setSortOrder] = useState<ProcessInstanceEventLogSortOrder>('DESC');
    const [isLoading, setIsLoading] = useState(false);
    const [isLoadingMore, setIsLoadingMore] = useState(false);
    const [loadFailed, setLoadFailed] = useState(false);
    const [loadMoreFailed, setLoadMoreFailed] = useState(false);
    const [reloadVersion, setReloadVersion] = useState(0);
    const requestVersionRef = useRef(0);

    const loadFirstPage = useCallback((signal?: AbortSignal) => {
        const requestVersion = ++requestVersionRef.current;
        setIsLoading(true);
        setIsLoadingMore(false);
        setLoadFailed(false);
        setLoadMoreFailed(false);
        setEventLog((current) => {
            const matchesInstance = current?.instance.id === instanceId;
            const matchesTask = taskId == null ? current?.task == null : current?.task?.id === taskId;
            return matchesInstance && matchesTask ? current : null;
        });

        eventApiService.getEventLog({
            processInstanceId: instanceId,
            processInstanceTaskId: taskId ?? undefined,
            page: 0,
            size: EVENT_PAGE_SIZE,
            search,
            filter,
            sortOrder,
            abort: signal,
        })
            .then((nextEventLog) => {
                if (requestVersion !== requestVersionRef.current) {
                    return;
                }

                setEventLog(nextEventLog);
                setSelectedEventId(nextEventLog.events.content[0]?.id ?? null);
            })
            .catch(() => {
                if (signal?.aborted || requestVersion !== requestVersionRef.current) {
                    return;
                }
                setLoadFailed(true);
            })
            .finally(() => {
                if (requestVersion === requestVersionRef.current) {
                    setIsLoading(false);
                }
            });
    }, [filter, instanceId, search, sortOrder, taskId]);

    useEffect(() => {
        if (!open) {
            return;
        }

        const controller = new AbortController();
        loadFirstPage(controller.signal);
        return () => controller.abort();
    }, [loadFirstPage, open, reloadVersion]);

    const handleLoadMore = () => {
        if (eventLog == null || isLoadingMore) {
            return;
        }

        const nextPage = eventLog.events.page.number + 1;
        const requestVersion = requestVersionRef.current;
        setIsLoadingMore(true);
        setLoadMoreFailed(false);
        eventApiService.getEventLog({
            processInstanceId: instanceId,
            processInstanceTaskId: taskId ?? undefined,
            page: nextPage,
            size: EVENT_PAGE_SIZE,
            search,
            filter,
            sortOrder,
        })
            .then((nextEventLog) => {
                if (requestVersion !== requestVersionRef.current) {
                    return;
                }
                setEventLog((current) => current == null ? nextEventLog : ({
                    ...nextEventLog,
                    events: {
                        ...nextEventLog.events,
                        content: [...current.events.content, ...nextEventLog.events.content],
                    },
                }));
            })
            .catch(() => {
                if (requestVersion === requestVersionRef.current) {
                    setLoadMoreFailed(true);
                }
            })
            .finally(() => {
                if (requestVersion === requestVersionRef.current) {
                    setIsLoadingMore(false);
                }
            });
    };

    const selectedEvent = useMemo(() => eventLog?.events.content
        .find(event => event.id === selectedEventId) ?? null, [eventLog, selectedEventId]);
    const totalEvents = eventLog?.events.page.totalElements ?? 0;
    const hasMoreEvents = eventLog != null && eventLog.events.page.number + 1 < eventLog.events.page.totalPages;

    const handleClose = () => {
        requestVersionRef.current += 1;
        onClose();
    };

    return (
        <Dialog
            open={open}
            onClose={handleClose}
            fullWidth
            maxWidth="xl"
            slotProps={{
                paper: {
                    sx: {
                        height: 'min(52rem, calc(100vh - 4rem))',
                        maxHeight: 'calc(100vh - 4rem)',
                    },
                },
            }}
        >
            <DialogTitleWithClose onClose={handleClose}>
                Ereignisprotokoll
            </DialogTitleWithClose>

            <DialogContent
                sx={{
                    p: 0,
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                }}
            >
                <EventLogContextHeader
                    eventLog={eventLog}
                    isLoading={isLoading && eventLog == null}
                    taskRequested={taskId != null}
                />

                <EventLogToolbar
                    disabled={eventLog == null}
                    filter={filter}
                    loading={isLoading && eventLog != null}
                    search={search}
                    sortOrder={sortOrder}
                    totalEvents={totalEvents}
                    onFilterChange={setFilter}
                    onSearchChange={setSearch}
                    onSortOrderChange={setSortOrder}
                />

                <Divider/>

                {
                    loadFailed ?
                        <EventLogError onRetry={() => setReloadVersion(version => version + 1)}/> :
                        <EventLogContent
                            eventLog={eventLog}
                            filter={filter}
                            hasMoreEvents={hasMoreEvents}
                            isLoading={isLoading}
                            isLoadingMore={isLoadingMore}
                            loadMoreFailed={loadMoreFailed}
                            search={search}
                            selectedEvent={selectedEvent}
                            selectedEventId={selectedEventId}
                            onLoadMore={handleLoadMore}
                            onSelectEvent={setSelectedEventId}
                        />
                }
            </DialogContent>
        </Dialog>
    );
}

function EventLogContextHeader(props: {
    eventLog: ProcessInstanceEventLog | null;
    isLoading: boolean;
    taskRequested: boolean;
}) {
    if (props.isLoading) {
        return (
            <Box sx={{px: 3, pb: 3}}>
                <Skeleton width={230} height={30}/>
                <Stack direction="row" spacing={5} sx={{mt: 1.5}}>
                    <Skeleton width={150}/>
                    <Skeleton width={150}/>
                    <Skeleton width={120}/>
                </Stack>
            </Box>
        );
    }
    if (props.eventLog == null) {
        return null;
    }

    const {instance, task} = props.eventLog;
    return (
        <Box
            sx={{
                px: 3,
                pb: 3,
                display: 'grid',
                gridTemplateColumns: props.taskRequested ? '1fr 1fr' : '1fr',
                gap: 4,
            }}
        >
            <RuntimeContext
                label="Vorgang"
                title={instance.caseNumber}
                started={instance.started}
                finished={instance.finished}
                runtime={instance.runtime}
            />
            {
                props.taskRequested && task != null &&
                <RuntimeContext
                    label="Aufgabe"
                    title={task.name}
                    started={task.started}
                    finished={task.finished}
                    runtime={task.runtime}
                    separated
                />
            }
        </Box>
    );
}

function RuntimeContext(props: {
    label: string;
    title: string;
    started: string;
    finished: string | null;
    runtime: number | null;
    separated?: boolean;
}) {
    return (
        <Box sx={{pl: props.separated ? 4 : 0, borderLeft: props.separated ? '1px solid' : 0, borderColor: 'divider'}}>
            <Typography variant="overline" sx={{
                color: "text.secondary"
            }}>
                {props.label}
            </Typography>
            <Typography variant="h6" component="div" sx={{lineHeight: 1.25}}>
                {props.title}
            </Typography>
            <Stack direction="row" spacing={4} sx={{mt: 1.5}}>
                <ContextValue label="Begonnen" value={formatEventTimestamp(props.started)}/>
                <ContextValue
                    label="Beendet"
                    value={props.finished == null ? 'Noch nicht beendet' : formatEventTimestamp(props.finished)}
                />
                <ContextValue label="Laufzeit" value={formatRuntime(props.runtime)}/>
            </Stack>
        </Box>
    );
}

function ContextValue(props: {label: string; value: string}) {
    return (
        <Box>
            <Typography variant="caption" component="div" sx={{
                color: "text.secondary"
            }}>
                {props.label}
            </Typography>
            <Typography variant="body2" component="div">
                {props.value}
            </Typography>
        </Box>
    );
}

function EventLogToolbar(props: {
    disabled: boolean;
    filter: ProcessInstanceEventLogFilter;
    loading: boolean;
    search: string;
    sortOrder: ProcessInstanceEventLogSortOrder;
    totalEvents: number;
    onFilterChange: (filter: ProcessInstanceEventLogFilter) => void;
    onSearchChange: (search: string) => void;
    onSortOrderChange: (sortOrder: ProcessInstanceEventLogSortOrder) => void;
}) {
    const countLabel = `${props.totalEvents} ${props.totalEvents === 1 ? 'Ereignis' : 'Ereignisse'}`;
    return (
        <Stack
            direction="row"
            spacing={2}
            sx={{
                alignItems: "center",
                px: 3,
                pb: 2
            }}>
            <SearchInput
                value={props.search}
                onChange={props.onSearchChange}
                label="Ereignisse durchsuchen"
                placeholder="Titel, Nachricht, Prozesselement oder Auslöser"
                debounce={300}
                disabled={props.disabled}
                sx={{width: 430, minWidth: 320}}
            />
            <ToggleButtonGroup
                value={props.filter}
                exclusive
                size="small"
                onChange={(_event, value: ProcessInstanceEventLogFilter | null) => {
                    if (value != null) {
                        props.onFilterChange(value);
                    }
                }}
                aria-label="Ereignisse filtern"
                disabled={props.disabled}
            >
                <ToggleButton value="all">Alle</ToggleButton>
                <ToggleButton value="notable">Warnungen und Fehler</ToggleButton>
            </ToggleButtonGroup>
            <Box sx={{flex: 1}}/>
            <CircularProgress
                size={16}
                aria-label="Ereignisse werden aktualisiert"
                sx={{visibility: props.loading ? 'visible' : 'hidden'}}
            />
            <Typography
                variant="body2"
                sx={{
                    color: "text.secondary",
                    whiteSpace: 'nowrap'
                }}>
                {countLabel}
            </Typography>
            <Tooltip
                arrow
                title={props.sortOrder === 'DESC' ? 'Älteste Ereignisse zuerst anzeigen' : 'Neueste Ereignisse zuerst anzeigen'}
            >
                <span>
                    <Button
                        variant="text"
                        size="small"
                        disabled={props.disabled}
                        startIcon={props.sortOrder === 'DESC' ? <ArrowDownward/> : <ArrowUpward/>}
                        onClick={() => props.onSortOrderChange(props.sortOrder === 'DESC' ? 'ASC' : 'DESC')}
                    >
                        {props.sortOrder === 'DESC' ? 'Neueste zuerst' : 'Älteste zuerst'}
                    </Button>
                </span>
            </Tooltip>
        </Stack>
    );
}

function EventLogContent(props: {
    eventLog: ProcessInstanceEventLog | null;
    filter: ProcessInstanceEventLogFilter;
    hasMoreEvents: boolean;
    isLoading: boolean;
    isLoadingMore: boolean;
    loadMoreFailed: boolean;
    search: string;
    selectedEvent: ProcessInstanceEventLogEntry | null;
    selectedEventId: number | null;
    onLoadMore: () => void;
    onSelectEvent: (eventId: number) => void;
}) {
    if (props.eventLog == null) {
        return <EventLogSkeleton/>;
    }

    if (!props.isLoading && props.eventLog.events.content.length === 0) {
        const filtered = props.search.trim().length > 0 || props.filter !== 'all';
        return (
            <Box sx={{flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                <EmptyDataListPlaceholder
                    title={filtered ? 'Keine passenden Ereignisse' : 'Noch keine Ereignisse vorhanden'}
                    description={filtered
                        ? 'Passen Sie die Suche oder den gewählten Filter an.'
                        : 'Für diesen Vorgang wurden bislang keine Ereignisse protokolliert.'}
                />
            </Box>
        );
    }

    return (
        <Box
            sx={{
                flex: 1,
                minHeight: 0,
                display: 'grid',
                gridTemplateColumns: 'minmax(22rem, 5fr) minmax(0, 7fr)',
            }}
        >
            <Box
                sx={{
                    minWidth: 0,
                    overflowY: 'auto',
                    borderRight: '1px solid',
                    borderColor: 'divider',
                    position: 'relative',
                }}
            >
                {
                    <>
                        <List disablePadding aria-label="Ereignisse" aria-busy={props.isLoading}>
                            {props.eventLog.events.content.map(event => (
                                <EventListItem
                                    key={event.id}
                                    event={event}
                                    selected={event.id === props.selectedEventId}
                                    onSelect={() => props.onSelectEvent(event.id)}
                                />
                            ))}
                        </List>
                        <Box sx={{display: 'flex', flexDirection: 'column', alignItems: 'center', py: 1.5}}>
                            {
                                props.loadMoreFailed &&
                                <Typography variant="body2" color="error" sx={{mb: 0.5}}>
                                    Weitere Ereignisse konnten nicht geladen werden.
                                </Typography>
                            }
                            {
                                props.hasMoreEvents &&
                                <Button
                                    variant="text"
                                    onClick={props.onLoadMore}
                                    disabled={props.isLoadingMore}
                                    startIcon={props.isLoadingMore ? <CircularProgress size={16}/> : undefined}
                                >
                                    Weitere Ereignisse laden
                                </Button>
                            }
                            <Typography variant="caption" sx={{
                                color: "text.secondary"
                            }}>
                                {props.eventLog.events.content.length} von {props.eventLog.events.page.totalElements} angezeigt
                            </Typography>
                        </Box>
                    </>
                }
            </Box>
            <Box sx={{minWidth: 0, overflowY: 'auto'}}>
                {
                    props.selectedEvent != null ?
                        <EventDetails event={props.selectedEvent}/> :
                        <Box sx={{height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center'}}>
                            <Typography sx={{
                                color: "text.secondary"
                            }}>Wählen Sie ein Ereignis aus.</Typography>
                        </Box>
                }
            </Box>
        </Box>
    );
}

function EventListItem(props: {
    event: ProcessInstanceEventLogEntry;
    selected: boolean;
    onSelect: () => void;
}) {
    const presentation = EVENT_LEVEL_PRESENTATION[props.event.level];
    return (
        <ListItemButton
            selected={props.selected}
            onClick={props.onSelect}
            aria-current={props.selected ? 'true' : undefined}
            sx={{
                alignItems: 'flex-start',
                gap: 1.75,
                px: 2.5,
                py: 1.75,
                borderBottom: '1px solid',
                borderColor: 'divider',
                '&:last-child': {borderBottom: 0},
            }}
        >
            <EventLevelIcon level={props.event.level} size="small"/>
            <Box sx={{minWidth: 0, flex: 1}}>
                <Stack direction="row" spacing={1.5} sx={{
                    alignItems: "baseline"
                }}>
                    <Typography variant="subtitle2" component="div" sx={{minWidth: 0, flex: 1}}>
                        {props.event.title}
                    </Typography>
                    <Typography
                        variant="caption"
                        sx={{
                            color: "text.secondary",
                            flexShrink: 0
                        }}>
                        {formatEventTimestamp(props.event.timestamp)}
                    </Typography>
                </Stack>
                <Typography
                    variant="body2"
                    sx={{
                        color: "text.secondary",
                        mt: 0.5,
                        display: '-webkit-box',
                        WebkitLineClamp: 2,
                        WebkitBoxOrient: 'vertical',
                        overflow: 'hidden'
                    }}>
                    {props.event.message}
                </Typography>
                <Typography
                    variant="caption"
                    sx={{
                        color: "text.secondary",
                        display: 'block',
                        mt: 0.75
                    }}>
                    {presentation.label} · {props.event.processNodeName ?? 'Vorgang'} · {getEventSource(props.event)}
                </Typography>
            </Box>
        </ListItemButton>
    );
}

function EventLevelIcon(props: {level: ProcessNodeExecutionLogLevel; size?: 'small' | 'medium'}) {
    const presentation = EVENT_LEVEL_PRESENTATION[props.level];
    const size = props.size === 'small' ? 34 : 42;
    return (
        <Tooltip arrow title={presentation.label}>
            <Box
                sx={(theme: Theme) => {
                    const color = presentation.palette == null
                        ? theme.palette.text.secondary
                        : (theme.palette[presentation.palette] as PaletteColor).main;
                    return {
                        width: size,
                        height: size,
                        flex: `0 0 ${size}px`,
                        borderRadius: '50%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color,
                        backgroundColor: alpha(color, theme.palette.mode === 'dark' ? 0.18 : 0.1),
                    };
                }}
            >
                {presentation.renderIcon(props.size === 'small' ? 'small' : 'medium')}
            </Box>
        </Tooltip>
    );
}

function EventDetails(props: {event: ProcessInstanceEventLogEntry}) {
    const {event} = props;
    const presentation = EVENT_LEVEL_PRESENTATION[event.level];
    const SourceIcon = event.triggeringUserId == null ? Memory : AccountBox;
    const hasDetails = Object.keys(event.details).length > 0;

    return (
        <Box sx={{p: 3, maxWidth: 900}}>
            <Stack direction="row" spacing={1.5} sx={{
                alignItems: "center"
            }}>
                <EventLevelIcon level={event.level}/>
                <Box sx={{minWidth: 0}}>
                    <Typography
                        variant="overline"
                        component="div"
                        sx={{
                            color: "text.secondary",
                            lineHeight: 1.25
                        }}>
                        {presentation.label}
                    </Typography>
                    <Typography variant="h5" component="h2" sx={{lineHeight: 1.25, mt: 0.25}}>
                        {event.title}
                    </Typography>
                </Box>
            </Stack>

            <Typography sx={{mt: 2.5, whiteSpace: 'pre-line'}}>
                {event.message}
            </Typography>

            <Divider sx={{my: 3}}/>

            <Typography variant="subtitle1" component="h3" sx={{mb: 1.5}}>
                Ereignisdetails
            </Typography>
            <Box
                component="dl"
                sx={{
                    m: 0,
                    display: 'grid',
                    gridTemplateColumns: '10rem minmax(0, 1fr)',
                    columnGap: 2,
                    rowGap: 1.25,
                    '& dt': {color: 'text.secondary'},
                    '& dd': {m: 0, minWidth: 0},
                }}
            >
                <Typography component="dt" variant="body2">Ereignis-ID</Typography>
                <Typography component="dd" variant="body2">{event.id}</Typography>
                <Typography component="dt" variant="body2">Zeitpunkt</Typography>
                <Typography component="dd" variant="body2">{formatEventTimestamp(event.timestamp)}</Typography>
                <Typography component="dt" variant="body2">Auslöser</Typography>
                <Stack component="dd" direction="row" spacing={1} sx={{
                    alignItems: "center"
                }}>
                    <SourceIcon fontSize="small" sx={{color: 'text.secondary'}}/>
                    <Typography variant="body2">{getEventSource(event)}</Typography>
                </Stack>
                <Typography component="dt" variant="body2">Prozesselement</Typography>
                <Typography component="dd" variant="body2">{event.processNodeName ?? 'Vorgang'}</Typography>
                <Typography component="dt" variant="body2">Klassifizierung</Typography>
                <Stack component="dd" direction="row" spacing={1} useFlexGap sx={{
                    flexWrap: "wrap"
                }}>
                    <Chip
                        size="small"
                        variant="outlined"
                        label={event.technical ? 'Technisch' : 'Nicht technisch'}
                    />
                    <Chip
                        size="small"
                        variant="outlined"
                        label={event.audit ? 'Audit-relevant' : 'Nicht audit-relevant'}
                    />
                </Stack>
            </Box>

            {
                hasDetails &&
                <Box sx={{mt: 3}}>
                    <Typography variant="subtitle1" component="h3" sx={{mb: 1.5}}>
                        Strukturierte Details
                    </Typography>
                    <ExpandableCodeBlock
                        value={JSON.stringify(event.details, null, 2)}
                        language="json"
                        wrapLines
                    />
                </Box>
            }
        </Box>
    );
}

function EventLogError(props: {onRetry: () => void}) {
    return (
        <Box sx={{p: 3}}>
            <AlertComponent
                color="error"
                title="Ereignisprotokoll konnte nicht geladen werden"
                text="Beim Laden der Ereignisse ist ein Fehler aufgetreten. Versuchen Sie es erneut."
            >
                <Button variant="outlined" startIcon={<Refresh/>} onClick={props.onRetry} sx={{mt: 1.5}}>
                    Erneut versuchen
                </Button>
            </AlertComponent>
        </Box>
    );
}

function EventLogSkeleton() {
    return (
        <Box sx={{flex: 1, minHeight: 0, display: 'grid', gridTemplateColumns: '5fr 7fr'}}>
            <EventListSkeleton/>
            <EventDetailsSkeleton/>
        </Box>
    );
}

function EventListSkeleton() {
    return (
        <Box sx={{borderRight: '1px solid', borderColor: 'divider', p: 2.5}}>
            {[0, 1, 2, 3].map(index => (
                <Stack key={index} direction="row" spacing={2} sx={{py: 1.5}}>
                    <Skeleton variant="circular" width={34} height={34}/>
                    <Box sx={{flex: 1}}>
                        <Skeleton width={`${72 - index * 6}%`}/>
                        <Skeleton width="94%"/>
                        <Skeleton width="55%"/>
                    </Box>
                </Stack>
            ))}
        </Box>
    );
}

function EventDetailsSkeleton() {
    return (
        <Box sx={{p: 3.5}}>
            <Stack direction="row" spacing={2}>
                <Skeleton variant="circular" width={42} height={42}/>
                <Box sx={{flex: 1}}>
                    <Skeleton width={90}/>
                    <Skeleton width="45%" height={34}/>
                </Box>
            </Stack>
            <Skeleton width="90%" sx={{mt: 3}}/>
            <Skeleton width="75%"/>
            <Skeleton width="82%"/>
        </Box>
    );
}
