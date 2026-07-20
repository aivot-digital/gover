import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import {Typography} from '@mui/material';
import EditOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import GroupOutlined from '@aivot/mui-material-symbols-400-n25-outlined/Group';
import {CellLink} from '../../../../components/cell-link/cell-link';
import Visibility from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import {TeamsApiService} from '../../services/teams-api-service';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {TeamEntity} from "../../entities/team-entity";
import React, {useCallback, useMemo} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectPermissions} from '../../../../slices/user-slice';
import {Permission} from '../../../../data/permissions/permission';
import {
    checkSystemPermission,
    checkTeamPermission,
    formatMissingPermissionTooltip,
} from '../../../permissions/utils/permission-utils';
import {CellContentWrapper} from '../../../../components/cell-content-wrapper/cell-content-wrapper';

export function TeamsListPage() {
    const navigate = useNavigate();
    const permissions = useAppSelector(selectPermissions);
    const canCreateTeam = checkSystemPermission(permissions, Permission.TEAM_CREATE);

    const header = useMemo(() => ({
        icon: ModuleIcons.teams,
        title: 'Teams',
        actions: [
            {
                label: 'Neues Team',
                icon: <AddOutlinedIcon />,
                to: '/teams/new',
                variant: 'contained' as const,
                disabled: !canCreateTeam,
                disabledTooltip: formatMissingPermissionTooltip(Permission.TEAM_CREATE),
            },
        ],
        helpDialog: {
            title: 'Hilfe zu Teams',
            tooltip: 'Hilfe anzeigen',
            content: (
                <>
                    <Typography>
                        Teams fassen Mitarbeiter:innen für fachliche oder organisatorische
                        Aufgabenbereiche zusammen und können unabhängig von
                        Organisationseinheiten genutzt werden.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Teammitgliedschaften können mit Domänenrollen kombiniert werden, um
                        Berechtigungen gezielt innerhalb eines Teams zu vergeben.
                    </Typography>
                </>
            ),
        },
    }), [canCreateTeam]);

    const fetchTeams = useCallback((options: GenericListPropsFetchOptions<TeamEntity>) => {
        return new TeamsApiService()
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

    const columnIcon = useCallback(() => ModuleIcons.teams, []);

    const columnDefinitions = useMemo(() => [
        {
            field: 'name',
            headerName: 'Name',
            flex: 1,
            renderCell: (params: any) => {
                const canReadTeam = checkTeamPermission(permissions, params.row.id, Permission.TEAM_READ);
                const canUpdateTeam = checkTeamPermission(permissions, params.row.id, Permission.TEAM_UPDATE);

                if (!canReadTeam) {
                    return (
                        <CellContentWrapper title={formatMissingPermissionTooltip(Permission.TEAM_READ)}>
                            {String(params.value)}
                        </CellContentWrapper>
                    );
                }

                return (
                    <CellLink
                        to={`/teams/${params.id}`}
                        title={canUpdateTeam ? 'Team bearbeiten' : 'Team ansehen'}
                    >
                        {String(params.value)}
                    </CellLink>
                );
            },
        },
    ], [permissions]);

    const getRowIdentifier = useCallback((row: TeamEntity) => row.id.toString(), []);

    const rowActions = useCallback((item: TeamEntity) => {
        const canReadTeam = checkTeamPermission(permissions, item.id, Permission.TEAM_READ);
        const canUpdateTeam = checkTeamPermission(permissions, item.id, Permission.TEAM_UPDATE);
        const canReadMemberships = checkTeamPermission(permissions, item.id, Permission.TEAM_MEMBERSHIP_READ);

        return [
            {
                icon: canUpdateTeam ? <EditOutlined /> : <Visibility />,
                to: `/teams/${item.id}`,
                tooltip: canUpdateTeam ? 'Team bearbeiten' : 'Team ansehen',
                disabled: !canReadTeam,
                disabledTooltip: formatMissingPermissionTooltip(Permission.TEAM_READ),
            },
            {
                icon: <GroupOutlined />,
                to: `/teams/${item.id}/members`,
                tooltip: 'Teammitglieder ansehen',
                disabled: !canReadMemberships,
                disabledTooltip: formatMissingPermissionTooltip(Permission.TEAM_MEMBERSHIP_READ),
            },
        ];
    }, [permissions]);

    return (
        <PageWrapper
            title="Teams"
            fullWidth
            background
        >
            <GenericListPage<TeamEntity>
                header={header}
                searchLabel="Team suchen"
                searchPlaceholder="Name des Teams eingeben…"
                fetch={fetchTeams}
                columnIcon={columnIcon}
                columnDefinitions={columnDefinitions}
                getRowIdentifier={getRowIdentifier}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Teams im Zugriff"
                        description="Es wurden keine Teams gefunden, auf die Sie Zugriff haben. Möglicherweise wurden noch keine Teams angelegt oder Ihnen fehlen die erforderlichen Leseberechtigungen."
                        addText="Neues Team anlegen"
                        onAdd={() => navigate('/teams/new')}
                        addDisabled={!canCreateTeam}
                        addDisabledTooltip={formatMissingPermissionTooltip(Permission.TEAM_CREATE)}
                    />
                }
                noSearchResultsPlaceholder="Keine Teams gefunden, die zu Ihrer Suche oder Ihren Berechtigungen passen"
                rowActionsCount={2}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
