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
import React, {useCallback} from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {SystemRoleEntity} from "../../entities/system-role-entity";
import {SystemRolesApiService} from "../../services/system-roles-api-service";
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {
    DefaultUserSystemRoleBadge,
    isDefaultUserSystemRole,
} from '../../components/default-user-system-role-badge';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {Permission} from '../../../../data/permissions/permission';
import {
    isMostPrivilegedSystemRole,
    MostPrivilegedSystemRoleBadge,
} from '../../components/most-privileged-system-role-badge';

const systemRolesListPermissionCheck: GenericListPagePermissionConfig<SystemRoleEntity> = {
    scope: {
        type: 'system',
    },
    read: Permission.SYSTEM_ROLE_READ,
    create: Permission.SYSTEM_ROLE_CREATE,
    update: Permission.SYSTEM_ROLE_UPDATE,
};

export function SystemRolesListPage() {
    const navigate = useNavigate();
    const defaultSystemRoleId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.users.defaultSystemRole));
    const mostPrivilegedSystemRoleId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.systemRoles.mostPrivilegedRole));

    const header = useCallback((permissions: GenericListPagePermissionState<SystemRoleEntity>) => ({
        icon: ModuleIcons.roles,
        title: 'Systemrollen',
        actions: [
            {
                label: 'Neue Systemrolle',
                icon: <AddOutlinedIcon/>,
                to: '/system-roles/new',
                variant: 'contained' as const,
                disabled: !permissions.canCreate,
                disabledTooltip: permissions.createDisabledTooltip,
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Systemrollen',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography
                        variant="body1"
                        component="p"
                    >
                        Systemrollen definieren Berechtigungen auf Systemebene und gelten
                        anwendungsweit in Gover.
                    </Typography>
                    <Typography
                        variant="body1"
                        component="p"
                    >
                        Sie steuern damit grundlegende Zugriffe, etwa auf Administration,
                        Konfiguration und andere globale Funktionen. Im Unterschied zu
                        Domänenrollen sind Systemrollen nicht an einzelne Organisationseinheiten
                        oder Teams gebunden.
                    </Typography>
                </>
            ),
        },
    }), []);

    const fetchSystemRoles = useCallback((options: GenericListPropsFetchOptions<SystemRoleEntity>) => {
        return new SystemRolesApiService()
            .list(
                options.page,
                options.size,
                options.sort,
                options.order,
                {
                    name: options.search,
                },
            );
    }, []);

    const columnIcon = useCallback(() => ModuleIcons.roles, []);

    const columnDefinitions = useCallback((permissions: GenericListPagePermissionState<SystemRoleEntity>) => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => {
                return (
                    <CellLink
                        to={`/system-roles/${params.id}`}
                        title={permissions.canUpdate(params.row) ? 'Systemrolle bearbeiten' : 'Systemrolle anzeigen'}
                    >
                        {String(params.value)}
                        {isDefaultUserSystemRole(params.row.id, defaultSystemRoleId) &&
                            <DefaultUserSystemRoleBadge sx={{ml: 1}}/>}
                        {isMostPrivilegedSystemRole(params.row.id, mostPrivilegedSystemRoleId) &&
                            <MostPrivilegedSystemRoleBadge sx={{ml: 1}}/>}
                    </CellLink>
                );
            },
        },
        {
            field: 'description',
            headerName: 'Beschreibung',
            flex: 2,
        },
    ], [defaultSystemRoleId, mostPrivilegedSystemRoleId]);

    const getRowIdentifier = useCallback((row: SystemRoleEntity) => row.id.toString(), []);

    const rowActions = useCallback((item: SystemRoleEntity, permissions: GenericListPagePermissionState<SystemRoleEntity>) => {
        const canUpdateSystemRole = permissions.canUpdate(item);

        return [
            {
                icon: canUpdateSystemRole ? <EditOutlined/> : <Visibility/>,
                to: `/system-roles/${item.id}`,
                tooltip: canUpdateSystemRole ? 'Systemrolle bearbeiten' : 'Systemrolle anzeigen',
            },
        ];
    }, []);

    const noDataPlaceholder = useCallback((permissions: GenericListPagePermissionState<SystemRoleEntity>) => (
        <EmptyDataListPlaceholder
            title="Keine Systemrollen vorhanden"
            description="Es wurden noch keine Systemrollen angelegt."
            addText="Neue Systemrolle anlegen"
            onAdd={() => navigate('/system-roles/new')}
            addDisabled={!permissions.canCreate}
            addDisabledTooltip={permissions.createDisabledTooltip}
        />
    ), [navigate]);

    return (
        <PageWrapper
            title="Systemrollen"
            fullWidth
            background
        >
            <GenericListPage<SystemRoleEntity>
                header={header}
                permissionCheck={systemRolesListPermissionCheck}
                searchLabel="Systemrolle suchen"
                searchPlaceholder="Name der Systemrolle eingeben…"
                fetch={fetchSystemRoles}
                columnIcon={columnIcon}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={noDataPlaceholder}
                noSearchResultsPlaceholder="Keine Systemrollen gefunden, die zu Ihrer Suche passen"
                rowActionsCount={1}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
