import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import React, {useCallback, useMemo} from 'react';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
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
import {selectPermissions} from '../../../../slices/user-slice';
import {Permission} from '../../../../data/permissions/permission';
import {checkSystemPermission, formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';

export function SystemRolesListPage() {
    const navigate = useNavigate();
    useHasSystemPermission(Permission.SYSTEM_ROLE_READ);
    const defaultSystemRoleId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.users.defaultSystemRole));
    const permissions = useAppSelector(selectPermissions);
    const canCreateSystemRole = checkSystemPermission(permissions, Permission.SYSTEM_ROLE_CREATE);
    const canUpdateSystemRoles = checkSystemPermission(permissions, Permission.SYSTEM_ROLE_UPDATE);

    const header = useMemo(() => ({
        icon: ModuleIcons.roles,
        title: 'Systemrollen',
        actions: [
            {
                label: 'Neue Systemrolle',
                icon: <AddOutlinedIcon/>,
                to: '/system-roles/new',
                variant: 'contained' as const,
                disabled: !canCreateSystemRole,
                disabledTooltip: formatMissingPermissionTooltip(Permission.SYSTEM_ROLE_CREATE),
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
    }), [canCreateSystemRole]);

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

    const columnDefinitions = useMemo(() => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => {
                const badge = isDefaultUserSystemRole(params.row.id, defaultSystemRoleId) &&
                    <DefaultUserSystemRoleBadge sx={{ml: 1}} />;

                return (
                    <CellLink
                        to={`/system-roles/${params.id}`}
                        title={canUpdateSystemRoles ? 'Systemrolle bearbeiten' : 'Systemrolle anzeigen'}
                    >
                        {String(params.value)}
                        {badge}
                    </CellLink>
                );
            },
        },
        {
            field: 'description',
            headerName: 'Beschreibung',
            flex: 2,
        },
    ], [canUpdateSystemRoles, defaultSystemRoleId]);

    const getRowIdentifier = useCallback((row: SystemRoleEntity) => row.id.toString(), []);

    const rowActions = useCallback((item: SystemRoleEntity) => [
        {
            icon: canUpdateSystemRoles ? <EditOutlined/> : <Visibility/>,
            to: `/system-roles/${item.id}`,
            tooltip: canUpdateSystemRoles ? 'Systemrolle bearbeiten' : 'Systemrolle anzeigen',
        },
    ], [canUpdateSystemRoles]);

    return (
        <PageWrapper
            title="Systemrollen"
            fullWidth
            background
        >
            <GenericListPage<SystemRoleEntity>
                header={header}
                searchLabel="Systemrolle suchen"
                searchPlaceholder="Name der Systemrolle eingeben…"
                fetch={fetchSystemRoles}
                columnIcon={columnIcon}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Systemrollen vorhanden"
                        description="Es wurden noch keine Systemrollen angelegt."
                        addText="Neue Systemrolle anlegen"
                        onAdd={() => navigate('/system-roles/new')}
                        addDisabled={!canCreateSystemRole}
                        addDisabledTooltip={formatMissingPermissionTooltip(Permission.SYSTEM_ROLE_CREATE)}
                    />
                }
                noSearchResultsPlaceholder="Keine Systemrollen gefunden, die zu Ihrer Suche passen"
                rowActionsCount={1}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
