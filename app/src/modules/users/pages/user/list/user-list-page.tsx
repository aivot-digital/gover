import {GenericListPage} from '../../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../../components/page-wrapper/page-wrapper';
import {Link, Typography} from '@mui/material';
import {EditOutlined, MailOutlined, PeopleOutlined} from '@mui/icons-material';
import React, {useEffect, useMemo, useState} from 'react';
import {CellLink} from '../../../../../components/cell-link/cell-link';
import {useAccessGuard} from '../../../../../hooks/use-admin-guard';
import {UserFilter, UsersApiService} from '../../../users-api-service';
import {type User} from '../../../../../models/entities/user';
import Chip from '@mui/material/Chip';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {UserStatusChip} from '../../../components/user-status-chip';
import Person from "@aivot/mui-material-symbols-400-outlined/dist/person/Person";
import {GenericListColDef} from "../../../../../components/generic-list/generic-list-props";
import Add from "@aivot/mui-material-symbols-400-outlined/dist/add/Add";
import {SystemRolesApiService} from '../../../../system/services/system-roles-api-service';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../../slices/snackbar-slice';

const Filters = [
    {
        label: 'Aktiv',
        value: 'active',
    },
    {
        label: 'Inaktiv',
        value: 'inactive',
    },
    {
        label: 'Gelöscht',
        value: 'deleted',
    },
];

export function UserListPage() {
    const dispatch = useAppDispatch();
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });
    const systemRolesApiService = useMemo(() => new SystemRolesApiService(), []);
    const [systemRoleNamesById, setSystemRoleNamesById] = useState<Record<number, string>>({});
    const [isSystemRolesLoading, setIsSystemRolesLoading] = useState(true);

    useEffect(() => {
        let isCancelled = false;

        setIsSystemRolesLoading(true);

        systemRolesApiService
            .listAllOrdered('name', 'ASC')
            .then((result) => {
                if (isCancelled) {
                    return;
                }

                setSystemRoleNamesById(Object.fromEntries(result.content.map((role) => [role.id, role.name])));
            })
            .catch((err) => {
                if (isCancelled) {
                    return;
                }

                setSystemRoleNamesById({});
                dispatch(showApiErrorSnackbar(err, 'Die Systemrollen konnten nicht geladen werden.'));
            })
            .finally(() => {
                if (!isCancelled) {
                    setIsSystemRolesLoading(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [dispatch, systemRolesApiService]);

    const columns = useMemo<GenericListColDef<User>[]>(() => [
        {
            field: 'lastName',
            headerName: 'Nachname',
            flex: 1,
            renderCell: (params) => (
                <CellLink
                    to={`/users/${params.id}`}
                    title="Mitarbeiter:in anzeigen"
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
        {
            field: 'firstName',
            headerName: 'Vorname',
            flex: 1,
        },
        {
            field: 'email',
            headerName: 'E-Mail-Adresse',
            flex: 1,
            renderCell: (params) => (
                <Link
                    href={`mailto:${params.value}`}
                    title="E-Mail an Mitarbeiter:in verfassen (im Standard-Mailprogramm, wenn verfügbar)"
                    sx={{
                        textDecoration: 'none',
                        color: 'inherit',
                        whiteSpace: 'nowrap',
                    }}
                >
                    <span>{params.value}</span>
                </Link>
            ),
        },
        {
            field: 'systemRoleId',
            headerName: 'Systemrolle',
            flex: 1,
            renderCell: (params) => {
                const systemRoleId = params.row.systemRoleId;

                let roleLabel: string;
                if (systemRoleId == null) {
                    roleLabel = 'Keine Systemrolle';
                } else if (systemRoleNamesById[systemRoleId] != null) {
                    roleLabel = systemRoleNamesById[systemRoleId];
                } else if (isSystemRolesLoading) {
                    roleLabel = 'Lade Systemrolle...';
                } else {
                    roleLabel = `Unbekannte Rolle (#${systemRoleId})`;
                }

                return (
                    <Chip
                        label={roleLabel}
                        size="small"
                        variant="outlined"
                    />
                );
            },
        },
        {
            field: 'enabled',
            headerName: 'Status',
            type: 'boolean',
            renderCell: (params) => (
                <UserStatusChip
                    userDeletedInIdp={params.row.deletedInIdp}
                    userEnabled={params.row.enabled}
                />
            ),
        },
    ], [isSystemRolesLoading, systemRoleNamesById]);

    return (
        <PageWrapper
            title="Mitarbeiter:innen"
            fullWidth
            background
        >
            <GenericListPage<User>
                filters={Filters}
                defaultFilter="active"
                header={{
                    icon: <PeopleOutlined/>,
                    title: 'Mitarbeiter:innen',
                    actions: [
                        {
                            label: 'Neue Mitarbeiter:in anlegen',
                            icon: <Add/>,
                            to: "/users/new",
                            variant: 'contained',
                            disabled: !hasAccess,
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Mitarbeiter:innen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Mitarbeiter:innen sind Benutzer:innen, die Zugriff auf das System haben und die
                                    Anwendung nutzen können.
                                    In dieser Oberfläche können Sie die im System verfügbaren Mitarbeiter:innen
                                    einsehen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Informationen zu Mitarbeitenden werden von einem Identity Provider (IDP) System
                                    bereitgestellt.
                                    Änderungen an den hier angezeigten Daten sind nur über die Verwaltungsoberfläche des
                                    IDP möglich.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Mitarbeiter:in suchen"
                searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                fetch={(options) => {
                    const filters: Partial<UserFilter> = {
                        name: options.search,
                    };

                    switch (options.filter) {
                        case 'active':
                            filters.deletedInIdp = false;
                            filters.disabledInIdp = false;
                            break;
                        case 'inactive':
                            filters.deletedInIdp = false;
                            filters.disabledInIdp = true;
                            break;
                        case 'deleted':
                            filters.deletedInIdp = true;
                            filters.disabledInIdp = undefined;
                            break;
                    }

                    return new UsersApiService()
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            filters,
                        );
                }}
                columnIcon={<Person/>}
                columnDefinitions={columns}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Mitarbeiter:innen angelegt"
                noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                rowActionsCount={2}
                rowActions={(item: User) => [
                    {
                        icon: hasAccess ? <EditOutlined/> : <Visibility/>,
                        to: `/users/${item.id}`,
                        tooltip: hasAccess ? 'Mitarbeiter:in bearbeiten' : 'Mitarbeiter:in anzeigen',
                    },
                    {
                        icon: <MailOutlined/>,
                        href: `mailto:${item.email}`,
                        tooltip: 'E-Mail an Mitarbeiter:in verfassen (im Standard-Mailprogramm, wenn verfügbar)',
                    },
                ]}
                defaultSortField="lastName"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
