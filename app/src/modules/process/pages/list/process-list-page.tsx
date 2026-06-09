import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box} from '@mui/material';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectMemberships} from '../../../../slices/user-slice';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Typography from '@mui/material/Typography';
import {format} from 'date-fns/format';
import {GridColDef} from '@mui/x-data-grid';
import {Link} from 'react-router-dom';
import HomeStorage from '@aivot/mui-material-symbols-400-outlined/dist/home-storage/HomeStorage';
import NewWindow from '@aivot/mui-material-symbols-400-outlined/dist/new-window/NewWindow';
import {GenericListPropsFetchOptions, ListControlRef} from '../../../../components/generic-list/generic-list-props';
import {Page} from '../../../../models/dtos/page';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {DepartmentApiService} from '../../../departments/services/department-api-service';
import {ProcessEntity} from '../../entities/process-entity';
import {ProcessDefinitionApiService} from '../../services/process-definition-api-service';
import {NewProcessDialog} from '../../dialogs/new-process-dialog';
import {ProcessDefinitionVersionApiService} from '../../services/process-definition-version-api-service';
import Route from '@aivot/mui-material-symbols-400-outlined/dist/route/Route';
import {GenericPageHeaderProps} from '../../../../components/generic-page-header/generic-page-header-props';
import {ProcessStatusChipGroup} from '../../components/process-status/process-status-chip-group';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {clearLoadingMessage, setLoadingMessage} from '../../../../slices/shell-slice';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {ProcessVersionsDialog} from '../../dialogs/process-versions-dialog';
import {MoveProcessToDepartmentDialog} from '../../dialogs/move-process-to-department-dialog';
import {ProcessListRowMenu} from '../../components/process-list-row-menu';

const availableFilter = [
    {
        label: 'Alle Prozesse',
        value: 'all',
    },
    {
        label: 'Entwürfe',
        value: 'drafted',
    },
    {
        label: 'Veröffentlicht',
        value: 'published',
    },
    {
        label: 'Zurückgezogen',
        value: 'revoked',
    },
];

interface ProcessListEntry extends ProcessEntity {
    managingDepartmentName?: string;
    lastEditorName?: string;
}

const columns: GridColDef<ProcessListEntry>[] = [
    {
        field: 'icon',
        headerName: '',
        renderCell: () => <CellContentWrapper
            sx={{alignItems: 'start', py: 2}}
        ><Route/></CellContentWrapper>,
        disableColumnMenu: true,
        width: 24,
        sortable: false,
    },
    {
        field: 'internalTitle',
        headerName: 'Prozesse',
        flex: 2,
        renderCell: (params) => {
            const {
                isDrafted,
                isPublished,
                isRevoked,
            } = {
                isDrafted: params.row.draftedVersion != null,
                isPublished: params.row.publishedVersion != null,
                isRevoked: false,
            };// getFormStatus(params.row);

            return (
                <Box
                    sx={{
                        py: 2,
                    }}
                >
                    <Typography
                        variant="h5"
                        sx={{
                            mb: 0.5,
                            fontSize: '1rem',
                        }}
                    >
                        <Link
                            style={{
                                color: 'inherit',
                                textDecoration: 'none',
                            }}
                            to={`/processes/${params.row.id}/versions/${params.row.draftedVersion ?? params.row.publishedVersion ?? ''}`}
                            title="Prozess bearbeiten"
                        >
                            {params.row.internalTitle}
                        </Link>
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            mt: -0.75,
                            fontSize: '0.875rem',
                            lineHeight: '1.5rem',
                        }}
                        color="textSecondary"
                    >
                        {
                            isPublished ?
                                <span>Veröffentlicht: Version {params.row.publishedVersion}</span> :
                                <span>{isRevoked ? 'Zurückgezogen' : 'Noch nicht veröffentlicht'}</span>
                        }
                        {
                            isDrafted &&
                            <span> &bull; In Bearbeitung: Version {params.row.draftedVersion}</span>
                        }
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            mt: -0.75,
                            fontSize: '0.875rem',
                            lineHeight: '1.5rem',
                            textOverflow: 'ellipsis',
                            overflow: 'hidden',
                            whiteSpace: 'nowrap',
                        }}
                        color="textSecondary"
                    >
                        Verwaltet von: {params.row.managingDepartmentName ?? 'Unbekannt'}
                    </Typography>
                </Box>
            );
        },
    },
    {
        field: 'updated',
        headerName: 'Zuletzt bearbeitet',
        flex: 1,
        renderCell: (params) => (
            <Box
                sx={{
                    py: 2,
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <Typography sx={{fontSize: '0.875rem'}}>
                    {format(params.row.updated, 'dd.MM.yyyy — HH:mm')} Uhr
                </Typography>
                <Typography
                    color="textSecondary"
                    sx={{fontSize: '0.875rem'}}
                >
                    {params.row.lastEditorName ?? 'Unbekannte Nutzer:in'}
                </Typography>
            </Box>
        ),
    },
    {
        field: 'publishedVersion',
        headerName: 'Status',
        flex: 0.75,
        sortable: false,
        renderCell: (params) => (
            <Box
                sx={{
                    py: 2,
                }}
            >
                <ProcessStatusChipGroup process={params.row}/>
            </Box>
        ),
    },
];

export function ProcessListPage() {
    const dispatch = useAppDispatch();
    const memberships = useAppSelector(selectMemberships);
    const listControlRef = useRef<ListControlRef>(null);

    const [showAddDialog, setShowAddDialog] = useState(false);
    const [showVersionsDialogForProcess, setShowVersionsDialogForProcess] = useState<ProcessEntity | null>(null);
    const [processToMove, setProcessToMove] = useState<ProcessEntity>();
    const [rowMenu, setRowMenu] = useState<{
        target: HTMLElement;
        process: ProcessListEntry;
    }>();

    useEffect(() => {
        new ProcessDefinitionVersionApiService()
            .listAll()
            .then(console.log);
    }, []);

    const handleAddDraft = useCallback((process: number, version?: number) => {
        dispatch(setLoadingMessage({
            message: 'Neue Version wird erzeugt',
            estimatedTime: 2000,
            blocking: true,
        }));

        new ProcessDefinitionApiService()
            .addNewVersion(process, version)
            .then(() => {
                if (listControlRef.current) {
                    listControlRef.current.refresh();
                }
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Anlegen einer neuen Version'));
            })
            .finally(() => {
                dispatch(clearLoadingMessage());
            });
    }, []);

    const header: GenericPageHeaderProps = useMemo(() => ({
        icon: <Route/>,
        title: 'Prozesse',
        actions: [
            {
                label: 'Neuer Prozess',
                icon: <AddOutlinedIcon/>,
                onClick: () => {
                    setShowAddDialog(true);
                },
                variant: 'contained',
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Prozessen',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography mb={2}>
                        Prozesse sind digitale Abläufe, die verschiedene Aufgaben und Genehmigungsschritte innerhalb
                        Ihrer Organisation abbilden.
                        In dieser Übersicht sehen Sie alle Prozesse, an deren Entwicklung Sie beteiligt sind oder die
                        Ihrer Organisationseinheit zugeordnet wurden.
                    </Typography>
                    <Typography mb={2}>
                        Sie können neue Prozesse anlegen, bestehende Prozesse bearbeiten oder veröffentlichte Versionen
                        einsehen.
                        Der Status eines Prozesses zeigt an, ob er sich noch im Entwurf befindet, bereits veröffentlicht
                        oder zurückgezogen wurde.
                    </Typography>
                    <Typography>
                        Um einen neuen Prozess zu starten, klicken Sie auf „Neuer Prozess“. Weitere Optionen finden Sie
                        in den Aktionen neben jedem Prozess.
                        Sollten Sie keiner Organisationseinheit zugeordnet sein, wenden Sie sich bitte an eine
                        Administrator:in.
                    </Typography>
                </>
            ),
        },
    }), []);

    const fetch = useCallback(async (options: GenericListPropsFetchOptions<ProcessListEntry>) => {
        const deps = (await new DepartmentApiService().listAll()).content;

        const processesPage = await new ProcessDefinitionApiService()
            .list(options.page, options.size, options.sort as any, options.order, {
                internalTitle: options.search,
                isPublished: options.filter === 'published',
                isDrafted: options.filter === 'drafted',
                isRevoked: options.filter === 'revoked',
            });

        const extendedProcessesPage: Page<ProcessListEntry> = {
            ...processesPage,
            content: processesPage.content.map(process => ({
                ...process,
                managingDepartmentName: deps.find(dep => dep.id === process.departmentId)?.name,
                lastEditorName: '',
            })),
        };

        return extendedProcessesPage;
    }, []);

    const noDataPlaceholder = useMemo(() => (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
                p: 4,
            }}
        >
            {
                (memberships == null ||
                    memberships.length === 0) &&
                <>
                    <Typography
                        variant="h5"
                        component="h2"
                    >
                        Noch keiner Organisationseinheit zugeordnet
                    </Typography>
                    <Typography>
                        Eine Administrator:in muss Sie einer Organisationseinheit zuordnen und Ihnen
                        eine
                        Domänenrolle zuweisen.
                        Erst dann können Sie mit der Entwicklung von Prozessen beginnen. Nach der
                        Zuweisung müssen Sie diese Seite ggf. einmal neu laden.
                    </Typography>
                </>
            }
            {
                memberships != null &&
                memberships.length > 0 &&
                <Typography>
                    Sie haben aktuell keine Prozesse. Starten Sie jetzt mit Ihrem ersten Prozess!
                </Typography>
            }
        </Box>
    ), [memberships]);

    const rowActions = useCallback((item: ProcessListEntry) => [
        {
            icon: <Edit/>,
            to: `/processes/${item.id}/versions/${item.draftedVersion}`,
            tooltip: 'Prozess bearbeiten',
            visible: item.draftedVersion != null,
        },
        {
            icon: <Visibility/>,
            to: `/processes/${item.id}/versions/${item.publishedVersion}`,
            tooltip: 'Prozess ansehen',
            visible: item.draftedVersion === null,
        },
        {
            icon: <NewWindow/>,
            onClick: () => {
                handleAddDraft(item.id);
            },
            tooltip: 'Neuen Entwurf anlegen',
            visible: item.draftedVersion == null,
            disabled: item.publishedVersion == null && item.draftedVersion != null,
        },
        {
            icon: <HomeStorage/>,
            onClick: () => {
                setShowVersionsDialogForProcess(item);
            },
            tooltip: 'Versionen anzeigen',
        },
        {
            icon: <MoreVertOutlinedIcon/>,
            onClick: (event: React.MouseEvent<HTMLElement>) => {
                setRowMenu({
                    target: event.currentTarget as HTMLElement,
                    process: item,
                });
            },
            tooltip: 'Optionen',
        },
    ], [handleAddDraft]);

    return (
        <>
            <PageWrapper
                title="Prozesse"
                fullWidth
                background
            >
                <GenericListPage<ProcessListEntry>
                    controlRef={listControlRef}
                    dynamicRowHeight={true}
                    filters={availableFilter}
                    defaultFilter="all"
                    header={header}
                    searchLabel="Prozess suchen"
                    searchPlaceholder="Titel des Prozesses eingeben…"
                    fetch={fetch}
                    columnDefinitions={columns}
                    getRowIdentifier={getRowId}
                    noDataPlaceholder={noDataPlaceholder}
                    noSearchResultsPlaceholder="Keine Prozesse gefunden"
                    rowActionsCount={5}
                    rowActions={rowActions}
                    defaultSortField="internalTitle"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            <NewProcessDialog
                open={showAddDialog}
                onCancel={() => {
                    setShowAddDialog(false);
                }}
            />

            {
                showVersionsDialogForProcess &&
                <ProcessVersionsDialog
                    open={true}
                    process={showVersionsDialogForProcess}
                    onClose={() => {
                        setShowVersionsDialogForProcess(null);
                    }}
                    onNewDraft={({process, version}) => {
                        handleAddDraft(process.id, version.processVersion);
                    }}
                    onDeleteVersion={() => {
                        if (listControlRef.current) {
                            listControlRef.current.refresh();
                        }
                    }}
                />
            }

            {
                rowMenu != null &&
                <ProcessListRowMenu
                    anchorEl={rowMenu.target}
                    process={rowMenu.process}
                    onClose={() => {
                        setRowMenu(undefined);
                    }}
                    onMoveProcessToDepartment={setProcessToMove}
                />
            }

            {
                processToMove != null &&
                <MoveProcessToDepartmentDialog
                    processId={processToMove.id}
                    onClose={() => {
                        setProcessToMove(undefined);
                    }}
                    onMoved={() => {
                        setProcessToMove(undefined);
                        listControlRef.current?.refresh();
                    }}
                />
            }
        </>
    );
}

function getRowId(row: ProcessListEntry) {
    return row.id.toString();
}
