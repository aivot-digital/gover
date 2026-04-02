import {Box, Button, Grid, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import LockResetOutlinedIcon from '@mui/icons-material/LockResetOutlined';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {useNavigate} from 'react-router-dom';
import {
    GenericDetailsPageContext,
    type GenericDetailsPageContextType,
} from '../../../../../components/generic-details-page/generic-details-page-context';
import {type User} from '../../../../../models/entities/user';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../../slices/snackbar-slice';
import {isApiError} from '../../../../../models/api-error';
import {GenericDetailsSkeleton} from '../../../../../components/generic-details-page/generic-details-skeleton';
import {ConfirmDialog} from '../../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintDialog} from '../../../../../dialogs/constraint-dialog/constraint-dialog';
import {ConstraintLinkProps} from '../../../../../dialogs/constraint-dialog/constraint-link-props';
import {TextFieldComponent} from '../../../../../components/text-field/text-field-component';
import {CheckboxFieldComponent} from '../../../../../components/checkbox-field/checkbox-field-component';
import {useFormManager} from '../../../../../hooks/use-form-manager';
import * as yup from 'yup';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {SelectFieldComponent} from '../../../../../components/select-field-2/select-field-component';
import {UsersApiService} from '../../../users-api-service';
import {useConfirm} from '../../../../../providers/confirm-provider';
import {SystemRolesApiService} from '../../../../system/services/system-roles-api-service';
import {useChangeBlocker} from '../../../../../hooks/use-change-blocker';
import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../../../../slices/shell-slice';
import {createOidcPath} from '../../../../../utils/create-oidc-path';
import {ProcessInstanceTaskApiService} from '../../../../process/services/process-instance-task-api-service';
import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../../../../process/enums/process-task-status';

const Schema = yup.object({
    firstName: yup
        .string()
        .trim()
        .required('Bitte einen Vornamen angeben.'),
    lastName: yup
        .string()
        .trim()
        .required('Bitte einen Nachnamen angeben.'),
    email: yup
        .string()
        .trim()
        .email('Bitte eine gültige E-Mail-Adresse angeben.')
        .required('Bitte eine E-Mail-Adresse angeben.'),
    enabled: yup
        .boolean()
        .required(),
    systemRoleId: yup
        .number()
        .nullable()
        .required('Bitte eine Systemrolle auswählen.'),
});

const DELETION_BLOCKING_TASK_STATUSES = new Set<ProcessTaskStatus>([
    ProcessTaskStatus.Running,
    ProcessTaskStatus.Paused,
    ProcessTaskStatus.Restarted,
]);

export function UserDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const confirm = useConfirm();

    const {
        item: user,
        isNewItem: isNewUser,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;

    useEffect(() => {
        if (isEditable) {
            return;
        }

        dispatch(addSnackbarMessage({
            key: 'access-denied-user-details',
            message: 'Diese Mitarbeiter:in kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.',
            type: SnackbarType.Dismissable,
            severity: SnackbarSeverity.Warning,
        }));

        return () => {
            dispatch(removeSnackbarMessage('access-denied-user-details'));
        };
    }, [dispatch, isEditable]);

    const {
        currentItem: editedUser,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<User>(user, Schema as any);

    const changeBlocker = useChangeBlocker(user, editedUser);

    const [showConfirmDialog, setShowConfirmDialog] = useState(false);
    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [relatedTasks, setRelatedTasks] = useState<ConstraintLinkProps[] | undefined>(undefined);
    const [systemRoleOptions, setSystemRoleOptions] = useState<Array<{ label: string; value: number }>>([]);
    const [isSystemRolesLoading, setIsSystemRolesLoading] = useState(true);
    const [hasSystemRolesLoadingError, setHasSystemRolesLoadingError] = useState(false);

    const keycloakAdminConsoleUrl = useMemo(() => {
        if (editedUser?.id == null || editedUser.id.length === 0) {
            return undefined;
        }

        return createOidcPath(`/admin/${AppConfig.oidc.realm}/console/#/realms/${AppConfig.oidc.realm}/users/${editedUser.id}`);
    }, [editedUser?.id]);

    const canEditUser = isEditable && !user?.deletedInIdp;

    useEffect(() => {
        setIsSystemRolesLoading(true);
        setHasSystemRolesLoadingError(false);

        new SystemRolesApiService()
            .listAll()
            .then((result) => {
                const options = result.content
                    .map((role) => ({
                        label: role.name,
                        value: role.id,
                    }))
                    .sort((a, b) => a.label.localeCompare(b.label));
                setSystemRoleOptions(options);
            })
            .catch((err) => {
                setHasSystemRolesLoadingError(true);
                dispatch(showApiErrorSnackbar(err, 'Beim Laden der Systemrollen ist ein Fehler aufgetreten.'));
            })
            .finally(() => {
                setIsSystemRolesLoading(false);
            });
    }, [dispatch]);

    if (editedUser == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (!validate()) {
            dispatch(showErrorSnackbar('Bitte prüfen Sie die Pflichtfelder.'));
            return;
        }

        if (systemRoleOptions.length === 0) {
            dispatch(showErrorSnackbar('Es sind keine Systemrollen verfügbar.'));
            return;
        }

        setIsBusy(true);

        if (isNewUser) {
            new UsersApiService()
                .create(editedUser)
                .then((createdUser) => {
                    setItem(createdUser);
                    reset();

                    dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich erstellt.'));

                    setTimeout(() => {
                        navigate(`/users/${createdUser.id}`, {
                            replace: true,
                        });
                    }, 0);
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Beim Erstellen der Mitarbeiter:in ist ein Fehler aufgetreten.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        } else {
            new UsersApiService()
                .update(user!.id, editedUser)
                .then((savedUser) => {
                    setItem(savedUser);
                    reset();

                    dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich gespeichert.'));
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Beim Speichern der Mitarbeiter:in ist ein Fehler aufgetreten.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const checkAndHandleDelete = async () => {
        if (isNewUser || user?.id == null || user.deletedInIdp) {
            return;
        }

        setIsBusy(true);

        try {
            const tasks = await new ProcessInstanceTaskApiService().listAll({
                assignedUserId: user.id,
            });

            const blockingTasks = tasks.content.filter((task) =>
                task.assignedUserId === user.id &&
                DELETION_BLOCKING_TASK_STATUSES.has(task.status),
            );

            if (blockingTasks.length === 0) {
                setShowConfirmDialog(true);
                return;
            }

            const maxVisibleLinks = 5;
            const processedLinks: ConstraintLinkProps[] = blockingTasks
                .slice(0, maxVisibleLinks)
                .map((task) => ({
                    label: `Aufgabe #${task.id} in Vorgang #${task.processInstanceId} (${ProcessTaskStatusLabels[task.status]})`,
                    to: `/tasks/${task.processInstanceId}/${task.id}`,
                }));

            if (blockingTasks.length > maxVisibleLinks) {
                processedLinks.push({
                    label: 'Weitere zugewiesene Aufgaben anzeigen…',
                    to: '/tasks',
                });
            }

            setRelatedTasks(processedLinks);
            setShowConstraintDialog(true);
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Beim Prüfen zugewiesener Aufgaben ist ein Fehler aufgetreten.'));
        } finally {
            setIsBusy(false);
        }
    };

    const handleDelete = () => {
        if (isNewUser || user?.id == null || user.deletedInIdp) {
            return;
        }

        setIsBusy(true);

        new UsersApiService()
            .destroy(user.id)
            .then(() => {
                reset();
                navigate('/users', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich gelöscht.'));
            })
            .catch((err) => {
                if (isApiError(err) && 'message' in err.details) {
                    dispatch(showErrorSnackbar(err.details.message));
                } else {
                    dispatch(showErrorSnackbar('Beim Löschen der Mitarbeiter:in ist ein Fehler aufgetreten.'));
                }
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    const handlePasswordReset = () => {
        if (isNewUser || user?.id == null || user.deletedInIdp) {
            return;
        }

        confirm({
            title: 'Passwort zurücksetzen',
            children: (
                <>
                    <Typography>
                        Möchten Sie das Passwort für diese Mitarbeiter:in wirklich zurücksetzen?
                    </Typography>
                    <Typography>
                        Dazu wird an die hinterlegte E-Mail-Adresse ein Link zum Zurücksetzen des Passworts gesendet.
                    </Typography>
                </>
            ),
        })
            .then((isConfirmed) => {
                if (!isConfirmed) {
                    return;
                }

                setIsBusy(true);

                new UsersApiService()
                    .resetPassword(user.id)
                    .then(() => {
                        dispatch(showSuccessSnackbar('Der Passwort-Zurücksetzen-Link wurde erfolgreich versendet.'));
                    })
                    .catch((err) => {
                        dispatch(showApiErrorSnackbar(err, 'Beim Zurücksetzen des Passworts ist ein Fehler aufgetreten.'));
                    })
                    .finally(() => {
                        setIsBusy(false);
                    });
            });
    };

    return (
        <>
            <Box
                sx={{
                    pt: 1.5,
                }}
            >
                <Typography
                    variant="h5"
                    sx={{mb: 1}}
                >
                    Mitarbeiter:in verwalten
                </Typography>

                <Typography sx={{mb: 3, maxWidth: 900}}>
                    Hier können Sie die in Gover relevanten Basisdaten und die Systemrolle dieser Mitarbeiter:in pflegen.
                </Typography>

                <Grid
                    container
                    columnSpacing={2}
                >
                    <Grid size={6}>
                        <TextFieldComponent
                            label="Vorname"
                            value={editedUser.firstName}
                            onChange={handleInputChange('firstName')}
                            onBlur={handleInputBlur('firstName')}
                            error={errors.firstName}
                            disabled={isBusy || !canEditUser}
                            required
                        />
                    </Grid>
                    <Grid size={6}>
                        <TextFieldComponent
                            label="Nachname"
                            value={editedUser.lastName}
                            onChange={handleInputChange('lastName')}
                            onBlur={handleInputBlur('lastName')}
                            error={errors.lastName}
                            disabled={isBusy || !canEditUser}
                            required
                        />
                    </Grid>
                    <Grid size={6}>
                        <TextFieldComponent
                            label="E-Mail-Adresse"
                            value={editedUser.email}
                            onChange={handleInputChange('email')}
                            onBlur={handleInputBlur('email')}
                            error={errors.email}
                            disabled={isBusy || !canEditUser}
                            required
                        />
                    </Grid>
                    <Grid size={6}>
                        <SelectFieldComponent
                            label="Systemrolle"
                            value={editedUser.systemRoleId}
                            onChange={handleInputChange('systemRoleId')}
                            options={systemRoleOptions}
                            placeholder="Systemrolle auswählen"
                            emptyStatePlaceholder={
                                isSystemRolesLoading
                                    ? 'Systemrollen werden geladen…'
                                    : hasSystemRolesLoadingError
                                        ? 'Systemrollen konnten nicht geladen werden'
                                        : 'Keine Systemrollen vorhanden'
                            }
                            hint={
                                hasSystemRolesLoadingError
                                    ? 'Die Rollen konnten nicht geladen werden. Bitte laden Sie die Seite neu oder wenden Sie sich an eine Administrator:in.'
                                    : undefined
                            }
                            error={errors.systemRoleId}
                            disabled={isBusy || !canEditUser || isSystemRolesLoading}
                            required
                        />
                    </Grid>
                    <Grid size={6}>
                        <CheckboxFieldComponent
                            label="Konto aktiviert"
                            value={editedUser.enabled}
                            onChange={handleInputChange('enabled')}
                            error={errors.enabled}
                            variant="switch"
                            disabled={isBusy || !canEditUser}
                            busy={isBusy}
                        />
                    </Grid>
                </Grid>

                {
                    editedUser.deletedInIdp ? (
                        <Typography
                            variant="body2"
                            sx={{
                                mt: 2,
                                mb: 3,
                                maxWidth: 900,
                                color: 'warning.dark',
                            }}
                        >
                            Dieses Konto wurde im Identity Provider bereits gelöscht. Änderungen, Passwort-Resets und weitere Verwaltungsaktionen sind nicht mehr möglich.
                            Der Datensatz bleibt in Gover erhalten, damit bestehende Zuordnungen und Historien nachvollziehbar bleiben.
                        </Typography>
                    ) : (
                        !isNewUser &&
                        <Box
                            sx={{
                                mt: 2,
                                mb: 3,
                                display: 'flex',
                                flexWrap: 'wrap',
                                alignItems: 'center',
                                gap: 2,
                            }}
                        >
                            <Typography
                                variant="body2"
                                color="text.secondary"
                                sx={{maxWidth: 900}}
                            >
                                Weitergehende konto- oder sicherheitsbezogene Änderungen erfolgen direkt in Keycloak. Benutzer-ID:{' '}
                                <Box
                                    component="span"
                                    sx={{
                                        fontFamily: 'monospace',
                                        fontSize: '0.95em',
                                    }}
                                >
                                    {editedUser.id}
                                </Box>
                                . Wenn die Mitarbeiter:in keinen Zugriff mehr benötigt, können Sie das Konto im IDP löschen.
                            </Typography>

                            {
                                isEditable && keycloakAdminConsoleUrl != null &&
                                <Button
                                    component="a"
                                    href={keycloakAdminConsoleUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    variant="text"
                                    size="small"
                                    startIcon={<OpenInNewIcon />}
                                    sx={{whiteSpace: 'nowrap'}}
                                >
                                    In Keycloak öffnen
                                </Button>
                            }
                        </Box>
                    )
                }

                {
                    canEditUser &&
                    <Box
                        sx={{
                            display: 'flex',
                            marginTop: 5,
                            gap: 2,
                        }}
                    >
                        <Button
                            variant="contained"
                            color="primary"
                            startIcon={<SaveOutlinedIcon />}
                            disabled={isBusy || hasNotChanged || isSystemRolesLoading || systemRoleOptions.length === 0}
                            onClick={handleSave}
                        >
                            Speichern
                        </Button>

                        {
                            !isNewUser &&
                            <Button
                                color="error"
                                disabled={isBusy || hasNotChanged}
                                onClick={() => {
                                    reset();
                                }}
                            >
                                Zurücksetzen
                            </Button>
                        }

                        {
                            !isNewUser &&
                            <>
                                <Button
                                    variant="outlined"
                                    color="primary"
                                    startIcon={<LockResetOutlinedIcon />}
                                    disabled={isBusy}
                                    onClick={handlePasswordReset}
                                    sx={{
                                        ml: 'auto',
                                    }}
                                >
                                    Passwort zurücksetzen
                                </Button>
                                <Button
                                    onClick={checkAndHandleDelete}
                                    variant="outlined"
                                    color="error"
                                    startIcon={<Delete />}

                                    disabled={isBusy}
                                >
                                    Löschen
                                </Button>
                            </>
                        }
                    </Box>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Mitarbeiter:in löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={editedUser.email || editedUser.id}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Mitarbeiter:in wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>

                <Typography
                    sx={{
                        mt: 2,
                        color: 'text.secondary',
                    }}
                >
                    Das Konto wird im Identity Provider gelöscht und in Gover als gelöscht markiert.
                    Anschließend sind keine weiteren Anmeldungen oder Passwort-Resets mehr möglich.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Diese Mitarbeiter:in kann (noch) nicht gelöscht werden, da ihr noch nicht abgeschlossene Aufgaben zugewiesen sind."
                solutionText="Bitte weisen Sie die Aufgaben einer anderen Mitarbeiter:in zu oder schließen Sie sie ab und versuchen Sie es erneut:"
                links={relatedTasks}
            />
        </>
    );
}
