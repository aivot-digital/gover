import {useCallback, useMemo, useRef, useState} from 'react';
import {Alert, Typography} from '@mui/material';
import Refresh from '@aivot/mui-material-symbols-400-n25-outlined/Refresh';
import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {GenericListPropsFetchOptions, ListControlRef} from '../../../../components/generic-list/generic-list-props';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {ProcessInstanceListEntry} from '../../entities/process-list';
import {ProcessListApiService} from '../../services/process-list-api-service';
import {useProcessListFilters} from '../../components/use-process-list-filters';
import {StorageKey} from '../../../../data/storage-key';
import {ProcessInstanceStatusIcon} from '../../components/process-instance-status-icon';
import {processListColumns} from '../../components/process-list-columns';
import {useListFilter} from '../../../../components/generic-list/use-list-filter';
import {ProcessListFilterMenu} from '../../components/process-list-filter-menu';

const filters = [
    {
        value: 'all',
        label: 'Alle Vorgänge',
    },
    {
        value: 'active',
        label: 'Laufende Vorgänge',
    },
    {
        value: 'ended',
        label: 'Beendete Vorgänge',
    },
    {
        value: 'failed',
        label: 'Fehlerhafte Vorgänge',
    },
];
const visibility = {
    assignedFileNumbers: false,
    finished: false,
    processVersion: false,
    test: false,
};

export function ProcessInstanceListPage() {
    const listRef = useRef<ListControlRef | null>(null);
    const {assignee, processId, processVersion, refreshOptions, ...filterProps} = useProcessListFilters(false);
    const {value: includeTestsValue, setValue: setIncludeTests} = useListFilter('includeTests');
    const includeTests = includeTestsValue !== 'false';
    const [loadFailed, setLoadFailed] = useState(false);
    const refresh = useCallback(() => {
        listRef.current?.refresh();
        refreshOptions();
    }, [refreshOptions]);
    const columns = useMemo(() => processListColumns<ProcessInstanceListEntry>(false, refresh), [refresh]);
    const fetch = useCallback(
        async (options: GenericListPropsFetchOptions<ProcessInstanceListEntry>) => {
            try {
                const result = await new ProcessListApiService().instances(
                    options.page,
                    options.size,
                    options.sort,
                    options.order,
                    {
                        assignee,
                        processId,
                        processVersion,
                        includeTests,
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
        [assignee, processId, processVersion, includeTests],
    );

    return (
        <PageWrapper
            title="Vorgänge"
            fullWidth
            background
        >
            <GenericListPage<ProcessInstanceListEntry>
                {...filterProps}
                hasActiveAdditionalFilters={filterProps.hasActiveAdditionalFilters || !includeTests}
                filterActions={
                    <ProcessListFilterMenu
                        includeTests={includeTests}
                        onChange={(value) => setIncludeTests(value ? null : 'false')}
                    />
                }
                controlRef={listRef}
                header={{
                    icon: ModuleIcons.submissions,
                    title: 'Vorgänge',
                    actions: [
                        {
                            icon: <Refresh />,
                            tooltip: 'Liste aktualisieren',
                            onClick: refresh,
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zur Vorgangsliste',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    Ein Vorgang ist die konkrete Ausführung eines Prozesses, zum Beispiel die
                                    Bearbeitung eines eingereichten Antrags. Der Prozess legt fest, welche Schritte
                                    erforderlich sind und wie sie zusammenhängen. In einem Vorgang können mehrere
                                    Aufgaben entstehen, die unterschiedlichen Personen zugewiesen sind.
                                </Typography>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    Diese Liste gibt Ihnen einen Überblick über Ihre zugänglichen Vorgänge. Zunächst
                                    sehen Sie laufende Vorgänge. Über die Ansichten und Filter grenzen Sie die Auswahl
                                    nach Status, zugewiesener Person und Prozess ein. Die Suche findet Vorgangskennungen
                                    und Aktenzeichen.
                                </Typography>
                                <Typography
                                    component="p"
                                    sx={{mb: 2}}
                                >
                                    Über die Vorgangskennung öffnen Sie die Details. Im Menü „Weitere Aktionen“ können
                                    Sie unter anderem alle Aufgaben eines Vorgangs aufrufen oder seine Zuweisung ändern.
                                    Die verfügbaren Aktionen richten sich nach Ihren Berechtigungen und dem Stand des
                                    Vorgangs.
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
                                  Die Vorgänge konnten nicht geladen werden. Bitte aktualisieren Sie die Liste.
                              </Alert>,
                          ]
                        : []),
                ]}
                fetch={fetch}
                filters={filters}
                defaultFilter="active"
                searchLabel="Vorgangskennung / Aktenzeichen"
                searchPlaceholder="Kennung oder Aktenzeichen eingeben…"
                columnIcon={(row) => (
                    <ProcessInstanceStatusIcon
                        status={row.status}
                        statusOverride={row.statusOverride?.trim() || null}
                    />
                )}
                columnDefinitions={columns}
                enableColumnSelection
                initialColumnVisibilityModel={visibility}
                columnSettingsStorageKey={StorageKey.ProcessInstanceListColumns}
                getRowIdentifier={(row) => row.id.toString()}
                defaultSortField="started"
                defaultSortOrder="desc"
                noDataPlaceholder="Keine laufenden Vorgänge vorhanden."
                noSearchResultsPlaceholder="Keine passenden Vorgänge gefunden."
            />
        </PageWrapper>
    );
}
