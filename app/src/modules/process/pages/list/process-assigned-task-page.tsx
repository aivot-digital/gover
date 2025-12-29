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
import Replay from "@aivot/mui-material-symbols-400-outlined/dist/replay/Replay";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {clearLoadingMessage, setLoadingMessage} from "../../../../slices/shell-slice";
import TaskAlt from "@aivot/mui-material-symbols-400-outlined/dist/task-alt/TaskAlt";
import DataObject from "@aivot/mui-material-symbols-400-outlined/dist/data-object/DataObject";
import FactCheck from "@aivot/mui-material-symbols-400-outlined/dist/fact-check/FactCheck";
import {ProcessDefinitionEntity} from "../../entities/process-definition-entity";
import {ProcessDefinitionNodeEntity} from "../../entities/process-definition-node-entity";
import {ProcessDefinitionApiService} from "../../services/process-definition-api-service";
import {ProcessDefinitionNodeApiService} from "../../services/process-definition-node-api-service";
import {ProcessNodeProvider, ProcessNodeProviderApiService} from "../../services/process-node-provider-api-service";
import {getNodeName} from "../details/components/process-flow-editor/utils/node-utils";
import {CellLink} from "../../../../components/cell-link/cell-link";

interface ProcessInstanceTaskEntityWithInstance extends ProcessInstanceTaskEntity {
    instance: ProcessInstanceEntity;
    process: ProcessDefinitionEntity;
    node: ProcessDefinitionNodeEntity;
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
                            tooltip: 'Refresh',
                            icon: <Refresh/>,
                            onClick: () => {
                                if (listRef.current != null) {
                                    listRef.current.refresh();
                                }
                            },
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Teams',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Ein Fachbereich ist eine zentrale Verwaltungseinheit in Gover und essenziell für den
                                    Betrieb der Anwendung. Er speichert wichtige Stammdaten wie Adress- und Kontaktdaten
                                    sowie rechtliche Informationen (z.
                                    B. Impressum, Datenschutzerklärung), die in Formularen wiederverwendet werden
                                    können.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Jedem Fachbereich sind Mitarbeiter:innen mit einer spezifischen Rolle zugeordnet,
                                    die deren Berechtigungen innerhalb des Fachbereichs definiert.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Team suchen"
                searchPlaceholder="Name des Teams eingeben…"
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
                        new ProcessDefinitionNodeApiService()
                            .listAll(),
                        new ProcessNodeProviderApiService()
                            .getNodeProviders(),
                    ])
                        .then(([tasks, instances, processes, nodes, providers]) => {
                            return {
                                ...tasks,
                                content: tasks.content.map((task) => {
                                    const node = nodes.content.find(n => n.id === task.processDefinitionNodeId)!;
                                    return {
                                        ...task,
                                        instance: instances.content.find(i => i.id === task.processInstanceId)!,
                                        process: processes.content.find(p => p.id === task.processDefinitionId)!,
                                        node: nodes.content.find(n => n.id === task.processDefinitionNodeId)!,
                                        provider: providers.find(p => p.key === node.processNodeDefinitionKey)!,
                                    };
                                })
                            }
                        })
                }}
                columnIcon={<TaskAlt/>}
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
                noDataPlaceholder="Keine Team angelegt"
                noSearchResultsPlaceholder="Keine Teams gefunden"
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