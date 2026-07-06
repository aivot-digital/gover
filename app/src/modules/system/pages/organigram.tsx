import {Box, Skeleton, Stack, Typography} from '@mui/material';
import FamilyHistory from '@aivot/mui-material-symbols-400-outlined/dist/family-history/FamilyHistory';
import Groups from '@aivot/mui-material-symbols-400-outlined/dist/groups/Groups';
import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {stringToPastelColor} from '../../../components/avatar/string-avatar';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {DepartmentMembershipApiService} from '../../departments/services/department-membership-api-service';
import {VDepartmentShadowedApiService} from '../../departments/services/v-department-shadowed-api-service';
import {TeamsApiService} from '../../teams/services/teams-api-service';
import {TeamMembershipsApiService} from '../../teams/services/team-memberships-api-service';
import {UsersApiService} from '../../users/users-api-service';
import {OrganigramFlow, type OrganigramFlowView} from './organigram/organigram-flow';
import {getDepartmentTypeIcons} from '../../departments/utils/department-utils';
import {createSimulatedOrganigramData} from './organigram/organigram-simulation';
import {
    type OrganigramDepartmentItem,
    type OrganigramTeamItem,
} from './organigram/organigram-types';
import {
    compareNamedItems,
    compareOrganigramUsers,
    sortOrganigramDepartmentTrees,
} from './organigram/organigram-utils';

const SIMULATE_QUERY_PARAM = 'simulate';

export function Organigram(): React.ReactElement {
    const dispatch = useAppDispatch();
    const [searchParams] = useSearchParams();
    const [rootDepartments, setRootDepartments] = useState<OrganigramDepartmentItem[]>();
    const [teams, setTeams] = useState<OrganigramTeamItem[]>();
    const [organigramView, setOrganigramView] = useState<OrganigramFlowView>('departments');
    const shouldSimulate = searchParams.get(SIMULATE_QUERY_PARAM) === '1';

    useEffect(() => {
        let isActive = true;

        async function loadOrganigram(): Promise<void> {
            if (shouldSimulate) {
                const simulatedOrganigram = createSimulatedOrganigramData();
                setRootDepartments(simulatedOrganigram.rootDepartments);
                setTeams(simulatedOrganigram.teams);
                return;
            }

            setRootDepartments(undefined);
            setTeams(undefined);

            try {
                const [
                    departments,
                    departmentMemberships,
                    fetchedTeams,
                    teamMemberships,
                    users,
                ] = await Promise.all([
                    new VDepartmentShadowedApiService().listAll(),
                    new DepartmentMembershipApiService().listAll(),
                    new TeamsApiService().listAll(),
                    new TeamMembershipsApiService().listAll(),
                    new UsersApiService().listAll({
                        deletedInIdp: false,
                    }),
                ]);

                const usersById = new Map(users.content.map((user) => [user.id, user]));

                const departmentMap: Record<number, OrganigramDepartmentItem> = {};
                for (const dept of departments.content) {
                    departmentMap[dept.id] = {
                        ...dept,
                        color: stringToPastelColor(dept.name),
                        children: [],
                        members: [],
                    };
                }

                const nextRootDepartments: OrganigramDepartmentItem[] = [];
                for (const dept of departments.content) {
                    if (dept.parentDepartmentId != null && departmentMap[dept.parentDepartmentId] != null) {
                        departmentMap[dept.parentDepartmentId].children.push(departmentMap[dept.id]);
                    } else {
                        nextRootDepartments.push(departmentMap[dept.id]);
                    }
                }

                for (const membership of departmentMemberships.content) {
                    const dept = departmentMap[membership.departmentId];
                    const user = usersById.get(membership.userId);
                    if (dept != null && user != null) {
                        dept.members.push(user);
                    }
                }

                const nextTeams: OrganigramTeamItem[] = fetchedTeams.content
                    .map((team) => ({
                        ...team,
                        color: stringToPastelColor(team.name),
                        members: [],
                    }));

                const teamsById = new Map(nextTeams.map((team) => [team.id, team]));
                for (const membership of teamMemberships.content) {
                    const team = teamsById.get(membership.teamId);
                    const user = usersById.get(membership.userId);
                    if (team != null && user != null) {
                        team.members.push(user);
                    }
                }

                sortOrganigramDepartmentTrees(nextRootDepartments);
                nextTeams.sort(compareNamedItems);
                nextTeams.forEach((team) => {
                    team.members.sort(compareOrganigramUsers);
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

        loadOrganigram().catch(console.error);
        return () => {
            isActive = false;
        };
    }, [dispatch, shouldSimulate]);

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
                            color: organigramView === 'departments' ? 'primary' : 'inherit',
                            activeStyle: {
                                borderBottom: 2,
                                borderColor: organigramView === 'departments' ? 'primary.main' : 'transparent',
                                borderRadius: 0,
                            },
                            onClick: () => {
                                setOrganigramView('departments');
                            },
                        },
                        {
                            label: 'Teams',
                            icon: <Groups />,
                            iconPosition: 'start',
                            variant: 'text',
                            color: organigramView === 'teams' ? 'primary' : 'inherit',
                            activeStyle: {
                                borderBottom: 2,
                                borderColor: organigramView === 'teams' ? 'primary.main' : 'transparent',
                                borderRadius: 0,
                            },
                            onClick: () => {
                                setOrganigramView('teams');
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
                        <OrganigramLoadingSkeleton />
                    ) : (
                        <OrganigramFlow
                            view={organigramView}
                            rootDepartments={rootDepartments}
                            teams={teams}
                        />
                    )
                }
            </Box>
        </PageWrapper>
    );
}

function OrganigramLoadingSkeleton(): React.ReactElement {
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
