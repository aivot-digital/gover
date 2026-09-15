import React, {type ReactNode, useMemo, useState} from 'react';
import {
    Autocomplete,
    Box,
    Button,
    Checkbox,
    Chip,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Deselect from '@aivot/mui-material-symbols-400-n25-outlined/Deselect';
import SelectAll from '@aivot/mui-material-symbols-400-n25-outlined/SelectAll';
import {Actions} from '../../../../components/actions/actions';
import {type PermissionEntry} from '../../../permissions/models/permission-provider';
import {type VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {getDepartmentPath, getDepartmentTypeIcons} from '../../../departments/utils/department-utils';
import {type TeamEntity} from '../../../teams/entities/team-entity';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';

export interface ProcessSettingsAccessControlDraftBase {
    clientId: string;
    sourceDepartmentId: number | null;
    sourceTeamId: number | null;
    permissions: string[];
}

export interface ProcessSettingsAccessControlAddDomainOption {
    label: string;
    value: number;
    subLabel?: string;
    disabled?: boolean;
    icon: ReactNode;
    type: 'department' | 'team';
}

interface ProcessSettingsDialogAccessControlMatrixProps<AccessControl extends ProcessSettingsAccessControlDraftBase> {
    permissions: PermissionEntry[];
    accessControls: AccessControl[];
    owningDepartmentId: number;
    departments: VDepartmentShadowedEntity[];
    teams: TeamEntity[];
    isBusy?: boolean;
    onAccessControlsChange: (nextAccessControls: AccessControl[]) => void;
    onAddAccessControl: (domainOption: ProcessSettingsAccessControlAddDomainOption) => void;
    onDeleteAccessControl: (accessControl: AccessControl) => void;
}

interface DomainDisplayData {
    label: string;
    subLabel: string;
    icon: ReactNode;
}

const DOMAIN_COLUMN_WIDTH = 340;
const PERMISSION_COLUMN_WIDTH = 136;

function getPermissionKeys(permissions: PermissionEntry[]): string[] {
    return permissions.map((permission) => permission.permission);
}

function isOwningDepartmentAccess(
    accessControl: Pick<ProcessSettingsAccessControlDraftBase, 'sourceDepartmentId'>,
    owningDepartmentId: number,
): boolean {
    return accessControl.sourceDepartmentId === owningDepartmentId;
}

function getAccessControlDomainKey(accessControl: Pick<ProcessSettingsAccessControlDraftBase, 'sourceDepartmentId' | 'sourceTeamId'>): string {
    if (accessControl.sourceDepartmentId != null) {
        return `department-${accessControl.sourceDepartmentId}`;
    }

    if (accessControl.sourceTeamId != null) {
        return `team-${accessControl.sourceTeamId}`;
    }

    return 'unknown';
}

function createPermissionSet(accessControl: ProcessSettingsAccessControlDraftBase, permissionKeys: string[]): Set<string> {
    const matrixPermissionSet = new Set(permissionKeys);

    return new Set(accessControl.permissions.filter((permission) => matrixPermissionSet.has(permission)));
}

function mergeMatrixPermissions(
    currentPermissions: string[],
    permissionKeys: string[],
    nextSelectedPermissionKeys: ReadonlySet<string>,
): string[] {
    // Preserve permissions that are not rendered by this matrix; another UI may own them.
    const matrixPermissionSet = new Set(permissionKeys);
    const preservedPermissions = currentPermissions.filter((permission) => !matrixPermissionSet.has(permission));
    const nextMatrixPermissions = permissionKeys.filter((permission) => nextSelectedPermissionKeys.has(permission));

    return [
        ...preservedPermissions,
        ...nextMatrixPermissions,
    ];
}

function getDomainDisplayData(
    accessControl: Pick<ProcessSettingsAccessControlDraftBase, 'sourceDepartmentId' | 'sourceTeamId'>,
    departments: VDepartmentShadowedEntity[],
    teams: TeamEntity[],
): DomainDisplayData {
    if (accessControl.sourceTeamId != null) {
        const team = teams.find((entry) => entry.id === accessControl.sourceTeamId);

        return {
            label: team?.name ?? `Team #${accessControl.sourceTeamId}`,
            subLabel: 'Team',
            icon: ModuleIcons.teams,
        };
    }

    if (accessControl.sourceDepartmentId != null) {
        const department = departments.find((entry) => entry.id === accessControl.sourceDepartmentId);

        return {
            label: department?.name ?? `Organisationseinheit #${accessControl.sourceDepartmentId}`,
            subLabel: department != null ? getDepartmentPath(department) : 'Organisationseinheit',
            icon: department != null ? getDepartmentTypeIcons(department.depth) : ModuleIcons.departments,
        };
    }

    return {
        label: 'Unbekannte Domäne',
        subLabel: 'Organisationseinheit oder Team',
        icon: ModuleIcons.organization,
    };
}

function formatPermissionTooltip(permission: PermissionEntry): string {
    const description = permission.description?.trim();

    if (description != null && description.length > 0) {
        return `${permission.label}\n${description}\nPermission Key: ${permission.permission}`;
    }

    return `${permission.label}\nPermission Key: ${permission.permission}`;
}

export function ProcessSettingsDialogAccessControlMatrix<AccessControl extends ProcessSettingsAccessControlDraftBase>(
    props: ProcessSettingsDialogAccessControlMatrixProps<AccessControl>,
) {
    const {
        permissions,
        accessControls,
        owningDepartmentId,
        departments,
        teams,
        isBusy = false,
        onAccessControlsChange,
        onAddAccessControl,
        onDeleteAccessControl,
    } = props;

    const [targetDomainOption, setTargetDomainOption] = useState<ProcessSettingsAccessControlAddDomainOption | null>(null);
    const [showPermissionKeys, setShowPermissionKeys] = useState(false);

    const permissionKeys = useMemo(() => getPermissionKeys(permissions), [permissions]);
    // The owning department is shown for context, but its permissions are fixed by the process ownership.
    const editableAccessControls = useMemo(
        () => accessControls.filter((accessControl) => !isOwningDepartmentAccess(accessControl, owningDepartmentId)),
        [accessControls, owningDepartmentId],
    );

    const addDomainOptions = useMemo(() => {
        const assignedDomainKeys = new Set(accessControls.map((accessControl) => getAccessControlDomainKey(accessControl)));

        return [
            ...departments.map((department) => ({
                label: department.name,
                value: department.id,
                subLabel: getDepartmentPath(department),
                icon: getDepartmentTypeIcons(department.depth),
                type: 'department',
                disabled: department.id === owningDepartmentId || assignedDomainKeys.has(`department-${department.id}`),
            } as ProcessSettingsAccessControlAddDomainOption)),
            ...teams.map((team) => ({
                label: team.name,
                value: team.id,
                icon: ModuleIcons.teams,
                type: 'team',
                disabled: assignedDomainKeys.has(`team-${team.id}`),
            } as ProcessSettingsAccessControlAddDomainOption)),
        ];
    }, [accessControls, departments, owningDepartmentId, teams]);

    const selectedEditablePermissionCount = useMemo(() => {
        const matrixPermissionSet = new Set(permissionKeys);

        return editableAccessControls.reduce((count, accessControl) => {
            return count + accessControl.permissions.filter((permission) => matrixPermissionSet.has(permission)).length;
        }, 0);
    }, [editableAccessControls, permissionKeys]);

    const editablePermissionCount = editableAccessControls.length * permissionKeys.length;
    const areAllEditablePermissionsSelected = editablePermissionCount > 0 && selectedEditablePermissionCount === editablePermissionCount;
    const hasSelectedEditablePermissions = selectedEditablePermissionCount > 0;

    const owningDepartmentDisplayData = useMemo(() => getDomainDisplayData(
        {
            sourceDepartmentId: owningDepartmentId,
            sourceTeamId: null,
        },
        departments,
        teams,
    ), [departments, owningDepartmentId, teams]);

    const updateAccessPermissions = (accessControl: AccessControl, nextSelectedPermissionKeys: ReadonlySet<string>) => {
        if (isBusy) {
            return;
        }

        onAccessControlsChange(accessControls.map((currentAccessControl) => {
            if (currentAccessControl.clientId !== accessControl.clientId) {
                return currentAccessControl;
            }

            return {
                ...currentAccessControl,
                permissions: mergeMatrixPermissions(currentAccessControl.permissions, permissionKeys, nextSelectedPermissionKeys),
            };
        }));
    };

    const handleTogglePermission = (accessControl: AccessControl, permission: string, checked: boolean) => {
        const nextPermissionSet = createPermissionSet(accessControl, permissionKeys);

        if (checked) {
            nextPermissionSet.add(permission);
        } else {
            nextPermissionSet.delete(permission);
        }

        updateAccessPermissions(accessControl, nextPermissionSet);
    };

    const handleSetRowPermissions = (accessControl: AccessControl, checked: boolean) => {
        updateAccessPermissions(accessControl, new Set(checked ? permissionKeys : []));
    };

    const handleSetPermissionForAllRows = (permission: string, checked: boolean) => {
        if (isBusy) {
            return;
        }

        onAccessControlsChange(accessControls.map((accessControl) => {
            if (isOwningDepartmentAccess(accessControl, owningDepartmentId)) {
                return accessControl;
            }

            const nextPermissionSet = createPermissionSet(accessControl, permissionKeys);

            if (checked) {
                nextPermissionSet.add(permission);
            } else {
                nextPermissionSet.delete(permission);
            }

            return {
                ...accessControl,
                permissions: mergeMatrixPermissions(accessControl.permissions, permissionKeys, nextPermissionSet),
            };
        }));
    };

    const handleSetAllRowsPermissions = (checked: boolean) => {
        if (isBusy) {
            return;
        }

        const nextPermissionSet = new Set(checked ? permissionKeys : []);

        onAccessControlsChange(accessControls.map((accessControl) => {
            if (isOwningDepartmentAccess(accessControl, owningDepartmentId)) {
                return accessControl;
            }

            return {
                ...accessControl,
                permissions: mergeMatrixPermissions(accessControl.permissions, permissionKeys, nextPermissionSet),
            };
        }));
    };

    const renderDomainCell = (
        displayData: DomainDisplayData,
        protectedLabel?: string,
        actions?: ReactNode,
    ) => (
        <Stack
            direction="row"
            spacing={1.25}
            sx={{
                alignItems: "center",
                minWidth: 0
            }}>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    color: 'text.secondary',
                    flexShrink: 0,
                    '& svg': {
                        fontSize: 22,
                    },
                }}
            >
                {displayData.icon}
            </Box>
            <Box
                sx={{
                    minWidth: 0,
                    flex: 1,
                }}
            >
                <Typography
                    variant="body2"
                    sx={{
                        fontWeight: 500,
                        lineHeight: 1.35,
                    }}
                    noWrap
                    title={displayData.label}
                >
                    {displayData.label}
                </Typography>
                <Typography
                    variant="caption"
                    noWrap
                    title={displayData.subLabel}
                    sx={{
                        color: "text.secondary",
                        display: 'block',
                        lineHeight: 1.35
                    }}>
                    {displayData.subLabel}
                </Typography>
                {
                    protectedLabel != null &&
                    <Chip
                        label={protectedLabel}
                        size="small"
                        variant="outlined"
                        sx={{
                            mt: 0.75,
                            height: 24,
                            maxWidth: '100%',
                        }}
                    />
                }
            </Box>
            {
                actions != null &&
                <Box
                    sx={{
                        ml: 0.5,
                        flexShrink: 0,
                    }}
                >
                    {actions}
                </Box>
            }
        </Stack>
    );

    const renderPermissionHeaderCell = (permission: PermissionEntry) => {
        const selectedCount = editableAccessControls.filter((accessControl) => accessControl.permissions.includes(permission.permission)).length;
        const checked = editableAccessControls.length > 0 && selectedCount === editableAccessControls.length;
        const indeterminate = selectedCount > 0 && selectedCount < editableAccessControls.length;

        return (
            <TableCell
                key={permission.permission}
                align="center"
                sx={{
                    minWidth: PERMISSION_COLUMN_WIDTH,
                    verticalAlign: 'top',
                    borderLeft: (theme) => `1px solid ${theme.palette.divider}`,
                }}
            >
                <Stack
                    spacing={0.75}
                    sx={{
                        alignItems: "center"
                    }}
                >
                    <Tooltip
                        title={formatPermissionTooltip(permission)}
                        placement="top"
                        arrow
                    >
                        <Typography
                            variant="caption"
                            sx={{
                                fontWeight: 600,
                                lineHeight: 1.3,
                                minHeight: 38,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                            }}
                        >
                            {permission.label}
                        </Typography>
                    </Tooltip>
                    {
                        showPermissionKeys &&
                        <Typography
                            component="code"
                            variant="caption"
                            sx={{
                                color: "text.secondary",
                                display: 'block',
                                fontFamily: 'monospace',
                                fontSize: 11,
                                lineHeight: 1.35,
                                maxWidth: PERMISSION_COLUMN_WIDTH - 16,
                                overflowWrap: 'anywhere'
                            }}>
                            {permission.permission}
                        </Typography>
                    }
                    <Tooltip
                        title="Für alle Domänen umschalten"
                        arrow
                    >
                        <span>
                            <Checkbox
                                size="small"
                                checked={checked}
                                indeterminate={indeterminate}
                                disabled={isBusy || editableAccessControls.length === 0}
                                onChange={(event) => {
                                    handleSetPermissionForAllRows(permission.permission, event.target.checked);
                                }}
                                sx={{p: 0.5}}
                                slotProps={{
                                    input: {
                                        'aria-label': `Berechtigung ${permission.label} für alle Domänen umschalten`,
                                    }
                                }}
                            />
                        </span>
                    </Tooltip>
                </Stack>
            </TableCell>
        );
    };

    const renderEditableAccessControlRow = (accessControl: AccessControl) => {
        const displayData = getDomainDisplayData(accessControl, departments, teams);
        const selectedPermissionSet = createPermissionSet(accessControl, permissionKeys);
        const areAllRowPermissionsSelected = permissionKeys.length > 0 && selectedPermissionSet.size === permissionKeys.length;
        const hasSelectedRowPermissions = selectedPermissionSet.size > 0;
        const rowActions = (
            <Actions
                isBusy={isBusy}
                dense
                sx={{
                    height: 'auto',
                }}
                actions={[
                    {
                        icon: <SelectAll fontSize="small"/>,
                        tooltip: 'Alle Rechte für diese Domäne auswählen',
                        ariaLabel: 'Alle Rechte für diese Domäne auswählen',
                        disabled: areAllRowPermissionsSelected || permissionKeys.length === 0,
                        onClick: () => {
                            handleSetRowPermissions(accessControl, true);
                        },
                    },
                    {
                        icon: <Deselect fontSize="small"/>,
                        tooltip: 'Alle Rechte für diese Domäne abwählen',
                        ariaLabel: 'Alle Rechte für diese Domäne abwählen',
                        disabled: !hasSelectedRowPermissions,
                        onClick: () => {
                            handleSetRowPermissions(accessControl, false);
                        },
                    },
                    {
                        icon: <Delete fontSize="small"/>,
                        tooltip: 'Berechtigung entfernen',
                        ariaLabel: 'Berechtigung entfernen',
                        color: 'error',
                        onClick: () => {
                            onDeleteAccessControl(accessControl);
                        },
                    },
                ]}
                color="primary"
            />
        );

        return (
            <TableRow
                key={accessControl.clientId}
                hover
            >
                <TableCell
                    sx={{
                        position: 'sticky',
                        left: 0,
                        zIndex: 1,
                        bgcolor: 'background.paper',
                        minWidth: DOMAIN_COLUMN_WIDTH,
                        width: DOMAIN_COLUMN_WIDTH,
                        borderRight: (theme) => `1px solid ${theme.palette.divider}`,
                    }}
                >
                    {renderDomainCell(displayData, undefined, rowActions)}
                </TableCell>
                {
                    permissions.map((permission) => (
                        <TableCell
                            key={permission.permission}
                            align="center"
                            sx={{
                                borderLeft: (theme) => `1px solid ${theme.palette.divider}`,
                            }}
                        >
                            <Checkbox
                                size="small"
                                checked={accessControl.permissions.includes(permission.permission)}
                                disabled={isBusy}
                                onChange={(event) => {
                                    handleTogglePermission(accessControl, permission.permission, event.target.checked);
                                }}
                                sx={{p: 0.5}}
                                slotProps={{
                                    input: {
                                        'aria-label': `${permission.label} für ${displayData.label}`,
                                    }
                                }}
                            />
                        </TableCell>
                    ))
                }
            </TableRow>
        );
    };

    return (
        <Stack spacing={2}>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'flex-end',
                }}
            >
                <Actions
                    isBusy={isBusy}
                    dense
                    actions={[
                        {
                            label: 'Alle auswählen',
                            icon: <SelectAll fontSize="small"/>,
                            iconPosition: 'start',
                            onClick: () => {
                                handleSetAllRowsPermissions(true);
                            },
                            disabled: areAllEditablePermissionsSelected || editablePermissionCount === 0,
                        },
                        {
                            label: 'Alle abwählen',
                            icon: <Deselect fontSize="small"/>,
                            iconPosition: 'start',
                            onClick: () => {
                                handleSetAllRowsPermissions(false);
                            },
                            disabled: !hasSelectedEditablePermissions,
                        },
                        'separator',
                        {
                            label: showPermissionKeys ? 'Schlüssel ausblenden' : 'Schlüssel anzeigen',
                            variant: showPermissionKeys ? 'outlined' : 'text',
                            onClick: () => {
                                setShowPermissionKeys((current) => !current);
                            },
                        },
                    ]}
                    size="small"
                />
            </Box>

            <TableContainer
                sx={{
                    border: (theme) => `1px solid ${theme.palette.divider}`,
                    borderRadius: 1,
                    maxHeight: {
                        xs: 420,
                        sm: 'calc(100vh - 360px)',
                    },
                    maxWidth: '100%',
                    overflow: 'auto',
                }}
            >
                <Table
                    size="small"
                    stickyHeader
                    sx={{
                        minWidth: DOMAIN_COLUMN_WIDTH + permissions.length * PERMISSION_COLUMN_WIDTH,
                        '& th, & td': {
                            px: 1.25,
                            py: 1.1,
                        },
                        '& tbody tr': {
                            height: 56,
                        },
                    }}
                >
                    <TableHead>
                        <TableRow>
                            <TableCell
                                sx={{
                                    position: 'sticky',
                                    top: 0,
                                    left: 0,
                                    zIndex: 4,
                                    bgcolor: 'background.paper',
                                    minWidth: DOMAIN_COLUMN_WIDTH,
                                    width: DOMAIN_COLUMN_WIDTH,
                                    borderRight: (theme) => `1px solid ${theme.palette.divider}`,
                                }}
                            >
                                <Typography
                                    variant="caption"
                                    sx={{fontWeight: 600}}
                                >
                                    Organisationseinheit / Team
                                </Typography>
                            </TableCell>
                            {permissions.map(renderPermissionHeaderCell)}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        <TableRow hover>
                            <TableCell
                                sx={{
                                    position: 'sticky',
                                    left: 0,
                                    zIndex: 1,
                                    bgcolor: 'background.paper',
                                    minWidth: DOMAIN_COLUMN_WIDTH,
                                    width: DOMAIN_COLUMN_WIDTH,
                                    borderRight: (theme) => `1px solid ${theme.palette.divider}`,
                                }}
                            >
                                {renderDomainCell(owningDepartmentDisplayData, 'verwaltende Organisationseinheit')}
                            </TableCell>
                            {
                                permissions.map((permission) => (
                                    <TableCell
                                        key={permission.permission}
                                        align="center"
                                        sx={{
                                            borderLeft: (theme) => `1px solid ${theme.palette.divider}`,
                                        }}
                                    >
                                        <Checkbox
                                            size="small"
                                            checked
                                            disabled
                                            sx={{p: 0.5}}
                                            slotProps={{
                                                input: {
                                                    'aria-label': `${permission.label} für verwaltende Organisationseinheit`,
                                                }
                                            }}
                                        />
                                    </TableCell>
                                ))
                            }
                        </TableRow>

                        {editableAccessControls.map(renderEditableAccessControlRow)}
                    </TableBody>
                </Table>
            </TableContainer>

            <Stack
                direction={{
                    xs: 'column',
                    sm: 'row',
                }}
                spacing={1.5}
            >
                <Autocomplete<ProcessSettingsAccessControlAddDomainOption, false, false, false>
                    options={addDomainOptions}
                    value={targetDomainOption}
                    onChange={(_, value) => {
                        setTargetDomainOption(value);
                    }}
                    fullWidth
                    size="small"
                    disabled={isBusy}
                    groupBy={(option) => option.type === 'department' ? 'Organisationseinheiten' : 'Teams'}
                    getOptionLabel={(option) => option.label}
                    isOptionEqualToValue={(option, value) => option.type === value.type && option.value === value.value}
                    getOptionDisabled={(option) => option.disabled ?? false}
                    noOptionsText="Keine verfügbare Domäne"
                    renderOption={({key, ...props}, option) => (
                        <Box
                            key={key}
                            component="li"
                            {...props}
                            sx={{
                                display: 'flex',
                                alignItems: 'flex-start',
                                py: 0.5,
                                minHeight: 40,
                            }}
                        >
                            <Box
                                sx={{
                                    mr: 1,
                                    display: 'flex',
                                    alignItems: 'center',
                                    color: 'text.secondary',
                                    '& svg': {
                                        fontSize: 20,
                                    },
                                }}
                            >
                                {option.icon}
                            </Box>
                            <Box
                                sx={{
                                    minWidth: 0,
                                    display: 'flex',
                                    flexDirection: 'column',
                                    gap: 0.125,
                                    flex: 1,
                                }}
                            >
                                <Typography
                                    variant="body2"
                                    sx={{lineHeight: 1.2}}
                                >
                                    {option.label}
                                </Typography>
                                {
                                    option.subLabel != null &&
                                    <Typography
                                        variant="caption"
                                        sx={{
                                            color: "text.secondary",
                                            lineHeight: 1.2
                                        }}>
                                        {option.subLabel}
                                    </Typography>
                                }
                            </Box>
                        </Box>
                    )}
                    renderInput={(params) => (
                        <TextField
                            {...params}
                            label="Neue, berechtigte Domäne"
                            placeholder="Domäne suchen…"
                        />
                    )}
                />

                <Box>
                    <Button
                        variant="outlined"
                        startIcon={<Add/>}
                        disabled={targetDomainOption == null || isBusy}
                        onClick={() => {
                            if (targetDomainOption == null) {
                                return;
                            }

                            onAddAccessControl(targetDomainOption);
                            setTargetDomainOption(null);
                        }}
                        sx={{
                            height: 40,
                            mt: 2,
                            whiteSpace: 'nowrap',
                        }}
                    >
                        Hinzufügen
                    </Button>
                </Box>
            </Stack>
        </Stack>
    );
}
