import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
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
import Refresh from '@aivot/mui-material-symbols-400-n25-outlined/Refresh';
import {ProcessInstanceEntity} from '../../entities/process-instance-entity';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import Replay from '@aivot/mui-material-symbols-400-n25-outlined/Replay';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import TaskAlt from '@aivot/mui-material-symbols-400-n25-outlined/TaskAlt';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import FactCheck from '@aivot/mui-material-symbols-400-n25-outlined/FactCheck';
import {ProcessInstanceEventDialog} from '../../dialogs/process-instance-event-dialog';
import News from '@aivot/mui-material-symbols-400-n25-outlined/News';
import {formatInstantInApplicationTimeZone} from '../../../../utils/temporal-utils';

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
            title: 'Hilfe zu Aufgaben',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography>
                        Auf dieser Seite erhalten Sie einen Überblick über eine Aufgaben dieses Vorgangs.
                        Sie sehen den aktuellen Status, Start- und Endzeitpunkt sowie das zugehörige
                        Prozesselement.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Über die Aktionen können Sie die Aufgabenansicht öffnen, die weitergegebenen
                        Vorgangs- und Elementdaten einsehen oder fehlgeschlagene Aufgaben erneut starten.
                        Die Vorgangsereignisse öffnen Sie über die Aktion „Events“.
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
                const formatted = formatInstantInApplicationTimeZone(params.row.started, 'dd.MM.yyyy – HH:mm');
                return formatted != null ? `${formatted} Uhr` : '—';
            },
        },
        {
            field: 'finished',
            headerName: 'Beendet',
            flex: 1,
            renderCell: (params: any) => {
                if (!params.row.finished) return '—';
                const formatted = formatInstantInApplicationTimeZone(params.row.finished, 'dd.MM.yyyy – HH:mm');
                return formatted != null ? `${formatted} Uhr` : '—';
            },
        },
        {
            field: 'processDefinitionNodeId',
            headerName: 'Element',
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
            to: `/tasks/${instance?.id}/${item.id}`,
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
                    searchLabel="Aufgabe suchen"
                    searchPlaceholder="Name der Aufgabe eingeben…"
                    fetch={fetchTasks}
                    columnIcon={columnIcon}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Keine Aufgaben in diesem Vorgang vorhanden"
                            description="Aufgaben dokumentieren die Bearbeitungsschritte, die dieser Vorgang an Personen oder Teams übergibt."
                        />
                    }
                    noSearchResultsPlaceholder="Keine Aufgaben gefunden"
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
