import {Box, Button, Grid, Typography} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import {Department} from '../../models/department';
import {DepartmentsApiService} from '../../departments-api-service';
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {RichTextEditorComponentView} from "../../../../components/richt-text-editor/rich-text-editor.component.view";
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import {useChangeBlocker} from "../../../../hooks/use-change-blocker";
import {useFormManager} from '../../../../hooks/use-form-manager';
import {FormsApiService} from "../../../forms/forms-api-service";
import {ConfirmDialog} from "../../../../dialogs/confirm-dialog/confirm-dialog";
import {ConstraintDialog} from "../../../../dialogs/constraint-dialog/constraint-dialog";
import {ConstraintLinkProps} from "../../../../dialogs/constraint-dialog/constraint-link-props";
import * as yup from "yup";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";

export const DepartmentSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, "Der Name des Fachbereichs muss mindestens 3 Zeichen lang sein.")
        .max(96, "Der Name des Fachbereichs darf maximal 96 Zeichen lang sein.")
        .required("Der Name des Fachbereichs ist ein Pflichtfeld."),
    address: yup.string()
        .trim()
        .min(3, "Die Adresse muss mindestens 3 Zeichen lang sein.")
        .max(255, "Die Adresse darf maximal 255 Zeichen lang sein.")
        .required("Die Adresse ist ein Pflichtfeld."),
    specialSupportAddress: yup.string()
        .trim()
        .email("Bitte eine gültige E-Mail-Adresse eingeben.")
        .max(255, "Die E-Mail-Adresse darf maximal 255 Zeichen lang sein.")
        .required("Die E-Mail-Adresse für fachliche Unterstützung ist ein Pflichtfeld."),
    technicalSupportAddress: yup.string()
        .trim()
        .email("Bitte eine gültige E-Mail-Adresse eingeben.")
        .max(255, "Die E-Mail-Adresse darf maximal 255 Zeichen lang sein.")
        .required("Die E-Mail-Adresse für technische Unterstützung ist ein Pflichtfeld."),
    imprint: yup.string()
        .trim()
        .min(10, "Das Impressum muss mindestens 10 Zeichen lang sein.")
        .required("Das Impressum ist ein Pflichtfeld."),
    privacy: yup.string()
        .trim()
        .min(10, "Die Datenschutzerklärung muss mindestens 10 Zeichen lang sein.")
        .required("Die Datenschutzerklärung ist ein Pflichtfeld."),
    accessibility: yup.string()
        .trim()
        .min(10, "Die Barrierefreiheitserklärung muss mindestens 10 Zeichen lang sein.")
        .required("Die Barrierefreiheitserklärung ist ein Pflichtfeld."),
    departmentMail: yup.string()
        .optional()
        .nullable()
        .test("valid-email-list", "Bitte eine oder mehrere gültige E-Mail-Adressen, durch Komma getrennt, eingeben.", (val) => {
            if (!val) return true;
            return val.split(',').every(email => /\S+@\S+\.\S+/.test(email.trim()));
        }),
});

export function DepartmentsDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const api = useApi();
    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Department, undefined>;

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<Department>(item, DepartmentSchema as any);

    const apiService = useMemo(() => new DepartmentsApiService(api), [api]);
    const department = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedApplications, setRelatedApplications] = useState<ConstraintLinkProps[] | undefined>(undefined);

    if (department == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (department != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar("Bitte überprüfen Sie Ihre Eingaben."));
                return;
            }

            setIsBusy(true);

            if (department.id === 0) {
                apiService
                    .create(department)
                    .then((newDepartment) => {
                        setItem(newDepartment);
                        reset();

                        dispatch(showSuccessSnackbar('Neuer Fachbereich erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/departments/${newDepartment.id}`, { replace: true });
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
                    .update(department.id, department)
                    .then((updatedDepartment) => {
                        setItem(updatedDepartment);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen an Fachbereich erfolgreich gespeichert.'));
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

    const checkAndHandleDelete = async () => {
        if (department.id === 0) return;

        setIsBusy(true);
        try {
            const formsApi = new FormsApiService(api);
            const developingForms = await formsApi.list(0, 999, undefined, undefined, { developingDepartmentId: department.id });
            const managingForms = await formsApi.list(0, 999, undefined, undefined, { managingDepartmentId: department.id });
            const responsibleForms = await formsApi.list(0, 999, undefined, undefined, { responsibleDepartmentId: department.id });

            const uniqueForms = Array.from(
                new Map(
                    [...developingForms.content, ...managingForms.content, ...responsibleForms.content]
                        .map(form => [form.id, form])
                ).values()
            );

            if (uniqueForms.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = uniqueForms.slice(0, maxVisibleLinks).map(f => ({
                    label: f.title,
                    to: `/forms/${f.id}`
                }));

                if (uniqueForms.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: "Weitere Formulare anzeigen…",
                        to: `/departments/${department.id}/forms`
                    });
                }

                setRelatedApplications(processedLinks);
                setShowConstraintDialog(true);
            } else {
                setConfirmDeleteAction(() => confirmDelete);
            }
        } catch (error) {
            console.error(error);
            dispatch(showErrorSnackbar('Fehler beim Prüfen der Löschbarkeit.'));
        } finally {
            setIsBusy(false);
        }
    };

    const confirmDelete = () => {
        if (department.id === 0) return;

        setIsBusy(true);
        apiService.destroy(department.id)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                navigate('/departments', {
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
                sx={{mt: 1.5, mb: 1}}
            >
                Öffentliche Informationen des Fachbereichs
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Hinterlegen Sie grundsätzliche Informationen über diesen Fachbereich. Diese Informationen werden in der Anwendung angezeigt und sind für die Nutzer:innen sichtbar.
            </Typography>

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={6}
                >
                    <TextFieldComponent
                        label="Name des Fachbereichs"
                        value={department.name}
                        onChange={handleInputChange("name")}
                        onBlur={handleInputBlur("name")}
                        required
                        maxCharacters={96}
                        minCharacters={3}
                        error={errors.name}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={6}
                />
                <Grid
                    item
                    xs={12}
                    lg={6}
                >
                    <TextFieldComponent
                        label="Adresse des Fachbereichs"
                        value={department.address}
                        onChange={handleInputChange("address")}
                        onBlur={handleInputBlur("address")}
                        required
                        maxCharacters={255}
                        multiline
                        rows={3}
                        error={errors.address}
                    />
                </Grid>
            </Grid>

            <Typography
                variant="h6"
                sx={{
                    mt: 2,
                    mb: 1,
                }}
            >
                Kontakt-E-Mail-Adressen für antragstellende Personen
            </Typography>

            <Typography sx={{mb: 2}}>
                Die hier hinterlegten Kontaktinformationen werden Nutzer:innen zum Beispiel im Hilfe-Dialog zur Verfügung gestellt.
            </Typography>

            <Grid
                container
                columnSpacing={4}
            >
                <Grid
                    item
                    xs={12}
                    lg={6}
                >
                    <TextFieldComponent
                        label="Kontakt-E-Mail-Adresse fachliche Unterstützung"
                        type="email"
                        value={department.specialSupportAddress}
                        onChange={handleInputChange("specialSupportAddress")}
                        onBlur={handleInputBlur("specialSupportAddress")}
                        required
                        maxCharacters={255}
                        error={errors.specialSupportAddress}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={6}
                >
                    <TextFieldComponent
                        label="Kontakt-E-Mail-Adresse technische Unterstützung"
                        type="email"
                        value={department.technicalSupportAddress}
                        onChange={handleInputChange("technicalSupportAddress")}
                        onBlur={handleInputBlur("technicalSupportAddress")}
                        required
                        maxCharacters={255}
                        error={errors.technicalSupportAddress}
                    />
                </Grid>
            </Grid>

            <Typography
                variant="h6"
                sx={{mt: 2, mb: 1}}
            >
                Rechtliche Informationen
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Die folgenden rechtlichen Angaben und Texte können in Formularen referenziert werden.
            </Typography>

            <Box sx={{mb: 3}}>
                <RichTextEditorComponentView
                    label="Impressum"
                    value={department.imprint}
                    onChange={handleInputChange("imprint")}
                    required
                    error={errors.imprint}
                />
            </Box>

            <Box sx={{mb: 3}}>
                <RichTextEditorComponentView
                    label="Datenschutzerklärung"
                    value={department.privacy}
                    onChange={handleInputChange("privacy")}
                    required
                    error={errors.privacy}
                />
            </Box>

            <Box sx={{mb: 3}}>
                <RichTextEditorComponentView
                    label="Barrierefreiheitserklärung"
                    value={department.accessibility}
                    onChange={handleInputChange("accessibility")}
                    required
                    error={errors.accessibility}
                />
            </Box>

            <Typography
                variant="h5"
                sx={{
                    mt: 6,
                    mb: 1,
                }}
            >
                Zentrale E-Mail-Adressen für Systembenachrichtigungen
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Systembenachrichtigungen (wie z.B. Eingang eines neuen Antrags) werden grundsätzlich an jede Mitarbeiter:in im Fachbereich gesendet.
                Wenn Sie hier eine oder mehrere zentrale E-Mail-Adressen hinterlegen, erhalten nur noch diese die Systembenachrichtigungen.
            </Typography>

            <TextFieldComponent
                label="Zentrale E-Mail-Adressen für Systembenachrichtigungen"
                value={department.departmentMail ?? undefined}
                onChange={handleInputChange("departmentMail")}
                onBlur={handleInputBlur("departmentMail")}
                maxCharacters={255}
                error={errors.departmentMail}
                hint="Sie können mehrere E-Mail-Adressen durch ein Komma getrennt eingeben."
            />

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 4,
                    gap: 2,
                }}
            >
                <Button
                    onClick={handleSave}
                    disabled={isBusy || hasNotChanged}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon />}
                >
                    Speichern
                </Button>

                {
                    department.id !== 0 &&
                    <Button
                        onClick={() => {
                            reset();
                        }}
                        disabled={isBusy || hasNotChanged}
                        color="error"
                    >
                        Zurücksetzen
                    </Button>
                }

                {
                    department.id !== 0 &&
                    <Button
                        variant={'outlined'}
                        onClick={checkAndHandleDelete}
                        disabled={isBusy}
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
                confirmationText={department.name}
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