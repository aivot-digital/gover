import {GenericListPage} from '../../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../../components/page-wrapper/page-wrapper';
import {Link, Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import MailOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Mail';
import PeopleOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Group';
import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {CellLink} from '../../../../../components/cell-link/cell-link';
import {useAccessGuard} from '../../../../../hooks/use-admin-guard';
import {UserFilter, UsersApiService} from '../../../users-api-service';
import {type User} from '../../../../../models/entities/user';
import Chip from '@mui/material/Chip';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {UserStatusChip} from '../../../components/user-status-chip';
import Person from '@aivot/mui-material-symbols-400-n25-outlined/Person';
import {GenericListColDef} from "../../../../../components/generic-list/generic-list-props";
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {SystemRolesApiService} from '../../../../system/services/system-roles-api-service';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../../../slices/snackbar-slice';
import {GenericListPropsFetchOptions} from '../../../../../components/generic-list/generic-list-props';
import {useCheckSystemPermission} from '../../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../../data/permissions/permission';
import {isApiError} from '../../../../../models/api-error';

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
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });
    const systemRolesApiService = useMemo(() => new SystemRolesApiService(), []);
    const canReadSystemRoles = useCheckSystemPermission(Permission.SYSTEM_ROLE_READ);
    const [systemRoleNamesById, setSystemRoleNamesById] = useState<Record<number, string>>({});
    const [isSystemRolesLoading, setIsSystemRolesLoading] = useState(true);
    const [systemRolesAccessDenied, setSystemRolesAccessDenied] = useState(false);

    useEffect(() => {
        let isCancelled = false;

        setSystemRolesAccessDenied(false);

        if (!canReadSystemRoles) {
            setSystemRoleNamesById({});
            setIsSystemRolesLoading(false);
            return () => {
                isCancelled = true;
            };
        }

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
                if (isApiError(err) && err.status === 403) {
                    setSystemRolesAccessDenied(true);
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Die Systemrollen konnten nicht geladen werden.'));
                }
            })
            .finally(() => {
                if (!isCancelled) {
                    setIsSystemRolesLoading(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [canReadSystemRoles, dispatch, systemRolesApiService]);

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
            renderCell: (params) => params.row.deletedInIdp ? (
                <Typography
                    component="span"
                    variant="body2"
                    color="text.disabled"
                    title="Für im Identity Provider gelöschte Mitarbeiter:innen können keine E-Mails mehr verfasst werden."
                    sx={{whiteSpace: 'nowrap'}}
                >
                    {String(params.value)}
                </Typography>
            ) : (
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
                } else if (!canReadSystemRoles || systemRolesAccessDenied) {
                    roleLabel = 'Keine Berechtigung zur Einsicht';
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
                        title={
                            systemRoleId != null && (!canReadSystemRoles || systemRolesAccessDenied)
                                ? 'Für die Anzeige der Systemrolle ist die Berechtigung system_role.read erforderlich.'
                                : undefined
                        }
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
    ], [canReadSystemRoles, isSystemRolesLoading, systemRoleNamesById, systemRolesAccessDenied]);

    const header = useMemo(() => ({
        icon: <PeopleOutlined/>,
        title: 'Mitarbeiter:innen',
        actions: [
            {
                label: 'Neue Mitarbeiter:in anlegen',
                icon: <Add/>,
                to: "/users/new",
                variant: 'contained' as const,
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
    }), [hasAccess]);

    const fetchUsers = useCallback((options: GenericListPropsFetchOptions<User>) => {
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
    }, []);

    const columnIcon = useMemo(() => <Person/>, []);

    const getRowIdentifier = useCallback((row: User) => row.id.toString(), []);

    const rowActions = useCallback((item: User) => [
        {
            icon: hasAccess ? <EditOutlined/> : <Visibility/>,
            to: `/users/${item.id}`,
            tooltip: hasAccess ? 'Mitarbeiter:in bearbeiten' : 'Mitarbeiter:in anzeigen',
        },
        {
            icon: <MailOutlined/>,
            href: `mailto:${item.email}`,
            tooltip: 'E-Mail an Mitarbeiter:in verfassen (im Standard-Mailprogramm, wenn verfügbar)',
            disabled: item.deletedInIdp,
            disabledTooltip: 'Für im Identity Provider gelöschte Mitarbeiter:innen können keine E-Mails mehr verfasst werden.',
        },
    ], [hasAccess]);

    return (
        <PageWrapper
            title="Mitarbeiter:innen"
            fullWidth
            background
        >
            <GenericListPage<User>
                filters={Filters}
                defaultFilter="active"
                header={header}
                searchLabel="Mitarbeiter:in suchen"
                searchPlaceholder="Name der Mitarbeiter:in eingeben…"
                fetch={fetchUsers}
                columnIcon={columnIcon}
                columnDefinitions={columns}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Noch keine Mitarbeiter:innen angelegt"
                        description="Mitarbeiter:innen sind Benutzerkonten für Personen, die Gover verwalten oder Aufgaben in Vorgängen bearbeiten."
                        addText={hasAccess ? "Mitarbeiter:in anlegen" : undefined}
                        onAdd={hasAccess ? () => navigate('/users/new') : undefined}
                    />
                }
                noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="lastName"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
