import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Alert, Typography} from '@mui/material';
import Refresh from '@aivot/mui-material-symbols-400-n25-outlined/Refresh';
import Task from '@aivot/mui-material-symbols-400-n25-outlined/Task';
import {StorageKey} from '../../../../data/storage-key';
import FolderShared from '@aivot/mui-material-symbols-400-n25-outlined/FolderShared';
import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {GenericListPropsFetchOptions, ListControlRef} from '../../../../components/generic-list/generic-list-props';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {ProcessTaskListEntry} from '../../entities/process-list';
import {ProcessListApiService} from '../../services/process-list-api-service';
import {useProcessListFilters} from '../../components/use-process-list-filters';
import {processListColumns} from '../../components/process-list-columns';
import {dispatchProcessAssignedTaskCountRefreshEvent} from '../../utils/process-assigned-task-count-events';
import {Action} from '../../../../components/actions/actions-props';

const filters = [
    {
        value: 'all',
        label: 'Alle Aufgaben',
    },
    {
        value: 'open',
        label: 'Offene Aufgaben',
    },
    {
        value: 'overdue',
        label: 'Überfällige Aufgaben',
    },
    {
        value: 'failed',
        label: 'Fehlerhafte Aufgaben',
    },
];
const visibility = {
    assignedFileNumbers: false,
    processName: false,
    description: false,
    started: false,
    finished: false,
    processVersion: false,
    test: false,
};

export function ProcessTaskList({instanceId}: {instanceId?: number}) {
    const listRef = useRef<ListControlRef | null>(null);
    const {assignee, processId, processVersion, refreshOptions, ...filterProps} = useProcessListFilters(
        true,
        instanceId,
    );
    const [loadFailed, setLoadFailed] = useState(false);
    const refresh = useCallback(() => {
        listRef.current?.refresh();
        refreshOptions();
        dispatchProcessAssignedTaskCountRefreshEvent();
    }, [refreshOptions]);
    useEffect(() => {
        dispatchProcessAssignedTaskCountRefreshEvent();
    }, []);
    const columns = useMemo(() => processListColumns<ProcessTaskListEntry>(true, refresh), [refresh]);
    const fetch = useCallback(
        async (options: GenericListPropsFetchOptions<ProcessTaskListEntry>) => {
            try {
                const result = await new ProcessListApiService().tasks(
                    options.page,
                    options.size,
                    options.sort,
                    options.order,
                    {
                        assignee,
                        processId,
                        processVersion,
                        instanceId,
                        search: options.search,
                        view: options.filter,
                    },
                );
                setLoadFailed(false);
                return result;
            } catch (error) {
                setLoadFailed(true);
                throw error;
            }
        },
        [assignee, processId, processVersion, instanceId],
    );
    const actions: Action[] = [
        ...(instanceId == null
            ? []
            : [
                  {
                      label: 'Vorgang aufrufen',
                      icon: <FolderShared />,
                      iconPosition: 'end' as const,
                      to: `/process-instances/${instanceId}`,
                  },
              ]),
        {
            icon: <Refresh />,
            tooltip: 'Liste aktualisieren',
            onClick: refresh,
        },
    ];
    return (
        <PageWrapper
            title="Aufgaben"
            fullWidth
            background
        >
            <GenericListPage<ProcessTaskListEntry>
                {...filterProps}
                controlRef={listRef}
                header={{
                    icon: ModuleIcons.tasks,
                    title: instanceId == null ? 'Aufgaben' : 'Aufgaben des Vorgangs',
                    actions,
                    helpDialog: {
                        title: instanceId == null ? 'Hilfe zur Aufgabenliste' : 'Hilfe zu den Aufgaben des Vorgangs',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    Eine Aufgabe ist ein einzelner Bearbeitungsschritt innerhalb eines Vorgangs. Der
                                    zugehörige Prozess legt fest, welche Schritte erforderlich sind und wie sie
                                    zusammenhängen. In einem Vorgang können dadurch mehrere Aufgaben entstehen, die von
                                    unterschiedlichen Personen bearbeitet werden.
                                </Typography>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    {instanceId == null
                                        ? 'Hier sehen Sie zunächst Ihre eigenen offenen Aufgaben. Mit den Filtern wählen Sie andere Zuweisungen, einen Prozess oder eine Aufgabenansicht aus.'
                                        : 'Hier sehen Sie zunächst alle Aufgaben dieses Vorgangs. Mit den Filtern grenzen Sie die Auswahl nach Zuweisung und Aufgabenansicht ein.'}{' '}
                                    Die Suche findet Vorgangskennungen und Aktenzeichen.
                                </Typography>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    Über den Aufgabennamen öffnen Sie die Details und gelangen zur Bearbeitung. „Weitere
                                    Aktionen“ führt zum Vorgang und bietet abhängig von Ihren Berechtigungen und dem
                                    Aufgabenstatus weitere Möglichkeiten. Die Zuweisung einer Aufgabe ist unabhängig von
                                    der Zuweisung des Vorgangs.
                                </Typography>
                                <Typography component="p">
                                    Mit „Spalten“ passen Sie die angezeigten Angaben an. Spaltenauswahl, Breiten und die
                                    Nutzung der vollen Bildschirmbreite werden pro Liste in Ihrem Browser gespeichert.
                                    „Standardspalten wiederherstellen“ setzt Auswahl und Spaltenbreiten zurück.
                                </Typography>
                            </>
                        ),
                    },
                }}
                listContextElements={[
                    ...filterProps.listContextElements,
                    ...(loadFailed
                        ? [
                              <Alert
                                  key="load"
                                  severity="error"
                              >
                                  Die Aufgaben konnten nicht geladen werden. Bitte aktualisieren Sie die Liste.
                              </Alert>,
                          ]
                        : []),
                ]}
                fetch={fetch}
                filters={filters}
                defaultFilter={instanceId == null ? 'open' : 'all'}
                searchLabel="Vorgangskennung / Aktenzeichen"
                searchPlaceholder="Kennung oder Aktenzeichen eingeben…"
                columnIcon={<Task />}
                columnDefinitions={columns}
                enableColumnSelection
                initialColumnVisibilityModel={visibility}
                columnSettingsStorageKey={
                    instanceId == null ? StorageKey.ProcessTaskListColumns : StorageKey.ProcessInstanceTaskListColumns
                }
                getRowIdentifier={(row) => row.id.toString()}
                defaultSortField={instanceId == null ? 'deadline' : 'started'}
                defaultSortOrder={instanceId == null ? 'asc' : 'desc'}
                noDataPlaceholder={
                    instanceId == null
                        ? 'Ihnen sind derzeit keine offenen Aufgaben zugewiesen.'
                        : 'Dieser Vorgang hat noch keine Aufgaben.'
                }
                noSearchResultsPlaceholder="Keine passenden Aufgaben gefunden."
            />
        </PageWrapper>
    );
}
