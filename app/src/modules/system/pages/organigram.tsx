import {VDepartmentShadowedEntity} from "../../departments/entities/v-department-shadowed-entity";
import {User} from "../../users/models/user";
import {TeamEntity} from "../../teams/entities/team-entity";
import {useEffect, useState} from "react";
import {VDepartmentShadowedApiService} from "../../departments/services/v-department-shadowed-api-service";
import {DepartmentMembershipApiService} from "../../departments/services/department-membership-api-service";
import {TeamMembershipsApiService} from "../../teams/services/team-memberships-api-service";
import {
    VDepartmentUserRoleAssignmentWithDetailsService
} from "../../departments/services/v-department-user-role-assignment-with-details-service";
import {
    VTeamUserRoleAssignmentWithDetailsApiService
} from "../../teams/services/v-team-user-role-assignment-with-details-api-service";
import {useAppDispatch} from "../../../hooks/use-app-dispatch";
import {showApiErrorSnackbar} from "../../../slices/snackbar-slice";
import {UsersApiService} from "../../users/users-api-service";
import {UserRolesApiService} from "../../user-roles/user-roles-api-service";
import {Avatar, Box, Container, Paper, Skeleton, Typography} from "@mui/material";
import {UserRoleResponseDTO} from "../../user-roles/dtos/user-role-response-dto";
import {UserRoleChips} from "../../user-roles/components/user-role-chips";
import {resolveUserName} from "../../users/utils/resolve-user-name";
import {PageWrapper} from "../../../components/page-wrapper/page-wrapper";
import {GenericPageHeader} from "../../../components/generic-page-header/generic-page-header";
import FamilyHistory from "@aivot/mui-material-symbols-400-outlined/dist/family-history/FamilyHistory";
import {getDepartmentTypeIcons, getDepartmentTypeLabel} from "../../departments/utils/department-utils";
import {Link} from "react-router-dom";

interface OrganigramDepartmentItem extends VDepartmentShadowedEntity {
    _color: string;
    _children: OrganigramDepartmentItem[];
    _members: OrganigramUserItem[];
}

interface OrganigramTeamItem extends TeamEntity {
    _color: string;
    _members: OrganigramUserItem[];
}

interface OrganigramUserItem extends User {
    _roles: UserRoleResponseDTO[];
}

export function Organigram() {
    const dispatch = useAppDispatch();

    const [rootDepartments, setRootDepartments] = useState<OrganigramDepartmentItem[]>();
    const [teams, setTeams] = useState<OrganigramTeamItem[]>();

    useEffect(() => {
        Promise
            .all([
                new VDepartmentShadowedApiService()
                    .listAll(),
                new DepartmentMembershipApiService()
                    .listAll(),
                new TeamMembershipsApiService()
                    .listAll(),
                new VDepartmentUserRoleAssignmentWithDetailsService()
                    .listAll(),
                new VTeamUserRoleAssignmentWithDetailsApiService()
                    .listAll(),
                new UsersApiService()
                    .listAll({
                        deletedInIdp: false,
                        disabledInIdp: false,
                    }),
                new UserRolesApiService()
                    .listAll(),
            ])
            .then(results => {
                const [
                    departments,
                    departmentMemberships,
                    teamMemberships,
                    departmentUserRoleAssignments,
                    teamUserRoleAssignments,
                    users,
                    userRoles,
                ] = results;

                return {
                    departments: departments.content,
                    departmentMemberships: departmentMemberships.content,
                    teamMemberships: teamMemberships.content,
                    departmentUserRoleAssignments: departmentUserRoleAssignments.content,
                    teamUserRoleAssignments: teamUserRoleAssignments.content,
                    users: users.content,
                    userRoles: userRoles.content,
                };
            })
            .then(results => {
                const {
                    departments,
                    departmentMemberships,
                    teamMemberships,
                    departmentUserRoleAssignments,
                    teamUserRoleAssignments,
                    users,
                    userRoles,
                } = results;

                // Build department tree
                const departmentMap: Record<number, OrganigramDepartmentItem> = {};
                for (const dept of departments) {
                    departmentMap[dept.id] = {
                        ...dept,
                        _color: stringToColour(dept.name),
                        _children: [],
                        _members: [],
                    };
                }
                const rootDepartments: OrganigramDepartmentItem[] = [];
                for (const dept of departments) {
                    if (dept.parentDepartmentId && departmentMap[dept.parentDepartmentId]) {
                        departmentMap[dept.parentDepartmentId]._children.push(departmentMap[dept.id]);
                    } else {
                        rootDepartments.push(departmentMap[dept.id]);
                    }
                }

                // Assign members to departments
                for (const membership of departmentMemberships) {
                    const dept = departmentMap[membership.departmentId];
                    const user = users.find(u => u.id === membership.userId);
                    if (dept && user) {
                        const organigramUser: OrganigramUserItem = {
                            ...user,
                            _roles: departmentUserRoleAssignments
                                .filter(ra => ra.userId === user.id && ra.departmentId === dept.id)
                                .map(ra => userRoles.find(ur => ur.id === ra.userRoleId)!)
                                .filter(ur => ur != null),
                        };
                        dept._members.push(organigramUser);
                    }
                }


                setRootDepartments(rootDepartments);
            })
            .catch(err => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim laden des Organigramms'));
            });
    }, []);

    return (
        <PageWrapper
            title="Organigramm"
            fullWidth
            background
        >
            <Box
                sx={{
                    px: 4,
                }}
            >
                <GenericPageHeader
                    icon={<FamilyHistory />}
                    title="Organigramm"
                />
                {
                    rootDepartments == null ? (
                        <Skeleton variant="rectangular"
                                  width="100%"
                                  height={400} />
                    ) : (
                        <Box
                            sx={{
                                display: 'flex',
                                gap: 2,
                                mt: 2,
                                pb: 8,
                                overflowY: 'auto',
                            }}
                        >
                            {
                                rootDepartments.map(dept => (
                                    <DepartmentNode key={dept.id}
                                                    department={dept} />
                                ))
                            }
                        </Box>
                    )
                }
            </Box>
        </PageWrapper>
    );

}

interface DepartmentNodeProps {
    department: OrganigramDepartmentItem;
}

function DepartmentNode(props: DepartmentNodeProps) {
    return (
        <Box>
            <Paper
                sx={{
                    px: 2,
                    py: 1,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                        minWidth: 192,
                    }}
                >
                    <Avatar
                        sx={{
                            bgcolor: props.department._color,
                        }}
                    >
                        {getDepartmentTypeIcons(props.department.depth)}
                    </Avatar>

                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                        }}
                    >
                        <Typography
                            component={Link}
                            to={`/departments/${props.department.id}`}
                        >
                            {props.department.name}
                        </Typography>
                        <Typography variant="caption">
                            {getDepartmentTypeLabel(props.department.depth)}
                        </Typography>
                    </Box>
                </Box>

                {
                    props.department._members.length > 0 &&
                    <ul>
                        {
                            props.department._members.map(member => (
                                <li>
                                    <Link
                                        to={`/users/${member.id}`}
                                    >
                                    {resolveUserName(member)}
                                        </Link>

                                    {
                                        member._roles.length > 0 &&
                                        <UserRoleChips
                                            roles={member._roles.map((role) => ({
                                                id: role.id,
                                                name: role.name ?? '',
                                            }))}
                                        />
                                    }
                                </li>
                            ))
                        }
                    </ul>
                }
            </Paper>

            {
                props.department._children.length > 0 &&
                <Box
                    sx={{
                        display: 'flex',
                        gap: 2,
                        mt: 2,
                        ml: 4,
                    }}
                >
                  {
                      props.department._children.map(childDept => (
                          <DepartmentNode key={childDept.id}
                                          department={childDept} />
                      ))
                  }
              </Box>
            }
      </Box>
    );
}

function stringToColour(str: string) {
    let hash = 0;
    str.split('').forEach(char => {
        hash = char.charCodeAt(0) + ((hash << 5) - hash)
    })
    let colour = '#'
    for (let i = 0; i < 3; i++) {
        const value = (hash >> (i * 8)) & 0x88
        colour += value.toString(16).padStart(2, '0')
    }
    return colour
}