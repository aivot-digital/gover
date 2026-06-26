import {GenericListPage} from '../../../../components/generic-list-page/generic-list-page';
import {useNavigate} from 'react-router-dom';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {Typography} from '@mui/material';
import {EditOutlined, GroupOutlined} from '@mui/icons-material';
import {CellLink} from '../../../../components/cell-link/cell-link';
import {useAccessGuard} from '../../../../hooks/use-admin-guard';
import Visibility from '@aivot/mui-material-symbols-400-outlined/dist/visibility/Visibility';
import {TeamsApiService} from '../../services/teams-api-service';
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {TeamEntity} from "../../entities/team-entity";
import React, {useCallback, useMemo} from 'react';
import {GenericListPropsFetchOptions} from '../../../../components/generic-list/generic-list-props';

export function TeamsListPage() {
    const navigate = useNavigate();
    const hasAccess = useAccessGuard({
        onlyGlobalAdmin: true,
        messageType: 'snackbar',
    });

    const header = useMemo(() => ({
        icon: ModuleIcons.teams,
        title: 'Teams',
        actions: [
            {
                label: 'Neues Team',
                icon: <AddOutlinedIcon />,
                to: '/teams/new',
                variant: 'contained' as const,
                disabled: !hasAccess,
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
    }), [hasAccess]);

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
            renderCell: (params: any) => (
                <CellLink
                    to={`/teams/${params.id}`}
                    title={hasAccess ? 'Team bearbeiten' : 'Team ansehen'}
                >
                    {String(params.value)}
                </CellLink>
            ),
        },
    ], [hasAccess]);

    const getRowIdentifier = useCallback((row: TeamEntity) => row.id.toString(), []);

    const rowActions = useCallback((item: TeamEntity) => [
        {
            icon: hasAccess ? <EditOutlined /> : <Visibility />,
            to: `/teams/${item.id}`,
            tooltip: hasAccess ? 'Team bearbeiten' : 'Team ansehen',
        },
        {
            icon: <GroupOutlined />,
            to: `/teams/${item.id}/members`,
            tooltip: hasAccess ? 'Teammitglieder verwalten' : 'Teammitglieder ansehen',
        },
    ], [hasAccess]);

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
                        title="Noch keine Teams angelegt"
                        description="Teams bündeln Mitarbeiter:innen für gemeinsame Zuständigkeiten, Berechtigungen oder Aufgaben in Prozessen."
                        addText={hasAccess ? "Neues Team anlegen" : undefined}
                        onAdd={hasAccess ? () => navigate('/teams/new') : undefined}
                    />
                }
                noSearchResultsPlaceholder="Keine Teams gefunden"
                rowActionsCount={3}
                rowActions={rowActions}
                defaultSortField="name"
                disableFullWidthToggle={true}
            />
        </PageWrapper>
    );
}
