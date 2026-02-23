import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {useUser} from '../../../../hooks/use-admin-guard';
import {ProcessInstanceTaskApiService} from "../../services/process-instance-task-api-service";
import {ProcessInstanceTaskEntity} from "../../entities/process-instance-task-entity";
import {useConfirm} from "../../../../providers/confirm-provider";
import {ExpandableCodeBlock} from "../../../../components/expandable-code-block/expandable-code-block";
import {ProcessTaskStatus, ProcessTaskStatusLabels} from "../../enums/process-task-status";
import React, {useRef} from "react";
import {ListControlRef} from "../../../../components/generic-list/generic-list-props";
import Refresh from "@aivot/mui-material-symbols-400-outlined/dist/refresh/Refresh";
import {ProcessInstanceEntity} from "../../entities/process-instance-entity";
import {ProcessInstanceApiService} from "../../services/process-instance-api-service";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import TaskAlt from "@aivot/mui-material-symbols-400-outlined/dist/task-alt/TaskAlt";
import DataObject from "@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject";
import {ProcessEntity} from "../../entities/process-entity";
import {ProcessNodeEntity} from "../../entities/process-node-entity";
import {ProcessDefinitionApiService} from "../../services/process-definition-api-service";
import {ProcessNodeApiService} from "../../services/process-node-api-service";
import {ProcessNodeProvider, ProcessNodeProviderApiService} from "../../services/process-node-provider-api-service";
import {getNodeName} from "../details/components/process-flow-editor/utils/node-utils";
import {CellLink} from "../../../../components/cell-link/cell-link";
import Task from '@aivot/mui-material-symbols-400-outlined/dist/task/Task';

interface ProcessInstanceTaskEntityWithInstance extends ProcessInstanceTaskEntity {
    instance: ProcessInstanceEntity;
    process: ProcessEntity;
    node: ProcessNodeEntity;
    provider: ProcessNodeProvider;
}

export function ProcessAssignedTaskListPage() {
    const dispatch = useAppDispatch();
    const user = useUser();

    const listRef = useRef<ListControlRef | null>(null);

    const confirm = useConfirm();

    return (
        <PageWrapper
            title="Aufgaben"
            fullWidth
            background
        >
            <GenericListPage<ProcessInstanceTaskEntityWithInstance>
                controlRef={listRef}
                header={{
                    icon: <TaskAlt/>,
                    title: 'Aufgaben',
                    actions: [
                        {
                            tooltip: 'Liste aktualisieren',
                            icon: <Refresh/>,
                            onClick: () => {
                                if (listRef.current != null) {
                                    listRef.current.refresh();
                                }
                            },
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
                }}
                searchLabel="Aufgabe suchen"
                searchPlaceholder="Name der Aufgabe eingeben…"
                fetch={(options) => {
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
                    ])
                        .then(([tasks, instances, processes, nodes, providers]) => {
                            return {
                                ...tasks,
                                content: tasks.content.map((task) => {
                                    const node = nodes.content.find(n => n.id === task.processNodeId)!;
                                    return {
                                        ...task,
                                        instance: instances.content.find(i => i.id === task.processInstanceId)!,
                                        process: processes.content.find(p => p.id === task.processId)!,
                                        node: nodes.content.find(n => n.id === task.processNodeId)!,
                                        provider: providers.find(p => p.key === node.processNodeDefinitionKey)!,
                                    };
                                })
                            }
                        })
                }}
                columnIcon={<Task/>}
                columnDefinitions={[
                    {
                        field: 'status',
                        headerName: 'Status',
                        flex: 1,
                        renderCell: (params) => {
                            if (params.row.statusOverride != null) {
                                return params.row.statusOverride;
                            }
                            return ProcessTaskStatusLabels[params.row.status];
                        },
                    },
                    {
                        field: 'processDefinitionNodeId',
                        headerName: 'Aufgabe',
                        flex: 2,
                        renderCell: (params) => {
                            const node = params.row.node;
                            const provider = params.row.provider;
                            return (
                                <CellLink to={`/tasks/${params.row.instance?.id}/${params.row.id}`}>
                                    {getNodeName(node, provider)}
                                </CellLink>
                            );
                        }
                    },
                    {
                        field: 'started',
                        headerName: 'Erstellt',
                        flex: 1,
                        renderCell: (params) => {
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
                        }
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Aufgaben vorhanden"
                noSearchResultsPlaceholder="Keine Aufgaben gefunden"
                rowActionsCount={3}
                rowActions={(item) => [
                    {
                        icon: <DataObject/>,
                        onClick: () => {
                            confirm({
                                title: 'Daten der Aufgabe',
                                children: (
                                    <>
                                        <Typography variant="h6">
                                            Arbeitsdaten der Aufgabe:
                                        </Typography>
                                        <ExpandableCodeBlock
                                            value={JSON.stringify(item.processData, null, 2)}
                                        />
                                        <Typography variant="h6">
                                            Metadaten der Aufgabe:
                                        </Typography>
                                        <ExpandableCodeBlock
                                            value={JSON.stringify(item.nodeData, null, 2)}
                                        />
                                    </>
                                ),
                            })
                        },
                        tooltip: 'Daten ansehen',
                    },
                ]}
                defaultSortField="started"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}