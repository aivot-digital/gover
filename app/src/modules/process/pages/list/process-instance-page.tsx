import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import {ProcessInstanceEntity} from "../../entities/process-instance-entity";
import {ProcessInstanceApiService} from "../../services/process-instance-api-service";
import {useParams} from "react-router-dom";
import {ProcessInstanceStatus, ProcessInstanceStatusLabels} from "../../enums/process-instance-status";
import Refresh from "@aivot/mui-material-symbols-400-outlined/dist/refresh/Refresh";
import React, {useRef} from "react";
import {ListControlRef} from "../../../../components/generic-list/generic-list-props";
import Delete from "@aivot/mui-material-symbols-400-outlined/dist/delete/Delete";
import ProcessChart from "@aivot/mui-material-symbols-400-outlined/dist/process-chart/ProcessChart";
import {ProcessInstanceHistoryEventDialog} from "../../dialogs/process-instance-history-event-dialog";
import News from "@aivot/mui-material-symbols-400-outlined/dist/news/News";

export function ProcessInstanceListPage() {
    const params = useParams();

    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const listRef = useRef<ListControlRef | null>(null);

    const handleListRefresh = () => {
        if (listRef.current != null) {
            listRef.current.refresh();
        }
    };

    const handleDelete = (item: ProcessInstanceEntity) => {
        const apiService = new ProcessInstanceApiService();
        apiService
            .destroy(item.id)
            .then(() => {
                handleListRefresh();
            });
    }

    const [showEventsForInstanceId, setShowEventsForInstanceId] = React.useState<number | null>(null);

    return (
        <>
            <PageWrapper
                title="Vorgänge"
                fullWidth
                background
            >
                <GenericListPage<ProcessInstanceEntity>
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
                    searchLabel="Team suchen"
                    searchPlaceholder="Name des Teams eingeben…"
                    fetch={(options) => {
                        return new ProcessInstanceApiService()
                            .list(
                                options.page,
                                options.size,
                                options.sort,
                                options.order,
                                {
                                    processDefinitionId: parseInt(params.processId!),
                                    processDefinitionVersion: parseInt(params.processVersion!),
                                    statusIsNot: options.filter === 'notCompleted' ? ProcessInstanceStatus.Completed : undefined,
                                },
                            );
                    }}
                    columnIcon={<ProcessChart/>}
                    columnDefinitions={[
                        {
                            field: 'accessKey',
                            headerName: 'Schlüssel',
                            flex: 1,
                            renderCell: (params) => (
                                <CellLink
                                    to={`/processes/${params.row.processDefinitionId}/versions/${params.row.processDefinitionVersion}/instances/${params.row.id}/tasks`}
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
                    getRowIdentifier={row => row.id.toString()}
                    noDataPlaceholder="Keine Team angelegt"
                    noSearchResultsPlaceholder="Keine Teams gefunden"
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
                            tooltip: 'Events Einsehen',
                            onClick: () => {
                                setShowEventsForInstanceId(item.id);
                            },
                        },
                    ]}
                    defaultSortField="started"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            <ProcessInstanceHistoryEventDialog
                open={showEventsForInstanceId != null}
                onClose={() => setShowEventsForInstanceId(null)}
                instanceId={showEventsForInstanceId ?? 0}
                taskId={null}
            />
        </>
    );
}