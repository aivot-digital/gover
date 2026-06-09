import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {ProcessEntity} from '../../entities/process-entity';
import React, {ReactNode, useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {PermissionEntry} from '../../../permissions/models/permission-provider';
import {ProcessAccessControlEntity} from '../../entities/process-access-control-entity';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {ProcessAccessControlApiService} from '../../services/process-access-control-api-service';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {
    Autocomplete,
    Box, Stack,
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
import Save from '@aivot/mui-material-symbols-400-outlined/dist/save/Save';
import {deepEquals} from '../../../../utils/equality-utils';
import {ElementEditorSectionHeader} from '../../../../components/element-editor-section-header/element-editor-section-header';

interface ProcessSettingsDialogTabProps {
    open: boolean;
    process: ProcessEntity;
    departments: VDepartmentShadowedEntity[];
    teams: TeamEntity[];
    onUnsavedChangesChange?: (hasUnsavedChanges: boolean) => void;
}

interface ProcessAccessControlDraft extends ProcessAccessControlEntity {
    clientId: string;
}

interface ProcessAccessControlDraftWithDepartmentOrTeam extends ProcessAccessControlDraft {
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

function createComparableAccessControl(access: ProcessAccessControlEntity | ProcessAccessControlDraft) {
    return {
        id: access.id,
        sourceDepartmentId: access.sourceDepartmentId,
        sourceTeamId: access.sourceTeamId,
        targetProcessId: access.targetProcessId,
        permissions: [...access.permissions].sort(),
    };
}

function getAccessDomainKey(access: Pick<ProcessAccessControlEntity, 'sourceDepartmentId' | 'sourceTeamId'>): string {
    if (access.sourceDepartmentId != null) {
        return `department-${access.sourceDepartmentId}`;
    }

    if (access.sourceTeamId != null) {
        return `team-${access.sourceTeamId}`;
    }

    return 'unknown';
}

export function ProcessSettingsDialogProcessAccessTab(props: ProcessSettingsDialogTabProps) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();

    const {
        open,
        process,
        departments,
        teams,
        onUnsavedChangesChange,
    } = props;

    const {
        id: processesId,
    } = process;

    const [permissions, setPermissions] = useState<PermissionEntry[]>([]);
    const [persistedAccess, setPersistedAccess] = useState<ProcessAccessControlEntity[]>([]);
    const [draftAccess, setDraftAccess] = useState<ProcessAccessControlDraft[]>([]);
    const [targetDomainOption, setTargetDomainOption] = useState<AddDomainOption | null>(null);
    const [isSaving, setIsSaving] = useState(false);
    const nextClientIdRef = useRef(0);

    const createDraftAccess = useCallback((access: ProcessAccessControlEntity): ProcessAccessControlDraft => {
        return {
            ...access,
            clientId: `server-${access.id}`,
        };
    }, []);

    const createNewClientId = useCallback(() => {
        nextClientIdRef.current += 1;
        return `new-${nextClientIdRef.current}`;
    }, []);

    const toAccessEntity = useCallback((access: ProcessAccessControlDraft): ProcessAccessControlEntity => {
        return {
            id: access.id,
            sourceDepartmentId: access.sourceDepartmentId,
            sourceTeamId: access.sourceTeamId,
            targetProcessId: access.targetProcessId,
            permissions: access.permissions,
            created: access.created,
            updated: access.updated,
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
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für Prozesse'));
            });
    }, [dispatch]);

    const loadAccess = useCallback(() => {
        new ProcessAccessControlApiService()
            .listAll({
                targetProcessId: processesId,
            })
            .then(({content}) => {
                nextClientIdRef.current = 0;
                setPersistedAccess(content);
                setDraftAccess(content.map(createDraftAccess));
                setTargetDomainOption(null);
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für diesen Prozess'));
            });
    }, [createDraftAccess, dispatch, processesId]);

    useEffect(() => {
        if (!open) {
            return;
        }

        loadAccess();
    }, [loadAccess, open]);

    const hasUnsavedChanges = useMemo(() => {
        const comparablePersistedAccess = persistedAccess
            .map((access) => createComparableAccessControl(access))
            .sort((left, right) => getAccessDomainKey(left).localeCompare(getAccessDomainKey(right)));
        const comparableDraftAccess = draftAccess
            .map((access) => createComparableAccessControl(access))
            .sort((left, right) => getAccessDomainKey(left).localeCompare(getAccessDomainKey(right)));

        return !deepEquals(comparablePersistedAccess, comparableDraftAccess);
    }, [draftAccess, persistedAccess]);

    useEffect(() => {
        onUnsavedChangesChange?.(hasUnsavedChanges);
    }, [hasUnsavedChanges, onUnsavedChangesChange]);

    useEffect(() => {
        return () => {
            onUnsavedChangesChange?.(false);
        };
    }, [onUnsavedChangesChange]);

    const resolvedAccessControl: ProcessAccessControlDraftWithDepartmentOrTeam[] = useMemo(() => {
        return draftAccess
            .map((accessControl) => {
                return {
                    ...accessControl,
                    department: accessControl.sourceDepartmentId != null ? departments.find((d) => d.id === accessControl.sourceDepartmentId) : undefined,
                    team: accessControl.sourceTeamId != null ? teams.find((d) => d.id === accessControl.sourceTeamId) : undefined,
                };
            });
    }, [departments, draftAccess, teams]);

    const owningDepartment = useMemo(() => {
        return departments.find((d) => d.id === process.departmentId)!;
    }, [departments, process.departmentId]);

    const addDomainOptions: AddDomainOption[] = useMemo(() => {
        const assignedDomainKeys = new Set(draftAccess.map((accessControl) => getAccessDomainKey(accessControl)));

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
    }, [departments, draftAccess, process.departmentId, teams]);

    const handleAddAccess = () => {
        if (targetDomainOption == null || isSaving) {
            return;
        }

        setDraftAccess((prev) => [
            ...prev,
            {
                ...ProcessAccessControlApiService.initialize(),
                clientId: createNewClientId(),
                sourceDepartmentId: targetDomainOption.type === 'department' ? targetDomainOption.value : null,
                sourceTeamId: targetDomainOption.type === 'team' ? targetDomainOption.value : null,
                targetProcessId: processesId,
                permissions: [],
            },
        ]);

        setTargetDomainOption(null);
    };

    const togglePermissionForAccess = (access: ProcessAccessControlDraft, permission: string) => {
        if (isSaving) {
            return;
        }

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
        };

        setDraftAccess((prev) => prev.map((a) => a.clientId === updatedAccess.clientId ? updatedAccess : a));
    };

    const getAccessLabel = (access: ProcessAccessControlDraftWithDepartmentOrTeam) => {
        if (access.team != null) {
            return access.team.name;
        }

        if (access.department != null) {
            return getDepartmentPath(access.department);
        }

        return 'diese Domäne';
    };

    const handleDeleteAccess = async (access: ProcessAccessControlDraftWithDepartmentOrTeam) => {
        if (access.sourceDepartmentId === process.departmentId || isSaving) {
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

        setDraftAccess((prev) => prev.filter((a) => a.clientId !== access.clientId));
    };

    const handleSave = async () => {
        if (!hasUnsavedChanges || isSaving) {
            return;
        }

        const apiService = new ProcessAccessControlApiService();
        let nextPersistedAccess = [...persistedAccess];
        let nextDraftAccess = [...draftAccess];

        setIsSaving(true);

        try {
            const draftAccessById = new Map(
                nextDraftAccess
                    .filter((access) => access.id > 0)
                    .map((access) => [access.id, access]),
            );
            const deletedAccess = nextPersistedAccess.filter((access) => !draftAccessById.has(access.id));

            for (const access of deletedAccess) {
                await apiService.destroy(access.id);
                nextPersistedAccess = nextPersistedAccess.filter((currentAccess) => currentAccess.id !== access.id);
            }

            const persistedAccessById = new Map(nextPersistedAccess.map((access) => [access.id, access]));
            const updatedAccess = nextDraftAccess.filter((access) => {
                if (access.id <= 0) {
                    return false;
                }

                const persistedAccessEntry = persistedAccessById.get(access.id);
                return persistedAccessEntry != null && !deepEquals(
                    createComparableAccessControl(persistedAccessEntry),
                    createComparableAccessControl(access),
                );
            });

            for (const access of updatedAccess) {
                const updated = await apiService.update(access.id, toAccessEntity(access));

                nextPersistedAccess = nextPersistedAccess.map((currentAccess) => currentAccess.id === updated.id ? updated : currentAccess);
                nextDraftAccess = nextDraftAccess.map((currentAccess) => currentAccess.clientId === access.clientId ? {
                    ...updated,
                    clientId: currentAccess.clientId,
                } : currentAccess);
            }

            const createdAccess = nextDraftAccess.filter((access) => access.id <= 0);

            for (const access of createdAccess) {
                const created = await apiService.create(toAccessEntity(access));

                nextPersistedAccess = [
                    ...nextPersistedAccess,
                    created,
                ];
                nextDraftAccess = nextDraftAccess.map((currentAccess) => currentAccess.clientId === access.clientId ? {
                    ...created,
                    clientId: currentAccess.clientId,
                } : currentAccess);
            }

            setPersistedAccess(nextPersistedAccess);
            setDraftAccess(nextDraftAccess);
            dispatch(showSuccessSnackbar('Die Berechtigungen für diesen Prozess wurden gespeichert.'));
        } catch (err) {
            setPersistedAccess(nextPersistedAccess);
            setDraftAccess(nextDraftAccess);
            dispatch(showApiErrorSnackbar(err, 'Fehler beim Speichern der Berechtigungen für diesen Prozess'));
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <>
            <ElementEditorSectionHeader
                title="Berechtigungen des Prozesses"
                variant="h5"
                disableMarginTop
                maxWidth={560}
            >
                Steuern Sie, welche Organisationseinheiten und Teams diesen Prozess in der Verwaltung sehen, bearbeiten, prüfen oder veröffentlichen dürfen.
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
                                .map((access) => (
                                    <TableRow key={access.clientId}>
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
                                                            busy={isSaving}
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
                                                isBusy={isSaving}
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
                            onClick: handleAddAccess,
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
