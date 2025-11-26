import {UserRoleResponseDTO} from '../dtos/user-role-response-dto';
import {OrgUserRoleAssignmentResponseDTO} from '../dtos/org-user-role-assignment-response-dto';
import {Box, Chip, Typography} from '@mui/material';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';
import {TeamUserRoleAssignmentResponseDTO} from '../dtos/team-user-role-assignment-response-dto';
import {VDepartmentUserRoleAssignmentWithDetailsEntity} from '../../departments/entities/v-department-user-role-assignment-with-details-entity';

interface UserRoleChipsProps {
    roles: UserRoleResponseDTO[] | OrgUserRoleAssignmentResponseDTO[] | TeamUserRoleAssignmentResponseDTO[] | VDepartmentUserRoleAssignmentWithDetailsEntity[];
    maxVisibleChips?: number;
}

function isUserRoleResponseDTO(role: UserRoleResponseDTO | OrgUserRoleAssignmentResponseDTO | TeamUserRoleAssignmentResponseDTO | VDepartmentUserRoleAssignmentWithDetailsEntity): role is UserRoleResponseDTO {
    return (role as UserRoleResponseDTO).name !== undefined;
}

export function UserRoleChips(props: UserRoleChipsProps) {
    const {
        roles,
        maxVisibleChips = 3,
    } = props;

    const visibleRoles = roles.slice(0, maxVisibleChips);
    const extraRolesCount = roles.length - maxVisibleChips;
    const hasExtraRoles = extraRolesCount > 0;
    const extraRoles = roles.slice(maxVisibleChips);
    const extraRolesNames = extraRoles
        .map(role => (isUserRoleResponseDTO(role) ? role.name : role.userRoleName) ?? 'Unbenannt')
        .join(', ');

    return (
        <Box>
            {
                visibleRoles.length === 0 &&
                <em>Keine Rolle</em>
            }
            {
                visibleRoles.length > 0 &&
                visibleRoles.map((role, index) => (
                    <Chip
                        key={(isUserRoleResponseDTO(role) ? role.id : role.userRoleName)}
                        label={(isUserRoleResponseDTO(role) ? role.name : role.userRoleName) ?? 'Unbenannt'}
                        variant="outlined"
                        size="small"
                        sx={{
                            mr: index < visibleRoles.length - 1 ? 0.5 : 0,
                        }}
                    />
                ))
            }
            {
                hasExtraRoles &&
                <HintTooltip
                    title={extraRolesNames}
                >
                    <Typography
                        variant="caption"
                        color="textSecondary"
                        sx={{
                            ml: 0.5,
                        }}
                    >
                        +{extraRolesCount} weitere
                    </Typography>
                </HintTooltip>
            }
        </Box>
    );
}