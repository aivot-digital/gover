import {Box, Button, Grid, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintDialog} from '../../../../dialogs/constraint-dialog/constraint-dialog';
import {ConstraintLinkProps} from '../../../../dialogs/constraint-dialog/constraint-link-props';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {ThemeResponseDTO} from '../../../themes/models/theme';
import {ThemesApiService} from '../../../themes/themes-api-service';
import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../../../slices/shell-slice';
import {TeamsApiService} from '../../services/teams-api-service';
import {TeamEntity} from "../../entities/team-entity";

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

    const api = useApi();
    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<TeamEntity, void>;

    useEffect(() => {
        if (isEditable) {
            return;
        }

        dispatch(addSnackbarMessage({
            severity: SnackbarSeverity.Warning,
            type: SnackbarType.Dismissable,
            message: 'Dieser Fachbereich kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.',
            key: 'no-edit-permission-team',
        }));

        return () => {
            dispatch(removeSnackbarMessage('no-edit-permission-team'));
        };
    }, [isEditable]);

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

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedApplications, setRelatedApplications] = useState<ConstraintLinkProps[] | undefined>(undefined);
    const [availableThemes, setAvailableThemes] = useState<ThemeResponseDTO[]>();

    useEffect(() => {
        new ThemesApiService(api)
            .listAll()
            .then((result) => {
                setAvailableThemes(result.content);
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Laden der verfügbaren Fabschemata.'));
            });
    }, []);

    if (team == null || availableThemes == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (team != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            if (team.id === 0) {
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
        if (team.id === 0) return;

        setIsBusy(true);
        apiService.destroy(team.id)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                navigate('/teams', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Der Fachbereich wurde erfolgreich gelöscht.'));
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
                Öffentliche Informationen des Fachbereichs
            </Typography>
            <Typography
                sx={{
                    mb: 2,
                    maxWidth: 900,
                }}
            >
                Hinterlegen Sie grundsätzliche Informationen über diesen Fachbereich. Diese Informationen werden in der Anwendung angezeigt und sind für die Nutzer:innen sichtbar.
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
                <Button
                    onClick={handleSave}
                    disabled={isBusy || hasNotChanged || !isEditable}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon />}
                >
                    Speichern
                </Button>

                {
                    team.id !== 0 &&
                    <Button
                        onClick={() => {
                            reset();
                        }}
                        disabled={isBusy || hasNotChanged || !isEditable}
                        color="error"
                    >
                        Zurücksetzen
                    </Button>
                }

                {
                    team.id !== 0 &&
                    <Button
                        variant="outlined"
                        onClick={() => {
                            setConfirmDeleteAction(() => confirmDelete);
                        }}
                        disabled={isBusy || !isEditable}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<DeleteOutlinedIcon />}
                    >
                        Löschen
                    </Button>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Fachbereich löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                confirmationText={team.name ?? ''}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diesen Fachbereich wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Dieser Fachbereich kann (noch) nicht gelöscht werden, da er noch für Formulare als entwickelnder, zuständiger oder bewirtschaftender Fachbereich zugewiesen ist."
                solutionText="Bitte übertragen Sie die Formulare an einen anderen Fachbereich und versuchen Sie es erneut:"
                links={relatedApplications}
            />
        </Box>
    );
}