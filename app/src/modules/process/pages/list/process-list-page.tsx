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
import {FormStatusChip} from '../../../forms/components/form-status-chip';
import {FormStatus} from '../../../forms/enums/form-status';
import {GenericPageHeaderProps} from '../../../../components/generic-page-header/generic-page-header-props';
import {useNotImplemented} from '../../../../hooks/use-not-implemented';

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
    developingDepartmentName?: string;
    lastEditorName?: string;
}

const columns: GridColDef<ProcessListEntry>[] = [
    {
        field: 'icon',
        headerName: '',
        renderCell: () => <CellContentWrapper
            sx={{alignItems: 'start', py: 2}}><Route/></CellContentWrapper>,
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
                        Entwickelt von: {params.row.developingDepartmentName ?? 'Unbekannt'}
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
                {params.row.publishedVersion != null && <FormStatusChip
                    status={FormStatus.Published}
                    size="small"
                    variant="soft"
                />}
                {params.row.draftedVersion != null && <FormStatusChip
                    status={FormStatus.Drafted}
                    size="small"
                    variant="soft"
                />}
            </Box>
        ),
    },
];

export function ProcessListPage() {
    const memberships = useAppSelector(selectMemberships);

    const listControlRef = useRef<ListControlRef>(null);

    const [showAddDialog, setShowAddDialog] = useState(false);
    const notImplemented = useNotImplemented();

    useEffect(() => {
        new ProcessDefinitionVersionApiService()
            .listAll()
            .then(console.log);
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

        const formsPage = await new ProcessDefinitionApiService()
            .list(options.page, options.size, options.sort as any, options.order, {
                internalTitle: options.search,
                isPublished: options.filter === 'published',
                isDrafted: options.filter === 'drafted',
                isRevoked: options.filter === 'revoked',
            });

        const extendedFormsPage: Page<ProcessListEntry> = {
            ...formsPage,
            content: formsPage.content.map(form => ({
                ...form,
                developingDepartmentName: deps.find(dep => dep.id === form.departmentId)?.name,
                lastEditorName: '',
            })),
        };

        return extendedFormsPage;
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
            to: `/processes/${item.id}`,
            tooltip: 'Prozess ansehen',
            visible: item.draftedVersion === null,
        },
        {
            icon: <NewWindow/>,
            onClick: () => {
                notImplemented();
            },
            tooltip: 'Neuen Entwurf anlegen',
            visible: item.draftedVersion == null,
            disabled: item.publishedVersion == null && item.draftedVersion != null,
        },
        {
            icon: <HomeStorage/>,
            onClick: () => {
                notImplemented();
            },
            tooltip: 'Versionen anzeigen',
        },
        {
            icon: <MoreVertOutlinedIcon/>,
            onClick: () => {
                notImplemented();
            },
            tooltip: 'Optionen',
        },
    ], []);

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
                    rowActionsCount={4}
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
        </>
    );
}

function getRowId(row: ProcessListEntry) {
    return row.id.toString();
}