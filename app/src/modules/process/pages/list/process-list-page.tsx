import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Box} from '@mui/material';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import React, {useEffect, useRef, useState} from 'react';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectMemberships} from '../../../../slices/user-slice';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import Typography from '@mui/material/Typography';
import {format} from 'date-fns/format';
import {GridColDef} from '@mui/x-data-grid';
import {Link, useNavigate} from 'react-router-dom';
import HomeStorage from '@aivot/mui-material-symbols-400-outlined/dist/home-storage/HomeStorage';
import NewWindow from '@aivot/mui-material-symbols-400-outlined/dist/new-window/NewWindow';
import {ListControlRef} from '../../../../components/generic-list/generic-list-props';
import {Page} from '../../../../models/dtos/page';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {DepartmentApiService} from '../../../departments/services/department-api-service';
import {ProcessEntity} from "../../entities/process-entity";
import {ProcessDefinitionApiService} from "../../services/process-definition-api-service";
import {NewProcessDialog} from "../../dialogs/new-process-dialog";
import {ProcessDefinitionVersionApiService} from "../../services/process-definition-version-api-service";
import Route from '@aivot/mui-material-symbols-400-outlined/dist/route/Route';

const availableFilter = [
    {
        label: 'Alle Formulare',
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
        headerName: 'Formular',
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
                            title="Formular bearbeiten"
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
            <Box>
                {params.row.publishedVersion}
            </Box>
        ),
    },
];

export function ProcessListPage() {
    const memberships = useAppSelector(selectMemberships);

    const listControlRef = useRef<ListControlRef>(null);

    const [showAddDialog, setShowAddDialog] = useState(false);

    console.log('Looping Check List Page');

    useEffect(() => {
        new ProcessDefinitionVersionApiService()
            .listAll()
            .then(console.log);
    }, []);

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
                    header={{
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
                            content: <Box> TODO </Box>,
                        },
                    }}
                    searchLabel="Prozess suchen"
                    searchPlaceholder="Titel des Prozesses eingeben…"
                    fetch={async (options) => {
                        const deps = (await new DepartmentApiService().listAll()).content;

                        const formsPage = await new ProcessDefinitionApiService()
                            .list(options.page, options.size, options.sort as any, options.order, {
                                name: options.search,
                                //isPublished: options.filter === 'published',
                                //isDrafted: options.filter === 'drafted',
                                //isRevoked: options.filter === 'revoked',
                            });

                        const formIds = formsPage.content.map(form => form.id);

                        //const editorsList = await new FormApiService()
                        //    .listEditorsForForms(formIds);

                        const extendedFormsPage: Page<ProcessListEntry> = {
                            ...formsPage,
                            content: formsPage.content.map(form => ({
                                ...form,
                                developingDepartmentName: deps.find(dep => dep.id === form.departmentId)?.name,
                                lastEditorName: '',
                            })),
                        };

                        return extendedFormsPage;
                    }}
                    columnDefinitions={columns}
                    getRowIdentifier={row => row.id.toString()}
                    noDataPlaceholder={
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
                                        Noch keinem Fachbereich zugeordnet
                                    </Typography>
                                    <Typography>
                                        Eine Administrator:in muss Sie noch einem Fachbereich zuordnen und Ihnen eine
                                        Rolle
                                        zuweisen.
                                        Erst dann können Sie mit der Entwicklung von Formularen loslegen.
                                    </Typography>
                                </>
                            }
                            {
                                memberships != null &&
                                memberships.length > 0 &&
                                <Typography>
                                    Sie haben aktuell keine Formulare. Starten Sie jetzt mit Ihrem ersten Formular!
                                </Typography>
                            }
                        </Box>
                    }
                    noSearchResultsPlaceholder="Keine Prozesse gefunden"
                    rowActionsCount={4}
                    rowActions={(item) => [
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
                                // TODO
                            },
                            tooltip: 'Neuen Entwurf anlegen',
                            visible: item.draftedVersion == null,
                            disabled: item.publishedVersion == null && item.draftedVersion != null,
                        },
                        {
                            icon: <HomeStorage/>,
                            onClick: () => {
                                // TODO
                            },
                            tooltip: 'Versionen anzeigen',
                        },
                        {
                            icon: <MoreVertOutlinedIcon/>,
                            onClick: () => {
                                // TODO
                            },
                            tooltip: 'Optionen',
                        },
                    ]}
                    defaultSortField="internalTitle"
                    disableFullWidthToggle={true}
                />
            </PageWrapper>

            <NewProcessDialog
                open={showAddDialog}
                onNew={(process) => {
                    setShowAddDialog(false);
                    listControlRef.current?.refresh();
                }}
                onCancel={() => {
                    setShowAddDialog(false);
                }}
            />
        </>
    );
}

