import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import KeyOutlinedIcon from '@mui/icons-material/KeyOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import React from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {UserRoleResponseDTO} from '../../dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles-api-service';

export function UserRolesListPage() {
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    return (
        <PageWrapper
            title="Nutzerrollen"
            fullWidth
            background
        >
            <GenericListPage<UserRoleResponseDTO>
                header={{
                    icon: <KeyOutlinedIcon />,
                    title: 'Nutzerrollen',
                    actions: [
                        {
                            label: 'Neue Nutzerrolle',
                            icon: <AddOutlinedIcon />,
                            to: '/user-roles/new',
                            variant: 'contained',
                            disabled: !hasAccess,
                        },
                    ],
                    helpDialog: {
                        title: 'Hilfe zu Geheimnissen',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <>
                                <Typography
                                    variant="body1"
                                    paragraph
                                >
                                    Verwalten Sie hier sicher die Geheimnisse Ihrer Webanwendung, wie API-Schlüssel, Passwörter oder andere vertrauliche Daten.
                                    Diese werden getrennt vom Code gespeichert, um Sicherheitsrisiken zu minimieren und eine einfache Aktualisierung ohne Anpassung der Anwendung zu ermöglichen.
                                </Typography>
                                <Typography
                                    variant="body1"
                                    paragraph
                                >
                                    Alle Geheimnisse sind verschlüsselt und nur für autorisierte Nutzer:innen oder Dienste mit entsprechender Berechtigung zugänglich.
                                </Typography>
                            </>
                        ),
                    },
                }}
                searchLabel="Geheimnis suchen"
                searchPlaceholder="Name des Geheimnisses eingeben…"
                fetch={(options) => {
                    return new UserRolesApiService()
                        .list(
                            options.page,
                            options.size,
                            options.sort,
                            options.order,
                            {name: options.search},
                        );
                }}
                columnDefinitions={[
                    {
                        field: 'icon',
                        headerName: '',
                        renderCell: () => <CellContentWrapper><KeyOutlinedIcon /></CellContentWrapper>,
                        disableColumnMenu: true,
                        width: 24,
                        sortable: false,
                    },
                    {
                        field: 'name',
                        headerName: 'Name',
                        flex: 1,
                        renderCell: (params) => (
                            <CellLink
                                to={`/user-roles/${params.id}`}
                                title={hasAccess ? 'Geheimnis bearbeiten' : 'Geheimnis anzeigen'}
                            >
                                {String(params.value)}
                            </CellLink>
                        ),
                    },
                    {
                        field: 'description',
                        headerName: 'Beschreibung',
                        flex: 2,
                    },
                ]}
                getRowIdentifier={row => row.id.toString()}
                noDataPlaceholder="Keine Geheimnisse angelegt"
                noSearchResultsPlaceholder="Keine Geheimnisse gefunden"
                rowActionsCount={1}
                rowActions={(item: UserRoleResponseDTO) => [
                    {
                        icon: hasAccess ? <EditOutlined /> : <Visibility />,
                        to: `/user-roles/${item.id}`,
                        tooltip: hasAccess ? 'Geheimnis bearbeiten' : 'Geheimnis anzeigen',
                    },
                ]}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}