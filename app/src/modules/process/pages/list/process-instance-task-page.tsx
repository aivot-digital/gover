import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {useParams} from 'react-router-dom';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {ProcessNodeEntity} from '../../entities/process-node-entity';
import {ProcessNodeProvider, ProcessNodeProviderApiService} from '../../services/process-node-provider-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {useConfirm} from '../../../../providers/confirm-provider';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';
import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../../enums/process-task-status';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {ListControlRef} from '../../../../components/generic-list/generic-list-props';
import Refresh from '@aivot/mui-material-symbols-400-outlined/dist/refresh/Refresh';
import {ProcessInstanceEntity} from '../../entities/process-instance-entity';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import Replay from '@aivot/mui-material-symbols-400-outlined/dist/replay/Replay';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import TaskAlt from '@aivot/mui-material-symbols-400-outlined/dist/task-alt/TaskAlt';
import DataObject from '@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject';
import FactCheck from '@aivot/mui-material-symbols-400-outlined/dist/fact-check/FactCheck';
import {ProcessInstanceEventDialog} from '../../dialogs/process-instance-event-dialog';
import News from '@aivot/mui-material-symbols-400-outlined/dist/news/News';

interface ProcessInstanceTaskEntityWithNodeAndProvider extends ProcessInstanceTaskEntity {
    node: ProcessNodeEntity;
    provider: ProcessNodeProvider;
}

export function ProcessInstanceTaskListPage() {
    const params = useParams();
    const dispatch = useAppDispatch();
    const [instance, setInstance] = useState<ProcessInstanceEntity>();
    const listRef = useRef<ListControlRef | null>(null);
    const confirm = useConfirm();
    const [showEvents, setShowEvents] = useState(false);

    const processId = useMemo(() => Number.parseInt(params.processId ?? '0', 10), [params.processId]);
    const processVersion = useMemo(() => Number.parseInt(params.processVersion ?? '0', 10), [params.processVersion]);
    const instanceId = useMemo(() => Number.parseInt(params.instanceId ?? '0', 10), [params.instanceId]);

    useEffect(() => {
        new ProcessInstanceApiService()
            .retrieve(instanceId)
            .then(setInstance);
    }, [instanceId]);

    const handleRefresh = useCallback(() => {
        listRef.current?.refresh();
    }, []);

    const header = useMemo(() => ({
        icon: <TaskAlt/>,
        title: 'Aufgaben',
        actions: [
            {
                tooltip: 'Events',
                icon: <News/>,
                onClick: () => {
                    setShowEvents(true);
                },
            },
            {
                tooltip: 'Refresh',
                icon: <Refresh/>,
                onClick: handleRefresh,
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
    }), [handleRefresh]);

    const fetchTasks = useCallback((options: {
        page: number;
        size: number;
        sort?: string;
        order?: 'ASC' | 'DESC';
    }) => {
        return Promise.all([
            new ProcessNodeProviderApiService()
                .getNodeProviders(),
            new ProcessNodeApiService()
                .listAll({
                    processId,
                    processVersion,
                }),
            new ProcessInstanceTaskApiService()
                .list(
                    options.page,
                    options.size,
                    options.sort as any,
                    options.order,
                    {
                        processDefinitionId: processId,
                        processDefinitionVersion: processVersion,
                        processInstanceId: instanceId,
                    },
                ),
        ]).then(([providers, {content: nodes}, tasksPage]) => {
            const enrichedTasks: ProcessInstanceTaskEntityWithNodeAndProvider[] = [];

            for (const task of tasksPage.content) {
                const node = nodes.find((entry) => entry.id === task.processNodeId);
                const provider = node
                    ? providers.find((entry) => entry.key === node.processNodeDefinitionKey)
                    : undefined;
                enrichedTasks.push({
                    ...task,
                    node: node!,
                    provider: provider!,
                });
            }

            console.log(enrichedTasks);

            return {
                ...tasksPage,
                content: enrichedTasks,
            };
        });
    }, [instanceId, processId, processVersion]);

    const columnIcon = useMemo(() => <TaskAlt/>, []);

    const columnDefinitions = useMemo(() => [
        {
            field: 'status',
            headerName: 'Status',
            flex: 1,
            renderCell: (params: any) => {
                const row = params.row as ProcessInstanceTaskEntityWithNodeAndProvider;
                if (row.statusOverride != null) {
                    return row.statusOverride;
                }
                return ProcessTaskStatusLabels[row.status];
            },
        },
        {
            field: 'started',
            headerName: 'Gestartet',
            flex: 1,
            renderCell: (params: any) => {
                if (!params.row.started) return '—';
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
            field: 'finished',
            headerName: 'Beendet',
            flex: 1,
            renderCell: (params: any) => {
                if (!params.row.finished) return '—';
                const date = new Date(params.row.finished);
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
            field: 'processDefinitionNodeId',
            headerName: 'Knoten',
            flex: 1,
            renderCell: (params: any) => {
                const node = params.row.node;
                const provider = params.row.provider;
                return provider
                    ? `${provider.name} (${node.id})`
                    : node
                        ? node.id
                        : 'Unbekannt';
            },
        },
    ], []);

    const getRowIdentifier = useCallback((row: ProcessInstanceTaskEntityWithNodeAndProvider) => row.id.toString(), []);

    const rowActions = useCallback((item: ProcessInstanceTaskEntityWithNodeAndProvider) => [
        {
            icon: <FactCheck/>,
            to: `/tasks/${instance?.accessKey}/${item.accessKey}`,
            tooltip: 'Aufgabenansicht öffnen',
        },
        {
            icon: <DataObject/>,
            onClick: () => {
                confirm({
                    title: 'Daten der Aufgabe',
                    width: 'md',
                    children: (
                        <>
                            <Typography variant="h6">
                                Vorgangsdaten der Aufgabe
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Die Vorgangsdaten, welche die Aufgabe weitergegeben hat.
                            </Typography>
                            <ExpandableCodeBlock
                                value={JSON.stringify(item.processData, null, 2)}
                            />

                            <Typography variant="h6">
                                Elementdaten der Aufgabe
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Die Elementdaten, die diese Aufgabe erzeugt hat.
                            </Typography>
                            <ExpandableCodeBlock
                                value={JSON.stringify(item.nodeData, null, 2)}
                            />
                        </>
                    ),
                });
            },
            tooltip: 'Daten ansehen',
        },
        {
            icon: <Replay/>,
            onClick: () => {
                if (item.status !== ProcessTaskStatus.Failed) {
                    return;
                }

                confirm({
                    title: 'Aufgabe neu starten',
                    children: (
                        <Typography>
                            Wirklich neu starten?
                        </Typography>
                    ),
                })
                    .then((confirmed) => {
                        if (!confirmed) {
                            return;
                        }

                        return new ProcessInstanceTaskApiService()
                            .rerunFailedTask(item.id)
                            .then(() => {
                                dispatch(setLoadingMessage({
                                    message: 'Task wird neu gestartet',
                                    blocking: true,
                                    estimatedTime: 2000,
                                }));
                                setTimeout(() => {
                                    handleRefresh();
                                    dispatch(clearLoadingMessage());
                                }, 2000);
                            });
                    });
            },
            tooltip: 'Fehlgeschlagenen Task neu starten',
            disabled: item.status !== ProcessTaskStatus.Failed,
        },
    ], [confirm, dispatch, handleRefresh, instance?.accessKey]);

    return (
        <>
            <PageWrapper
                title="Aufgaben"
                fullWidth
                background
            >
                <GenericListPage<ProcessInstanceTaskEntityWithNodeAndProvider>
                    controlRef={listRef}
                    header={header}
                    searchLabel="Team suchen"
                    searchPlaceholder="Name des Teams eingeben…"
                    fetch={fetchTasks}
                    columnIcon={columnIcon}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder="Keine Team angelegt"
                    noSearchResultsPlaceholder="Keine Teams gefunden"
                    rowActionsCount={3}
                    rowActions={rowActions}
                    defaultSortField="started"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            <ProcessInstanceEventDialog
                open={showEvents}
                onClose={() => setShowEvents(false)}
                instanceId={instanceId}
                taskId={null}
            />
        </>
    );
}
