import {Box, Button, Grid, Typography} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {isAdmin} from '../../../../utils/is-admin';
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import {useChangeBlocker} from "../../../../hooks/use-change-blocker";
import {useFormManager} from '../../../../hooks/use-form-manager';
import {FormsApiService} from "../../../forms/forms-api-service";
import {ConfirmDialog} from "../../../../dialogs/confirm-dialog/confirm-dialog";
import {ConstraintDialog} from "../../../../dialogs/constraint-dialog/constraint-dialog";
import {ConstraintLinkProps} from "../../../../dialogs/constraint-dialog/constraint-link-props";
import * as yup from "yup";
import {Destination} from "../../models/destination";
import {DestinationsApiService} from "../../destinations-api-service";
import {NumberFieldComponent} from "../../../../components/number-field/number-field-component";
import {DestinationType} from "../../../../data/destination-type";
import {SelectFieldComponent} from "../../../../components/select-field/select-field-component";
import {MailProcessingNotice} from "../../../../components/mail-processing-notice/mail-processing-notice";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";

export const DestinationSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, "Der Name der Schnittstelle muss mindestens 3 Zeichen lang sein.")
        .max(96, "Der Name der Schnittstelle darf maximal 96 Zeichen lang sein.")
        .required("Der Name der Schnittstelle ist ein Pflichtfeld."),
    type: yup.mixed<DestinationType>()
        .oneOf(Object.values(DestinationType), "Ungültiger Schnittstellentyp.")
        .required("Der Schnittstellentyp ist ein Pflichtfeld."),
    mailTo: yup.string()
        .when("type", {
            is: DestinationType.Mail,
            then: (schema) => schema
                .trim()
                .max(255, "Die E-Mail-Adresse darf maximal 255 Zeichen lang sein.")
                .matches(
                    /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}(\s*,\s*[\w.-]+@[\w.-]+\.[a-zA-Z]{2,})*$/,
                    "Bitte eine oder mehrere gültige E-Mail-Adressen, durch Komma getrennt, eingeben."
                )
                .required("Die primäre Mail-To-Adresse ist erforderlich."),
            otherwise: (schema) => schema.notRequired().nullable(),
        }),
    mailCC: yup.string()
        .when("type", {
            is: DestinationType.Mail,
            then: (schema) => schema
                .trim()
                .max(255, "Die CC-Adresse darf maximal 255 Zeichen lang sein.")
                .nullable()
                .optional()
                .test("valid-email-list", "Bitte eine oder mehrere gültige E-Mail-Adressen, durch Komma getrennt, eingeben.", (val) => {
                    if (!val) return true;
                    return val.split(',').every(email => /\S+@\S+\.\S+/.test(email.trim()));
                }),
            otherwise: (schema) => schema.notRequired().nullable(),
        }),
    mailBCC: yup.string()
        .when("type", {
            is: DestinationType.Mail,
            then: (schema) => schema
                .trim()
                .max(255, "Die BCC-Adresse darf maximal 255 Zeichen lang sein.")
                .nullable()
                .optional()
                .test("valid-email-list", "Bitte eine oder mehrere gültige E-Mail-Adressen, durch Komma getrennt, eingeben.", (val) => {
                    if (!val) return true;
                    return val.split(',').every(email => /\S+@\S+\.\S+/.test(email.trim()));
                }),
            otherwise: (schema) => schema.notRequired().nullable(),
        }),
    apiAddress: yup.string()
        .when("type", {
            is: DestinationType.HTTP,
            then: (schema) => schema
                .trim()
                .max(255, "Die API-Adresse darf maximal 255 Zeichen lang sein.")
                .matches(
                    /^https?:\/\/[\w.-]+(:\d+)?(\/[\w./-]*)?$/,
                    "Bitte eine gültige API-URL eingeben (z. B. https://example.com/api)."
                )
                .required("Die API-Adresse ist erforderlich."),
            otherwise: (schema) => schema.notRequired().nullable(),
        }),
    authorizationHeader: yup.string()
        .when("type", {
            is: DestinationType.HTTP,
            then: (schema) => schema
                .trim()
                .max(255, "Der API-Schlüssel darf maximal 255 Zeichen lang sein.")
                .nullable()
                .optional(),
            otherwise: (schema) => schema.notRequired().nullable(),
        }),
    maxAttachmentMegaBytes: yup.number()
        .min(1, "Die maximale Anlagengröße muss mindestens 1 MB betragen.")
        .max(100, "Die maximale Anlagengröße darf maximal 100 MB betragen.")
        .nullable()
        .optional(),
}).defined();


export function DestinationDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    const api = useApi();
    const {
        item,
        setItem,
        isBusy,
        setIsBusy,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<Destination, undefined>;

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<Destination>(item, DestinationSchema as any);

    const apiService = useMemo(() => new DestinationsApiService(api), [api]);
    const destination = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);

    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [confirmDeleteAction, setConfirmDeleteAction] = useState<(() => void) | undefined>(undefined);
    const [relatedApplications, setRelatedApplications] = useState<ConstraintLinkProps[] | undefined>(undefined);

    if (destination == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (destination != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar("Bitte überprüfen Sie Ihre Eingaben."));
                return;
            }

            setIsBusy(true);

            if (destination.id === 0) {
                apiService
                    .create(destination)
                    .then((newDestination) => {
                        setItem(newDestination);
                        reset();

                        dispatch(showSuccessSnackbar('Neue Schnittstelle erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/destinations/${newDestination.id}`, { replace: true });
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
                    .update(destination.id, destination)
                    .then((updatedDepartment) => {
                        setItem(updatedDepartment);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen an Schnittstelle erfolgreich gespeichert.'));
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
        if (destination.id === 0) return;

        setIsBusy(true);
        try {
            const uniqueForms = await new FormsApiService(api)
                .list(0, 999, undefined, undefined, {destinationId: destination.id})

            if (uniqueForms.content.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = uniqueForms.content.slice(0, maxVisibleLinks).map(f => ({
                    label: f.title,
                    to: `/forms/${f.id}`
                }));

                if (uniqueForms.content.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: "Weitere Formulare anzeigen…",
                        to: `/departments/${destination.id}/forms`
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
        if (destination.id === 0) return;

        setIsBusy(true);
        apiService.destroy(destination.id)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                navigate('/destinations', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Die Schnittstelle wurde erfolgreich gelöscht.'));
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
                Schnittstelle konfigurieren
            </Typography>

            <Typography sx={{mb: 2, maxWidth: 900}}>
                Konfigurieren Sie die Schnittstelle, an die Anträge gesendet werden sollen. Sie können die Einstellungen jederzeit anpassen, auch wenn die Schnittstelle bereits für Formulare verwendet wird.
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
                        label="Name der Schnittstelle"
                        placeholder="Schnittstelle"
                        hint="Der Name wird in der Formularentwicklung angezeigt und identifiziert diese Schnittstelle."
                        value={destination.name}
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
                >
                    <SelectFieldComponent
                        label="Schnittstellen-Typ"
                        hint="Der Typ bestimmt die Übertragungsart für diese Schnittstelle."
                        value={destination?.type}
                        onChange={(val) => handleInputChange("type")((val ?? DestinationType.Mail) as DestinationType)}
                        options={[
                            {
                                value: DestinationType.Mail,
                                label: 'Mail-Schnittstelle',
                            },
                            {
                                value: DestinationType.HTTP,
                                label: 'HTTP-Schnittstelle',
                            },
                        ]}
                        required
                        error={errors.type}
                    />
                </Grid>
            </Grid>

            {
                destination?.type === DestinationType.Mail &&
                <>
                    <Typography
                        variant="h6"
                        sx={{
                            mt: 4,
                            mb: 1,
                        }}
                    >
                        Einstellungen für E-Mail-Schnittstelle
                    </Typography>

                    <Typography sx={{mb: 2}}>
                        Konfigurieren Sie die E-Mail-Adressen, an die Anträge gesendet werden sollen.
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
                                label="Mail-To-Adressen"
                                placeholder="destination@gover.digital"
                                hint="Die primäre Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                                value={destination.mailTo}
                                onChange={handleInputChange("mailTo")}
                                onBlur={handleInputBlur("mailTo")}
                                required
                                maxCharacters={255}
                                error={errors.mailTo}
                            />
                        </Grid>
                        <Grid
                            item
                            xs={12}
                            lg={6}
                        >
                            <TextFieldComponent
                                label="Mail CC-Adressen"
                                placeholder="other-destination@gover.digital"
                                hint="Die CC-Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                                value={destination.mailCC}
                                onChange={handleInputChange("mailCC")}
                                onBlur={handleInputBlur("mailCC")}
                                maxCharacters={255}
                                error={errors.mailCC}
                            />
                        </Grid>
                        <Grid
                            item
                            xs={12}
                            lg={6}
                        >
                            <TextFieldComponent
                                label="Mail BCC-Adressen"
                                placeholder="yet-another-destination@gover.digital"
                                hint="Die BCC-Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                                value={destination.mailBCC}
                                onChange={handleInputChange("mailBCC")}
                                onBlur={handleInputBlur("mailBCC")}
                                maxCharacters={255}
                                error={errors.mailBCC}
                            />
                        </Grid>
                        <Grid
                            item
                            xs={12}
                            lg={12}
                            sx={{mb: 3}}
                        >
                            <Box>
                                <MailProcessingNotice />
                            </Box>
                        </Grid>
                    </Grid>
                </>
            }

            {
                destination?.type === DestinationType.HTTP &&
                <>
                    <Typography
                        variant="h6"
                        sx={{
                            mt: 4,
                            mb: 1,
                        }}
                    >
                        Einstellungen für HTTP-Schnittstelle
                    </Typography>

                    <Typography sx={{mb: 2}}>
                        Konfigurieren Sie die HTTP-Schnittstelle, an die Anträge gesendet werden sollen.
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
                                label="API Adresse"
                                placeholder="https://my-api-hostname.com:9000/v1/gover-hook"
                                hint="Die API Adresse, an die die Daten der antragstellenden Person via POST-Anfrage übermittelt werden."
                                value={destination.apiAddress}
                                onChange={handleInputChange("apiAddress")}
                                onBlur={handleInputBlur("apiAddress")}
                                required
                                maxCharacters={255}
                                error={errors.apiAddress}
                            />
                        </Grid>
                        <Grid
                            item
                            xs={12}
                            lg={6}
                        >
                            <TextFieldComponent
                                label="API Schlüssel"
                                placeholder="my-super-secret-api-key"
                                hint="Der API Schlüssel, der über den Authorization-Header beim Übertragen der Daten mitgesendet wird."
                                value={destination.authorizationHeader}
                                onChange={handleInputChange("authorizationHeader")}
                                onBlur={handleInputBlur("authorizationHeader")}
                                maxCharacters={255}
                                error={errors.authorizationHeader}
                            />
                        </Grid>
                    </Grid>
                </>
            }

            <Typography
                variant="h6"
                sx={{
                    mt: 4,
                    mb: 1,
                }}
            >
                Einstellungen für Anlagen
            </Typography>

            <Typography sx={{mb: 2}}>
                Konfigurieren Sie die maximale Größe der Anlagen, die an die Schnittstelle übermittelt werden können.
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
                    <NumberFieldComponent
                        label="Maximale Gesamtgröße der Anlagen (MB)"
                        placeholder="20"
                        hint="Sollten die Anlagen einer antragstellenden Person diese überschreiten, kann ein Antrag für diese Schnittstelle nicht abgesendet werden."
                        value={destination?.maxAttachmentMegaBytes}
                        onChange={handleInputChange("maxAttachmentMegaBytes")}
                        decimalPlaces={2}
                        minValue={1}
                        maxValue={100}
                        error={errors.maxAttachmentMegaBytes}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    lg={6}
                />
            </Grid>

            {
                userIsAdmin &&
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
                        destination.id !== 0 &&
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
                        destination.id !== 0 &&
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
            }

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Schnittstelle löschen"
                onCancel={() => setConfirmDeleteAction(undefined)}
                onConfirm={confirmDeleteAction}
                confirmationText={destination.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Schnittstelle wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Diese Schnittstelle kann (noch) nicht gelöscht werden, da sie noch für Formulare verwendet wird."
                solutionText="Bitte konfigurieren Sie für diese Formulare eine andere Schnittstelle und versuchen Sie es erneut:"
                links={relatedApplications}
            />
        </Box>
    );
}