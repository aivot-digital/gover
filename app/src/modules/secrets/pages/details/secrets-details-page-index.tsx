import {Box, Button, Typography} from '@mui/material';
import React, {useContext, useMemo, useState} from 'react';
import {GenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../../../utils/string-utils';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {isAdmin} from '../../../../utils/is-admin';
import {SecretsApiService} from '../../secrets-api-service';
import {Secret} from '../../models/secret';
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {useFormManager} from "../../../../hooks/use-form-manager";
import {useChangeBlocker} from "../../../../hooks/use-change-blocker";
import {showErrorSnackbar, showSuccessSnackbar} from "../../../../slices/snackbar-slice";
import ContentPasteOutlinedIcon from "@mui/icons-material/ContentPasteOutlined";
import {ConfirmDialog} from "../../../../dialogs/confirm-dialog/confirm-dialog";
import {AlertComponent} from "../../../../components/alert/alert-component";
import * as yup from "yup";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";

export const SecretSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, "Der Name muss mindestens 3 Zeichen lang sein.")
        .max(64, "Der Name darf maximal 64 Zeichen lang sein.")
        .required("Der Name ist ein Pflichtfeld."),
    description: yup.string()
        .trim()
        .min(3, "Die Beschreibung muss mindestens 3 Zeichen lang sein.")
        .max(255, "Die Beschreibung darf maximal 255 Zeichen lang sein.")
        .required("Die Beschreibung ist ein Pflichtfeld."),
    value: yup.string()
        .trim()
        .min(1, "Der Wert darf nicht leer sein.")
        .required("Der Wert ist ein Pflichtfeld."),
});

export function SecretsDetailsPageIndex() {
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
    } = useContext(GenericDetailsPageContext);

    const {
        currentItem,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<Secret>(item, SecretSchema as any);

    const apiService = useMemo(() => new SecretsApiService(api), [api]);

    const secret = currentItem;
    const changeBlocker = useChangeBlocker(item, currentItem);
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);

    if (secret == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = () => {
        if (secret != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar("Bitte überprüfen Sie Ihre Eingaben."));
                return;
            }

            setIsBusy(true);

            if (isStringNullOrEmpty(secret.key)) {
                apiService
                    .create(secret)
                    .then((newSecret) => {
                        setItem(newSecret);
                        reset();

                        dispatch(showSuccessSnackbar('Neues Geheimnis erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/secrets/${newSecret.key}`, { replace: true });
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
                    .update(secret.key, secret)
                    .then((updatedSecret) => {
                        setItem(updatedSecret);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen an Geheimnis erfolgreich gespeichert.'));
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

    const handleDelete = () => {
        if (isStringNotNullOrEmpty(secret.key)) {
            setIsBusy(true);

            apiService
                .destroy(secret.key)
                .then(() => {
                    reset(); // prevent change blocker by resetting unsaved changes
                    navigate('/secrets', {
                        replace: true,
                    });
                    dispatch(showSuccessSnackbar('Das Geheimnis wurde erfolgreich gelöscht.'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Beim Löschen des Geheimnisses ist ein Fehler aufgetreten.'));
                    setIsBusy(false);
                });
        }
    };

    return (
        <Box>
            {
                isStringNotNullOrEmpty(secret.key) &&
                <TextFieldComponent
                    label="Schlüssel"
                    value={secret.key}
                    onChange={handleInputChange("key")}
                    onBlur={handleInputBlur("key")}
                    disabled={true}
                    sx={{
                        marginTop: 0,
                    }}
                    endAction={
                        [
                            {
                                icon: <ContentPasteOutlinedIcon />,
                                tooltip: 'Schlüssel (ID) in Zwischenablage kopieren',
                                onClick: () => {
                                    navigator
                                        .clipboard
                                        .writeText(secret.key)
                                        .then(() => {
                                            dispatch(showSuccessSnackbar('Link in Zwischenablage kopiert!'));
                                        })
                                        .catch((err) => {
                                            console.error(err);
                                            dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
                                        });
                                },
                            }
                        ]
                    }
                />
            }

            <TextFieldComponent
                label="Name"
                required
                value={secret.name}
                onChange={handleInputChange("name")}
                onBlur={handleInputBlur("name")}
                disabled={isBusy || !userIsAdmin}
                error={errors.name}
                minCharacters={3}
                maxCharacters={64}
            />

            <TextFieldComponent
                label="Beschreibung"
                required
                value={secret.description}
                onChange={handleInputChange("description")}
                onBlur={handleInputBlur("description")}
                multiline={true}
                disabled={isBusy || !userIsAdmin}
                error={errors.description}
                minCharacters={3}
                maxCharacters={255}
            />

            {
                userIsAdmin &&
                <TextFieldComponent
                    label="Wert"
                    required
                    value={secret.value}
                    onChange={handleInputChange("value")}
                    onBlur={handleInputBlur("value")}
                    disabled={isBusy}
                    error={errors.value}
                />
            }

            {
                userIsAdmin &&
                <Box
                    sx={{
                        display: 'flex',
                        marginTop: 2,
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
                        isStringNotNullOrEmpty(secret.key) &&
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
                        isStringNotNullOrEmpty(secret.key) &&
                        <Button
                            variant={'outlined'}
                            onClick={() => setShowConfirmDialog(true)}
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
                title="Geheimnis löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={secret.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie dieses Geheimnis wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
                <AlertComponent color={"warning"}>
                    Vergewissern Sie sich, dass dieses Geheimnis nicht mehr benötigt wird, bevor Sie fortfahren. Wir können nicht prüfen, ob es noch an Stellen wie Low-Code-Funktionen oder Konfigurationen von Zahlungsdienstleistern verwendet wird.
                </AlertComponent>
            </ConfirmDialog>
        </Box>
    );
}