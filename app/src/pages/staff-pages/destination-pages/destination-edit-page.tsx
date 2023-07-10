import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Divider } from '@mui/material';
import { TextFieldComponent } from '../../../components/text-field/text-field-component';
import { validateEmail } from '../../../utils/validate-email';
import { useAppDispatch } from '../../../hooks/use-app-dispatch';
import { showErrorSnackbar, showSuccessSnackbar } from '../../../slices/snackbar-slice';
import { type Destination } from '../../../models/entities/destination';
import { DestinationType } from '../../../data/destination-type/destination-type';
import { DestinationsService } from '../../../services/destinations-service';
import { SelectFieldComponent } from '../../../components/select-field/select-field-component';
import { NumberFieldComponent } from '../../../components/number-field/number-field-component';
import { useChangeBlocker } from '../../../hooks/use-change-blocker';
import { FormPageWrapper } from '../../../components/form-page-wrapper/form-page-wrapper';
import { isStringNotNullOrEmpty, isStringNullOrEmpty } from '../../../utils/string-utils';
import { useAdminGuard } from '../../../hooks/use-admin-guard';

type Errors = {
    [key in keyof Destination]?: string;
};

export function DestinationEditPage(): JSX.Element {
    useAdminGuard();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const destinationId = useParams().id;

    const [originalDest, setOriginalDest] = useState<Destination>();
    const [editedDest, setEditedDest] = useState<Destination>();

    const [isLoading, setIsLoading] = useState(false);
    const [isNotFound, setIsNotFound] = useState(false);

    const hasChanged = useChangeBlocker(originalDest, editedDest);

    const [errors, setErrors] = useState<Errors>({});

    useEffect(() => {
        setIsLoading(true);
        setIsNotFound(false);

        if (destinationId == null || destinationId === 'new') {
            const newDest: Destination = {
                id: 0,
                name: '',
                type: DestinationType.Mail,
                created: '',
                updated: '',
            };
            setOriginalDest(newDest);
            setEditedDest(newDest);
            setIsLoading(false);
        } else {
            DestinationsService
                .retrieve(parseInt(destinationId))
                .then((dest) => {
                    setOriginalDest(dest);
                    setEditedDest(dest);
                })
                .catch((err) => {
                    console.error(err);
                    setIsNotFound(true);
                })
                .finally(() => {
                    setIsLoading(false);
                });
        }
    }, [destinationId]);

    const handlePatch = (patch: Partial<Destination>): void => {
        if (editedDest != null) {
            setEditedDest({
                ...editedDest,
                ...patch,
            });
        }
    };

    const handleSave = (): void => {
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
                    if (!editedDest.mailTo.split(',').every((mail) => validateEmail(mail.trim()))) {
                        errors.mailTo = 'Bitte geben Sie nur gültige E-Mail-Adressen ein';
                    }
                }

                if (isStringNotNullOrEmpty(editedDest.mailCC) && !editedDest.mailCC!.split(',').every((mail) => validateEmail(mail.trim()))) {
                    errors.mailCC = 'Bitte geben Sie nur gültige Mail CC-Adressen ein';
                }

                if (isStringNotNullOrEmpty(editedDest.mailBCC) && !editedDest.mailBCC!.split(',').every((mail) => validateEmail(mail.trim()))) {
                    errors.mailBCC = 'Bitte geben Sie nur gültige Mail CC-Adressen ein';
                }
            } else {
                if (isStringNullOrEmpty(editedDest.mailBCC)) {
                    errors.apiAddress = 'Bitte geben Sie eine API Adresse ein';
                }
            }

            if (Object.keys(errors).length > 0) {
                setErrors(errors);
            } else {
                setErrors({});
                setIsLoading(true);

                DestinationsService
                    .save(editedDest.id !== 0 ? editedDest.id : undefined, editedDest)
                    .then((createdUser) => {
                        setOriginalDest(createdUser);
                        setEditedDest(createdUser);
                        dispatch(showSuccessSnackbar('Schnittstelle erfolgreich gespeichert'));
                    })
                    .catch((err) => {
                        console.error(err);
                        dispatch(showErrorSnackbar('Schnittstelle konnte nicht gespeichert werden'));
                    })
                    .finally(() => {
                        setIsLoading(false);
                    });
            }
        }
    };

    const handleReset = (): void => {
        if (originalDest != null) {
            setEditedDest(originalDest);
        }
    };

    const handleDelete = (): void => {
        if (editedDest != null) {
            setIsLoading(true);

            DestinationsService
                .destroy(editedDest.id)
                .then(() => {
                    navigate('/destinations');
                })
                .catch((err) => {
                    if (err.response?.status === 409) {
                        dispatch(showErrorSnackbar('Schnittstelle wird noch verwendet und kann nicht gelöscht werden'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Schnittstelle konnte nicht gelöscht werden'));
                    }

                    setIsLoading(false);
                });
        }
    };

    return (
        <FormPageWrapper
            title="Schnittstelle bearbeiten"
            isLoading={ isLoading }
            is404={ isNotFound }

            hasChanged={ hasChanged }

            onSave={ handleSave }
            onReset={ editedDest?.id !== 0 ? handleReset : undefined }
            onDelete={ editedDest?.id !== 0 ? handleDelete : undefined }
        >
            <TextFieldComponent
                label="Name"
                placeholder="Neue Schnittstelle"
                hint="Der Name wird in der Formularentwicklung angezeigt und identifiziert diese Schnittstelle."
                value={ editedDest?.name }
                onChange={ (val) => {
                    handlePatch({
                        name: val ?? '',
                    });
                } }
                required
                maxCharacters={ 96 }
                error={ errors.name }
            />

            <SelectFieldComponent
                label="Schnittstellen-Typ"
                hint="Der Typ bestimmt die Übertragungsart für diese Schnittstelle."
                value={ editedDest?.type }
                onChange={ (val) => {
                    handlePatch({
                        type: (val ?? DestinationType.Mail) as DestinationType,
                    });
                } }
                options={ [
                    {
                        value: DestinationType.Mail,
                        label: 'Mail-Schnittstelle',
                    },
                    {
                        value: DestinationType.HTTP,
                        label: 'HTTP-Schnittstelle',
                    },
                ] }
                required
                error={ errors.type }
            />

            <Divider
                sx={ {
                    my: 4,
                } }
            >
                Verbindungs-Details
            </Divider>

            {
                editedDest?.type === DestinationType.Mail &&
                <>
                    <TextFieldComponent
                        label="Mail-To-Adressen"
                        placeholder="destination@gover.digital"
                        hint="Die primäre Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                        value={ editedDest.mailTo }
                        onChange={ (val) => {
                            handlePatch({
                                mailTo: val ?? '',
                            });
                        } }
                        required
                        maxCharacters={ 255 }
                        error={ errors.mailTo }
                    />

                    <TextFieldComponent
                        label="Mail CC-Adressen"
                        placeholder="other-destination@gover.digital"
                        hint="Die CC-Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                        value={ editedDest.mailCC }
                        onChange={ (val) => {
                            handlePatch({
                                mailCC: val ?? '',
                            });
                        } }
                        maxCharacters={ 255 }
                        error={ errors.mailCC }
                    />

                    <TextFieldComponent
                        label="Mail BCC-Adressen"
                        placeholder="yet-another-destination@gover.digital"
                        hint="Die BCC-Adresse, an die der ausgefüllte Antrag geschickt wird. Mehrere Adressen werden durch ein Komma getrennt."
                        value={ editedDest.mailBCC }
                        onChange={ (val) => {
                            handlePatch({
                                mailBCC: val ?? '',
                            });
                        } }
                        maxCharacters={ 255 }
                        error={ errors.mailBCC }
                    />
                </>
            }

            {
                editedDest?.type === DestinationType.HTTP &&
                <>
                    <TextFieldComponent
                        label="API Adresse"
                        placeholder="https://my-api-hostname.com:9000/v1/gover-hook"
                        hint="Die API Adresse, an die die Daten des Antragstellenden via POST-Anfrage übermittelt werden."
                        value={ editedDest.apiAddress }
                        onChange={ (val) => {
                            handlePatch({
                                apiAddress: val ?? '',
                            });
                        } }
                        required
                        maxCharacters={ 255 }
                        error={ errors.apiAddress }
                    />

                    <TextFieldComponent
                        label="API Schlüssel"
                        placeholder="my-super-secret-api-key"
                        hint="Der API Schlüssel, der über den Authorization-Header beim Übertragen der Daten mitgesendet wird."
                        value={ editedDest.authorizationHeader }
                        onChange={ (val) => {
                            handlePatch({
                                authorizationHeader: val ?? '',
                            });
                        } }
                        maxCharacters={ 255 }
                        error={ errors.authorizationHeader }
                    />
                </>
            }

            <Divider
                sx={ {
                    my: 4,
                } }
            >
                Anlagen
            </Divider>

            <NumberFieldComponent
                label="Maximale Gesamtgröße der Anlagen (MB)"
                placeholder="20"
                hint="Die maximale Gesamtgröße der Anlagen in Megabyte. Sollten die Anlagen einer Antragsteller:in diese überschreiten, kann ein Antrag für diese Schnittstelle nicht abgesendet werden."
                value={ editedDest?.maxAttachmentMegaBytes }
                onChange={ (val) => {
                    handlePatch({
                        maxAttachmentMegaBytes: val,
                    });
                } }
                decimalPlaces={ 2 }
                error={ errors.maxAttachmentMegaBytes }
            />
        </FormPageWrapper>
    );
}
