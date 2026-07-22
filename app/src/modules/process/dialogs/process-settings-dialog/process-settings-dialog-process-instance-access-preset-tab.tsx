import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {ProcessEntity} from '../../entities/process-entity';
import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState} from 'react';
import {PermissionEntry} from '../../../permissions/models/permission-provider';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {Typography} from '@mui/material';
import {getDepartmentPath} from '../../../departments/utils/department-utils';
import {TeamEntity} from '../../../teams/entities/team-entity';
import {useConfirm} from '../../../../providers/confirm-provider';
import {ProcessInstanceAccessControlPresetEntity} from '../../entities/process-instance-access-control-preset-entity';
import {ProcessInstanceAccessControlPresetApiService} from '../../services/process-instance-access-control-preset-api-service';
import {ProcessVersionEntity} from '../../entities/process-version-entity';
import {deepEquals} from '../../../../utils/equality-utils';
import {ElementEditorSectionHeader} from '../../../../components/element-editor-section-header/element-editor-section-header';
import {
    ProcessSettingsDialogAccessControlMatrix,
    type ProcessSettingsAccessControlAddDomainOption,
} from './process-settings-dialog-access-control-matrix';

interface ProcessSettingsDialogTabProps {
    open: boolean;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    departments: VDepartmentShadowedEntity[];
    teams: TeamEntity[];
    onUnsavedChangesChange?: (hasUnsavedChanges: boolean) => void;
    onSavingChange?: (isSaving: boolean) => void;
}

export interface ProcessSettingsDialogProcessInstanceAccessPresetTabHandle {
    save: () => void;
    reset: () => void;
}

interface ProcessInstanceAccessControlPresetDraft extends ProcessInstanceAccessControlPresetEntity {
    clientId: string;
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

export const ProcessSettingsDialogProcessInstanceAccessPresetTab = forwardRef<ProcessSettingsDialogProcessInstanceAccessPresetTabHandle, ProcessSettingsDialogTabProps>(function ProcessSettingsDialogProcessInstanceAccessPresetTab(props, ref) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();

    const {
        open,
        process,
        version,
        departments,
        teams,
        onUnsavedChangesChange,
        onSavingChange,
    } = props;

    const {
        id: processId,
    } = process;

    const {
        processVersion,
    } = version;

    const [permissions, setPermissions] = useState<PermissionEntry[]>([]);
    const [persistedAccessPresets, setPersistedAccessPresets] = useState<ProcessInstanceAccessControlPresetEntity[]>([]);
    const [draftAccessPresets, setDraftAccessPresets] = useState<ProcessInstanceAccessControlPresetDraft[]>([]);
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
                const processInstancePermissions = permissionProviders
                    .find((p) => p.contextLabel === 'Vorgänge');
                if (processInstancePermissions) {
                    setPermissions(processInstancePermissions.permissions.filter(p => relevantPermissions.includes(p.permission)));
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
                targetProcessId: processId,
                targetProcessVersion: processVersion,
            })
            .then(({content}) => {
                nextClientIdRef.current = 0;
                setPersistedAccessPresets(content);
                setDraftAccessPresets(content.map(createDraftAccessPreset));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für neue Vorgänge'));
            });
    }, [createDraftAccessPreset, dispatch, processId, processVersion]);

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

    useEffect(() => {
        onSavingChange?.(isSaving);
    }, [isSaving, onSavingChange]);

    useEffect(() => {
        return () => {
            onSavingChange?.(false);
        };
    }, [onSavingChange]);

    const handleAddAccessPreset = (targetDomainOption: ProcessSettingsAccessControlAddDomainOption) => {
        if (isSaving) {
            return;
        }

        setDraftAccessPresets((prev) => [
            ...prev,
            {
                ...ProcessInstanceAccessControlPresetApiService.initialize(),
                clientId: createNewClientId(),
                sourceDepartmentId: targetDomainOption.type === 'department' ? targetDomainOption.value : null,
                sourceTeamId: targetDomainOption.type === 'team' ? targetDomainOption.value : null,
                targetProcessId: processId,
                targetProcessVersion: processVersion,
                permissions: [],
            },
        ]);
    };

    const getAccessLabel = (accessPreset: ProcessInstanceAccessControlPresetDraft) => {
        if (accessPreset.sourceTeamId != null) {
            return teams.find((team) => team.id === accessPreset.sourceTeamId)?.name ?? `Team #${accessPreset.sourceTeamId}`;
        }

        if (accessPreset.sourceDepartmentId != null) {
            const department = departments.find((entry) => entry.id === accessPreset.sourceDepartmentId);

            return department != null ? getDepartmentPath(department) : `Organisationseinheit #${accessPreset.sourceDepartmentId}`;
        }

        return 'diese Domäne';
    };

    const handleDeleteAccessPreset = async (accessPreset: ProcessInstanceAccessControlPresetDraft) => {
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

    const handleSave = useCallback(async () => {
        if (!hasUnsavedChanges || isSaving) {
            return;
        }

        const apiService = new ProcessInstanceAccessControlPresetApiService();
        let nextPersistedAccessPresets = [...persistedAccessPresets];
        let nextDraftAccessPresets = [...draftAccessPresets];

        // Advance these snapshots after each request so partial failures keep already-saved changes in sync.
        setIsSaving(true);

        try {
            const draftAccessPresetsById = new Map(
                nextDraftAccessPresets
                    .filter((accessPreset) => accessPreset.id > 0)
                    .map((accessPreset) => [accessPreset.id, accessPreset]),
            );
            const deletedAccessPresets = nextPersistedAccessPresets.filter((accessPreset) => !draftAccessPresetsById.has(accessPreset.id));

            // Delete first so removing and re-adding the same domain in one save does not violate uniqueness.
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
    }, [dispatch, draftAccessPresets, hasUnsavedChanges, isSaving, persistedAccessPresets, toAccessPresetEntity]);

    const handleReset = useCallback(() => {
        setDraftAccessPresets(persistedAccessPresets.map(createDraftAccessPreset));
    }, [createDraftAccessPreset, persistedAccessPresets]);

    useImperativeHandle(ref, () => ({
        save: () => {
            void handleSave();
        },
        reset: handleReset,
    }), [handleReset, handleSave]);

    return (
        <>
            <ElementEditorSectionHeader
                title="Standardrechte für neue Vorgänge"
                variant="h5"
                disableMarginTop
                maxWidth={700}
            >
                Definieren Sie, welche Organisationseinheiten und Teams bei der Anlage eines neuen Vorgangs einmalig als Berechtigungen übernommen werden.
                Ab diesem Zeitpunkt verfügt der Vorgang über eigene Berechtigungen, die unabhängig von diesen Standardeinstellungen verwaltet werden.
            </ElementEditorSectionHeader>

            <ProcessSettingsDialogAccessControlMatrix
                permissions={permissions}
                accessControls={draftAccessPresets}
                owningDepartmentId={process.departmentId}
                departments={departments}
                teams={teams}
                isBusy={isSaving}
                onAccessControlsChange={setDraftAccessPresets}
                onAddAccessControl={handleAddAccessPreset}
                onDeleteAccessControl={(accessPreset) => {
                    void handleDeleteAccessPreset(accessPreset);
                }}
            />
        </>
    );
});
