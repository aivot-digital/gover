import {Box, Button, Divider, ListItem, ListItemAvatar, ListItemButton, ListItemText, Paper, Skeleton, Stack, Typography} from '@mui/material';
import {alpha, useTheme} from '@mui/material/styles';
import FamilyHistory from '@aivot/mui-material-symbols-400-outlined/dist/family-history/FamilyHistory';
import Groups from '@aivot/mui-material-symbols-400-outlined/dist/groups/Groups';
import OpenInNew from '@aivot/mui-material-symbols-400-outlined/dist/open-in-new/OpenInNew';
import {useEffect, useState} from 'react';
import {Link as RouterLink} from 'react-router-dom';
import {StringAvatar, stringToPastelColor} from '../../../components/avatar/string-avatar';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar} from '../../../slices/snackbar-slice';
import {VDepartmentShadowedEntity} from '../../departments/entities/v-department-shadowed-entity';
import {DepartmentMembershipApiService} from '../../departments/services/department-membership-api-service';
import {VDepartmentUserRoleAssignmentWithDetailsService} from '../../departments/services/v-department-user-role-assignment-with-details-service';
import {VDepartmentShadowedApiService} from '../../departments/services/v-department-shadowed-api-service';
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from '../../departments/utils/department-utils';
import {UserRoleChips} from '../../user-roles/components/user-role-chips';
import {UserRoleResponseDTO} from '../../user-roles/dtos/user-role-response-dto';
import {UserRolesApiService} from '../../user-roles/user-roles-api-service';
import {TeamsApiService} from '../../teams/services/teams-api-service';
import {TeamMembershipsApiService} from '../../teams/services/team-memberships-api-service';
import {VTeamUserRoleAssignmentWithDetailsApiService} from '../../teams/services/v-team-user-role-assignment-with-details-api-service';
import {UsersApiService} from '../../users/users-api-service';
import {User} from '../../users/models/user';
import {resolveUserName} from '../../users/utils/resolve-user-name';
import {TeamEntity} from '../../teams/entities/team-entity';

interface OrganigramDepartmentItem extends VDepartmentShadowedEntity {
    color: string;
    children: OrganigramDepartmentItem[];
    members: OrganigramUserItem[];
}

interface OrganigramTeamItem extends TeamEntity {
    color: string;
    members: OrganigramUserItem[];
}

interface OrganigramUserItem extends User {
    roles: UserRoleResponseDTO[];
}

const MAX_VISIBLE_MEMBERS_PER_CARD = 3;
const CARD_MIN_WIDTH = 400;
const CARD_MAX_WIDTH = 480;
const CARD_GRID_BASE_SX = {
    display: 'grid',
    gap: 2.5,
    alignItems: 'start',
};
const CARD_GRID_PACKED_SX = {
    ...CARD_GRID_BASE_SX,
    gridTemplateColumns: `repeat(auto-fill, minmax(min(100%, ${CARD_MIN_WIDTH}px), ${CARD_MAX_WIDTH}px))`,
    justifyContent: 'start',
};
const CARD_GRID_FLUID_SX = {
    ...CARD_GRID_BASE_SX,
    gridTemplateColumns: `repeat(auto-fit, minmax(min(100%, ${CARD_MIN_WIDTH}px), 1fr))`,
};
const ROOT_CARD_CONTAINER_SX = {
    border: 1,
    borderColor: 'divider',
    borderRadius: 3,
    p: 1.5,
    bgcolor: 'background.default',
};

function getCardGridSx(itemCount: number) {
    return itemCount >= 3 ? CARD_GRID_FLUID_SX : CARD_GRID_PACKED_SX;
}

function getCardItemSx(itemCount: number) {
    return {
        width: '100%',
        maxWidth: itemCount < 3 ? `${CARD_MAX_WIDTH}px` : undefined,
    };
}

function getRootCardItemSx(itemCount: number) {
    return {
        ...ROOT_CARD_CONTAINER_SX,
        ...getCardItemSx(itemCount),
    };
}

function scopedKey(scopeId: number, userId: string): string {
    return `${scopeId}:${userId}`;
}

function buildScopedRolesMap<T extends {userId: string; userRoleId?: number | null | undefined}>(
    assignments: T[],
    roleById: Map<number | null | undefined, UserRoleResponseDTO>,
    getScopeId: (assignment: T) => number | null | undefined
): Map<string, UserRoleResponseDTO[]> {
    const rolesByScopeUser = new Map<string, UserRoleResponseDTO[]>();

    for (const assignment of assignments) {
        const scopeId = getScopeId(assignment);
        if (scopeId == null) {
            continue;
        }
        const role = roleById.get(assignment.userRoleId);
        if (role == null) {
            continue;
        }
        const key = scopedKey(scopeId, assignment.userId);
        const existing = rolesByScopeUser.get(key) ?? [];
        existing.push(role);
        rolesByScopeUser.set(key, existing);
    }

    return rolesByScopeUser;
}

export function Organigram(): React.ReactElement {
    const dispatch = useAppDispatch();
    const [rootDepartments, setRootDepartments] = useState<OrganigramDepartmentItem[]>();
    const [teams, setTeams] = useState<OrganigramTeamItem[]>();

    useEffect(() => {
        let isActive = true;

        async function loadOrganigram(): Promise<void> {
            try {
                const [
                    departments,
                    departmentMemberships,
                    departmentUserRoleAssignments,
                    fetchedTeams,
                    teamMemberships,
                    teamUserRoleAssignments,
                    users,
                    userRoles,
                ] = await Promise.all([
                    new VDepartmentShadowedApiService().listAll(),
                    new DepartmentMembershipApiService().listAll(),
                    new VDepartmentUserRoleAssignmentWithDetailsService().listAll(),
                    new TeamsApiService().listAll(),
                    new TeamMembershipsApiService().listAll(),
                    new VTeamUserRoleAssignmentWithDetailsApiService().listAll(),
                    new UsersApiService().listAll({
                        deletedInIdp: false,
                        disabledInIdp: false,
                    }),
                    new UserRolesApiService().listAll(),
                ]);

                const usersById = new Map(users.content.map((user) => [user.id, user]));
                const roleById = new Map<number | null | undefined, UserRoleResponseDTO>(
                    userRoles.content.map((role) => [role.id, role])
                );

                const rolesByDepartmentUser = buildScopedRolesMap(
                    departmentUserRoleAssignments.content,
                    roleById,
                    (assignment) => assignment.departmentId
                );
                const rolesByTeamUser = buildScopedRolesMap(
                    teamUserRoleAssignments.content,
                    roleById,
                    (assignment) => assignment.teamId
                );

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
                    if (dept.parentDepartmentId && departmentMap[dept.parentDepartmentId]) {
                        departmentMap[dept.parentDepartmentId].children.push(departmentMap[dept.id]);
                    } else {
                        nextRootDepartments.push(departmentMap[dept.id]);
                    }
                }

                for (const membership of departmentMemberships.content) {
                    const dept = departmentMap[membership.departmentId];
                    const user = usersById.get(membership.userId);
                    if (dept != null && user != null) {
                        dept.members.push({
                            ...user,
                            roles: rolesByDepartmentUser.get(scopedKey(dept.id, user.id)) ?? [],
                        });
                    }
                }

                const nextTeams: OrganigramTeamItem[] = fetchedTeams.content
                    .map((team) => ({
                        ...team,
                        color: stringToPastelColor(team.name),
                        members: [],
                    }))
                    .sort((a, b) => a.name.localeCompare(b.name, 'de'));

                const teamsById = new Map(nextTeams.map((team) => [team.id, team]));
                for (const membership of teamMemberships.content) {
                    const team = teamsById.get(membership.teamId);
                    const user = usersById.get(membership.userId);
                    if (team != null && user != null) {
                        team.members.push({
                            ...user,
                            roles: rolesByTeamUser.get(scopedKey(team.id, user.id)) ?? [],
                        });
                    }
                }

                if (!isActive) {
                    return;
                }
                setRootDepartments(nextRootDepartments);
                setTeams(nextTeams);
            } catch (err) {
                if (!isActive) {
                    return;
                }
                dispatch(showApiErrorSnackbar(err, 'Fehler beim laden des Organigramms'));
            }
        }

        loadOrganigram().catch(console.error);
        return () => {
            isActive = false;
        };
    }, [dispatch]);

    const isLoading = rootDepartments == null || teams == null;

    return (
        <PageWrapper
            title="Organigramm"
            fullWidth
            background
        >
            <Box
                sx={{
                    px: {xs: 2, md: 4},
                }}
            >
                <GenericPageHeader
                    icon={<FamilyHistory />}
                    title="Organigramm"
                    helpDialog={{
                        title: 'Hilfe zum Organigramm',
                        tooltip: 'Hilfe anzeigen',
                        content: (
                            <Stack spacing={2}>
                                <Typography variant="body1" component="p">
                                    Das Organigramm dient der übersichtlichen Visualisierung Ihrer Organisationsstruktur.
                                </Typography>
                                <Typography variant="body1" component="p">
                                    Organisationseinheiten werden hierarchisch dargestellt. Teams werden separat aufgeführt, damit fachliche und organisatorische Strukturen schnell erkennbar bleiben.
                                </Typography>
                                <Typography variant="body1" component="p">
                                    Über die Aktionen in den Karten können Sie Mitglieder verwalten und direkt zu den zugehörigen Detailseiten navigieren.
                                </Typography>
                            </Stack>
                        ),
                    }}
                />
                {
                    isLoading ? (
                        <OrganigramLoadingSkeleton />
                    ) : (
                        <Stack spacing={5} sx={{mt: 2.5, pb: 7}}>
                            {
                                rootDepartments.length === 0 ? (
                                    <OrganigramSectionEmptyState message="Keine Organisationseinheiten vorhanden." />
                                ) : (
                                    <Box
                                        sx={getCardGridSx(rootDepartments.length)}
                                    >
                                        {
                                            rootDepartments.map((dept) => (
                                                <Box
                                                    key={dept.id}
                                                    sx={getRootCardItemSx(rootDepartments.length)}
                                                >
                                                    <DepartmentNode
                                                        department={dept}
                                                    />
                                                </Box>
                                            ))
                                        }
                                    </Box>
                                )
                            }

                            <Divider />

                            <Box>
                                <Typography variant="h6" sx={{mb: 2.5, fontWeight: 700}}>
                                    Teams
                                </Typography>
                                {
                                    teams.length === 0 ? (
                                        <OrganigramSectionEmptyState message="Keine Teams vorhanden." />
                                    ) : (
                                        <Box
                                            sx={getCardGridSx(teams.length)}
                                        >
                                            {
                                                teams.map((team) => (
                                                    <Box
                                                        key={team.id}
                                                        sx={getCardItemSx(teams.length)}
                                                    >
                                                        <TeamNode
                                                            team={team}
                                                        />
                                                    </Box>
                                                ))
                                            }
                                        </Box>
                                    )
                                }
                            </Box>
                        </Stack>
                    )
                }
            </Box>
        </PageWrapper>
    );
}

interface OrganigramSectionEmptyStateProps {
    message: string;
}

function OrganigramSectionEmptyState(props: OrganigramSectionEmptyStateProps): React.ReactElement {
    const {message} = props;

    return (
        <Paper
            variant="outlined"
            sx={{
                borderColor: 'divider',
                borderRadius: 2,
                px: 2,
                py: 2,
                bgcolor: 'background.paper',
            }}
        >
            <Typography
                variant="body2"
                color="text.secondary"
            >
                {message}
            </Typography>
        </Paper>
    );
}

function OrganigramLoadingSkeleton(): React.ReactElement {
    return (
        <Stack spacing={5} sx={{mt: 2.5, pb: 7}}>
            <Box sx={CARD_GRID_PACKED_SX}>
                {[0, 1].map((index) => (
                    <Box
                        key={index}
                        sx={getRootCardItemSx(2)}
                    >
                        <Skeleton variant="rounded" width="100%" height={248} />
                    </Box>
                ))}
            </Box>

            <Divider />

            <Box>
                <Skeleton variant="text" width={100} height={34} sx={{mb: 2.5}} />
                <Box sx={CARD_GRID_PACKED_SX}>
                    {[0, 1].map((index) => (
                        <Box
                            key={index}
                            sx={getCardItemSx(2)}
                        >
                            <Skeleton variant="rounded" width="100%" height={220} />
                        </Box>
                    ))}
                </Box>
            </Box>
        </Stack>
    );
}

interface DepartmentNodeProps {
    department: OrganigramDepartmentItem;
    depth?: number;
}

function DepartmentNode(props: DepartmentNodeProps): React.ReactElement {
    const {department, depth = 0} = props;
    const theme = useTheme();
    const connectorColor = theme.palette.mode === 'dark'
        ? theme.palette.grey[600]
        : theme.palette.grey[400];
    const iconCenterX = 35;
    const childrenIndent = 52;
    const iconCenterY = 33;
    const elbowSize = 14;
    const connectorY = iconCenterY + 5;
    const elbowTop = connectorY - elbowSize + 2;
    const elbowLeft = iconCenterX - childrenIndent;
    const horizontalExtensionLeft = elbowLeft + elbowSize - 1;
    const horizontalExtensionWidth = childrenIndent - iconCenterX - elbowSize + 3;

    return (
        <Box sx={{width: '100%'}}>
            <Paper
                variant="outlined"
                sx={{
                    px: 2,
                    py: 1.75,
                    borderRadius: 2,
                    borderColor: 'divider',
                    bgcolor: 'background.paper',
                    boxShadow: depth === 0 ? 1 : 0,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1.25,
                        pb: 1,
                        mb: 1.25,
                        borderBottom: `1px solid ${theme.palette.divider}`,
                    }}
                >
                    <StringAvatar
                        name={department.name}
                        backgroundMode={'oklch'}
                        showInitials={false}
                        sx={{
                            width: 38,
                            height: 38,
                            border: '1px solid',
                            borderColor: 'divider',
                            '& svg': {
                                fontSize: 22,
                            },
                        }}
                    >
                        {getDepartmentTypeIcons(department.depth)}
                    </StringAvatar>

                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            minWidth: 0,
                            flex: 1,
                        }}
                    >
                        <Typography
                            component={RouterLink}
                            to={`/departments/${department.id}`}
                            variant="subtitle1"
                            title={department.name}
                            sx={{
                                color: 'text.primary',
                                textDecoration: 'none',
                                fontWeight: 700,
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                                '&:hover': {
                                    textDecoration: 'underline',
                                },
                            }}
                        >
                            {department.name}
                        </Typography>
                        <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{
                                alignSelf: 'flex-start',
                                borderRadius: 1,
                                bgcolor: alpha(department.color, 0.25),
                                mt: -0.25,
                            }}
                        >
                            {getDepartmentTypeLabel(department.depth)}
                        </Typography>
                    </Box>

                    <Button
                        component={RouterLink}
                        to={`/departments/${department.id}/members`}
                        target="_blank"
                        rel="noopener noreferrer"
                        variant="outlined"
                        size="small"
                    >
                        Verwalten
                    </Button>
                </Box>

                {
                    department.members.length > 0 ? (
                        <MembersList
                            members={department.members}
                            managementLinkTo={`/departments/${department.id}/members`}
                        />
                    ) : (
                        <Typography
                            variant="body2"
                            color="text.secondary"
                            sx={{mt: 0.5}}
                        >
                            Keine Mitarbeiter:innen zugewiesen.
                        </Typography>
                    )
                }
            </Paper>

            {
                department.children.length > 0 &&
                <Box
                    sx={{
                        display: 'grid',
                        gap: 2,
                        mt: 0,
                        pt: 2,
                        pl: `${childrenIndent}px`,
                        ml: 0,
                        position: 'relative',
                        '&::before': {
                            content: '""',
                            position: 'absolute',
                            top: 0,
                            bottom: 0,
                            left: `${iconCenterX}px`,
                            width: 2,
                            bgcolor: connectorColor,
                            zIndex: 0,
                        },
                    }}
                >
                    {
                        department.children.map((childDept, index) => (
                            <Box
                                key={childDept.id}
                                sx={{
                                    position: 'relative',
                                    zIndex: 1,
                                    '&::before': {
                                        content: '""',
                                        position: 'absolute',
                                        left: `${elbowLeft}px`,
                                        top: `${elbowTop}px`,
                                        width: `${elbowSize}px`,
                                        height: `${elbowSize}px`,
                                        borderLeft: `2px solid ${connectorColor}`,
                                        borderBottom: `2px solid ${connectorColor}`,
                                        borderBottomLeftRadius: 10,
                                        zIndex: 2,
                                    },
                                    '&::after': {
                                        content: '""',
                                        position: 'absolute',
                                        left: `${horizontalExtensionLeft}px`,
                                        top: `${connectorY}px`,
                                        width: `${horizontalExtensionWidth}px`,
                                        height: 2,
                                        borderRadius: 999,
                                        bgcolor: connectorColor,
                                        zIndex: 2,
                                    },
                                }}
                            >
                                {
                                    index === department.children.length - 1 &&
                                    <Box
                                        sx={{
                                            position: 'absolute',
                                            left: `${iconCenterX - childrenIndent}px`,
                                            top: `${iconCenterY}px`,
                                            width: 2,
                                            bottom: -4,
                                            bgcolor: 'background.default',
                                            zIndex: 1,
                                        }}
                                    />
                                }
                                <DepartmentNode
                                    department={childDept}
                                    depth={depth + 1}
                                />
                            </Box>
                        ))
                    }
                </Box>
            }
        </Box>
    );
}

interface TeamNodeProps {
    team: OrganigramTeamItem;
}

function TeamNode(props: TeamNodeProps): React.ReactElement {
    const {team} = props;
    const theme = useTheme();

    return (
        <Paper
            variant="outlined"
            sx={{
                px: 2,
                py: 1.75,
                borderRadius: 2,
                borderColor: 'divider',
                bgcolor: 'background.paper',
                boxShadow: 1,
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.25,
                    pb: 1,
                    mb: 1.25,
                    borderBottom: `1px solid ${theme.palette.divider}`,
                }}
            >
                <StringAvatar
                    name={team.name}
                    backgroundMode={'oklch'}
                    showInitials={false}
                    sx={{
                        width: 38,
                        height: 38,
                        border: '1px solid',
                        borderColor: 'divider',
                        '& svg': {
                            fontSize: 22,
                        },
                    }}
                >
                    <Groups />
                </StringAvatar>
                <Box sx={{display: 'flex', flexDirection: 'column', minWidth: 0, flex: 1}}>
                    <Typography
                        component={RouterLink}
                        to={`/teams/${team.id}`}
                        variant="subtitle1"
                        title={team.name}
                        sx={{
                            color: 'text.primary',
                            textDecoration: 'none',
                            fontWeight: 700,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                            '&:hover': {
                                textDecoration: 'underline',
                            },
                        }}
                    >
                        {team.name}
                    </Typography>
                    <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{mt: -0.25}}
                    >
                        Team
                    </Typography>
                </Box>

                <Button
                    component={RouterLink}
                    to={`/teams/${team.id}/members`}
                    target="_blank"
                    rel="noopener noreferrer"
                    variant="outlined"
                    size="small"
                    sx={{ml: 'auto'}}
                >
                    Verwalten
                </Button>
            </Box>

            {
                team.members.length > 0 ? (
                    <MembersList
                        members={team.members}
                        managementLinkTo={`/teams/${team.id}/members`}
                    />
                ) : (
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{mt: 0.5}}
                    >
                        Keine Mitarbeiter:innen zugewiesen.
                    </Typography>
                )
            }
        </Paper>
    );
}

interface MembersListProps {
    members: OrganigramUserItem[];
    managementLinkTo: string;
}

function MembersList(props: MembersListProps): React.ReactElement {
    const {members, managementLinkTo} = props;
    const visibleMembers = members.slice(0, MAX_VISIBLE_MEMBERS_PER_CARD);
    const hiddenCount = members.length - visibleMembers.length;

    return (
        <Stack
            spacing={1}
            sx={{
                mt: 0.25,
            }}
        >
            {
                visibleMembers.map((member) => {
                    const memberName = resolveUserName(member);

                    return (
                    <Box
                        key={member.id}
                        sx={{
                            p: 0.25,
                        }}
                    >
                        <ListItem
                            disablePadding
                            sx={{display: 'block'}}
                        >
                            <ListItemButton
                                component={RouterLink}
                                to={`/users/${member.id}`}
                                sx={{
                                    alignItems: 'center',
                                    borderRadius: 1.5,
                                    border: (theme) => `1px solid ${theme.palette.action.hover}`,
                                    bgcolor: 'action.hover',
                                    px: 0.75,
                                    py: 0.5,
                                    '&:hover': {
                                        bgcolor: 'action.selected',
                                        borderColor: 'action.selected',
                                    },
                                }}
                            >
                                <ListItemAvatar sx={{minWidth: 36}}>
                                    <StringAvatar
                                        name={memberName}
                                        sx={{width: 28, height: 28, fontSize: 12}}
                                        backgroundMode={'oklch'}
                                        showInitials={true}
                                    />
                                </ListItemAvatar>
                                <ListItemText
                                    sx={{my: 0}}
                                    primary={(
                                        <Typography
                                            component="span"
                                            title={memberName}
                                            noWrap
                                            sx={{
                                                display: 'block',
                                                fontWeight: 600,
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                            }}
                                        >
                                            {memberName}
                                        </Typography>
                                    )}
                                    secondary={(
                                        member.roles.length > 0 ? (
                                            <Box sx={{mt: 0.75}}>
                                                <UserRoleChips
                                                    roles={member.roles.map((role) => ({
                                                        id: role.id,
                                                        name: role.name ?? '',
                                                    }))}
                                                />
                                            </Box>
                                        ) : null
                                    )}
                                />
                            </ListItemButton>
                        </ListItem>

                    </Box>
                );
                })
            }
            {
                hiddenCount > 0 &&
                <Button
                    component={RouterLink}
                    to={managementLinkTo}
                    target="_blank"
                    rel="noopener noreferrer"
                    size="small"
                    variant="text"
                    endIcon={
                        <OpenInNew
                            sx={{
                                fontSize: '1rem!important',
                                color: 'text.disabled',
                            }}
                        />
                    }
                    sx={{alignSelf: 'flex-start', mt: 0.25}}
                >
                    {`und ${hiddenCount} weitere`}
                </Button>
            }
        </Stack>
    );
}
