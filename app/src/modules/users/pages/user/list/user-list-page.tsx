import {GenericListPage} from '../../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../../components/page-wrapper/page-wrapper';
import {Link, Typography} from '@mui/material';
import {EditOutlined, MailOutlined, PeopleOutlined, PersonOutlined} from '@mui/icons-material';
import React from 'react';
import {CellLink} from '../../../../../components/cell-link/cell-link';
import {useAdminGuard} from '../../../../../hooks/use-admin-guard';
import {UserFilter, UsersApiService} from '../../../users-api-service';
import {AppConfig} from '../../../../../app-config';
import {type User} from '../../../../../models/entities/user';
import Chip from '@mui/material/Chip';
import ManageAccountsOutlinedIcon from '@mui/icons-material/ManageAccountsOutlined';

export function UserListPage() {
    useAdminGuard();

    return (
        <PageWrapper
            title="Mitarbeiter:innen"
            fullWidth
            background
        >
            <GenericListPage<User>
                filters={[
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
                ]}
                defaultFilter="active"
                header={{
                    icon: <PeopleOutlined />,
                    title: 'Mitarbeiter:innen',
                    actions: [
                        {
                            label: 'Mitarbeiter:innen verwalten',
                            icon: <ManageAccountsOutlinedIcon />,
                            href: `${AppConfig.staff.host}/admin/${AppConfig.staff.realm}/console/#/${AppConfig.staff.realm}/users`,
                            variant: 'contained',
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Mitarbeiter:innen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography>
                                    Mitarbeiter:innen sind Benutzer:innen, die Zugriff auf das System haben und die Anwendung nutzen können.
                                    In dieser Oberfläche können Sie die im System verfügbaren Mitarbeiter:innen einsehen.
                                </Typography>
                                <Typography sx={{mt: 2}}>
                                    Informationen zu Mitarbeitenden werden von einem Identity Provider (IDP) System bereitgestellt.
                                    Änderungen an den hier angezeigten Daten sind nur über die Verwaltungsoberfläche des IDP möglich.
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

                    return new UsersApiService(options.api)
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            filters,
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: () => <PersonOutlined />,
                        disableColumnMenu: true,
                        width: 24,
                        sortable: false,
                    },
                    {
                        field: 'lastName',
                        headerName: 'Nachname',
                        flex: 1,
                        renderCell: (params) => (
                            <CellLink
                                to={`/users/${params.id}`}
                                title="Mitarbeiter:in bearbeiten"
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
                                sx={{textDecoration: 'none', color: 'inherit'}}
                            >
                                {String(params.value)}
                            </Link>
                        ),
                    },
                    {
                        field: 'enabled',
                        headerName: 'Status',
                        type: 'boolean',
                        renderCell: (params) => (
                            params.row.deletedInIdp ?
                                <Chip
                                    label="Gelöscht"
                                    color="error"
                                    variant="outlined"
                                    size={'small'}
                                    title="Diese Mitarbeiter:in wurde im Identity Provider gelöscht und kann sich nicht anmelden."
                                /> : (
                                    params.value ?
                                        <Chip
                                            label="Aktiv"
                                            variant="outlined"
                                            size={'small'}
                                        /> :
                                        <Chip
                                            label="Inaktiv"
                                            color="warning"
                                            variant="outlined"
                                            size={'small'}
                                            title="Diese Mitarbeiter:in ist inaktiv und kann sich nicht anmelden."
                                        />
                                )
                        ),
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Mitarbeiter:innen angelegt"
                noSearchResultsPlaceholder="Keine Mitarbeiter:innen gefunden"
                rowActionsCount={2}
                rowActions={(item: User) => [
                    {
                        icon: <EditOutlined />,
                        to: `/users/${item.id}`,
                        tooltip: 'Mitarbeiter:in bearbeiten',
                    },
                    {
                        icon: <MailOutlined />,
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