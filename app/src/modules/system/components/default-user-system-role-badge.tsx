import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import {Box, type SxProps, type Theme} from '@mui/material';
import React from 'react';
import {Chip} from '../../../components/chip/chip';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';

const DEFAULT_USER_SYSTEM_ROLE_HINT = 'Diese Systemrolle ist die Standardrolle für neue Benutzer:innen, wenn keine andere Rolle angegeben wurde. Sie kann in den Allgemeinen Systemeinstellungen geändert werden.';

interface DefaultUserSystemRoleBadgeProps {
    showHintIcon?: boolean;
    sx?: SxProps<Theme>;
}

export function isDefaultUserSystemRole(roleId: number | string | undefined, defaultSystemRoleId: string | undefined): boolean {
    return roleId != null &&
        defaultSystemRoleId != null &&
        defaultSystemRoleId.trim().length > 0 &&
        roleId.toString() === defaultSystemRoleId.trim();
}

export function DefaultUserSystemRoleBadge(props: DefaultUserSystemRoleBadgeProps) {
    const label = props.showHintIcon
        ? (
            <Box
                component="span"
                sx={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 0.5,
                }}
            >
                Standard-Systemrolle
                <HelpOutlineIcon sx={{fontSize: 16}} />
            </Box>
        )
        : 'Standard-Systemrolle';

    return (
        <HintTooltip
            arrow
            placement="top"
            title={DEFAULT_USER_SYSTEM_ROLE_HINT}
        >
            <Box
                component="span"
                sx={props.sx}
            >
                <Chip
                    component="span"
                    label={label}
                    color="info"
                    mode="soft"
                    size="small"
                />
            </Box>
        </HintTooltip>
    );
}
