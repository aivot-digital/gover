import {Box, Button, Grid, InputAdornment, TextField, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import OpenInNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import SaveOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import LockResetOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/LockReset';
import FileDownloadOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';
import {useLocation, useNavigate} from 'react-router-dom';
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
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {SelectFieldComponent} from '../../../../../components/select-field-2/select-field-component';
import {CreateUserResponseDTO, UsersApiService} from '../../../users-api-service';
import {useConfirm} from '../../../../../providers/confirm-provider';
import {SystemRolesApiService} from '../../../../system/services/system-roles-api-service';
import {useChangeBlocker} from '../../../../../hooks/use-change-blocker';
import {createOidcPath} from '../../../../../utils/create-oidc-path';
import {ProcessInstanceTaskApiService} from '../../../../process/services/process-instance-task-api-service';
import {ProcessTaskStatus, ProcessTaskStatusLabels} from '../../../../process/enums/process-task-status';
import {InfoDialog} from '../../../../../dialogs/info-dialog/info-dialog';
import {CopyToClipboardButton} from '../../../../../components/copy-to-clipboard-button/copy-to-clipboard-button';
import {downloadTextFile} from '../../../../../utils/download-utils';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import {useHasSystemPermission, useRefreshPermissionSet} from '../../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../../permissions/utils/permission-utils';
import {DisabledTooltip} from '../../../../../components/disabled-tooltip/disabled-tooltip';

const KEYCLOAK_PERSON_NAME_MAX_CHARACTERS = 255;
const KEYCLOAK_EMAIL_MAX_CHARACTERS = 255;
// Mirrors Keycloak's default `person-name-prohibited-characters` validator.
const KEYCLOAK_PERSON_NAME_REGEX = /^[^<>&"$%!#?§;*~/\\|^=\[\]{}()\u0000-\u001F\u007F]+$/;

const createPersonNameSchema = (fieldLabel: 'Vorname' | 'Nachname') => yup
    .string()
    .trim()
    .required(`Bitte einen ${fieldLabel === 'Vorname' ? 'Vornamen' : 'Nachnamen'} angeben.`)
    .max(KEYCLOAK_PERSON_NAME_MAX_CHARACTERS, `${fieldLabel} darf maximal ${KEYCLOAK_PERSON_NAME_MAX_CHARACTERS} Zeichen lang sein.`)
    .matches(
        KEYCLOAK_PERSON_NAME_REGEX,
        {
            excludeEmptyString: true,
            message: `${fieldLabel} enthält unzulässige Zeichen. Bitte entfernen Sie Sonderzeichen wie ^, <, >, &, $, %, !, #, ?, §, ;, *, ~, /, \\, |, =, [ ], { } oder ( ).`,
        },
    );

const Schema = yup.object({
    firstName: createPersonNameSchema('Vorname'),
    lastName: createPersonNameSchema('Nachname'),
    email: yup
        .string()
        .trim()
        .email('Bitte eine gültige E-Mail-Adresse angeben.')
        .max(KEYCLOAK_EMAIL_MAX_CHARACTERS, `Die E-Mail-Adresse darf maximal ${KEYCLOAK_EMAIL_MAX_CHARACTERS} Zeichen lang sein.`)
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

type UserDetailsLocationState = {
    userProvisioningResult?: CreateUserResponseDTO;
};

function sanitizeFilenameSegment(input: string): string {
    return input
        .trim()
        .replace(/[<>:"/\\|?*\u0000-\u001F]/g, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .toLowerCase();
}

export function UserDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const location = useLocation();
    const confirm = useConfirm();
    const refreshPermissionSet = useRefreshPermissionSet();
    const canReadSystemRoles = useHasSystemPermission(Permission.SYSTEM_ROLE_READ);
    const canDeleteUser = useHasSystemPermission(Permission.USER_DELETE);
    const deleteUserDisabledTooltip = formatMissingPermissionTooltip(Permission.USER_DELETE);
    const systemRoleReadDisabledTooltip = formatMissingPermissionTooltip(Permission.SYSTEM_ROLE_READ);

    const {
        item: user,
        isNewItem: isNewUser,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<User, undefined>;
    const updateUserDisabledTooltip = formatMissingPermissionTooltip(isNewUser ? Permission.USER_CREATE : Permission.USER_UPDATE);

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
    const [sendInitialCredentialsByEmail, setSendInitialCredentialsByEmail] = useState(false);
    const [userProvisioningResult, setUserProvisioningResult] = useState<CreateUserResponseDTO | null>(null);

    const keycloakAdminConsoleUrl = useMemo(() => {
        if (editedUser?.id == null || editedUser.id.length === 0) {
            return undefined;
        }

        const realm = encodeURIComponent(AppConfig.oidc.realm);
        const userId = encodeURIComponent(editedUser.id);

        return createOidcPath(`/admin/${realm}/console/#/${realm}/users/${userId}/settings`);
    }, [editedUser?.id]);

    const canEditUser = isEditable && !user?.deletedInIdp && !user?.artificialUser;
    const canResetUserPassword = canEditUser && !isNewUser;
    const canDeleteExistingUser = canDeleteUser && !isNewUser && !user?.deletedInIdp && !user?.artificialUser;
    const saveDisabled =
        isBusy ||
        hasNotChanged ||
        !canEditUser ||
        !canReadSystemRoles ||
        isSystemRolesLoading ||
        systemRoleOptions.length === 0;
    const saveDisabledTooltip =
        !canEditUser
            ? updateUserDisabledTooltip
            : !canReadSystemRoles
                ? systemRoleReadDisabledTooltip
                : undefined;

    const refreshPermissionsAfterUserRoleChange = () => {
        // Effective permissions may include system grants inherited through deputy assignments.
        // The frontend cannot know whether the edited user is currently represented by the active user.
        refreshPermissionSet({broadcast: true})
            .catch((err) => dispatch(showApiErrorSnackbar(
                err,
                'Die Berechtigungen konnten nach der Änderung der Mitarbeiter:in nicht aktualisiert werden.',
            )));
    };

    useEffect(() => {
        if (!canReadSystemRoles) {
            setSystemRoleOptions([]);
            setHasSystemRolesLoadingError(false);
            setIsSystemRolesLoading(false);
            return;
        }

        let isActive = true;
        setIsSystemRolesLoading(true);
        setHasSystemRolesLoadingError(false);

        new SystemRolesApiService()
            .listAll()
            .then((result) => {
                if (!isActive) {
                    return;
                }

                const options = result.content
                    .map((role) => ({
                        label: role.name,
                        value: role.id,
                    }))
                    .sort((a, b) => a.label.localeCompare(b.label));
                setSystemRoleOptions(options);
            })
            .catch((err) => {
                if (!isActive) {
                    return;
                }

                setHasSystemRolesLoadingError(true);
                dispatch(showApiErrorSnackbar(err, 'Beim Laden der Systemrollen ist ein Fehler aufgetreten.'));
            })
            .finally(() => {
                if (isActive) {
                    setIsSystemRolesLoading(false);
                }
            });

        return () => {
            isActive = false;
        };
    }, [canReadSystemRoles, dispatch]);

    useEffect(() => {
        const navigationState = location.state as UserDetailsLocationState | null;
        if (navigationState?.userProvisioningResult?.initialCredentials == null) {
            return;
        }

        setUserProvisioningResult(navigationState.userProvisioningResult);
        navigate(location.pathname, {
            replace: true,
            state: null,
        });
    }, [location.pathname, location.state, navigate]);

    useEffect(() => {
        if (isNewUser && user?.id === '') {
            setSendInitialCredentialsByEmail(false);
        }
    }, [isNewUser, user?.id]);

    if (editedUser == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const initialCredentials = userProvisioningResult?.initialCredentials;
    const hasInitialCredentialsDeliveryError = userProvisioningResult?.initialCredentialsDeliveryError != null;

    const buildInitialCredentialsExport = () => {
        if (initialCredentials == null) {
            return null;
        }

        return [
            'Initiale Zugangsdaten für Prosuna',
            '',
            `Name: ${initialCredentials.fullName}`,
            `E-Mail-Adresse: ${initialCredentials.email}`,
            `Systemrolle: ${initialCredentials.systemRoleName}`,
            `Temporäres Passwort: ${initialCredentials.temporaryPassword}`,
            '',
            'Hinweis: Nur das temporäre Passwort wird einmalig angezeigt.',
            'Beim ersten Login muss die Mitarbeiter:in ein neues Passwort vergeben, die E-Mail-Adresse bestätigen und gegebenenfalls eine Zwei-Faktor-Authentifizierung einrichten, sofern dies in der System-Konfiguration vorgesehen ist.',
        ].join('\n');
    };

    const handleDownloadInitialCredentials = () => {
        const content = buildInitialCredentialsExport();
        if (content == null || initialCredentials == null) {
            return;
        }

        const filenameSegment = sanitizeFilenameSegment(initialCredentials.fullName);
        const filename = filenameSegment.length > 0
            ? `prosuna-zugangsdaten-${filenameSegment}.txt`
            : 'prosuna-zugangsdaten.txt';

        downloadTextFile(filename, content, 'text/plain;charset=utf-8');
    };

    const renderInitialCredentialField = (label: string, value: string, copyLabel: string) => (
        <TextField
            label={label}
            value={value}
            fullWidth
            InputProps={{
                readOnly: true,
                endAdornment: (
                    <InputAdornment position="end">
                        <CopyToClipboardButton
                            text={value}
                            tooltip={`${copyLabel} kopieren`}
                            copiedTooltip={`${copyLabel} kopiert`}
                            ariaLabel={`${copyLabel} kopieren`}
                        />
                    </InputAdornment>
                ),
            }}
        />
    );

    const handleSave = () => {
        if (!canEditUser || !canReadSystemRoles) {
            return;
        }

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
                .provision({
                    user: editedUser,
                    sendInitialCredentialsByEmail: sendInitialCredentialsByEmail,
                })
                .then((result) => {
                    setItem(result.user);
                    reset();
                    refreshPermissionsAfterUserRoleChange();
                    setSendInitialCredentialsByEmail(false);

                    if (result.initialCredentialsSentByEmail) {
                        dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich erstellt und die initialen Zugangsdaten wurden per E-Mail versendet.'));
                    } else if (result.initialCredentialsDeliveryError != null) {
                        dispatch(showErrorSnackbar('Die Mitarbeiter:in wurde erstellt, die E-Mail mit den initialen Zugangsdaten konnte jedoch nicht versendet werden.'));
                    } else {
                        dispatch(showSuccessSnackbar('Die Mitarbeiter:in wurde erfolgreich erstellt. Bitte geben Sie die initialen Zugangsdaten manuell weiter.'));
                    }

                    setTimeout(() => {
                        navigate(`/users/${result.user.id}`, {
                            replace: true,
                            state: result.initialCredentials != null
                                ? {
                                    userProvisioningResult: result,
                                } satisfies UserDetailsLocationState
                                : undefined,
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
                    refreshPermissionsAfterUserRoleChange();

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
        if (!canDeleteUser || isNewUser || user?.id == null || user.deletedInIdp || user.artificialUser) {
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
        if (!canDeleteUser || isNewUser || user?.id == null || user.deletedInIdp || user.artificialUser) {
            return;
        }

        setIsBusy(true);

        new UsersApiService()
            .destroy(user.id)
            .then(() => {
                reset();
                refreshPermissionsAfterUserRoleChange();
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
        if (!canResetUserPassword || user?.id == null || user.deletedInIdp) {
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
                    {isNewUser ? 'Mitarbeiter:in anlegen' : 'Mitarbeiter:in verwalten'}
                </Typography>

                {
                    editedUser.artificialUser &&
                    <AlertComponent
                        color="info"
                        sx={{
                            my: 2,
                        }}
                    >
                        Bei dieser Mitarbeiter:in handelt es sich um einen Beispieldatensatz.
                        Eine Änderung der Daten, sowie ein Löschen ist nicht möglich.
                    </AlertComponent>
                }

                <Typography sx={{mb: 3, maxWidth: 900}}>
                    {
                        isNewUser
                            ? 'Legen Sie hier eine neue Mitarbeiter:in an. Für das Konto wird ein temporäres Passwort gesetzt. Beim ersten Login muss dieses ersetzt und die E-Mail-Adresse bestätigt werden. Je nach System-Konfiguration kann zusätzlich die Einrichtung einer Zwei-Faktor-Authentifizierung erforderlich sein.'
                            : 'Hier können Sie die in Prosuna relevanten Basisdaten und die Systemrolle dieser Mitarbeiter:in pflegen.'
                    }
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
                            maxCharacters={KEYCLOAK_PERSON_NAME_MAX_CHARACTERS}
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
                            maxCharacters={KEYCLOAK_PERSON_NAME_MAX_CHARACTERS}
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
                            maxCharacters={KEYCLOAK_EMAIL_MAX_CHARACTERS}
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
                                !canReadSystemRoles
                                    ? 'Keine Berechtigung zur Einsicht'
                                    : isSystemRolesLoading
                                    ? 'Systemrollen werden geladen…'
                                    : hasSystemRolesLoadingError
                                        ? 'Systemrollen konnten nicht geladen werden'
                                        : 'Keine Systemrollen vorhanden'
                            }
                            hint={
                                !canReadSystemRoles
                                    ? systemRoleReadDisabledTooltip
                                    : hasSystemRolesLoadingError
                                    ? 'Die Rollen konnten nicht geladen werden. Bitte laden Sie die Seite neu oder wenden Sie sich an eine Administrator:in.'
                                    : undefined
                            }
                            error={errors.systemRoleId}
                            disabled={isBusy || !canEditUser || !canReadSystemRoles || isSystemRolesLoading}
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
                    {
                        isNewUser &&
                        <Grid size={12}>
                            <CheckboxFieldComponent
                                label="Initiale Zugangsdaten automatisch per E-Mail senden"
                                value={sendInitialCredentialsByEmail}
                                onChange={setSendInitialCredentialsByEmail}
                                variant="switch"
                                disabled={isBusy || !canEditUser}
                                busy={isBusy}
                                hint="Wenn deaktiviert, wird das temporäre Passwort nach der Anlage einmalig angezeigt."
                            />
                        </Grid>
                    }
                </Grid>

                {
                    editedUser.deletedInIdp ? (
                        <AlertComponent
                            color="warning"
                            sx={{
                                my: 3,
                            }}
                        >
                            Dieses Konto wurde im Identity Provider bereits gelöscht. Änderungen, Passwort-Resets und weitere Verwaltungsaktionen sind nicht mehr möglich.
                            Der Datensatz bleibt in Prosuna erhalten, damit bestehende Zuordnungen und Historien nachvollziehbar bleiben.
                        </AlertComponent>
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
                                Weitergehende konto- oder sicherheitsbezogene Änderungen erfolgen direkt in Keycloak.
                                Benutzer-ID:{' '}
                                <Box
                                    component="span"
                                    sx={{
                                        fontFamily: 'monospace',
                                        fontSize: '0.95em',
                                    }}
                                >
                                    {editedUser.id}
                                </Box>
                                . Wenn die Mitarbeiter:in keinen Zugriff mehr benötigt, können Sie das Konto im IdP
                                löschen.
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
                                    startIcon={<OpenInNewIcon/>}
                                    sx={{whiteSpace: 'nowrap'}}
                                >
                                    In Keycloak öffnen
                                </Button>
                            }
                        </Box>
                    )
                }

                <Box
                    sx={{
                        display: 'flex',
                        marginTop: 5,
                        gap: 2,
                    }}
                >
                    <DisabledTooltip
                        disabled={saveDisabled && saveDisabledTooltip != null}
                        title={saveDisabledTooltip}
                    >
                        <Button
                            variant="contained"
                            color="primary"
                            startIcon={<SaveOutlinedIcon/>}
                            disabled={saveDisabled}
                            onClick={handleSave}
                        >
                            {isNewUser ? 'Mitarbeiter:in anlegen' : 'Speichern'}
                        </Button>
                    </DisabledTooltip>

                    {
                        !isNewUser &&
                        <DisabledTooltip
                            disabled={!canEditUser}
                            title={updateUserDisabledTooltip}
                        >
                            <Button
                                color="error"
                                disabled={isBusy || hasNotChanged || !canEditUser}
                                onClick={() => {
                                    reset();
                                }}
                            >
                                Zurücksetzen
                            </Button>
                        </DisabledTooltip>
                    }

                    {
                        !isNewUser &&
                        <>
                            <DisabledTooltip
                                disabled={!canResetUserPassword}
                                title={updateUserDisabledTooltip}
                                wrapperSx={{
                                    ml: 'auto',
                                }}
                            >
                                <Button
                                    variant="outlined"
                                    color="primary"
                                    startIcon={<LockResetOutlinedIcon/>}
                                    disabled={isBusy || !canResetUserPassword}
                                    onClick={handlePasswordReset}
                                >
                                    Passwort zurücksetzen
                                </Button>
                            </DisabledTooltip>
                            <DisabledTooltip
                                disabled={!canDeleteExistingUser}
                                title={user?.deletedInIdp ? undefined : deleteUserDisabledTooltip}
                            >
                                <Button
                                    onClick={checkAndHandleDelete}
                                    variant="outlined"
                                    color="error"
                                    startIcon={<Delete/>}

                                    disabled={isBusy || !canDeleteExistingUser}
                                >
                                    Löschen
                                </Button>
                            </DisabledTooltip>
                        </>
                    }
                </Box>
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
                    Möchten Sie diese Mitarbeiter:in wirklich löschen? Diese Aktion kann nicht rückgängig gemacht
                    werden.
                </Typography>

                <Typography
                    sx={{
                        mt: 2,
                        color: 'text.secondary',
                    }}
                >
                    Das Konto wird im Identity Provider gelöscht und in Prosuna als gelöscht markiert.
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

            {
                initialCredentials != null &&
                <InfoDialog
                    open
                    onClose={() => setUserProvisioningResult(null)}
                    title="Initiale Zugangsdaten"
                    severity={hasInitialCredentialsDeliveryError ? 'warning' : 'info'}
                    actions={
                        <Button
                            startIcon={<FileDownloadOutlinedIcon/>}
                            onClick={handleDownloadInitialCredentials}
                        >
                            Als Textdatei herunterladen
                        </Button>
                    }
                >
                    <Typography>
                        {
                            hasInitialCredentialsDeliveryError
                                ? 'Die Mitarbeiter:in wurde erstellt, die initialen Zugangsdaten konnten jedoch nicht automatisch per E-Mail versendet werden. Bitte geben Sie die folgenden Informationen manuell weiter.'
                                : 'Die Mitarbeiter:in wurde erstellt. Bitte geben Sie die folgenden Informationen manuell weiter.'
                        }
                    </Typography>

                    <Grid
                        container
                        spacing={2}
                        sx={{mt: 0.5}}
                    >
                        <Grid size={6}>
                            {renderInitialCredentialField('Name', initialCredentials.fullName, 'Name')}
                        </Grid>
                        <Grid size={6}>
                            {renderInitialCredentialField('E-Mail-Adresse', initialCredentials.email, 'E-Mail-Adresse')}
                        </Grid>
                        <Grid size={6}>
                            {renderInitialCredentialField('Systemrolle', initialCredentials.systemRoleName, 'Systemrolle')}
                        </Grid>
                        <Grid size={6}>
                            {renderInitialCredentialField('Temporäres Passwort', initialCredentials.temporaryPassword, 'Temporäres Passwort')}
                        </Grid>
                    </Grid>

                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{mt: 2}}
                    >
                        Das temporäre Passwort wird an dieser Stelle einmalig angezeigt. Name, E-Mail-Adresse und
                        Systemrolle können später weiterhin im Profil eingesehen und geändert werden. Die Mitarbeiter:in
                        muss beim ersten Login ein neues Passwort vergeben, die E-Mail-Adresse bestätigen und
                        gegebenenfalls eine Zwei-Faktor-Authentifizierung einrichten, sofern dies in der
                        System-Konfiguration vorgesehen ist.
                    </Typography>
                </InfoDialog>
            }
        </>
    );
}
