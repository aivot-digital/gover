import {
    GenericListPage,
    type GenericListPagePermissionConfig,
    type GenericListPagePermissionState,
} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import {CellLink} from '../../../../components/cell-link/cell-link';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {UserRoleResponseDTO} from '../../dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles-api-service';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import React, {useCallback} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {Permission} from '../../../../data/permissions/permission';

const userRolesListPermissionCheck: GenericListPagePermissionConfig<UserRoleResponseDTO> = {
    scope: {
        type: 'system',
    },
    read: Permission.DOMAIN_ROLE_READ,
    create: Permission.DOMAIN_ROLE_CREATE,
    update: Permission.DOMAIN_ROLE_UPDATE,
};

export function UserRolesListPage() {
    const navigate = useNavigate();

    const header = useCallback((permissions: GenericListPagePermissionState<UserRoleResponseDTO>) => ({
        icon: ModuleIcons.roles,
        title: 'Domänenrollen',
        actions: [
            {
                label: 'Neue Domänenrolle',
                icon: <AddOutlinedIcon />,
                to: '/user-roles/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Domänenrollen',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography
                        variant="body1"
                        component="p"
                    >
                        Domänenrollen definieren Berechtigungen innerhalb fachlicher Domänen,
                        zum Beispiel in Organisationseinheiten oder Teams.
                    </Typography>
                    <Typography
                        variant="body1"
                        component="p"
                    >
                        Sie ergänzen die globalen Systemrollen um kontextbezogene Rechte und
                        werden Mitgliedschaften von Mitarbeiter:innen in Teams und
                        Organisationseinheiten zugewiesen.
                    </Typography>
                </>
            ),
        },
    }), []);

    const fetchUserRoles = useCallback((options: GenericListPropsFetchOptions<UserRoleResponseDTO>) => {
        return new UserRolesApiService()
            .list(
                options.page,
                options.size,
                options.sort,
                options.order,
                {name: options.search},
            );
    }, []);

    const columnIcon = useCallback(() => ModuleIcons.roles, []);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<UserRoleResponseDTO>) => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/user-roles/${params.id}`}
                    title={permissions.canUpdate(params.row) ? 'Domänenrolle bearbeiten' : 'Domänenrolle anzeigen'}
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
    ], []);

    const getRowIdentifier = useCallback((row: UserRoleResponseDTO) => row.id.toString(), []);

    const rowActions = useCallback((item: UserRoleResponseDTO, permissions: GenericListPagePermissionState<UserRoleResponseDTO>) => {
        const canUpdateUserRole = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateUserRole ? <EditOutlined /> : <Visibility />,
                to: `/user-roles/${item.id}`,
                tooltip: canUpdateUserRole ? 'Domänenrolle bearbeiten' : 'Domänenrolle anzeigen',
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<UserRoleResponseDTO>) => (
        <EmptyDataListPlaceholder
            title="Keine Domänenrollen vorhanden"
            description="Es wurden noch keine Domänenrollen angelegt."
            addText="Neue Domänenrolle anlegen"
            onAdd={() => navigate('/user-roles/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <PageWrapper
            title="Domänenrollen"
            fullWidth
            background
        >
            <GenericListPage<UserRoleResponseDTO>
                header={header}
                permissionCheck={userRolesListPermissionCheck}
                searchLabel="Domänenrolle suchen"
                searchPlaceholder="Name der Domänenrolle eingeben…"
                fetch={fetchUserRoles}
                columnIcon={columnIcon}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Domänenrollen gefunden, die zu Ihrer Suche passen"
                rowActionsCount={1}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
