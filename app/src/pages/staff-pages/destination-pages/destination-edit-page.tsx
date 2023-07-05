import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {FormEvent, useEffect, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {Box, Button, Divider} from "@mui/material";
import {TextFieldComponent} from "../../../components/text-field/text-field-component";
import {validateEmail} from "../../../utils/validate-email";
import {useAppDispatch} from "../../../hooks/use-app-dispatch";
import {showSuccessSnackbar} from "../../../slices/snackbar-slice";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {Destination} from "../../../models/entities/destination";
import {DestinationType} from "../../../data/destination-type/destination-type";
import {DestinationsService} from "../../../services/destinations-service";
import {PageWrapper} from "../../../components/page-wrapper/page-wrapper";
import {SelectFieldComponent} from "../../../components/select-field/select-field-component";
import {NumberFieldComponent} from "../../../components/number-field/number-field-component";
import {useChangeBlocker} from "../../../hooks/use-change-blocker";
import {ConfirmDialog} from "../../../dialogs/confirm-dialog/confirm-dialog";

type Errors = {
    [key in keyof Destination]?: string;
}

export function DestinationEditPage() {
    useAuthGuard();
    useUserGuard(user => user != null && user.admin);

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const {id} = useParams();

    const [originalDest, setOriginalDest] = useState<Destination>();
    const [editedDest, setEditedDest] = useState<Destination>();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();


    const hasChanged = useChangeBlocker(originalDest, editedDest);

    const [errors, setErrors] = useState<Errors>({});


    useEffect(() => {
        function handleUnload(event: BeforeUnloadEvent) {
            event.preventDefault();
            event.returnValue = 'Sollen die Änderungen an der Schnittstelle wirklich verworfen werden?';
        }

        if (hasChanged) {
            window.addEventListener('beforeunload', handleUnload);
        } else {
            window.removeEventListener('beforeunload', handleUnload);
        }

        return () => window.removeEventListener('beforeunload', handleUnload);
    }, [hasChanged]);

    useEffect(() => {
        if (id == null || id === 'new') {
            const newDest: Destination = {
                id: 0,
                name: '',
                type: DestinationType.Mail,
                created: '',
                updated: '',
            };
            setOriginalDest(newDest);
            setEditedDest(newDest);
        } else {
            DestinationsService
                .retrieve(parseInt(id))
                .then(dest => {
                    setOriginalDest(dest);
                    setEditedDest(dest);
                });
        }
    }, [id]);

    const patchDest = (patch: Partial<Destination>) => {
        if (editedDest != null) {
            setEditedDest({
                ...editedDest,
                ...patch,
            });
        }
    };

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();
        event.stopPropagation();

        if (editedDest != null) {
            const errors: Errors = {};

            if (editedDest.name.length < 3) {
                errors.name = 'Bitte geben Sie einen Namen ein';
            }

            if ((editedDest.maxAttachmentMegaBytes ?? 0) > 100) {
                errors.maxAttachmentMegaBytes = 'Sie Überschreiten das feste Limit von 100 MB.';
            }

            if (editedDest.type === DestinationType.Mail) {
                if (editedDest.mailTo == null) {
                    errors.mailTo = 'Bitte geben Sie mindestens eine Mail-To-Adresse ein';
                } else {
                    if (!editedDest.mailTo.split(',').every(mail => validateEmail(mail.trim()))) {
                        errors.mailTo = 'Bitte geben Sie nur gültige E-Mail-Adressen ein';
                    }
                }

                if (editedDest.mailCC != null && !editedDest.mailCC.split(',').every(mail => validateEmail(mail.trim()))) {
                    errors.mailCC = 'Bitte geben Sie nur gültige Mail CC-Adressen ein';
                }

                if (editedDest.mailBCC != null && !editedDest.mailBCC.split(',').every(mail => validateEmail(mail.trim()))) {
                    errors.mailBCC = 'Bitte geben Sie nur gültige Mail CC-Adressen ein';
                }
            } else {
                if (editedDest.apiAddress == null) {
                    errors.apiAddress = 'Bitte geben Sie eine API Adresse ein';
                }
            }

            if (Object.keys(errors).length > 0) {
                setErrors(errors);
            } else {
                setErrors({});

                if (editedDest.id === 0) {
                    DestinationsService
                        .create(editedDest)
                        .then(createdUser => {
                            setOriginalDest(createdUser);
                            setEditedDest(createdUser);
                            dispatch(showSuccessSnackbar('Schnittstelle erfolgreich erstellt!'));
                        });
                } else {
                    DestinationsService
                        .update(editedDest.id, editedDest)
                        .then(updatedUser => {
                            setOriginalDest(updatedUser);
                            setEditedDest(updatedUser);
                            dispatch(showSuccessSnackbar('Schnittstelle erfolgreich gespeichert!'));
                        });
                }
            }
        }

        return false;
    };

    return (
        <PageWrapper
            title="Schnittstelle bearbeiten"
            isLoading={editedDest == null}
        >
            {
                editedDest != null &&
                <form onSubmit={handleSubmit}>
                    <Divider sx={{mb: 4}}>
                        Allgemein
                    </Divider>

                    <TextFieldComponent
                        label="Name"
                        placeholder="Neue Schnittstelle"
                        hint="Der Name wird in der Antragsentwicklung angezeigt und identifiziert diese Schnittstelle."
                        value={editedDest.name}
                        onChange={val => patchDest({
                            name: val ?? '',
                        })}
                        required
                        maxCharacters={96}
                        error={errors.name}
                    />

                    <SelectFieldComponent
                        label="Schnittstellen-Typ"
                        hint="Der Typ bestimmt die Übertragungsart für diese Schnittstelle."
                        value={editedDest.type}
                        onChange={val => patchDest({
                            type: (val ?? DestinationType.Mail) as DestinationType,
                        })}
                        options={[
                            {
                                value: DestinationType.Mail,
                                label: 'Mail-Schnittstelle',
                            },
                            {
                                value: DestinationType.HTTP,
                                label: 'HTTP-Schnittstelle',
                            }
                        ]}
                        required
                        error={errors.type}
                    />

                    <Divider sx={{my: 4}}>
                        Verbindungs-Details
                    </Divider>

                    {
                        editedDest.type === DestinationType.Mail &&
                        <>
                            <TextFieldComponent
                                label="Mail-To-Adressen"
                                placeholder="destination@gover.digital"
                                hint="Die primäre Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                                value={editedDest.mailTo}
                                onChange={val => patchDest({
                                    mailTo: val ?? '',
                                })}
                                required
                                maxCharacters={255}
                                error={errors.mailTo}
                            />

                            <TextFieldComponent
                                label="Mail CC-Adressen"
                                placeholder="other-destination@gover.digital"
                                hint="Die CC-Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                                value={editedDest.mailCC}
                                onChange={val => patchDest({
                                    mailCC: val ?? '',
                                })}
                                maxCharacters={255}
                                error={errors.mailCC}
                            />

                            <TextFieldComponent
                                label="Mail BCC-Adressen"
                                placeholder="yet-another-destination@gover.digital"
                                hint="Die BCC-Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                                value={editedDest.mailBCC}
                                onChange={val => patchDest({
                                    mailBCC: val ?? '',
                                })}
                                maxCharacters={255}
                                error={errors.mailBCC}
                            />
                        </>
                    }

                    {
                        editedDest.type === DestinationType.HTTP &&
                        <>
                            <TextFieldComponent
                                label="API Adresse"
                                placeholder="https://my-api-hostname.com:9000/v1/gover-hook"
                                hint="Die API Adresse, an die die Daten des Antragstellenden via POST-Anfrage übermittelt werden."
                                value={editedDest.apiAddress}
                                onChange={val => patchDest({
                                    apiAddress: val ?? '',
                                })}
                                required
                                maxCharacters={255}
                                error={errors.apiAddress}
                            />

                            <TextFieldComponent
                                label="API Schlüssel"
                                placeholder="my-super-secret-api-key"
                                hint="Der API Schlüssel, der über den Authorization-Header beim Übertragen der Daten mitgesendet wird."
                                value={editedDest.authorizationHeader}
                                onChange={val => patchDest({
                                    authorizationHeader: val ?? '',
                                })}
                                maxCharacters={255}
                                error={errors.authorizationHeader}
                            />
                        </>
                    }

                    <Divider sx={{my: 4}}>
                        Anlagen
                    </Divider>

                    <NumberFieldComponent
                        label="Maximale gesamtgröße der Anlagen (MB)"
                        placeholder="20"
                        hint="Die maximale gesamtgröße der Anlagen in Megabyte. Sollten die Anlagen einer Antragsteller:in diese überschreiten, kann ein Antrag für diese Schnittstelle nicht abgesendet werden."
                        value={editedDest.maxAttachmentMegaBytes}
                        onChange={val => patchDest({
                            maxAttachmentMegaBytes: val,
                        })}
                        decimalPlaces={2}
                        error={errors.maxAttachmentMegaBytes}
                    />

                    <Box sx={{mt: 4, display: 'flex'}}>
                        <Button
                            type="submit"
                            disabled={!hasChanged}
                        >
                            Speichern
                        </Button>

                        {
                            editedDest.id !== 0 &&
                            <Button
                                sx={{ml: 2}}
                                type="reset"
                                color="error"
                                disabled={!hasChanged}
                                onClick={() => {
                                    setEditedDest(originalDest!);
                                    setErrors({});
                                }}
                            >
                                Zurücksetzen
                            </Button>
                        }

                        {
                            editedDest.id !== 0 &&
                            <Button
                                sx={{ml: 'auto'}}
                                type="button"
                                color="error"
                                onClick={() => setConfirmDelete(() => () => {
                                    DestinationsService.destroy(editedDest?.id!);
                                    navigate('/destinations');
                                })}
                            >
                                Löschen
                            </Button>
                        }
                    </Box>
                </form>
            }

            <ConfirmDialog
                title="Schnittstelle wirklich löschen"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Sind Sie sicher, dass Sie die Schnittstelle <strong>{editedDest?.name}</strong> wirklich löschen wollen?
                Bitte beachten Sie, dass dies <u>nicht rückgängig</u> gemacht werden kann!<br/>
                Alle Verlinkungen auf diese Schnittstelle von Formularen werden entfernt und es findet <u>keine
                                                                                                          Übertragung</u> mehr
                statt.
                Eingehende Anträge finden Sie weiterhin in der Antragsübersicht der jeweiligen Formulare.
            </ConfirmDialog>
        </PageWrapper>
    );
}