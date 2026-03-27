import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {ProcessEntity} from '../../entities/process-entity';
import React, {ReactNode, useEffect, useMemo, useState} from 'react';
import {PermissionEntry} from '../../../permissions/models/permission-provider';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {showApiErrorSnackbar} from '../../../../slices/snackbar-slice';
import {
    Autocomplete,
    Box,
    Stack,
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
import {ProcessInstanceAccessControlPresetEntity} from '../../entities/process-instance-access-control-preset-entity';
import {ProcessInstanceAccessControlPresetApiService} from '../../services/process-instance-access-control-preset-api-service';
import {ProcessVersionEntity} from '../../entities/process-version-entity';

interface ProcessSettingsDialogTabProps {
    process: ProcessEntity;
    version: ProcessVersionEntity;
    departments: VDepartmentShadowedEntity[];
    teams: TeamEntity[];
}

interface ProcessInstanceAccessControlPresetEntityWithDepartmentOrTeam extends ProcessInstanceAccessControlPresetEntity {
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
    'process_instance.trigger',
    'process_instance.read',
    'process_instance.update',
    'process_instance.delete',
    'process_instance.pause_resume',
    'process_instance.edit_data',
    'process_instance.reassign',
    'process_instance.communication.internal',
    'process_instance.communication.external',
    'process_instance.edit_task',
    'process_instance.migrate',
];

export function ProcessSettingsDialogProcessInstanceAccessPresetTab(props: ProcessSettingsDialogTabProps) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();

    const {
        process,
        version,
        departments,
        teams,
    } = props;

    const {
        id: processesId,
    } = process;


    const {
        processVersion: processVersion,
    } = version;

    const [permissions, setPermissions] = useState<PermissionEntry[]>([]);
    const [accessPresets, setAccessPresets] = useState<ProcessInstanceAccessControlPresetEntity[]>([]);
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
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für neue Vorgänge'));
            });
    }, []);

    useEffect(() => {
        new ProcessInstanceAccessControlPresetApiService()
            .listAll({
                targetProcessId: processesId,
                targetProcessVersion: processVersion,
            })
            .then(({content}) => {
                setAccessPresets(content);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für neue Vorgänge'));
            });
    }, [processesId, processVersion]);

    const resolvedAccessControl: ProcessInstanceAccessControlPresetEntityWithDepartmentOrTeam[] = useMemo(() => {
        return accessPresets
            .map((accessPreset) => {
                return {
                    ...accessPreset,
                    department: accessPreset.sourceDepartmentId != null ? departments.find((d) => d.id === accessPreset.sourceDepartmentId) : undefined,
                    team: accessPreset.sourceTeamId != null ? teams.find((d) => d.id === accessPreset.sourceTeamId) : undefined,
                };
            });
    }, [accessPresets, departments, teams]);

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
    }, [departments, teams, accessPresets]);

    const handleAddAccessPreset = () => {
        if (targetDomainOption == null) {
            return;
        }

        new ProcessInstanceAccessControlPresetApiService()
            .create({
                ...ProcessInstanceAccessControlPresetApiService.initialize(),
                sourceDepartmentId: targetDomainOption.type === 'department' ? targetDomainOption.value : null,
                sourceTeamId: targetDomainOption.type === 'team' ? targetDomainOption.value : null,
                targetProcessId: processesId,
                targetProcessVersion: processVersion,
                permissions: [],
            })
            .then((created) => {
                setAccessPresets((prev) => [...prev, created]);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Hinzufügen der Berechtigungen für neue Vorgänge'));
            });

        setTargetDomainOption(null);
    };

    const togglePermissionForAccessPreset = (accessPreset: ProcessInstanceAccessControlPresetEntity, permission: string) => {
        const updatedPermissions = [
            ...accessPreset.permissions,
        ];
        if (updatedPermissions.includes(permission)) {
            const index = updatedPermissions.indexOf(permission);
            updatedPermissions.splice(index, 1);
        } else {
            updatedPermissions.push(permission);
        }

        const updatedAccessPreset = {
            ...accessPreset,
            permissions: updatedPermissions,
        };

        setAccessPresets((prev) => prev.map((a) => a.id === updatedAccessPreset.id ? updatedAccessPreset : a));

        new ProcessInstanceAccessControlPresetApiService()
            .update(accessPreset.id, updatedAccessPreset)
            .then((updated) => {
                setAccessPresets((prev) => prev.map((a) => a.id === updated.id ? updated : a));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Aktualisieren der Berechtigungen für neue Vorgänge'));
            });
    };

    const getAccessLabel = (accessPreset: ProcessInstanceAccessControlPresetEntityWithDepartmentOrTeam) => {
        if (accessPreset.team != null) {
            return accessPreset.team.name;
        }

        if (accessPreset.department != null) {
            return getDepartmentPath(accessPreset.department);
        }

        return 'diese Domäne';
    };

    const handleDeleteAccessPreset = async (accessPreset: ProcessInstanceAccessControlPresetEntityWithDepartmentOrTeam) => {
        if (accessPreset.sourceDepartmentId === process.departmentId) {
            return;
        }

        const confirmed = await confirm({
            title: 'Berechtigung entfernen',
            confirmButtonText: 'Entfernen',
            children: (
                <Typography>
                    Möchten Sie die Berechtigung für <strong>{getAccessLabel(accessPreset)}</strong> wirklich entfernen?
                </Typography>
            ),
        });

        if (!confirmed) {
            return;
        }

        new ProcessInstanceAccessControlPresetApiService()
            .destroy(accessPreset.id)
            .then(() => {
                setAccessPresets((prev) => prev.filter((a) => a.id !== accessPreset.id));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Entfernen der Berechtigungen für neue Vorgänge'));
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
                                .map((accessPreset) => (
                                    <TableRow key={accessPreset.id}>
                                        <TableCell>
                                            {
                                                accessPreset.team != null &&
                                                accessPreset.team.name
                                            }
                                            {
                                                accessPreset.department != null &&
                                                getDepartmentPath(accessPreset.department)
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
                                                            value={accessPreset.permissions.includes(permission.permission)}
                                                            onChange={() => {
                                                                togglePermissionForAccessPreset(accessPreset, permission.permission);
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
                                                        disabled: accessPreset.sourceDepartmentId === process.departmentId,
                                                        disabledTooltip: 'Die verwaltende Organisationseinheit kann nicht entfernt werden',
                                                        ariaLabel: 'Berechtigung entfernen',
                                                        onClick: () => {
                                                            void handleDeleteAccessPreset(accessPreset);
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
                        onClick: handleAddAccessPreset,
                        icon: <Add/>,
                    },
                ]}/>
            </Stack>
        </>
    );
}
