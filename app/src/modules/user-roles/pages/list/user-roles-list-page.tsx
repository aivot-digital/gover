import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined} from '@mui/icons-material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {UserRoleResponseDTO} from '../../dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles-api-service';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import React, {useCallback, useMemo} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {Permission} from '../../../../data/permissions/permission';
import {checkSystemPermission, formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';

export function UserRolesListPage() {
    const navigate = useNavigate();
    useHasSystemPermission(Permission.DOMAIN_ROLE_READ);
    const permissions = useAppSelector(selectPermissions);
    const canCreateDomainRole = checkSystemPermission(permissions, Permission.DOMAIN_ROLE_CREATE);
    const canUpdateDomainRoles = checkSystemPermission(permissions, Permission.DOMAIN_ROLE_UPDATE);

    const header = useMemo(() => ({
        icon: ModuleIcons.roles,
        title: 'Domänenrollen',
        actions: [
            {
                label: 'Neue Domänenrolle',
                icon: <AddOutlinedIcon />,
                to: '/user-roles/new',
                variant: 'contained' as const,
                disabled: !canCreateDomainRole,
                disabledTooltip: formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_CREATE),
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
    }), [canCreateDomainRole]);

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

    const columnDefinitions = useMemo(() => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => (
                <CellLink
                    to={`/user-roles/${params.id}`}
                    title={canUpdateDomainRoles ? 'Domänenrolle bearbeiten' : 'Domänenrolle anzeigen'}
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
    ], [canUpdateDomainRoles]);

    const getRowIdentifier = useCallback((row: UserRoleResponseDTO) => row.id.toString(), []);

    const rowActions = useCallback((item: UserRoleResponseDTO) => [
        {
            icon: canUpdateDomainRoles ? <EditOutlined /> : <Visibility />,
            to: `/user-roles/${item.id}`,
            tooltip: canUpdateDomainRoles ? 'Domänenrolle bearbeiten' : 'Domänenrolle anzeigen',
        },
    ], [canUpdateDomainRoles]);

    return (
        <PageWrapper
            title="Domänenrollen"
            fullWidth
            background
        >
            <GenericListPage<UserRoleResponseDTO>
                header={header}
                searchLabel="Domänenrolle suchen"
                searchPlaceholder="Name der Domänenrolle eingeben…"
                fetch={fetchUserRoles}
                columnIcon={columnIcon}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Domänenrollen vorhanden"
                        description="Es wurden noch keine Domänenrollen angelegt."
                        addText="Neue Domänenrolle anlegen"
                        onAdd={() => navigate('/user-roles/new')}
                        addDisabled={!canCreateDomainRole}
                        addDisabledTooltip={formatMissingPermissionTooltip(Permission.DOMAIN_ROLE_CREATE)}
                    />
                }
                noSearchResultsPlaceholder="Keine Domänenrollen gefunden, die zu Ihrer Suche passen"
                rowActionsCount={1}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
