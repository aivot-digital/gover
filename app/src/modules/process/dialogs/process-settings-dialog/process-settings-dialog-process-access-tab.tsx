import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {VDepartmentShadowedEntity} from '../../../departments/entities/v-department-shadowed-entity';
import {ProcessEntity} from '../../entities/process-entity';
import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useRef, useState} from 'react';
import {PermissionEntry} from '../../../permissions/models/permission-provider';
import {ProcessAccessControlEntity} from '../../entities/process-access-control-entity';
import {PermissionApiService} from '../../../permissions/permission-api-service';
import {ProcessAccessControlApiService} from '../../services/process-access-control-api-service';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {Typography} from '@mui/material';
import {getDepartmentPath} from '../../../departments/utils/department-utils';
import {TeamEntity} from '../../../teams/entities/team-entity';
import {useConfirm} from '../../../../providers/confirm-provider';
import {deepEquals} from '../../../../utils/equality-utils';
import {ElementEditorSectionHeader} from '../../../../components/element-editor-section-header/element-editor-section-header';
import {
    ProcessSettingsDialogAccessControlMatrix,
    type ProcessSettingsAccessControlAddDomainOption,
} from './process-settings-dialog-access-control-matrix';

interface ProcessSettingsDialogTabProps {
    open: boolean;
    process: ProcessEntity;
    departments: VDepartmentShadowedEntity[];
    teams: TeamEntity[];
    onUnsavedChangesChange?: (hasUnsavedChanges: boolean) => void;
    onSavingChange?: (isSaving: boolean) => void;
}

export interface ProcessSettingsDialogProcessAccessTabHandle {
    save: () => void;
    reset: () => void;
}

interface ProcessAccessControlDraft extends ProcessAccessControlEntity {
    clientId: string;
}

const relevantPermissions: string[] = [
    'process_definition.read',
    'process_definition.update',
    'process_definition.audit',
    'process_definition.publish.test',
    'process_definition.publish.local',
    'process_definition.publish.marketplace',
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

export const ProcessSettingsDialogProcessAccessTab = forwardRef<ProcessSettingsDialogProcessAccessTabHandle, ProcessSettingsDialogTabProps>(function ProcessSettingsDialogProcessAccessTab(props, ref) {
    const dispatch = useAppDispatch();
    const confirm = useConfirm();

    const {
        open,
        process,
        departments,
        teams,
        onUnsavedChangesChange,
        onSavingChange,
    } = props;

    const {
        id: processId,
    } = process;

    const [permissions, setPermissions] = useState<PermissionEntry[]>([]);
    const [persistedAccess, setPersistedAccess] = useState<ProcessAccessControlEntity[]>([]);
    const [draftAccess, setDraftAccess] = useState<ProcessAccessControlDraft[]>([]);
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
                targetProcessId: processId,
            })
            .then(({content}) => {
                nextClientIdRef.current = 0;
                setPersistedAccess(content);
                setDraftAccess(content.map(createDraftAccess));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Fehler beim Laden der Berechtigungen für diesen Prozess'));
            });
    }, [createDraftAccess, dispatch, processId]);

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

    useEffect(() => {
        onSavingChange?.(isSaving);
    }, [isSaving, onSavingChange]);

    useEffect(() => {
        return () => {
            onSavingChange?.(false);
        };
    }, [onSavingChange]);

    const handleAddAccess = (targetDomainOption: ProcessSettingsAccessControlAddDomainOption) => {
        if (isSaving) {
            return;
        }

        setDraftAccess((prev) => [
            ...prev,
            {
                ...ProcessAccessControlApiService.initialize(),
                clientId: createNewClientId(),
                sourceDepartmentId: targetDomainOption.type === 'department' ? targetDomainOption.value : null,
                sourceTeamId: targetDomainOption.type === 'team' ? targetDomainOption.value : null,
                targetProcessId: processId,
                permissions: [],
            },
        ]);
    };

    const getAccessLabel = (access: ProcessAccessControlDraft) => {
        if (access.sourceTeamId != null) {
            return teams.find((team) => team.id === access.sourceTeamId)?.name ?? `Team #${access.sourceTeamId}`;
        }

        if (access.sourceDepartmentId != null) {
            const department = departments.find((entry) => entry.id === access.sourceDepartmentId);

            return department != null ? getDepartmentPath(department) : `Organisationseinheit #${access.sourceDepartmentId}`;
        }

        return 'diese Domäne';
    };

    const handleDeleteAccess = async (access: ProcessAccessControlDraft) => {
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

    const handleSave = useCallback(async () => {
        if (!hasUnsavedChanges || isSaving) {
            return;
        }

        const apiService = new ProcessAccessControlApiService();
        let nextPersistedAccess = [...persistedAccess];
        let nextDraftAccess = [...draftAccess];

        // Advance these snapshots after each request so partial failures keep already-saved changes in sync.
        setIsSaving(true);

        try {
            const draftAccessById = new Map(
                nextDraftAccess
                    .filter((access) => access.id > 0)
                    .map((access) => [access.id, access]),
            );
            const deletedAccess = nextPersistedAccess.filter((access) => !draftAccessById.has(access.id));

            // Delete first so removing and re-adding the same domain in one save does not violate uniqueness.
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
    }, [dispatch, draftAccess, hasUnsavedChanges, isSaving, persistedAccess, toAccessEntity]);

    const handleReset = useCallback(() => {
        setDraftAccess(persistedAccess.map(createDraftAccess));
    }, [createDraftAccess, persistedAccess]);

    useImperativeHandle(ref, () => ({
        save: () => {
            void handleSave();
        },
        reset: handleReset,
    }), [handleReset, handleSave]);

    return (
        <>
            <ElementEditorSectionHeader
                title="Prozessberechtigungen"
                variant="h5"
                disableMarginTop
                maxWidth={700}
            >
                Legen Sie fest, welche Organisationseinheiten und Teams diesen Prozess in der Verwaltung sehen, bearbeiten, prüfen oder veröffentlichen dürfen.
                Die verwaltende Organisationseinheit ist immer berechtigt und kann nicht entfernt werden.
            </ElementEditorSectionHeader>

            <ProcessSettingsDialogAccessControlMatrix
                permissions={permissions}
                accessControls={draftAccess}
                owningDepartmentId={process.departmentId}
                departments={departments}
                teams={teams}
                isBusy={isSaving}
                onAccessControlsChange={setDraftAccess}
                onAddAccessControl={handleAddAccess}
                onDeleteAccessControl={(access) => {
                    void handleDeleteAccess(access);
                }}
            />
        </>
    );
});
