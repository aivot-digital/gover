import HelpOutlineIcon from '@aivot/mui-material-symbols-400-n25-outlined/Help';
import {Box, type SxProps, type Theme} from '@mui/material';
import React from 'react';
import {Chip} from '../../../components/chip/chip';
import {HintTooltip} from '../../../components/hint-tooltip/hint-tooltip';

const MOST_PRIVILEGED_SYSTEM_ROLE_HINT = 'Diese Rolle ist in den Systemeinstellungen als Systemrolle mit der höchsten Berechtigungsstufe festgelegt.';

interface MostPrivilegedSystemRoleBadgeProps {
    showHintIcon?: boolean;
    sx?: SxProps<Theme>;
}

export function isMostPrivilegedSystemRole(
    roleId: number | string | undefined,
    mostPrivilegedSystemRoleId: string | undefined,
): boolean {
    return roleId != null &&
        mostPrivilegedSystemRoleId != null &&
        mostPrivilegedSystemRoleId.trim().length > 0 &&
        roleId.toString() === mostPrivilegedSystemRoleId.trim();
}

export function MostPrivilegedSystemRoleBadge(props: MostPrivilegedSystemRoleBadgeProps) {
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
                Höchste Berechtigungsstufe
                <HelpOutlineIcon sx={{fontSize: 16}}/>
            </Box>
        )
        : 'Höchste Berechtigungsstufe';

    return (
        <HintTooltip
            arrow
            placement="top"
            title={MOST_PRIVILEGED_SYSTEM_ROLE_HINT}
        >
            <Box
                component="span"
                sx={props.sx}
            >
                <Chip
                    component="span"
                    label={label}
                    color="warning"
                    mode="soft"
                    size="small"
                />
            </Box>
        </HintTooltip>
    );
}
