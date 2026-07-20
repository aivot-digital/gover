import {Box, Button, Grid, Typography} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useNavigate} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {TeamsApiService} from '../../services/teams-api-service';
import {TeamEntity} from "../../entities/team-entity";
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {useCheckTeamPermission} from '../../../permissions/hooks/use-permissions';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';

export const TeamSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name des Teams muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name des Teams darf maximal 96 Zeichen lang sein.')
        .required('Der Name des Teams ist ein Pflichtfeld.'),
});

export function TeamsDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
        isNewItem,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<TeamEntity, void>;

    const {
        currentItem: team,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<TeamEntity>(item, TeamSchema as any);

    const apiService = useMemo(() => new TeamsApiService(), []);
    const changeBlocker = useChangeBlocker(item, team);
    const isNewTeam = isNewItem === true;
    const editPermission = isNewTeam ? Permission.TEAM_CREATE : Permission.TEAM_UPDATE;
    const canDeleteTeam = useCheckTeamPermission(
        isNewTeam ? undefined : team?.id,
        Permission.TEAM_DELETE,
    );

    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);

    if (team == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const saveDisabledByPermission = !isEditable;
    const saveDisabledTooltip = saveDisabledByPermission
        ? formatMissingPermissionTooltip(editPermission)
        : undefined;
    const deleteDisabledByPermission = !canDeleteTeam;
    const deleteDisabledTooltip = deleteDisabledByPermission
        ? formatMissingPermissionTooltip(Permission.TEAM_DELETE)
        : undefined;

    const handleSave = () => {
        if (team != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            if (isNewTeam) {
                apiService
                    .create({
                        id: 0,
                        name: team.name ?? 'Unbenanntes Team',
                        created: new Date().toISOString(),
                        updated: new Date().toISOString(),
                    })
                    .then((newDepartment) => {
                        setItem(newDepartment);
                        reset();

                        dispatch(showSuccessSnackbar('Neues Team erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/teams/${newDepartment.id}`, {replace: true});
                        }, 0);
                    })
                    .catch(err => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    })
                    .finally(() => {
                        setIsBusy(false);
                    });
            } else {
                apiService
                    .update(team.id, {
                        id: team.id,
                        name: team.name ?? 'Unbenanntes Team',
                        created: team.created,
                        updated: team.updated,
                    })
                    .then((updatedDepartment) => {
                        setItem(updatedDepartment);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen am Team erfolgreich gespeichert.'));
                    })
                    .catch(err => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    })
                    .finally(() => {
                        setIsBusy(false);
                    });
            }
        }
    };

    const confirmDelete = () => {
        if (isNewTeam) return;

        setIsBusy(true);
        apiService.destroy(team.id)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                navigate('/teams', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Das Team wurde erfolgreich gelöscht.'));
            })
            .catch(() => dispatch(showErrorSnackbar('Beim Löschen ist ein Fehler aufgetreten.')))
            .finally(() => setIsBusy(false));
    };

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{
                    mt: 1.5,
                    mb: 1,
                }}
            >
                Angaben zum Team
            </Typography>
            <Typography
                sx={{
                    mb: 2,
                    maxWidth: 900,
                }}
            >
                Hinterlegen Sie interne Angaben dieses Teams. Diese Angaben werden in Gover zum Beispiel in Listen, Zuweisungen und Auswahlen verwendet.
            </Typography>
            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Name des Teams"
                        value={team.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        required
                        maxCharacters={96}
                        minCharacters={3}
                        error={errors.name}
                        disabled={!isEditable}
                    />
                </Grid>
            </Grid>

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 4,
                    gap: 2,
                }}
            >
                <DisabledTooltip
                    title={saveDisabledTooltip}
                    disabled={isBusy || hasNotChanged || !isEditable}
                >
                    <Button
                        onClick={handleSave}
                        disabled={isBusy || hasNotChanged || !isEditable}
                        variant="contained"
                        color="primary"
                        startIcon={<SaveOutlinedIcon />}
                    >
                        Speichern
                    </Button>
                </DisabledTooltip>

                {
                    !isNewTeam &&
                    <DisabledTooltip
                        title={saveDisabledTooltip}
                        disabled={isBusy || hasNotChanged || !isEditable}
                    >
                        <Button
                            onClick={() => {
                                reset();
                            }}
                            disabled={isBusy || hasNotChanged || !isEditable}
                            color="error"
                        >
                            Zurücksetzen
                        </Button>
                    </DisabledTooltip>
                }

                {
                    !isNewTeam &&
                    <DisabledTooltip
                        title={deleteDisabledTooltip}
                        disabled={isBusy || deleteDisabledByPermission}
                        wrapperSx={{marginLeft: 'auto'}}
                    >
                        <Button
                            variant="outlined"
                            onClick={() => {
                                setConfirmDeleteAction(() => confirmDelete);
                            }}
                            disabled={isBusy || deleteDisabledByPermission}
                            color="error"
                            startIcon={<Delete />}
                        >
                            Löschen
                        </Button>
                    </DisabledTooltip>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Team löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                confirmationText={team.name ?? ''}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie dieses Team wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>
        </Box>
    );
}
