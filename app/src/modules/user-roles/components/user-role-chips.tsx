import {Box, Chip, Typography} from '@mui/material';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';

interface UserRoleChipsProps {
    roles: {
        id: number | string;
        name: string;
    }[];
    maxVisibleChips?: number;
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
        .map(role => role.name)
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
                        key={role.id}
                        label={role.name}
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