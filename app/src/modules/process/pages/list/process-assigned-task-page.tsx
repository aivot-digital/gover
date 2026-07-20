import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Dialog, DialogContent, Typography} from '@mui/material';
import {ProcessInstanceTaskApiService} from '../../services/process-instance-task-api-service';
import {ProcessInstanceTaskEntity} from '../../entities/process-instance-task-entity';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';
import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../../enums/process-task-status';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {ListControlRef} from '../../../../components/generic-list/generic-list-props';
import Refresh from '@aivot/mui-material-symbols-400-n25-outlined/Refresh';
import {ProcessInstanceEntity} from '../../entities/process-instance-entity';
import {ProcessInstanceApiService} from '../../services/process-instance-api-service';
import TaskAlt from '@aivot/mui-material-symbols-400-n25-outlined/TaskAlt';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {ProcessEntity} from '../../entities/process-entity';
import {ProcessNodeEntity} from '../../entities/process-node-entity';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {ProcessNodeApiService} from '../../services/process-node-api-service';
import {ProcessNodeProvider, ProcessNodeProviderApiService} from '../../services/process-node-provider-api-service';
import {getNodeName} from '../details/components/process-flow-editor/utils/node-utils';
import {CellLink} from '../../../../components/cell-link/cell-link';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import {dispatchProcessAssignedTaskCountRefreshEvent} from '../../utils/process-assigned-task-count-events';
import {DialogTitleWithClose} from '../../../../components/dialog-title-with-close/dialog-title-with-close';
import {useRetainedDialogValue} from '../../../../hooks/use-retained-dialog-value';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectUser} from '../../../../slices/user-slice';

interface ProcessInstanceTaskEntityWithInstance extends ProcessInstanceTaskEntity {
    instance: ProcessInstanceEntity;
    process: ProcessEntity;
    node: ProcessNodeEntity;
    provider: ProcessNodeProvider;
}

export function ProcessAssignedTaskListPage() {
    const user = useAppSelector(selectUser);
    const listRef = useRef<ListControlRef | null>(null);
    const [selectedTaskData, setSelectedTaskData] = useState<ProcessInstanceTaskEntityWithInstance | null>(null);
    const isTaskDataDialogOpen = selectedTaskData != null;
    const renderTaskData = useRetainedDialogValue(isTaskDataDialogOpen, selectedTaskData);

    useEffect(() => {
        dispatchProcessAssignedTaskCountRefreshEvent();
    }, []);

    const handleRefresh = useCallback(() => {
        listRef.current?.refresh();
    }, []);

    const header = useMemo(() => ({
        icon: <TaskAlt/>,
        title: 'Aufgaben',
        actions: [
            {
                tooltip: 'Liste aktualisieren',
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
                        Erhalten Sie hier einen Überblick über Ihre aktuellen Aufgaben in laufenden Vorgängen. Klicken Sie auf eine Aufgabe, um weitere Details zu sehen und die Aufgabe zu bearbeiten.
                    </Typography>
                </>
            ),
        },
    }), [handleRefresh]);

    const fetchAssignedTasks = useCallback((options: {
        page: number;
        size: number;
        sort?: string;
        order?: 'ASC' | 'DESC';
    }) => {
        return Promise.all([
            new ProcessInstanceTaskApiService()
                .list(options.page, options.size, options.sort as any, options.order, {
                    assignedUserId: user?.id,
                    status: ProcessTaskStatus.Running,
                }),
            new ProcessInstanceApiService()
                .listAll(),
            new ProcessDefinitionApiService()
                .listAll(),
            new ProcessNodeApiService()
                .listAll(),
            new ProcessNodeProviderApiService()
                .getNodeProviders(),
        ]).then(([tasks, instances, processes, nodes, providers]) => {
            return {
                ...tasks,
                content: tasks.content.map((task) => {
                    const node = nodes.content.find((entry) => entry.id === task.processNodeId)!;
                    return {
                        ...task,
                        instance: instances.content.find((entry) => entry.id === task.processInstanceId)!,
                        process: processes.content.find((entry) => entry.id === task.processId)!,
                        node: nodes.content.find((entry) => entry.id === task.processNodeId)!,
                        provider: providers.find((entry) => entry.key === node.processNodeDefinitionKey)!,
                    };
                }),
            };
        });
    }, [user?.id]);

    const columnIcon = useMemo(() => <Task/>, []);

    const columnDefinitions = useMemo(() => [
        {
            field: 'status',
            headerName: 'Status',
            flex: 1,
            renderCell: (params: any) => {
                const row = params.row as ProcessInstanceTaskEntityWithInstance;
                if (params.row.statusOverride != null) {
                    return row.statusOverride;
                }
                return ProcessTaskStatusLabels[row.status];
            },
        },
        {
            field: 'processDefinitionNodeId',
            headerName: 'Aufgabe',
            flex: 2,
            renderCell: (params: any) => {
                const node = params.row.node;
                const provider = params.row.provider;
                return (
                    <CellLink to={`/tasks/${params.row.instance?.id}/${params.row.id}`}>
                        {getNodeName(node, provider)}
                    </CellLink>
                );
            },
        },
        {
            field: 'started',
            headerName: 'Erstellt',
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
    ], []);

    const getRowIdentifier = useCallback((row: ProcessInstanceTaskEntityWithInstance) => row.id.toString(), []);

    const handleTaskDataDialogClose = useCallback(() => {
        setSelectedTaskData(null);
    }, []);

    const rowActions = useCallback((item: ProcessInstanceTaskEntityWithInstance) => [
        {
            icon: <DataObject/>,
            onClick: () => setSelectedTaskData(item),
            tooltip: 'Daten ansehen',
        },
    ], []);

    return (
        <>
            <PageWrapper
                title="Aufgaben"
                fullWidth
                background
            >
                <GenericListPage<ProcessInstanceTaskEntityWithInstance>
                    controlRef={listRef}
                    header={header}
                    searchLabel="Aufgabe suchen"
                    searchPlaceholder="Name der Aufgabe eingeben…"
                    fetch={fetchAssignedTasks}
                    columnIcon={columnIcon}
                    columnDefinitions={columnDefinitions}
                    getRowIdentifier={getRowIdentifier}
                    noDataPlaceholder={
                        <EmptyDataListPlaceholder
                            title="Aktuell keine Aufgaben zu bearbeiten"
                            description="Diese Liste zeigt Bearbeitungsschritte aus laufenden Vorgängen, für die aktuell Ihre Mitarbeit erforderlich ist."
                        />
                    }
                    noSearchResultsPlaceholder="Keine Aufgaben gefunden"
                    rowActionsCount={3}
                    rowActions={rowActions}
                    defaultSortField="started"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            <Dialog
                open={isTaskDataDialogOpen}
                onClose={handleTaskDataDialogClose}
                fullWidth
                maxWidth="md"
            >
                <DialogTitleWithClose onClose={handleTaskDataDialogClose}>
                    Daten der Aufgabe
                </DialogTitleWithClose>
                <DialogContent tabIndex={0}>
                    {
                        renderTaskData != null &&
                        <>
                            <Typography variant="h6">
                                Vorgangsdaten der Aufgabe
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Die Vorgangsdaten, welche die Aufgabe weitergegeben hat.
                            </Typography>
                            <ExpandableCodeBlock
                                value={JSON.stringify(renderTaskData.processData, null, 2)}
                            />

                            <Typography variant="h6">
                                Elementdaten der Aufgabe
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                                Die Elementdaten, die diese Aufgabe erzeugt hat.
                            </Typography>
                            <ExpandableCodeBlock
                                value={JSON.stringify(renderTaskData.nodeData, null, 2)}
                            />
                        </>
                    }
                </DialogContent>
            </Dialog>
        </>
    );
}
