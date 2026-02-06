import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {useParams} from "react-router-dom";
import {ProcessInstanceTaskApiService} from "../../services/process-instance-task-api-service";
import {ProcessInstanceTaskEntity} from "../../entities/process-instance-task-entity";
import {ProcessNodeEntity} from "../../entities/process-node-entity";
import {ProcessNodeProvider, ProcessNodeProviderApiService} from "../../services/process-node-provider-api-service";
import {ProcessNodeApiService} from "../../services/process-node-api-service";
import {useConfirm} from "../../../../providers/confirm-provider";
import {ExpandableCodeBlock} from "../../../../components/expandable-code-block/expandable-code-block";
import {ProcessTaskStatus, ProcessTaskStatusLabels} from "../../enums/process-task-status";
import React, {useEffect, useRef, useState} from "react";
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
import {ProcessInstanceHistoryEventDialog} from "../../dialogs/process-instance-history-event-dialog";
import News from "@aivot/mui-material-symbols-400-outlined/dist/news/News";

interface ProcessInstanceTaskEntityWithNodeAndProvider extends ProcessInstanceTaskEntity {
    node: ProcessNodeEntity;
    provider: ProcessNodeProvider;
}

export function ProcessInstanceTaskListPage() {
    const params = useParams();
    const dispatch = useAppDispatch();

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const [instance, setInstance] = useState<ProcessInstanceEntity>();

    useEffect(() => {
        new ProcessInstanceApiService()
            .retrieve(params.instanceId ? parseInt(params.instanceId) : 0)
            .then(setInstance);
    }, [params]);

    const listRef = useRef<ListControlRef | null>(null);

    const confirm = useConfirm();

    const [showEvents, setShowEvents] = useState(false);

    return (
        <>
            <PageWrapper
                title="Aufgaben"
                fullWidth
                background
            >
                <GenericListPage<ProcessInstanceTaskEntityWithNodeAndProvider>
                    controlRef={listRef}
                    header={{
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
                    searchLabel="Team suchen"
                    searchPlaceholder="Name des Teams eingeben…"
                    fetch={(options) => {
                        return Promise
                            .all([
                                new ProcessNodeProviderApiService()
                                    .getNodeProviders(),

                                new ProcessNodeApiService()
                                    .listAll({
                                        processDefinitionId: parseInt(params.processId!),
                                        processDefinitionVersion: parseInt(params.processVersion!),
                                    }),

                                new ProcessInstanceTaskApiService()
                                    .list(
                                        options.page,
                                        options.size,
                                        options.sort as any,
                                        options.order,
                                        {
                                            processDefinitionId: parseInt(params.processId!),
                                            processDefinitionVersion: parseInt(params.processVersion!),
                                            processInstanceId: parseInt(params.instanceId!),
                                        },
                                    )
                            ])
                            .then(([providers, {content: nodes}, tasksPage]) => {
                                const enrichedTasks: ProcessInstanceTaskEntityWithNodeAndProvider[] = [];

                                for (const task of tasksPage.content) {
                                    const node = nodes.find(n => n.id === task.processNodeId);
                                    const provider = node
                                        ? providers.find(p => p.key === node.processNodeDefinitionKey)
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
                            field: 'started',
                            headerName: 'Gestartet',
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
                        {
                            field: 'finished',
                            headerName: 'Beendet',
                            flex: 1,
                            renderCell: (params) => {
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
                            }
                        },
                        {
                            field: 'processDefinitionNodeId',
                            headerName: 'Knoten',
                            flex: 1,
                            renderCell: (params) => {
                                const node = params.row.node;
                                const provider = params.row.provider;
                                return provider
                                    ? `${provider.name} (${node.id})`
                                    : node
                                        ? node.id
                                        : 'Unbekannt';
                            }
                        },
                    ]}
                    getRowIdentifier={row => row.id.toString()}
                    noDataPlaceholder="Keine Team angelegt"
                    noSearchResultsPlaceholder="Keine Teams gefunden"
                    rowActionsCount={3}
                    rowActions={(item) => [
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
                                                Die von dieser Aufgabe weitergegebene Vorgangsdatenebene
                                            </Typography>
                                            <ExpandableCodeBlock
                                                value={JSON.stringify(item.processData, null, 2)}
                                            />
                                            <Typography variant="h6">
                                                Die von dieser Aufgabe erzeugten Prozesselementdatenebene
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
                        {
                            icon: <Replay/>,
                            onClick: () => {
                                confirm({
                                    title: 'Aufgabe neu starten',
                                    children: (
                                        <Typography>
                                            Wirklich neu starten?
                                        </Typography>
                                    ),
                                })
                                    .then((conf) => {
                                        if (conf) {
                                            new ProcessInstanceTaskApiService()
                                                .rerunFailedTask(item.id)
                                                .then(() => {
                                                    dispatch(setLoadingMessage({
                                                        message: 'Task wird neu gestartet',
                                                        blocking: true,
                                                        estimatedTime: 2000,
                                                    }));
                                                    setTimeout(() => {
                                                        if (listRef.current != null) {
                                                            listRef.current.refresh();
                                                        }
                                                        dispatch(clearLoadingMessage());
                                                    }, 2000);
                                                });
                                        }
                                    })
                            },
                            tooltip: 'Fehlgeschlagenen Task neu starten',
                            disabled: item.status !== ProcessTaskStatus.Failed,
                        },
                    ]}
                    defaultSortField="started"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            <ProcessInstanceHistoryEventDialog
                open={showEvents}
                onClose={() => setShowEvents(false)}
                instanceId={params.instanceId ? parseInt(params.instanceId) : 0}
                taskId={null}
            />
        </>
    );
}