import {Box, Button, Tooltip, Typography} from '@mui/material';
import React, {type ReactNode, useContext, useEffect, useMemo, useState} from 'react';
import {
    GenericDetailsPageContext,
    type GenericDetailsPageContextType,
} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useNavigate} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {AlertComponent} from '../../../../components/alert/alert-component';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {
    addSnackbarMessage,
    removeSnackbarMessage,
    SnackbarSeverity,
    SnackbarType,
} from '../../../../slices/shell-slice';
import {type SystemRoleEntity} from '../../entities/system-role-entity';
import {SystemRolesApiService} from '../../services/system-roles-api-service';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Grid from '@mui/material/Grid';
import {PermissionEditor} from '../../../permissions/components/permission-editor';
import {PermissionScope} from '../../../permissions/enums/permission-scope';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {selectSystemConfigValue, setSystemConfig} from '../../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../../data/system-config-keys';
import {UsersApiService} from '../../../users/users-api-service';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {type SelectFieldComponentOption} from '../../../../components/select-field/select-field-component-option';
import {pluralize} from '../../../../utils/humanization-utils';

export const SystemRoleSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name muss mindestens 3 Zeichen lang sein.')
        .max(64, 'Der Name darf maximal 64 Zeichen lang sein.')
        .required('Der Name ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .min(3, 'Die Beschreibung muss mindestens 3 Zeichen lang sein.')
        .max(255, 'Die Beschreibung darf maximal 255 Zeichen lang sein.')
        .required('Die Beschreibung ist ein Pflichtfeld.'),
});

function formatAssignedUsersAreMessage(count: number): string {
    return `Dieser Systemrolle ${pluralize(count, 'ist', 'sind')} aktuell ${count} ${pluralize(count, 'Mitarbeiter:in', 'Mitarbeiter:innen')} zugeordnet.`;
}

function formatMigratedUsersMessage(count: number, replacementRoleLabel: string): string {
    return `${count} ${pluralize(count, 'Mitarbeiter:in', 'Mitarbeiter:innen')} ${pluralize(count, 'wurde', 'wurden')} auf die Systemrolle „${replacementRoleLabel}“ umgestellt.`;
}

export function SystemRolesDetailsPageIndex(): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const defaultSystemRoleId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.users.defaultSystemRole));

    const {
        item: systemRole,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext<GenericDetailsPageContextType<SystemRoleEntity, void>>(GenericDetailsPageContext);

    useEffect(() => {
        if (isEditable) {
            return;
        }

        dispatch(addSnackbarMessage({
            key: 'access-denied-system-roles-details',
            message: 'Diese Systemrolle kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.',
            severity: SnackbarSeverity.Warning,
            type: SnackbarType.Dismissable,
        }));

        return () => {
            dispatch(removeSnackbarMessage('access-denied-system-roles-details'));
        };
    }, [dispatch, isEditable]);

    const {
        currentItem: editedSystemRole,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        handleInputPatch,
        validate,
        reset,
    } = useFormManager<SystemRoleEntity>(systemRole, SystemRoleSchema as any);

    const apiService = useMemo(() => new SystemRolesApiService(), []);
    const usersApiService = useMemo(() => new UsersApiService(), []);

    const changeBlocker = useChangeBlocker(systemRole, editedSystemRole);
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);
    const [replacementSystemRoleId, setReplacementSystemRoleId] = useState<string | undefined>(undefined);
    const [assignedUsersCount, setAssignedUsersCount] = useState<number | null>(null);
    const [isDeleteRequirementsLoading, setIsDeleteRequirementsLoading] = useState(false);
    const [hasDeleteRequirementsLoadingError, setHasDeleteRequirementsLoadingError] = useState(false);
    const [systemRoleOptions, setSystemRoleOptions] = useState<SelectFieldComponentOption[]>([]);
    const [isSystemRolesLoading, setIsSystemRolesLoading] = useState(false);
    const [hasSystemRolesLoadingError, setHasSystemRolesLoadingError] = useState(false);

    const currentSystemRoleId = editedSystemRole?.id ?? 0;
    const isDefaultSystemRole =
        currentSystemRoleId !== 0 &&
        defaultSystemRoleId != null &&
        defaultSystemRoleId.trim().length > 0 &&
        String(currentSystemRoleId) === defaultSystemRoleId.trim();
    const hasAssignedUsers = (assignedUsersCount ?? 0) > 0;
    const requiresReplacementSystemRole = isDefaultSystemRole || hasAssignedUsers;

    useEffect(() => {
        if (currentSystemRoleId === 0) {
            setAssignedUsersCount(0);
            setIsDeleteRequirementsLoading(false);
            setHasDeleteRequirementsLoadingError(false);
            return;
        }

        let isCancelled = false;

        setIsDeleteRequirementsLoading(true);
        setHasDeleteRequirementsLoadingError(false);

        usersApiService
            .list(0, 1, undefined, undefined, {
                systemRoleId: currentSystemRoleId,
            })
            .then((page) => {
                if (isCancelled) {
                    return;
                }

                setAssignedUsersCount(page.totalElements);
            })
            .catch((err) => {
                if (isCancelled) {
                    return;
                }

                setAssignedUsersCount(null);
                setHasDeleteRequirementsLoadingError(true);
                dispatch(showApiErrorSnackbar(
                    err,
                    'Es konnte nicht geprüft werden, ob dieser Systemrolle Mitarbeiter:innen zugeordnet sind.',
                ));
            })
            .finally(() => {
                if (!isCancelled) {
                    setIsDeleteRequirementsLoading(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [currentSystemRoleId, dispatch, usersApiService]);

    useEffect(() => {
        if (currentSystemRoleId === 0 || !requiresReplacementSystemRole) {
            setSystemRoleOptions([]);
            setIsSystemRolesLoading(false);
            setHasSystemRolesLoadingError(false);
            return;
        }

        let isCancelled = false;

        setIsSystemRolesLoading(true);
        setHasSystemRolesLoadingError(false);

        apiService
            .listAllOrdered('name', 'ASC')
            .then((response) => {
                if (isCancelled) {
                    return;
                }

                setSystemRoleOptions(response.content
                    .filter((role) => role.id !== currentSystemRoleId)
                    .map((role) => ({
                        value: String(role.id),
                        label: role.name,
                        subLabel: role.description ?? undefined,
                    })));
            })
            .catch((err) => {
                if (isCancelled) {
                    return;
                }

                setSystemRoleOptions([]);
                setHasSystemRolesLoadingError(true);
                dispatch(showApiErrorSnackbar(err, 'Die verfügbaren Ersatz-Systemrollen konnten nicht geladen werden.'));
            })
            .finally(() => {
                if (!isCancelled) {
                    setIsSystemRolesLoading(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [apiService, currentSystemRoleId, dispatch, requiresReplacementSystemRole]);

    useEffect(() => {
        if (!showConfirmDialog || !requiresReplacementSystemRole) {
            setReplacementSystemRoleId(undefined);
            return;
        }

        setReplacementSystemRoleId((previousValue) => {
            if (previousValue != null && systemRoleOptions.some((option) => option.value === previousValue)) {
                return previousValue;
            }

            return systemRoleOptions[0]?.value;
        });
    }, [requiresReplacementSystemRole, showConfirmDialog, systemRoleOptions]);

    if (editedSystemRole == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const selectedReplacementSystemRole = systemRoleOptions
        .find((option) => option.value === replacementSystemRoleId);

    const canDeleteWithoutReplacement = !requiresReplacementSystemRole;
    const canProvideReplacementRole = !isSystemRolesLoading && !hasSystemRolesLoadingError && systemRoleOptions.length > 0;
    const canOpenDeleteDialog =
        currentSystemRoleId !== 0 &&
        !isBusy &&
        isEditable &&
        !isDeleteRequirementsLoading &&
        !hasDeleteRequirementsLoadingError &&
        (canDeleteWithoutReplacement || canProvideReplacementRole);

    let deleteTooltip = 'Diese Systemrolle löschen';
    if (isDeleteRequirementsLoading) {
        deleteTooltip = 'Es wird geprüft, ob beim Löschen dieser Rolle eine Migration erforderlich ist.';
    } else if (hasDeleteRequirementsLoadingError) {
        deleteTooltip = 'Die Löschvoraussetzungen konnten nicht geprüft werden.';
    } else if (requiresReplacementSystemRole && isSystemRolesLoading) {
        deleteTooltip = 'Die verfügbaren Ersatz-Systemrollen werden geladen.';
    } else if (requiresReplacementSystemRole && hasSystemRolesLoadingError) {
        deleteTooltip = 'Die verfügbaren Ersatz-Systemrollen konnten nicht geladen werden.';
    } else if (requiresReplacementSystemRole && systemRoleOptions.length === 0) {
        deleteTooltip = 'Es ist keine andere Systemrolle verfügbar, auf die Mitarbeiter:innen oder Systemeinstellungen migriert werden können.';
    }

    const deleteImpactTexts: string[] = [];
    if (hasAssignedUsers && assignedUsersCount != null) {
        deleteImpactTexts.push(
            `${formatAssignedUsersAreMessage(assignedUsersCount)} Beim Löschen werden diese Mitarbeiter:innen auf die ausgewählte Ersatz-Systemrolle umgestellt.`,
        );
    }
    if (isDefaultSystemRole) {
        deleteImpactTexts.push(
            'Diese Rolle ist aktuell als Standard-Systemrolle für automatische Benutzerimporte konfiguriert. Beim Löschen wird diese Einstellung ebenfalls auf die ausgewählte Ersatz-Systemrolle geändert.',
        );
    }

    const handleCloseConfirmDialog = (): void => {
        setShowConfirmDialog(false);
        setReplacementSystemRoleId(undefined);
    };

    const handleSave = (): void => {
        const validationResult = validate();

        if (!validationResult) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);

        if (editedSystemRole.id === 0) {
            apiService
                .create(editedSystemRole as any)
                .then((newRole) => {
                    setItem(newRole);
                    reset();

                    dispatch(showSuccessSnackbar('Neue Systemrolle erfolgreich angelegt.'));

                    setTimeout(() => {
                        navigate(`/system-roles/${newRole.id}`, {
                            replace: true,
                        })?.catch(console.error);
                    }, 0);
                })
                .catch((err) => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });

            return;
        }

        apiService
            .update(editedSystemRole.id, editedSystemRole as any)
            .then((updatedRole) => {
                setItem(updatedRole);
                reset();

                dispatch(showSuccessSnackbar('Änderungen an der Systemrolle erfolgreich gespeichert.'));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    const handleDelete = (): void => {
        if (editedSystemRole.id === 0) {
            return;
        }

        if (requiresReplacementSystemRole && replacementSystemRoleId == null) {
            return;
        }

        const replacementRole = selectedReplacementSystemRole;
        const migratedUsersCount = hasAssignedUsers ? (assignedUsersCount ?? 0) : 0;
        const defaultSystemRoleWillBeUpdated = isDefaultSystemRole && replacementSystemRoleId != null;
        const newDefaultSystemRoleId = defaultSystemRoleWillBeUpdated ? Number(replacementSystemRoleId) : null;

        setIsBusy(true);

        apiService
            .destroyWithMigration(
                editedSystemRole.id,
                replacementSystemRoleId != null ? Number(replacementSystemRoleId) : null,
            )
            .then(() => {
                if (defaultSystemRoleWillBeUpdated && newDefaultSystemRoleId != null) {
                    dispatch(setSystemConfig({
                        key: SystemConfigKeys.users.defaultSystemRole,
                        value: String(newDefaultSystemRoleId),
                        publicConfig: false,
                    }));
                }

                const successMessages = ['Die Systemrolle wurde erfolgreich gelöscht.'];
                if (migratedUsersCount > 0 && replacementRole != null) {
                    successMessages.push(
                        formatMigratedUsersMessage(migratedUsersCount, replacementRole.label),
                    );
                }
                if (defaultSystemRoleWillBeUpdated && replacementRole != null) {
                    successMessages.push(
                        `Die Standard-Systemrolle für automatische Benutzerimporte wurde auf „${replacementRole.label}“ gesetzt.`,
                    );
                }

                handleCloseConfirmDialog();
                reset();
                dispatch(showSuccessSnackbar(successMessages.join(' ')));

                navigate('/system-roles', {
                    replace: true,
                })?.catch(console.error);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showApiErrorSnackbar(err, 'Beim Löschen der Systemrolle ist ein Fehler aufgetreten.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Systemrolle konfigurieren
            </Typography>
            <Typography sx={{mb: 3, maxWidth: 900}}>
                Systemrollen definieren Berechtigungen für Benutzer:innen auf Systemebene.
                Seien Sie vorsichtig bei der Vergabe von Berechtigungen, insbesondere bei solchen, die Zugriff auf
                sensible Daten oder kritische Funktionen ermöglichen.
            </Typography>

            <AlertComponent
                color="warning"
                title="Hinweis zum aktuellen Entwicklungsstand von Rollen und Berechtigungen"
                sx={{mb: 3}}
            >
                Rollen und die damit verbundenen Berechtigungen werden in Gover derzeit noch nicht überall vollständig
                berücksichtigt.
                Funktionen können unvollständig sein, sich ändern oder sich in einzelnen Bereichen noch nicht wie
                erwartet verhalten.
                Bitte verlassen Sie sich daher aktuell nicht darauf, dass konfigurierte Berechtigungen bereits
                konsistent an allen Stellen durchgesetzt werden.
            </AlertComponent>

            <Grid
                container
                spacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Name"
                        required
                        value={editedSystemRole.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        disabled={isBusy || !isEditable}
                        error={errors.name}
                        minCharacters={3}
                        maxCharacters={64}
                    />

                    <TextFieldComponent
                        label="Beschreibung"
                        required
                        value={editedSystemRole.description}
                        onChange={handleInputChange('description')}
                        onBlur={handleInputBlur('description')}
                        multiline={true}
                        disabled={isBusy || !isEditable}
                        error={errors.description}
                        minCharacters={3}
                        maxCharacters={255}
                    />
                </Grid>
            </Grid>

            <PermissionEditor
                originalPermissions={systemRole?.permissions ?? []}
                value={editedSystemRole.permissions ?? []}
                onChange={(next) => handleInputPatch({permissions: next})}
                isBusy={isBusy}
                isEditable={isEditable}
                scope={PermissionScope.System}
            />

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 3,
                    gap: 2,
                }}
            >
                <Button
                    onClick={handleSave}
                    disabled={isBusy || hasNotChanged || !isEditable}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon/>}
                >
                    Speichern
                </Button>

                {editedSystemRole.id !== 0 && (
                    <Button
                        onClick={() => reset()}
                        disabled={isBusy || hasNotChanged || !isEditable}
                        color="error"
                    >
                        Zurücksetzen
                    </Button>
                )}

                {editedSystemRole.id !== 0 && (
                    <Tooltip
                        title={deleteTooltip}
                        arrow
                    >
                        <Box
                            component="span"
                            sx={{
                                marginLeft: 'auto',
                            }}
                        >
                            <Button
                                variant="outlined"
                                onClick={() => setShowConfirmDialog(true)}
                                disabled={!canOpenDeleteDialog}
                                color="error"
                                startIcon={<Delete/>}
                            >
                                Löschen
                            </Button>
                        </Box>
                    </Tooltip>
                )}
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Systemrolle löschen"
                onCancel={handleCloseConfirmDialog}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmDisabled={requiresReplacementSystemRole && replacementSystemRoleId == null}
                confirmationText={editedSystemRole.name ?? ''}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography sx={{mb: 2}}>
                    Möchten Sie diese Systemrolle wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>

                {requiresReplacementSystemRole ? (
                    <>
                        <AlertComponent
                            color="warning"
                            text={deleteImpactTexts.join('\n\n')}
                            sx={{mb: 2}}
                        />

                        <SelectFieldComponent
                            label="Ersatz-Systemrolle"
                            value={replacementSystemRoleId}
                            onChange={setReplacementSystemRoleId}
                            options={systemRoleOptions}
                            required
                            disabled={isBusy || isSystemRolesLoading}
                            hint="Alle betroffenen Mitarbeiter:innen und gegebenenfalls die Systemeinstellung für automatische Benutzerimporte werden auf diese Rolle umgestellt."
                            emptyStatePlaceholder="Keine Ersatz-Systemrolle verfügbar"
                        />
                    </>
                ) : (
                    <AlertComponent color="info">
                        Diese Systemrolle ist aktuell keiner Mitarbeiter:in zugeordnet und nicht als Standard-Systemrolle
                        für automatische Benutzerimporte konfiguriert.
                    </AlertComponent>
                )}
            </ConfirmDialog>
        </Box>
    );
}
