import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {type ProcessInstanceEntity} from '../../entities/process-instance-entity';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import {ProcessInstanceStatus, ProcessInstanceStatusLabels} from '../../enums/process-instance-status';
import Refresh from '@aivot/mui-material-symbols-400-outlined/dist/refresh/Refresh';
import React, {type ReactNode, useEffect, useRef, useState} from 'react';
import {
    type GenericListPropsFetchOptions,
    type ListControlRef,
} from '../../../../components/generic-list/generic-list-props';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import ProcessChart from '@aivot/mui-material-symbols-400-outlined/dist/process-chart/ProcessChart';
import {ProcessInstanceHistoryEventDialog} from '../../dialogs/process-instance-history-event-dialog';
import News from '@aivot/mui-material-symbols-400-outlined/dist/news/News';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {type Page} from '../../../../models/dtos/page';
import {useSearchParams} from 'react-router-dom';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';

interface ProcessInstanceEntityWithProcessInfo extends ProcessInstanceEntity {
    processName: string;
}

export function ProcessInstanceListPage(): ReactNode {
    const dispatch = useAppDispatch();
    const [searchParams] = useSearchParams();
    const processIdParam = searchParams.get('processId');
    const processVersionParam = searchParams.get('processVersion');
    const processId = processIdParam !== null && processIdParam !== '' ? Number(processIdParam) : undefined;
    const processVersion = processVersionParam !== null && processVersionParam !== '' ? Number(processVersionParam) : undefined;

    // State for the process definition for the badge
    const [processDefinition, setProcessDefinition] = useState<any | null>(null);

    useEffect((): () => void => {
        let cancelled = false;

        async function fetchProcessDefinition(): Promise<void> {
            if (processId !== undefined) {
                const allProcesses = await new ProcessDefinitionApiService().listAll();
                const found = allProcesses.content.find(
                    (p) => p.id === processId,
                );
                if (!cancelled) {
                    setProcessDefinition(found ?? null);
                }
            } else {
                setProcessDefinition(null);
            }
        }

        void fetchProcessDefinition();

        return () => {
            cancelled = true;
        };
    }, [processId]);

    const listRef = useRef<ListControlRef | null>(null);

    const handleListRefresh = (): void => {
        if (listRef.current != null) {
            listRef.current.refresh();
        }
    };

    const handleDelete = (item: ProcessInstanceEntity): void => {
        const apiService = new ProcessInstanceApiService();
        apiService
            .destroy(item.id)
            .then(() => {
                handleListRefresh();
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Vorgang konnte nicht gelöscht werden'));
            });
    };

    const [showEventsForInstanceId, setShowEventsForInstanceId] = React.useState<number | null>(null);

    // Wrap fetchData to inject processId and processVersion
    const fetchDataWithParams = async (options: GenericListPropsFetchOptions<ProcessInstanceEntityWithProcessInfo>): Promise<Page<ProcessInstanceEntityWithProcessInfo>> => {
        const allProcesses = await new ProcessDefinitionApiService().listAll();
        const filter: any = {
            statusIsNot: options.filter === 'notCompleted' ? ProcessInstanceStatus.Completed : undefined,
        };
        if (processId !== undefined) {
            filter.processId = processId;
        }
        if (processVersion !== undefined) {
            filter.processVersion = processVersion;
        }
        const instances = await new ProcessInstanceApiService().list(
            options.page,
            options.size,
            options.sort !== 'processName' ? options.sort : 'processId',
            options.order,
            filter,
        );
        return {
            ...instances,
            content: instances.content.map((instance) => {
                const process = allProcesses.content.find((p) => p.id === instance.processId);
                return {
                    ...instance,
                    processName: process != null ? process.internalTitle : `Prozess #${instance.processId}`,
                };
            }),
        };
    };

    return (
        <>
            <PageWrapper
                title="Vorgänge"
                fullWidth
                background
            >
                <GenericListPage<ProcessInstanceEntityWithProcessInfo>
                    controlRef={listRef}
                    defaultFilter="notCompleted"
                    filters={[
                        {
                            label: 'Nicht abgeschlossen',
                            value: 'notCompleted',
                        },
                        {
                            label: 'Alle',
                            value: 'all',
                        },
                    ]}
                    header={{
                        icon: <ProcessChart/>,
                        title: 'Vorgänge',
                        badge:
                            (processDefinition !== null && processVersion !== undefined) ?
                                {
                                    label: `${String(processDefinition.internalTitle)} (Version ${processVersion})`,
                                    color: 'primary',
                                } :
                                undefined,
                        actions: [
                            {
                                tooltip: 'Refresh',
                                icon: <Refresh/>,
                                onClick: handleListRefresh,
                            },
                        ],
                        helpDialog: {
                            title: 'Hilfe zu Teams',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography>
                                        Ein Fachbereich ist eine zentrale Verwaltungseinheit in Gover und essenziell für
                                        den
                                        Betrieb der Anwendung. Er speichert wichtige Stammdaten wie Adress- und
                                        Kontaktdaten
                                        sowie rechtliche Informationen (z.
                                        B. Impressum, Datenschutzerklärung), die in Formularen wiederverwendet werden
                                        können.
                                    </Typography>
                                    <Typography sx={{mt: 2}}>
                                        Jedem Fachbereich sind Mitarbeiter:innen mit einer spezifischen Rolle
                                        zugeordnet,
                                        die deren Berechtigungen innerhalb des Fachbereichs definiert.
                                    </Typography>
                                </>
                            ),
                        },
                    }}
                    searchLabel="Vorgang suchen"
                    searchPlaceholder="Schlüssel des Vorgangs eingeben…"
                    fetch={fetchDataWithParams}
                    columnIcon={<ProcessChart/>}
                    columnDefinitions={[
                        {
                            field: 'processName',
                            headerName: 'Prozess',
                            flex: 1,
                            renderCell: (params) => (
                                <CellLink
                                    to={`/processes/${params.row.processId}/versions/${params.row.initialProcessVersion}/instances/${params.row.id}/tasks`}
                                    title="Aufrufen"
                                >
                                    {String(params.value)}
                                </CellLink>
                            ),
                        },
                        {
                            field: 'accessKey',
                            headerName: 'Schlüssel',
                            flex: 1,
                            renderCell: (params) => (
                                <CellLink
                                    to={`/processes/${params.row.processId}/versions/${params.row.initialProcessVersion}/instances/${params.row.id}/tasks`}
                                    title="Aufrufen"
                                >
                                    {String(params.value)}
                                </CellLink>
                            ),
                        },
                        {
                            field: 'started',
                            headerName: 'Gestartet am',
                            flex: 1,
                            renderCell: (params) => {
                                if (params.row.started === undefined || params.row.started === null || params.row.started === '') return '—';
                                const date = new Date(params.row.started);
                                return new Intl.DateTimeFormat('de-DE', {
                                    day: '2-digit',
                                    month: '2-digit',
                                    year: 'numeric',
                                    hour: '2-digit',
                                    minute: '2-digit',
                                    hour12: false,
                                }).format(date).replace(',', ' –') + ' Uhr';
                            },
                        },
                        {
                            field: 'status',
                            headerName: 'Status',
                            flex: 1,
                            renderCell: (params) => {
                                if (params.row.statusOverride != null) {
                                    return params.row.statusOverride;
                                }
                                return ProcessInstanceStatusLabels[params.row.status];
                            },
                        },
                    ]}
                    getRowIdentifier={(row) => row.id.toString()}
                    noDataPlaceholder="Keine Vorgänge gestartet"
                    noSearchResultsPlaceholder="Keine Vorgänge gefunden"
                    rowActionsCount={3}
                    rowActions={(item) => [
                        {
                            icon: <Delete/>,
                            tooltip: 'Vorgang löschen',
                            onClick: () => {
                                handleDelete(item);
                            },
                        },
                        {
                            icon: <News/>,
                            tooltip: 'Aufgaben Einsehen',
                            onClick: () => {
                                setShowEventsForInstanceId(item.id);
                            },
                        },
                        {
                            icon: ModuleIcons.processes,
                            tooltip: 'Prozessverlauf Einsehen',
                            to: `/processes/${item.processId}/versions/${item.initialProcessVersion}/?instanceId=${item.id}`,
                        },
                    ]}
                    defaultSortField="started"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            <ProcessInstanceHistoryEventDialog
                open={showEventsForInstanceId != null}
                onClose={() => {
                    setShowEventsForInstanceId(null);
                }}
                instanceId={showEventsForInstanceId ?? 0}
                taskId={null}
            />
        </>
    );
}
