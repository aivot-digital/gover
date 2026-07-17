import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {ProcessEntity} from '../../entities/process-entity';
import React, {ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {PermissionEntry} from '../../../permissions/models/permission-provider';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
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
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {ProcessInstanceAccessControlPresetEntity} from '../../entities/process-instance-access-control-preset-entity';
import {ProcessInstanceAccessControlPresetApiService} from '../../services/process-instance-access-control-preset-api-service';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {deepEquals} from '../../../../utils/equality-utils';
import {ElementEditorSectionHeader} from '../../../../components/element-editor-section-header/element-editor-section-header';

interface ProcessSettingsDialogTabProps {
    open: boolean;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    departments: VDepartmentShadowedEntity[];
    teams: TeamEntity[];
    onUnsavedChangesChange?: (hasUnsavedChanges: boolean) => void;
}

interface ProcessInstanceAccessControlPresetDraft extends ProcessInstanceAccessControlPresetEntity {
    clientId: string;
}

interface ProcessInstanceAccessControlPresetDraftWithDepartmentOrTeam extends ProcessInstanceAccessControlPresetDraft {
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

function createComparableAccessControlPreset(accessPreset: ProcessInstanceAccessControlPresetEntity | ProcessInstanceAccessControlPresetDraft) {
    return {
        id: accessPreset.id,
        sourceDepartmentId: accessPreset.sourceDepartmentId,
        sourceTeamId: accessPreset.sourceTeamId,
        targetProcessId: accessPreset.targetProcessId,
        targetProcessVersion: accessPreset.targetProcessVersion,
        permissions: [...accessPreset.permissions].sort(),
    };
}

function getAccessPresetDomainKey(accessPreset: Pick<ProcessInstanceAccessControlPresetEntity, 'sourceDepartmentId' | 'sourceTeamId'>): string {
    if (accessPreset.sourceDepartmentId != null) {
        return `department-${accessPreset.sourceDepartmentId}`;
    }

    if (accessPreset.sourceTeamId != null) {
        return `team-${accessPreset.sourceTeamId}`;
    }

    return 'unknown';
}

export function ProcessSettingsDialogProcessInstanceAccessPresetTab(props: ProcessSettingsDialogTabProps) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();

    const {
        open,
        process,
        version,
        departments,
        teams,
        onUnsavedChangesChange,
    } = props;

    const {
        id: processesId,
    } = process;


    const {
        processVersion: processVersion,
    } = version;

    const [permissions, setPermissions] = useState<PermissionEntry[]>([]);
    const [persistedAccessPresets, setPersistedAccessPresets] = useState<ProcessInstanceAccessControlPresetEntity[]>([]);
    const [draftAccessPresets, setDraftAccessPresets] = useState<ProcessInstanceAccessControlPresetDraft[]>([]);
    const [targetDomainOption, setTargetDomainOption] = useState<AddDomainOption | null>(null);
    const [isSaving, setIsSaving] = useState(false);
    const nextClientIdRef = useRef(0);

    const createDraftAccessPreset = useCallback((accessPreset: ProcessInstanceAccessControlPresetEntity): ProcessInstanceAccessControlPresetDraft => {
        return {
            ...accessPreset,
            clientId: `server-${accessPreset.id}`,
        };
    }, []);

    const createNewClientId = useCallback(() => {
        nextClientIdRef.current += 1;
        return `new-${nextClientIdRef.current}`;
    }, []);

    const toAccessPresetEntity = useCallback((accessPreset: ProcessInstanceAccessControlPresetDraft): ProcessInstanceAccessControlPresetEntity => {
        return {
            id: accessPreset.id,
            sourceDepartmentId: accessPreset.sourceDepartmentId,
            sourceTeamId: accessPreset.sourceTeamId,
            targetProcessId: accessPreset.targetProcessId,
            targetProcessVersion: accessPreset.targetProcessVersion,
            permissions: accessPreset.permissions,
            created: accessPreset.created,
            updated: accessPreset.updated,
        };
    }, []);

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
    }, [dispatch]);

    const loadAccessPresets = useCallback(() => {
        new ProcessInstanceAccessControlPresetApiService()
            .listAll({
                targetProcessId: processesId,
                targetProcessVersion: processVersion,
            })
            .then(({content}) => {
                nextClientIdRef.current = 0;
                setPersistedAccessPresets(content);
                setDraftAccessPresets(content.map(createDraftAccessPreset));
                setTargetDomainOption(null);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für neue Vorgänge'));
            });
    }, [createDraftAccessPreset, dispatch, processVersion, processesId]);

    useEffect(() => {
        if (!open) {
            return;
        }

        loadAccessPresets();
    }, [loadAccessPresets, open]);

    const hasUnsavedChanges = useMemo(() => {
        const comparablePersistedAccessPresets = persistedAccessPresets
            .map((accessPreset) => createComparableAccessControlPreset(accessPreset))
            .sort((left, right) => getAccessPresetDomainKey(left).localeCompare(getAccessPresetDomainKey(right)));
        const comparableDraftAccessPresets = draftAccessPresets
            .map((accessPreset) => createComparableAccessControlPreset(accessPreset))
            .sort((left, right) => getAccessPresetDomainKey(left).localeCompare(getAccessPresetDomainKey(right)));

        return !deepEquals(comparablePersistedAccessPresets, comparableDraftAccessPresets);
    }, [draftAccessPresets, persistedAccessPresets]);

    useEffect(() => {
        onUnsavedChangesChange?.(hasUnsavedChanges);
    }, [hasUnsavedChanges, onUnsavedChangesChange]);

    useEffect(() => {
        return () => {
            onUnsavedChangesChange?.(false);
        };
    }, [onUnsavedChangesChange]);

    const resolvedAccessControl: ProcessInstanceAccessControlPresetDraftWithDepartmentOrTeam[] = useMemo(() => {
        return draftAccessPresets
            .map((accessPreset) => {
                return {
                    ...accessPreset,
                    department: accessPreset.sourceDepartmentId != null ? departments.find((d) => d.id === accessPreset.sourceDepartmentId) : undefined,
                    team: accessPreset.sourceTeamId != null ? teams.find((d) => d.id === accessPreset.sourceTeamId) : undefined,
                };
            });
    }, [departments, draftAccessPresets, teams]);

    const owningDepartment = useMemo(() => {
        return departments.find((d) => d.id === process.departmentId)!;
    }, [departments, process.departmentId]);

    const addDomainOptions: AddDomainOption[] = useMemo(() => {
        const assignedDomainKeys = new Set(draftAccessPresets.map((accessPreset) => getAccessPresetDomainKey(accessPreset)));

        return [
            ...departments.map((department) => ({
                label: department.name,
                value: department.id,
                subLabel: getDepartmentPath(department),
                icon: getDepartmentTypeIcons(department.depth),
                type: 'department',
                disabled: department.id === process.departmentId || assignedDomainKeys.has(`department-${department.id}`),
            } as AddDomainOption)),
            ...teams.map((team) => ({
                label: team.name,
                value: team.id,
                icon: ModuleIcons.teams,
                type: 'team',
                disabled: assignedDomainKeys.has(`team-${team.id}`),
            } as AddDomainOption)),
        ];
    }, [departments, draftAccessPresets, process.departmentId, teams]);

    const handleAddAccessPreset = () => {
        if (targetDomainOption == null || isSaving) {
            return;
        }

        setDraftAccessPresets((prev) => [
            ...prev,
            {
                ...ProcessInstanceAccessControlPresetApiService.initialize(),
                clientId: createNewClientId(),
                sourceDepartmentId: targetDomainOption.type === 'department' ? targetDomainOption.value : null,
                sourceTeamId: targetDomainOption.type === 'team' ? targetDomainOption.value : null,
                targetProcessId: processesId,
                targetProcessVersion: processVersion,
                permissions: [],
            },
        ]);

        setTargetDomainOption(null);
    };

    const togglePermissionForAccessPreset = (accessPreset: ProcessInstanceAccessControlPresetDraft, permission: string) => {
        if (isSaving) {
            return;
        }

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

        setDraftAccessPresets((prev) => prev.map((a) => a.clientId === updatedAccessPreset.clientId ? updatedAccessPreset : a));
    };

    const getAccessLabel = (accessPreset: ProcessInstanceAccessControlPresetDraftWithDepartmentOrTeam) => {
        if (accessPreset.team != null) {
            return accessPreset.team.name;
        }

        if (accessPreset.department != null) {
            return getDepartmentPath(accessPreset.department);
        }

        return 'diese Domäne';
    };

    const handleDeleteAccessPreset = async (accessPreset: ProcessInstanceAccessControlPresetDraftWithDepartmentOrTeam) => {
        if (accessPreset.sourceDepartmentId === process.departmentId || isSaving) {
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

        setDraftAccessPresets((prev) => prev.filter((a) => a.clientId !== accessPreset.clientId));
    };

    const handleSave = async () => {
        if (!hasUnsavedChanges || isSaving) {
            return;
        }

        const apiService = new ProcessInstanceAccessControlPresetApiService();
        let nextPersistedAccessPresets = [...persistedAccessPresets];
        let nextDraftAccessPresets = [...draftAccessPresets];

        setIsSaving(true);

        try {
            const draftAccessPresetsById = new Map(
                nextDraftAccessPresets
                    .filter((accessPreset) => accessPreset.id > 0)
                    .map((accessPreset) => [accessPreset.id, accessPreset]),
            );
            const deletedAccessPresets = nextPersistedAccessPresets.filter((accessPreset) => !draftAccessPresetsById.has(accessPreset.id));

            for (const accessPreset of deletedAccessPresets) {
                await apiService.destroy(accessPreset.id);
                nextPersistedAccessPresets = nextPersistedAccessPresets.filter((currentAccessPreset) => currentAccessPreset.id !== accessPreset.id);
            }

            const persistedAccessPresetsById = new Map(nextPersistedAccessPresets.map((accessPreset) => [accessPreset.id, accessPreset]));
            const updatedAccessPresets = nextDraftAccessPresets.filter((accessPreset) => {
                if (accessPreset.id <= 0) {
                    return false;
                }

                const persistedAccessPresetEntry = persistedAccessPresetsById.get(accessPreset.id);
                return persistedAccessPresetEntry != null && !deepEquals(
                    createComparableAccessControlPreset(persistedAccessPresetEntry),
                    createComparableAccessControlPreset(accessPreset),
                );
            });

            for (const accessPreset of updatedAccessPresets) {
                const updated = await apiService.update(accessPreset.id, toAccessPresetEntity(accessPreset));

                nextPersistedAccessPresets = nextPersistedAccessPresets.map((currentAccessPreset) => currentAccessPreset.id === updated.id ? updated : currentAccessPreset);
                nextDraftAccessPresets = nextDraftAccessPresets.map((currentAccessPreset) => currentAccessPreset.clientId === accessPreset.clientId ? {
                    ...updated,
                    clientId: currentAccessPreset.clientId,
                } : currentAccessPreset);
            }

            const createdAccessPresets = nextDraftAccessPresets.filter((accessPreset) => accessPreset.id <= 0);

            for (const accessPreset of createdAccessPresets) {
                const created = await apiService.create(toAccessPresetEntity(accessPreset));

                nextPersistedAccessPresets = [
                    ...nextPersistedAccessPresets,
                    created,
                ];
                nextDraftAccessPresets = nextDraftAccessPresets.map((currentAccessPreset) => currentAccessPreset.clientId === accessPreset.clientId ? {
                    ...created,
                    clientId: currentAccessPreset.clientId,
                } : currentAccessPreset);
            }

            setPersistedAccessPresets(nextPersistedAccessPresets);
            setDraftAccessPresets(nextDraftAccessPresets);
            dispatch(showSuccessSnackbar('Die Berechtigungen für neue Vorgänge wurden gespeichert.'));
        } catch (err) {
            setPersistedAccessPresets(nextPersistedAccessPresets);
            setDraftAccessPresets(nextDraftAccessPresets);
            dispatch(showApiErrorSnackbar(err, 'Fehler beim Speichern der Berechtigungen für neue Vorgänge'));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <>
            <ElementEditorSectionHeader
                title="Berechtigungen für neue Vorgänge"
                variant="h5"
                disableMarginTop
                maxWidth={560}
            >
                Legen Sie fest, welche Organisationseinheiten und Teams bei neu erstellten Vorgängen standardmäßig Zugriff erhalten.
            </ElementEditorSectionHeader>

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
                                    <TableRow key={accessPreset.clientId}>
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
                                                            busy={isSaving}
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
                                                isBusy={isSaving}
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
                    disabled={isSaving}
                    getOptionLabel={(option) => option.label}
                    isOptionEqualToValue={(option, value) => option.type === value.type && option.value === value.value}
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

                <Actions
                    isBusy={isSaving}
                    actions={[
                        {
                            label: 'Hinzufügen',
                            onClick: handleAddAccessPreset,
                            icon: <Add/>,
                            disabled: targetDomainOption == null,
                        },
                        {
                            label: 'Speichern',
                            onClick: () => {
                                void handleSave();
                            },
                            icon: <Save/>,
                            disabled: !hasUnsavedChanges,
                        },
                    ]}
                />
            </Stack>
        </>
    );
}
