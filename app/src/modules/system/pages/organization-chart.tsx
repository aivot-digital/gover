import {Box, Skeleton, Stack, Typography} from '@mui/material';
import FamilyHistory from '@aivot/mui-material-symbols-400-n25-outlined/FamilyHistory';
import Groups from '@aivot/mui-material-symbols-400-n25-outlined/Groups';
import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {stringToPastelColor} from '../../../components/avatar/string-avatar';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {selectPermissions} from '../../../slices/user-slice';
import {DepartmentMembershipApiService} from '../../departments/services/department-membership-api-service';
import {VDepartmentShadowedApiService} from '../../departments/services/v-department-shadowed-api-service';
import {TeamsApiService} from '../../teams/services/teams-api-service';
import {TeamMembershipsApiService} from '../../teams/services/team-memberships-api-service';
import {UsersApiService} from '../../users/users-api-service';
import {OrganizationChartFlow, type OrganizationChartFlowView} from './organization-chart/organization-chart-flow';
import {getDepartmentTypeIcons} from '../../departments/utils/department-utils';
import {createSimulatedOrganizationChartData} from './organization-chart/organization-chart-simulation';
import {
    type OrganizationChartDepartmentItem,
    type OrganizationChartTeamItem,
} from './organization-chart/organization-chart-types';
import {
    compareNamedItems,
    compareOrganizationChartUsers,
    sortOrganizationChartDepartmentTrees,
} from './organization-chart/organization-chart-utils';
import {Permission} from '../../../data/permissions/permission';
import {
    hasDepartmentPermission,
    hasTeamPermission,
    formatMissingPermissionTooltip,
} from '../../permissions/utils/permission-utils';
import {
    useHasAnyDepartmentPermission,
    useHasAnyTeamPermission,
    useHasSystemPermission,
    useRequireAnyDepartmentPermission,
} from '../../permissions/hooks/use-permissions';

const SIMULATE_QUERY_PARAM = 'simulate';

export function OrganizationChart(): React.ReactElement {
    const dispatch = useAppDispatch();
    const permissionSet = useAppSelector(selectPermissions);
    const [searchParams] = useSearchParams();
    useRequireAnyDepartmentPermission(Permission.DEPARTMENT_READ);
    const canReadTeams = useHasAnyTeamPermission(Permission.TEAM_READ);
    const canReadUsers = useHasSystemPermission(Permission.USER_READ);
    const canReadAnyDepartmentMemberships = useHasAnyDepartmentPermission(Permission.DEPARTMENT_MEMBERSHIP_READ);
    const canReadAnyTeamMemberships = useHasAnyTeamPermission(Permission.TEAM_MEMBERSHIP_READ);
    const [rootDepartments, setRootDepartments] = useState<OrganizationChartDepartmentItem[]>();
    const [teams, setTeams] = useState<OrganizationChartTeamItem[]>();
    const [organizationChartView, setOrganizationChartView] = useState<OrganizationChartFlowView>('departments');
    const shouldSimulate = searchParams.get(SIMULATE_QUERY_PARAM) === '1';

    useEffect(() => {
        if (!canReadTeams && organizationChartView === 'teams') {
            setOrganizationChartView('departments');
        }
    }, [canReadTeams, organizationChartView]);

    useEffect(() => {
        let isActive = true;

        async function loadOrganizationChart(): Promise<void> {
            if (shouldSimulate) {
                const simulatedOrganizationChart = createSimulatedOrganizationChartData({
                    canReadDepartmentMemberships: canReadAnyDepartmentMemberships,
                    canReadTeamMemberships: canReadAnyTeamMemberships,
                });
                setRootDepartments(simulatedOrganizationChart.rootDepartments);
                setTeams(canReadTeams ? simulatedOrganizationChart.teams : []);
                return;
            }

            setRootDepartments(undefined);
            setTeams(undefined);

            try {
                const [
                    departments,
                    fetchedTeams,
                    users,
                    departmentMemberships,
                    teamMemberships,
                ] = await Promise.all([
                    new VDepartmentShadowedApiService().listAll({
                        includeAncestors: true,
                    }),
                    canReadTeams ? new TeamsApiService().listAll() : Promise.resolve(undefined),
                    canReadUsers ? new UsersApiService().listAll({
                        deletedInIdp: false,
                    }) : Promise.resolve(undefined),
                    canReadUsers && canReadAnyDepartmentMemberships ? new DepartmentMembershipApiService().listAll() : Promise.resolve(undefined),
                    canReadUsers && canReadTeams && canReadAnyTeamMemberships ? new TeamMembershipsApiService().listAll() : Promise.resolve(undefined),
                ]);

                const usersById = new Map((users?.content ?? []).map((user) => [user.id, user]));

                const departmentMap: Record<number, OrganizationChartDepartmentItem> = {};
                for (const dept of departments.content) {
                    departmentMap[dept.id] = {
                        ...dept,
                        color: stringToPastelColor(dept.name),
                        children: [],
                        canReadDetails: hasDepartmentPermission(permissionSet, dept.id, Permission.DEPARTMENT_READ),
                        canReadMemberships: hasDepartmentPermission(permissionSet, dept.id, Permission.DEPARTMENT_MEMBERSHIP_READ),
                        members: [],
                    };
                }

                const nextRootDepartments: OrganizationChartDepartmentItem[] = [];
                for (const dept of departments.content) {
                    if (dept.parentDepartmentId != null && departmentMap[dept.parentDepartmentId] != null) {
                        departmentMap[dept.parentDepartmentId].children.push(departmentMap[dept.id]);
                    } else {
                        nextRootDepartments.push(departmentMap[dept.id]);
                    }
                }

                for (const membership of departmentMemberships?.content ?? []) {
                    const dept = departmentMap[membership.departmentId];
                    const user = usersById.get(membership.userId);
                    if (dept != null && user != null) {
                        dept.members.push(user);
                    }
                }

                const nextTeams: OrganizationChartTeamItem[] = (fetchedTeams?.content ?? [])
                    .map((team) => ({
                        ...team,
                        color: stringToPastelColor(team.name),
                        canReadDetails: hasTeamPermission(permissionSet, team.id, Permission.TEAM_READ),
                        canReadMemberships: hasTeamPermission(permissionSet, team.id, Permission.TEAM_MEMBERSHIP_READ),
                        members: [],
                    }));

                const teamsById = new Map(nextTeams.map((team) => [team.id, team]));
                for (const membership of teamMemberships?.content ?? []) {
                    const team = teamsById.get(membership.teamId);
                    const user = usersById.get(membership.userId);
                    if (team != null && user != null) {
                        team.members.push(user);
                    }
                }

                sortOrganizationChartDepartmentTrees(nextRootDepartments);
                nextTeams.sort(compareNamedItems);
                nextTeams.forEach((team) => {
                    team.members.sort(compareOrganizationChartUsers);
                });

                if (!isActive) {
                    return;
                }
                setRootDepartments(nextRootDepartments);
                setTeams(nextTeams);
            } catch (err) {
                if (!isActive) {
                    return;
                }
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden des Organigramms'));
            }
        }

        loadOrganizationChart().catch(console.error);
        return () => {
            isActive = false;
        };
    }, [
        canReadAnyDepartmentMemberships,
        canReadAnyTeamMemberships,
        canReadTeams,
        canReadUsers,
        dispatch,
        permissionSet,
        shouldSimulate,
    ]);

    const isLoading = rootDepartments == null || teams == null;

    return (
        <PageWrapper
            title="Organigramm"
            fullWidth
            fullHeight
            background
        >
            <Box
                sx={{
                    px: {xs: 2, md: 4},
                    py: 2,
                    height: '100vh',
                    display: 'flex',
                    flexDirection: 'column',
                    minHeight: 0,
                }}
            >
                <GenericPageHeader
                    icon={<FamilyHistory />}
                    title="Organigramm"
                    actions={[
                        {
                            label: 'Organisationseinheiten',
                            icon: getDepartmentTypeIcons(0),
                            iconPosition: 'start',
                            variant: 'text',
                            color: organizationChartView === 'departments' ? 'primary' : 'inherit',
                            activeStyle: {
                                borderBottom: 2,
                                borderColor: organizationChartView === 'departments' ? 'primary.main' : 'transparent',
                                borderRadius: 0,
                            },
                            onClick: () => {
                                setOrganizationChartView('departments');
                            },
                        },
                        {
                            label: 'Teams',
                            icon: <Groups />,
                            iconPosition: 'start',
                            variant: 'text',
                            color: organizationChartView === 'teams' ? 'primary' : 'inherit',
                            disabled: !canReadTeams,
                            disabledTooltip: formatMissingPermissionTooltip(Permission.TEAM_READ),
                            activeStyle: {
                                borderBottom: 2,
                                borderColor: organizationChartView === 'teams' ? 'primary.main' : 'transparent',
                                borderRadius: 0,
                            },
                            onClick: () => {
                                setOrganizationChartView('teams');
                            },
                        },
                    ]}
                    helpDialog={{
                        title: 'Hilfe zum Organigramm',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <Stack spacing={2}>
                                <Typography variant="body1" component="p">
                                    Das Organigramm zeigt Organisationseinheiten und Teams als zoombare, verschiebbare Übersicht.
                                </Typography>
                                <Typography variant="body1" component="p">
                                    Organisationseinheiten werden hierarchisch dargestellt. Mehrere oberste Organisationen erscheinen als getrennte, beschriftete Gruppen in derselben Ansicht.
                                </Typography>
                                <Typography variant="body1" component="p">
                                    Teams werden ohne Hierarchie in einer eigenen Ansicht aufgeführt.
                                </Typography>
                                <Typography variant="body1" component="p">
                                    Die Karten zeigen eine alphabetisch sortierte Vorschau der zugeordneten Mitarbeiter:innen. Weitere Mitglieder sind über die Mitgliederliste erreichbar. Gelöschte Nutzer:innen werden nicht angezeigt, deaktivierte Nutzer:innen werden als inaktiv markiert.
                                </Typography>
                            </Stack>
                        ),
                    }}
                />
                {
                    isLoading ? (
                        <OrganizationChartLoadingSkeleton />
                    ) : (
                        <OrganizationChartFlow
                            view={organizationChartView}
                            rootDepartments={rootDepartments}
                            teams={teams}
                            canReadUsers={canReadUsers}
                        />
                    )
                }
            </Box>
        </PageWrapper>
    );
}

function OrganizationChartLoadingSkeleton(): React.ReactElement {
    return (
        <Stack
            spacing={2}
            sx={{
                mt: 1.5,
                flex: 1,
                minHeight: 0,
            }}
        >
            <Box
                sx={{
                    ml: {xs: -2, md: -4},
                    mr: {xs: -2, md: -4},
                    flex: 1,
                    minHeight: 0,
                }}
            >
                <Skeleton variant="rounded" width="100%" height="100%" />
            </Box>
        </Stack>
    );
}
