import React, {useEffect, useState} from 'react';
import {Box, ListItemIcon, ListItemText, Menu, MenuItem, Paper, Switch, Typography, useTheme} from '@mui/material';
import {useNavigate} from 'react-router-dom';
import {format, parseISO} from 'date-fns';
import {StorageScope, StorageService} from '../../../services/storage-service';
import {StorageKey} from '../../../data/storage-key';
import {getFullName, NewAnonymousUser, type User} from '../../../models/entities/user';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectMemberships, selectUser} from '../../../slices/user-slice';
import {type Destination} from '../../../models/entities/destination';
import {type GridColDef} from '@mui/x-data-grid';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {filterItems} from '../../../utils/filter-items';
import Tooltip from '@mui/material/Tooltip';
import DataObjectOutlinedIcon from '@mui/icons-material/DataObjectOutlined';
import ManageAccountsOutlinedIcon from '@mui/icons-material/ManageAccountsOutlined';
import FolderZipOutlinedIcon from '@mui/icons-material/FolderZipOutlined';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import {useUsersApi} from '../../../hooks/use-users-api';
import {Api, useApi} from '../../../hooks/use-api';
import {useDestinationsApi} from '../../../hooks/use-destinations-api';
import {useSubmissionsApi} from '../../../hooks/use-submissions-api';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {AppHeader} from '../../../components/app-header/app-header';
import {AppMode} from '../../../data/app-mode';
import {Introductory} from '../../../components/introductory/introductory';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {TableWrapper} from '../../../components/table-wrapper/table-wrapper';
import {ProviderLinks} from '../application-pages/components/provider-links';
import {AppFooter} from '../../../components/app-footer/app-footer';
import {SubmissionListProjection} from '../../../models/entities/submission';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {FormListProjection} from '../../../models/entities/form';
import {useFormsApi} from '../../../hooks/use-forms-api';
import FilterAltOutlinedIcon from '@mui/icons-material/FilterAltOutlined';
import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined';
import CreditCardOffOutlinedIcon from '@mui/icons-material/CreditCardOffOutlined';
import {createSubmissionState} from '../../../utils/submission-state';

type Submission = SubmissionListProjection & {
    assignee: User | undefined;
    destination: Destination | undefined;
    form: FormListProjection | undefined;
};

const statusOptions = [
    'Offen',
    'In Bearbeitung',
    'Abgeschlossen',
    'Fehlgeschlagen',
    'Übertragen',
];

async function fetchSubmissions(api: Api, user: User, includePaymentPending: boolean | undefined, includeArchived: boolean | undefined, includeTest: boolean | undefined, onlyAssigned: boolean | undefined, formId: string | undefined): Promise<Submission[]> {
    const submissions = await useSubmissionsApi(api)
        .list({
            includePaymentPending: includePaymentPending ? true : undefined,
            includeArchived: includeArchived ? true : undefined,
            includeTest: includeTest ? true : undefined,
            assigneeId: onlyAssigned ? user.id : undefined,
            formId: undefined,
        });

    const destinationIds: Set<number> = new Set(submissions.map(sub => sub.destinationId).filter(id => id != null) as number[]);
    const assigneeIds: Set<string> = new Set(submissions.map(sub => sub.assigneeId).filter(id => id != null) as string[]);
    const formIds: Set<number> = new Set(submissions.map(sub => sub.formId));

    const destinationCalls = useDestinationsApi(api)
        .list({ids: Array.from(destinationIds)});
    const assigneeCalls = useUsersApi(api)
        .list({ids: Array.from(assigneeIds)});
    const formCalls = useFormsApi(api)
        .list({ids: Array.from(formIds)});

    const [
        destinations,
        assignees,
        forms,
    ] = await Promise.all([destinationCalls, assigneeCalls, formCalls]);

    return submissions.map(sub => ({
        ...sub,
        destination: destinations.find(dest => dest.id === sub.destinationId),
        assignee: sub.assigneeId != null ? (assignees.find(assignee => assignee.id === sub.assigneeId) ?? NewAnonymousUser()) : undefined,
        form: forms.find(form => form.id === sub.formId),
    }));
}

const columns: Array<GridColDef<Submission>> = [
    {
        field: 'status',
        headerName: 'Status',
        type: 'singleSelect',
        valueOptions: statusOptions.map(status => ({
            value: status,
            label: status,
        })),
        renderCell: (params) => {
            const state = createSubmissionState(params.row);
            const Icon = state.icon;
            return (
                <Box
                    display="flex"
                    alignItems="center"
                    sx={{
                        width: '100%',
                    }}
                >
                    <Tooltip title={state.label}>
                        <Icon color={state.color} />
                    </Tooltip>

                    <Typography
                        sx={{
                            mx: 1,
                        }}
                    >
                        {state.label}
                    </Typography>

                    {
                        params.row.isTestSubmission &&
                        <Tooltip title="Test-Antrag">
                            <ScienceOutlinedIcon
                                sx={{
                                    ml: 'auto',
                                }}
                            />
                        </Tooltip>
                    }
                </Box>
            );
        },
        valueGetter: (params) => params.row.status,
        flex: 1,
    },
    {
        field: 'fileNumber',
        headerName: 'Aktenzeichen',
        valueGetter: (params) => isStringNotNullOrEmpty(params.row.fileNumber) ? params.row.fileNumber : 'Kein Aktenzeichen',
        flex: 1,
    },
    {
        field: 'created',
        headerName: 'Eingangsdatum',
        type: 'date',
        renderCell: (params) => {
            const created = parseISO(params.row.created);
            return `${format(created, 'dd.MM.yyyy')} - ${format(created, 'HH:mm')} Uhr`;
        },
        valueGetter: (params) => parseISO(params.row.created),
        flex: 1,
    },
    {
        field: 'assignee',
        headerName: 'Mitarbeiter:in',
        renderCell: (params) => (
            <>
                {
                    params.row.destinationId != null &&
                    <DataObjectOutlinedIcon
                        sx={{marginRight: '0.5em'}}
                    />
                }

                {
                    params.row.assignee != null &&
                    <ManageAccountsOutlinedIcon
                        sx={{marginRight: '0.5em'}}
                    />
                }

                {
                    params.row.destination != null &&
                    `Schnittstelle „${params.row.destination.name}“`
                }

                {
                    params.row.assignee != null &&
                    getFullName(params.row.assignee)
                }

                {
                    params.row.destination == null &&
                    params.row.assignee == null &&
                    <i>Nicht zugewiesen</i>
                }
            </>
        ),
        valueGetter: (params) => params.row.destination != null ? params.row.destination.name : (params.row.assignee != null ? getFullName(params.row.assignee) : null),
        flex: 1,
    },
    {
        field: 'form.title',
        headerName: 'Formular',
        type: 'string',
        flex: 1,
        valueGetter: (params) => params.row.form?.title,
    },
    {
        field: 'form.version',
        headerName: 'Version',
        type: 'string',
        flex: 1,
        valueGetter: (params) => params.row.form?.version,
    },
];

export function SubmissionListPage(): JSX.Element {
    const theme = useTheme();
    const dispatch = useAppDispatch();
    const api = useApi();
    const navigate = useNavigate();
    const user = useAppSelector(selectUser);
    const memberships = useAppSelector(selectMemberships);

    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const [filterMenuAnchorEl, setFilterMenuAnchorEl] = React.useState<undefined | HTMLElement>();

    const [includePaymentPending, setIncludePaymentPending] = useState(StorageService.loadFlag(StorageKey.SubmissionsIncludePaymentPending));
    const [includeArchived, setIncludeArchived] = useState(StorageService.loadFlag(StorageKey.SubmissionsIncludeArchived));
    const [onlyAssigned, setOnlyAssigned] = useState(StorageService.loadFlag(StorageKey.SubmissionsOnlyAssigned));
    const [includeTest, setIncludeTest] = useState(StorageService.loadFlag(StorageKey.SubmissionsIncludeTest));

    const [submissions, setSubmissions] = useState<Submission[]>();
    const [search, setSearch] = useState('');

    useEffect(() => {
        if (user != null && api != null) {
            fetchSubmissions(api, user, includePaymentPending, includeArchived, includeTest, onlyAssigned, undefined)
                .then(setSubmissions)
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Fehler beim Laden der Anträge'));
                });
        }
    }, [user, includePaymentPending, includeArchived, onlyAssigned, includeTest, api]);

    const handleToggleIncludePaymentPending = (): void => {
        setIncludePaymentPending(!includePaymentPending);
        StorageService.storeFlag(StorageKey.SubmissionsIncludePaymentPending, !includePaymentPending, StorageScope.Local);
    };

    const handleToggleIncludeArchived = (): void => {
        setIncludeArchived(!includeArchived);
        StorageService.storeFlag(StorageKey.SubmissionsIncludeArchived, !includeArchived, StorageScope.Local);
    };

    const handleToggleOnlyAssigned = (): void => {
        setOnlyAssigned(!onlyAssigned);
        StorageService.storeFlag(StorageKey.SubmissionsOnlyAssigned, !onlyAssigned, StorageScope.Local);
    };

    const handleToggleIncludeTest = (): void => {
        setIncludeTest(!includeTest);
        StorageService.storeFlag(StorageKey.SubmissionsIncludeTest, !includeTest, StorageScope.Local);
    };

    const handleOpenFilterMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
        setFilterMenuAnchorEl(event.currentTarget);
    };

    const handleCloseFilterMenu = () => {
        setFilterMenuAnchorEl(undefined);
    };

    const filteredSubmissions = filterItems(submissions, 'fileNumber', search);

    return (
        <>
            <MetaElement
                title={providerName != null && providerName.length > 0 ? providerName : 'powered by Aivot'}
            />

            <AppHeader
                mode={AppMode.Staff}
            />

            <Introductory
                mode={AppMode.Staff}
            />

            <Box
                sx={{
                    backgroundColor: '#F3F3F3',
                    minHeight: '60vh',
                }}
            >
                <Box
                    sx={{
                        mb: 5,
                        py: 4,
                        mx: 1,
                        [theme.breakpoints.up('md')]: {
                            mx: 8,
                        },
                        [theme.breakpoints.up('lg')]: {
                            mx: 12,
                        },
                        [theme.breakpoints.up('xl')]: {
                            mx: 16,
                        },
                    }}
                >

                    <Paper
                        sx={{
                            p: 4,
                            mt: 4,
                        }}
                    >
                        {
                            (memberships ?? []).length === 0 &&
                            <>
                                <Typography
                                    variant="h5"
                                    component="h2"
                                >
                                    Noch keinem Fachbereich zugeordnet
                                </Typography>
                                <Typography>
                                    Eine Administrator:in muss Sie noch einem Fachbereich zuordnen und Ihnen eine Rolle
                                    zuweisen.
                                    Erst dann können Sie mit der Bearbeitung von Anträgen beginnen.
                                </Typography>
                            </>
                        }
                        {
                            (memberships ?? []).length > 0 &&
                            <TableWrapper
                                title="Anträge"
                                noDataMessage="Keine Anträge vorhanden"
                                noDataFoundMessage="Ihre Suche ergab keine Treffer für dieses Aktenzeichen"

                                rows={filteredSubmissions}
                                columns={columns}
                                onRowClick={(sub) => {
                                    navigate(`/submissions/${sub.id}`);
                                }}

                                search={search}
                                onSearchChange={setSearch}
                                searchPlaceholder="Aktenzeichen suchen..."

                                actions={[
                                    {
                                        icon: <FilterAltOutlinedIcon />,
                                        tooltip: 'Filtereinstellungen',
                                        onClick: handleOpenFilterMenu,
                                        badge: {
                                            variant: (onlyAssigned || includeTest || includeArchived) ? 'dot' : undefined,
                                            color: 'error',
                                        },
                                    },
                                ]}
                            />
                        }
                    </Paper>
                </Box>
            </Box>

            <ProviderLinks />

            <AppFooter mode={AppMode.Staff} />

            <Menu
                id="filter-menu"
                anchorEl={filterMenuAnchorEl}
                open={Boolean(filterMenuAnchorEl)}
                onClose={handleCloseFilterMenu}
            >
                <MenuItem
                    dense
                    onClick={handleToggleIncludePaymentPending}
                >
                    <ListItemIcon>
                        <CreditCardOffOutlinedIcon color={includePaymentPending ? 'primary' : undefined} />
                    </ListItemIcon>
                    <ListItemText
                        id="switch-list-pending"
                        primary="Vorgänge mit ausstehender Zahlung anzeigen"
                    />
                    <Switch
                        edge="end"
                        checked={includePaymentPending}
                        inputProps={{
                            'aria-labelledby': 'switch-list-pending',
                        }}
                    />
                </MenuItem>

                <MenuItem
                    dense
                    onClick={handleToggleIncludeArchived}
                >
                    <ListItemIcon>
                        <FolderZipOutlinedIcon color={includeArchived ? 'primary' : undefined} />
                    </ListItemIcon>
                    <ListItemText
                        id="switch-list-archived"
                        primary="Abgeschlossene Vorgänge anzeigen"
                    />
                    <Switch
                        edge="end"
                        checked={includeArchived}
                        inputProps={{
                            'aria-labelledby': 'switch-list-archived',
                        }}
                    />
                </MenuItem>

                <MenuItem
                    dense
                    onClick={handleToggleIncludeTest}
                >
                    <ListItemIcon>
                        <ScienceOutlinedIcon color={includeTest ? 'primary' : undefined} />
                    </ListItemIcon>
                    <ListItemText
                        id="switch-list-test"
                        primary="Test-Vorgänge anzeigen"
                    />
                    <Switch
                        edge="end"
                        checked={includeTest}
                        inputProps={{
                            'aria-labelledby': 'switch-list-test',
                        }}
                    />
                </MenuItem>

                <MenuItem
                    dense
                    onClick={handleToggleOnlyAssigned}
                >
                    <ListItemIcon>
                        <AccountCircleOutlinedIcon color={onlyAssigned ? 'primary' : undefined} />
                    </ListItemIcon>
                    <ListItemText
                        id="switch-list-assigned"
                        primary="Nur mir zugewiesene Vorgänge anzeigen"
                    />
                    <Switch
                        edge="end"
                        checked={onlyAssigned}
                        inputProps={{
                            'aria-labelledby': 'switch-list-assigned',
                        }}
                    />
                </MenuItem>
            </Menu>
        </>
    );
}
