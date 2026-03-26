import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {ProcessEntity} from '../../entities/process-entity';
import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {PermissionEntry} from '../../../permissions/models/permission-provider';
import {ProcessAccessControlEntity} from '../../entities/process-access-control-entity';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {ProcessAccessControlApiService} from '../../services/process-access-control-api-service';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {
    Autocomplete,
    Box, Button, Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Typography,
} from '@mui/material';
import {getDepartmentPath, getDepartmentTypeIcons} from '../../../departments/utils/department-utils';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import {TeamEntity} from '../../../teams/entities/team-entity';
import {ModuleIcons} from '../../../../shells/staff/data/module-icons';
import {Actions} from '../../../../components/actions/actions';
import {useConfirm} from '../../../../providers/confirm-provider';
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';

interface ProcessSettingsDialogTabProps {
    process: ProcessEntity;
    departments: VDepartmentShadowedEntity[];
    teams: TeamEntity[];
}

interface ProcessAccessControlEntityWithDepartmentOrTeam extends ProcessAccessControlEntity {
    department?: VDepartmentShadowedEntity;
    team?: TeamEntity;
}

interface AddDomainOption {
    label: string;
    value: number;
    subLabel?: string;
    disabled?: boolean;
    icon: ReactNode;
    type: 'department' | 'team';
}

const relevantPermissions: string[] = [
    'process_definition.read',
    'process_definition.update',
    'process_definition.audit',
    'process_definition.publish.test',
    'process_definition.publish.local',
    'process_definition.publish.store',
];

export function ProcessSettingsDialogProcessAccessTab(props: ProcessSettingsDialogTabProps) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();

    const {
        process,
        departments,
        teams,
    } = props;

    const {
        id: processesId,
    } = process;

    const [permissions, setPermissions] = useState<PermissionEntry[]>([]);
    const [access, setAccess] = useState<ProcessAccessControlEntity[]>([]);
    const [targetDomainOption, setTargetDomainOption] = useState<AddDomainOption | null>(null);

    useEffect(() => {
        new PermissionApiService()
            .listPermissions()
            .then((permissionProviders) => {
                const processPermissions = permissionProviders
                    .find((p) => p.contextLabel === 'Prozesse');
                if (processPermissions) {
                    setPermissions(processPermissions.permissions.filter(p => relevantPermissions.includes(p.permission)));
                } else {
                    setPermissions([]);
                }
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für Prozesse'));
            });
    }, []);

    useEffect(() => {
        new ProcessAccessControlApiService()
            .listAll({
                targetProcessId: processesId,
            })
            .then(({content}) => {
                setAccess(content);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für diesen Prozess'));
            });
    }, [processesId]);

    const resolvedAccessControl: ProcessAccessControlEntityWithDepartmentOrTeam[] = useMemo(() => {
        return access
            .map((accessControl) => {
                return {
                    ...accessControl,
                    department: accessControl.sourceDepartmentId != null ? departments.find((d) => d.id === accessControl.sourceDepartmentId) : undefined,
                    team: accessControl.sourceTeamId != null ? teams.find((d) => d.id === accessControl.sourceTeamId) : undefined,
                };
            });
    }, [access, departments, teams]);

    const owningDepartment = useMemo(() => {
        return departments.find((d) => d.id === process.departmentId)!;
    }, [departments, process.departmentId]);

    const addDomainOptions: AddDomainOption[] = useMemo(() => {
        return [
            ...departments.map((department) => ({
                label: department.name,
                value: department.id,
                subLabel: getDepartmentPath(department),
                icon: getDepartmentTypeIcons(department.depth),
                type: 'department',
            } as AddDomainOption)),
            ...teams.map((team) => ({
                label: team.name,
                value: team.id,
                icon: ModuleIcons.teams,
                type: 'team',
            } as AddDomainOption)),
        ];
    }, [departments, teams, access]);

    const handleAddAccess = () => {
        if (targetDomainOption == null) {
            return;
        }

        new ProcessAccessControlApiService()
            .create({
                ...ProcessAccessControlApiService.initialize(),
                sourceDepartmentId: targetDomainOption.type === 'department' ? targetDomainOption.value : null,
                sourceTeamId: targetDomainOption.type === 'team' ? targetDomainOption.value : null,
                targetProcessId: processesId,
                permissions: [],
            })
            .then((created) => {
                setAccess((prev) => [...prev, created]);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Hinzufügen der Berechtigungen für diesen Prozess'));
            });

        setTargetDomainOption(null);
    };

    const togglePermissionForAccess = (access: ProcessAccessControlEntity, permission: string) => {
        const updatedPermissions = [
            ...access.permissions,
        ];
        if (updatedPermissions.includes(permission)) {
            const index = updatedPermissions.indexOf(permission);
            updatedPermissions.splice(index, 1);
        } else {
            updatedPermissions.push(permission);
        }

        const updatedAccess = {
            ...access,
            permissions: updatedPermissions,
        }

        setAccess((prev) => prev.map((a) => a.id === updatedAccess.id ? updatedAccess : a));

        new ProcessAccessControlApiService()
            .update(access.id, updatedAccess)
            .then((updated) => {
                setAccess((prev) => prev.map((a) => a.id === updated.id ? updated : a));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Aktualisieren der Berechtigungen für diesen Prozess'));
            });
    };

    const getAccessLabel = (access: ProcessAccessControlEntityWithDepartmentOrTeam) => {
        if (access.team != null) {
            return access.team.name;
        }

        if (access.department != null) {
            return getDepartmentPath(access.department);
        }

        return 'diese Domäne';
    };

    const handleDeleteAccess = async (access: ProcessAccessControlEntityWithDepartmentOrTeam) => {
        if (access.sourceDepartmentId === process.departmentId) {
            return;
        }

        const confirmed = await confirm({
            title: 'Berechtigung entfernen',
            confirmButtonText: 'Entfernen',
            children: (
                <Typography>
                    Möchten Sie die Berechtigung für <strong>{getAccessLabel(access)}</strong> wirklich entfernen?
                </Typography>
            ),
        });

        if (!confirmed) {
            return;
        }

        new ProcessAccessControlApiService()
            .destroy(access.id)
            .then(() => {
                setAccess((prev) => prev.filter((a) => a.id !== access.id));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Entfernen der Berechtigungen für diesen Prozess'));
            });
    };

    return (
        <>
            <TableContainer>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell
                                sx={{
                                    fontSize: '90%',
                                }}
                            >
                                Organisationseinheit / Team
                            </TableCell>
                            {
                                permissions
                                    .map((permission) => (
                                        <TableCell
                                            key={permission.permission}
                                            sx={{
                                                fontSize: '90%',
                                            }}
                                        >
                                            {permission.label}
                                        </TableCell>
                                    ))
                            }
                            <TableCell width={1}/>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        <TableRow>
                            <TableCell>
                                {getDepartmentPath(owningDepartment)} <br/>
                                <em>(Verwaltende Organisationseinheit)</em>
                            </TableCell>
                            {
                                permissions
                                    .map((permission) => (
                                        <TableCell
                                            key={permission.permission}
                                        >
                                            <CheckboxFieldComponent
                                                label=""
                                                value={true}
                                                onChange={() => {
                                                }}
                                                variant="switch"
                                                disabled={true}
                                            />
                                        </TableCell>
                                    ))
                            }
                            <TableCell/>
                        </TableRow>
                        {
                            resolvedAccessControl
                                .map((access) => (
                                    <TableRow key={access.id}>
                                        <TableCell>
                                            {
                                                access.team != null &&
                                                access.team.name
                                            }
                                            {
                                                access.department != null &&
                                                getDepartmentPath(access.department)
                                            }
                                        </TableCell>
                                        {
                                            permissions
                                                .map((permission) => (
                                                    <TableCell
                                                        key={permission.permission}
                                                    >
                                                        <CheckboxFieldComponent
                                                            label=""
                                                            value={access.permissions.includes(permission.permission)}
                                                            onChange={() => {
                                                                togglePermissionForAccess(access, permission.permission);
                                                            }}
                                                            variant="switch"
                                                        />
                                                    </TableCell>
                                                ))
                                        }
                                        <TableCell align="right">
                                            <Actions
                                                actions={[
                                                    {
                                                        icon: <Delete/>,
                                                        tooltip: 'Berechtigung entfernen',
                                                        disabled: access.sourceDepartmentId === process.departmentId,
                                                        disabledTooltip: 'Die verwaltende Organisationseinheit kann nicht entfernt werden',
                                                        ariaLabel: 'Berechtigung entfernen',
                                                        onClick: () => {
                                                            void handleDeleteAccess(access);
                                                        },
                                                    },
                                                ]}
                                                color="error"
                                            />
                                        </TableCell>
                                    </TableRow>
                                ))
                        }
                    </TableBody>
                </Table>
            </TableContainer>

            <Stack
                direction="row"
                alignItems="center"
                spacing={2}
                sx={{
                    mt: 2,
                }}
            >
                <Autocomplete<AddDomainOption, false, false, false>
                    options={addDomainOptions}
                    value={targetDomainOption}
                    onChange={(_, value) => {
                        setTargetDomainOption(value);
                    }}
                    fullWidth={true}
                    getOptionLabel={(option) => option.label}
                    isOptionEqualToValue={(option, value) => option.value === value.value}
                    getOptionDisabled={(option) => option.disabled ?? false}
                    noOptionsText="Keine gültigen Ziel-Domäne verfügbar"
                    renderOption={(props, option) => (
                        <Box
                            component="li"
                            {...props}
                            sx={{
                                display: 'flex',
                                alignItems: 'flex-start',
                                py: 0.5,
                                minHeight: 40,
                            }}
                        >
                            {
                                option.icon != null &&
                                <Box
                                    sx={{
                                        mr: 1,
                                        display: 'flex',
                                        alignItems: 'center',
                                    }}
                                >
                                    {option.icon}
                                </Box>
                            }
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
                                    sx={{
                                        lineHeight: 1.2,
                                    }}
                                >
                                    {option.label}
                                </Typography>
                                {
                                    option.subLabel != null &&
                                    <Typography
                                        variant="caption"
                                        color="textSecondary"
                                        sx={{
                                            lineHeight: 1.2,
                                        }}
                                    >
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

                <Actions actions={[
                    {
                        label: 'Hinzufügen',
                        onClick: handleAddAccess,
                        icon: <Add/>,
                    },
                ]}/>
            </Stack>
        </>
    );
}
