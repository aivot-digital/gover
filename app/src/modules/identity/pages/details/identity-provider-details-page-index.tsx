import {Box, Button, Divider, Grid, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {Api, useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../../../utils/string-utils';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {ConstraintDialog} from '../../../../dialogs/constraint-dialog/constraint-dialog';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintLinkProps} from '../../../../dialogs/constraint-dialog/constraint-link-props';
import HelpIconOutlined from '@mui/icons-material/HelpOutline';
import Tooltip from '@mui/material/Tooltip';
import * as yup from 'yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {FormsApiService} from '../../../forms/forms-api-service';
import {SecretEntityResponseDTO} from '../../../secrets/dtos/secret-entity-response-dto';
import {SecretsApiService} from '../../../secrets/secrets-api-service';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {Asset} from '../../../assets/models/asset';
import {AssetsApiService} from '../../../assets/assets-api-service';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {IdentityAdditionalParameter} from '../../models/identity-additional-parameter';
import {IdentityAttributeMapping} from '../../models/identity-attribute-mapping';
import {TableFieldComponent2} from '../../../../components/table-field/table-field-component-2';
import {StringListInput2} from '../../../../components/string-list-input/string-list-input-2';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {IdentityProviderIcon} from '../../components/identity-provider-icon/identity-provider-icon';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {useConfirm} from '../../../../providers/confirm-provider';
import {hideLoadingOverlay, showLoadingOverlay} from '../../../../slices/loading-overlay-slice';

// allows absolute and relative URLs
const urlRegex = /^(https?:\/\/[^\s]+|\/[^\s]*)$/;

export const formSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name des Nutzerkontenanbieters muss mindestens 3 Zeichen lang sein.')
        .max(64, 'Der Name des Nutzerkontenanbieters darf maximal 96 Zeichen lang sein.')
        .required('Der Name des Nutzerkontenanbieters ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .min(10, 'Die Beschreibung muss mindestens 10 Zeichen lang sein.')
        .max(255, 'Die Beschreibung darf maximal 500 Zeichen lang sein.')
        .required('Die Beschreibung des Nutzerkontenanbieters ist ein Pflichtfeld.'),
    iconAssetKey: yup.string(),
    metadataIdentifier: yup.string()
        .trim()
        .min(1, 'Der Metadaten-Identifikator ist ein Pflichtfeld.')
        .max(64, 'Der Metadaten-Identifikator darf maximal 64 Zeichen lang sein.')
        .required('Der Metadaten-Identifikator ist ein Pflichtfeld.'),
    authorizationEndpoint: yup.string()
        .trim()
        .min(1, 'Der Autorisierungsendpunkt ist ein Pflichtfeld.')
        .max(255, 'Der Autorisierungsendpunkt darf maximal 255 Zeichen lang sein.')
        .matches(urlRegex, 'Der Autorisierungsendpunkt muss eine absolute oder relative URL sein (z.B. https://... oder /../...)')
        .required('Der Autorisierungsendpunkt ist ein Pflichtfeld.'),
    tokenEndpoint: yup.string()
        .trim()
        .min(1, 'Der Tokenendpunkt ist ein Pflichtfeld.')
        .max(255, 'Der Tokenendpunkt darf maximal 255 Zeichen lang sein.')
        .matches(urlRegex, 'Der Tokenendpunkt muss eine absolute oder relative URL sein (z.B. https://... oder /../...)')
        .required('Der Tokenendpunkt ist ein Pflichtfeld.'),
    userinfoEndpoint: yup.string()
        .trim()
        .max(255, 'Der Userinfo-Endpunkt darf maximal 255 Zeichen lang sein.')
        .matches(urlRegex, 'Der Userinfo-Endpunkt muss eine absolute oder relative URL sein (z.B. https://... oder /../...)')
        .nullable(),
    endSessionEndpoint: yup.string()
        .trim()
        .max(255, 'Der End-Session-Endpunkt darf maximal 255 Zeichen lang sein.')
        .matches(urlRegex, 'Der End-Session-Endpunkt muss eine absolute oder relative URL sein (z.B. https://... oder /../...)')
        .nullable(),
    clientId: yup.string()
        .trim()
        .min(1, 'Die Client-ID ist ein Pflichtfeld.')
        .max(128, 'Die Client-ID darf maximal 128 Zeichen lang sein.')
        .required('Die Client-ID ist ein Pflichtfeld.'),
    attributes: yup.array()
        .of(
            yup.object({
                label: yup.string().trim(),
                description: yup.string().trim(),
                keyInData: yup.string().trim(),
                displayAttribute: yup.boolean().nullable(),
            }).test('row-completeness', 'Bitte füllen Sie alle Felder aus oder löschen Sie die Zeile.', function (row) {
                if (!row) return true;

                const { label, description, keyInData } = row;

                const isAnyFilled = !!label?.trim() || !!description?.trim() || !!keyInData?.trim();
                const areAllFilled = !!label?.trim() && !!description?.trim() && !!keyInData?.trim();

                if (!isAnyFilled) {
                    return this.createError({ message: 'Bitte füllen Sie alle Felder aus oder löschen Sie die Zeile.' });
                }

                if (!areAllFilled) {
                    return this.createError({ message: 'Bitte füllen Sie alle Felder vollständig aus.' });
                }

                return true;
            })
        ),
    defaultScopes: yup.array()
        .of(
            yup.string()
                .trim()
                .test('not-empty-if-present', 'Ein Scope darf nicht leer sein.', val => {
                    return val == null || val.trim().length > 0;
                })
        ),
    additionalParams: yup.array()
        .of(
            yup.object({
                key: yup.string().trim(),
                value: yup.string().trim(),
            }).test('row-completeness', 'Bitte füllen Sie Schlüssel und Wert aus oder löschen Sie die Zeile.', function (row) {
                if (!row) return true;

                const { key, value } = row;

                const isAnyFilled = !!key?.trim() || !!value?.trim();
                const areAllFilled = !!key?.trim() && !!value?.trim();

                if (!isAnyFilled) {
                    return this.createError({ message: 'Bitte füllen Sie Schlüssel und Wert aus oder löschen Sie die Zeile.' });
                }

                if (!areAllFilled) {
                    return this.createError({ message: 'Bitte füllen Sie Schlüssel und Wert vollständig aus.' });
                }

                return true;
            })
        )
});

function getIndexedFieldError(
    errors: Record<string, any> | undefined,
    fieldName: string,
    message: string
): string | undefined {
    if (!errors) return undefined;

    return Object.keys(errors).some(k => k.startsWith(`${fieldName}[`))
        ? message
        : undefined;
}

export function IdentityProviderDetailsPageIndex() {
    useAdminGuard();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const api = useApi();
    const showConfirm = useConfirm();

    const [assets, setAssets] = useState<Asset[]>();
    const [secrets, setSecrets] = useState<SecretEntityResponseDTO[]>();

    const [endpointConfigUrl, setEndpointConfigUrl] = useState('');
    const [endpointConfigUrlError, setEndpointConfigUrlError] = useState<string>();

    const apiService = useMemo(() => {
        return new IdentityProvidersApiService(api);
    }, [api]);

    const {
        item: originalIdentityProvider,
        setItem,
        isNewItem,
        isBusy,
        setIsBusy,
    } = useContext<GenericDetailsPageContextType<IdentityProviderDetailsDTO, void>>(GenericDetailsPageContext);

    const isSystemProvider = useMemo(() => (
        originalIdentityProvider != null && originalIdentityProvider.type !== IdentityProviderType.Custom
    ), [originalIdentityProvider]);

    const dynamicFormSchema = useMemo(() => {
        return formSchema.shape({
            iconAssetKey: isSystemProvider
                ? yup.string().nullable()
                : yup.string()
                    .required('Die Logo-Grafik ist eine Pflichtkonfiguration.'),
        });
    }, [isSystemProvider]);

    const {
        currentItem: identityProvider,
        errors,
        hasNotChanged,
        handleInputPatch,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<IdentityProviderDetailsDTO>(originalIdentityProvider, dynamicFormSchema as any);

    const changeBlocker = useChangeBlocker(originalIdentityProvider, identityProvider);

    const [showConfirmDialog, setShowConfirmDialog] = useState(false);
    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [relatedEntities, setRelatedEntities] = useState<ConstraintLinkProps[] | null>(null);

    useEffect(() => {
        fetchRelatedEntities(api)
            .then(({assets, secrets}) => {
                setAssets(assets);
                setSecrets(secrets);
            })
            .catch((err) => {
                console.error(err);
            });
    }, [api]);

    const inputsDisabled = useMemo(() => (
        isBusy || identityProvider == null
    ), [isBusy, identityProvider]);

    if (identityProvider == null || assets == null || secrets == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handlePrepareEntity = () => {
        setEndpointConfigUrlError(undefined);

        if (isStringNullOrEmpty(endpointConfigUrl)) {
            setEndpointConfigUrlError('Bitte geben Sie eine URL an.');
            return;
        }

        setIsBusy(true);
        dispatch(showLoadingOverlay('OpenID Endpoint wird abgerufen'));
        apiService
            .prepare(endpointConfigUrl)
            .then((preparedIDP) => {
                setEndpointConfigUrl('');
                handleInputPatch({
                    authorizationEndpoint: preparedIDP.authorizationEndpoint,
                    tokenEndpoint: preparedIDP.tokenEndpoint,
                    userinfoEndpoint: preparedIDP.userinfoEndpoint,
                    endSessionEndpoint: preparedIDP.endSessionEndpoint,
                    defaultScopes: preparedIDP.defaultScopes,
                });
            })
            .catch((err) => {
                console.error(err);

                const errorMessage = 'Fehler beim Laden der Konfiguration. Bitte überprüfen Sie die URL.';
                setEndpointConfigUrlError(errorMessage);
                dispatch(showErrorSnackbar(errorMessage));
            })
            .finally(() => {
                setIsBusy(false);
                dispatch(hideLoadingOverlay());
            });
    };

    const handleRefreshRelatedEntities = () => {
        setIsBusy(true);
        fetchRelatedEntities(api)
            .then(({assets, secrets}) => {
                setAssets(assets);
                setSecrets(secrets);
            })
            .then(() => {
                dispatch(showSuccessSnackbar('Auswahllisten wurden erfolgreich neu geladen.'));
            })
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Fehler beim Aktualisieren der Auswahllisten.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    const handleSave = () => {
        if (identityProvider == null) {
            return;
        }

        const validationResult = validate();

        if (!validationResult) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);

        if (isStringNullOrEmpty(identityProvider.key)) {
            apiService
                .create(identityProvider)
                .then((newPaymentProvider) => {
                    setItem(newPaymentProvider);
                    reset();

                    dispatch(showSuccessSnackbar('Neuer Nutzerkontenanbieter erfolgreich angelegt.'));

                    // use setTimeout instead of useEffect to prevent unnecessary rerender
                    setTimeout(() => {
                        navigate(`/identity-providers/${newPaymentProvider.key}`, {replace: true});
                    }, 0);
                })
                .catch(err => {
                    if (err.status === 409) {
                        dispatch(showErrorSnackbar('Es existieren noch veröffentlichte Formulare, die diesen Nutzerkontenanbieter verwenden'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    }
                })
                .finally(() => {
                    setIsBusy(false);
                });
        } else {
            apiService
                .update(identityProvider.key, identityProvider)
                .then((updatedPaymentProvider) => {
                    setItem(updatedPaymentProvider);
                    reset();

                    dispatch(showSuccessSnackbar('Änderungen am Nutzerkontenanbieter erfolgreich gespeichert.'));
                })
                .catch(err => {
                    if (err.status === 409) {
                        dispatch(showErrorSnackbar('Es existieren noch veröffentlichte Formulare, die diesen Nutzerkontenanbieter verwenden'));
                    } else {
                        console.error(err);
                        dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    }
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const checkAndHandleDelete = async () => {
        if (isStringNullOrEmpty(identityProvider.key)) {
            return;
        }

        setIsBusy(true);

        try {
            const relatedForms = await new FormsApiService(api)
                .listAll({
                    identityProviderKey: identityProvider.key,
                });

            if (relatedForms.content.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = relatedForms.content.slice(0, maxVisibleLinks).map(f => ({
                    label: f.title,
                    to: `/forms/${f.id}`,
                }));

                if (relatedForms.content.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: 'Weitere Formulare anzeigen…',
                        to: `/payment-provider/${identityProvider.key}/forms`,
                    });
                }

                setRelatedEntities(processedLinks);
                setShowConstraintDialog(true);
            } else {
                setShowConfirmDialog(true);
            }
        } catch (error) {
            console.error(error);
            dispatch(showErrorSnackbar('Fehler beim Prüfen der Löschbarkeit.'));
        } finally {
            setIsBusy(false);
        }
    };

    const handleDelete = () => {
        if (isStringNullOrEmpty(identityProvider.key)) {
            dispatch(showErrorSnackbar('Der Nutzerkontenanbieter konnte nicht gelöscht werden.'));
            return;
        }

        setIsBusy(true);

        apiService
            .destroy(identityProvider.key)
            .then(() => {
                reset(); // prevent change blocker by resetting unsaved changes
                dispatch(showSuccessSnackbar('Der Nutzerkontenanbieter wurde erfolgreich gelöscht.'));
                navigate('/identity-providers', {
                    replace: true,
                });
            })
            .catch(err => {
                console.error(err);
                dispatch(showErrorSnackbar('Beim Löschen des Nutzerkontenanbieters ist ein Fehler aufgetreten.'));
                setIsBusy(false);
            });
    };

    const handleStatusChange = async (newValue: boolean) => {
        // Bei Aktivierung und wenn es sich um ein offizielles Nutzerkonto handelt
        if (newValue === true && identityProvider.type !== IdentityProviderType.Custom) {
            const confirmed = await showConfirm({
                title: 'Hinweis zur Einrichtung',
                confirmButtonText: 'Ja, Einrichtung wurde durchgeführt',
                children: (
                    <>
                        <Typography gutterBottom>
                            Bitte bestätigen Sie, dass Sie die Hinweise zur erstmaligen Einrichtung des Nutzerkontos gelesen und umgesetzt haben.
                        </Typography>
                        <Typography gutterBottom>
                            Diese Hinweise finden Sie im Reiter <strong>Einrichtung</strong>.
                        </Typography>
                    </>
                ),
            });

            if (!confirmed) {
                return;
            }
        }

        // Bei Deaktivierung
        if (newValue === false) {
            const confirmed = await showConfirm({
                title: 'Deaktivierung bestätigen',
                confirmButtonText: 'Ja, Nutzerkontenanbieter deaktivieren',
                children: (
                    <>
                        <Typography gutterBottom>
                            Wenn Sie den Nutzerkontenanbieter deaktivieren, wird das Nutzerkonto automatisch aus Formularen mit dem Status "In Bearbeitung" entfernt.
                        </Typography>
                        <Typography gutterBottom>
                            Bitte beachten Sie, dass Sie den Nutzerkontenanbieter speichern müssen, um diese Änderung zu übernehmen.
                        </Typography>
                    </>
                ),
            });

            if (!confirmed) {
                return;
            }
        }

        handleInputChange('isEnabled')(newValue);
    };

    const defaultScopesError = getIndexedFieldError(
        errors,
        'defaultScopes',
        'Bitte entfernen Sie leere Scopes.'
    );

    const attributesError = getIndexedFieldError(
        errors,
        'attributes',
        'Bitte füllen Sie alle Attributszuweisungen vollständig aus.'
    );

    const additionalParamsError = getIndexedFieldError(
        errors,
        'additionalParams',
        'Bitte füllen Sie alle Schlüssel/Wert-Paare vollständig aus.'
    );

    return (
        <Box>
            {
                isNewItem &&
                <>
                    <Typography
                        variant="h5"
                        sx={{mt: 1.5, mb: 1}}
                    >
                        Konfiguration von OpenID Endpoint laden (optional)
                    </Typography>

                    <Typography sx={{mb: 3, maxWidth: 900}}>
                        Wenn Ihr Nutzerkontenanbieter dies anbietet, können Sie die Konfiguration automatisch laden. Bitte geben Sie hierfür den Link zur OpenID Endpoint Konfiguration ein und klicken Sie auf "Konfiguration laden".
                    </Typography>
                    <Box
                        sx={{
                            display: 'flex',
                            gap: '1rem',
                            alignItems: 'start',
                        }}
                    >
                        <TextFieldComponent
                            label="Link zu OpenID Endpoint Konfiguration"
                            placeholder="https://example.com/.well-known/openid-configuration"
                            value={endpointConfigUrl}
                            onChange={val => setEndpointConfigUrl(val ?? '')}
                            error={endpointConfigUrlError}
                            size={'small'}
                        />

                        <Button
                            onClick={handlePrepareEntity}
                            sx={{
                                flexShrink: 0,
                                mt: 2,
                            }}
                            variant={'contained'}
                        >
                            Konfiguration laden
                        </Button>
                    </Box>

                    <Divider sx={{my: 4}} />
                </>
            }

            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Nutzerkontenanbieter konfigurieren
            </Typography>

            <Typography sx={{mb: 3, maxWidth: 900}}>
                Konfigurieren Sie den Nutzerkontenanbieter, um Nutzerkonten dieses Anbieters zur Authentifizierung in Formularen verwenden zu können. Sie können die Einstellungen jederzeit anpassen, auch wenn die Konfiguration bereits für
                Formulare verwendet wird.
            </Typography>

            <Grid
                container
                spacing={2}
            >
                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <TextFieldComponent
                        label="Name"
                        placeholder="z.B. Keycloak"
                        required
                        value={identityProvider.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        disabled={inputsDisabled || isSystemProvider}
                        error={errors.name}
                        hint="Name des Nutzerkontenanbieters. Sichtbar auch für Antragsteller:innen im Formular."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                    sx={{
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                        transform: 'translateY(-10px)',
                    }}
                >
                    {
                        identityProvider.iconAssetKey &&
                        <Box
                            sx={{
                                display: 'inline-block',
                                py: 1,
                                px: 2,
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                            }}
                        >
                            <IdentityProviderIcon
                                name={identityProvider.name}
                                type={identityProvider.type}
                                iconAssetKey={identityProvider.iconAssetKey}
                            />
                        </Box>
                    }
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <TextFieldComponent
                        label="Interne Beschreibung"
                        placeholder="z.B. Keycloak für Testumgebung"
                        required
                        value={identityProvider.description}
                        onChange={handleInputChange('description')}
                        onBlur={handleInputBlur('description')}
                        multiline={true}
                        disabled={inputsDisabled || isSystemProvider}
                        error={errors.description}
                        hint="Interne Beschreibung des Nutzerkontenanbieters zur besseren Identifizierbarkeit. Sichtbar nur für Mitarbeiter:innen."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    {
                        identityProvider.type != IdentityProviderType.Custom &&
                        <AlertComponent
                            color="info"
                            sx={{mt: 2}}
                        >
                            <strong>Hinweis:</strong> Die Konfigurationen für die offiziellen Nutzerkonten von Bund und Ländern werden von Gover bereitgestellt und sind nicht veränderbar.
                        </AlertComponent>
                    }
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <SelectFieldComponent
                        label="Logo-Grafik"
                        value={identityProvider.iconAssetKey ?? undefined}
                        onChange={(value) => {
                            if (isStringNullOrEmpty(value)) {
                                handleInputChange('iconAssetKey')(undefined);
                            } else {
                                handleInputChange('iconAssetKey')(value);
                            }
                        }}
                        disabled={inputsDisabled || isSystemProvider}
                        required
                        options={
                            assets
                                .map((secret) => ({
                                    value: secret.key,
                                    label: secret.filename,
                                }))
                        }
                        error={errors.iconAssetKey}
                        hint={'Das Logo dient im Formular als Erkennungsmerkmal. Nutzen Sie am besten eine Vektordatei (z.B. SVG) für eine optimale Darstellung. Die Datei muss den öffentlichen Zugriff zulassen.'}
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                />

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <CheckboxFieldComponent
                        label="Aktiv (kann in konfigurierten Formularen genutzt werden)"
                        value={identityProvider.isEnabled}
                        onChange={handleStatusChange}
                        variant="switch"
                        error={errors.isEnabled}
                        hint="Gibt an, ob dieser Nutzerkontenanbieter aktiviert ist. Bei temporären technischen Problemen o.Ä. kann der Anbieter deaktiviert werden, ohne die Konfiguration zu verlieren."
                        disabled={inputsDisabled}
                    />
                </Grid>
                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <CheckboxFieldComponent
                        label="Es handelt sich um eine vorproduktive Konfiguration"
                        value={identityProvider.isTestProvider}
                        onChange={handleInputChange('isTestProvider')}
                        variant="switch"
                        error={errors.isTestProvider}
                        hint="Gibt an, ob diese Konfiguration für eine Testinstanz bestimmt ist. Das System verhindert den Einsatz von Testkonfigurationen in der Live-Umgebung, um Fehlkonfigurationen zu vermeiden."
                        disabled={inputsDisabled || isSystemProvider}
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <Typography
                        variant="h6"
                        sx={{mt: 4, mb: 0}}
                    >
                        Technische Konfiguration
                    </Typography>
                </Grid>
                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Metadaten-Identifikator"
                        required
                        value={identityProvider.metadataIdentifier}
                        onChange={handleInputChange('metadataIdentifier')}
                        onBlur={handleInputBlur('metadataIdentifier')}
                        disabled={inputsDisabled || isSystemProvider}
                        error={errors.metadataIdentifier}
                        hint="Technische ID zur Zuweisung der Metadaten. Wenn Sie eine Produktive und eine Testkonfiguration anlegen, verwenden Sie dieselbe ID, um zwischen beiden Umgebungen umschalten zu können."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Endpunkt zur Authorisierung"
                        placeholder="https://auth.example.com/xyz oder /idp/xyz"
                        required
                        value={identityProvider.authorizationEndpoint}
                        onChange={handleInputChange('authorizationEndpoint')}
                        onBlur={handleInputBlur('authorizationEndpoint')}
                        disabled={inputsDisabled || isSystemProvider}
                        error={errors.authorizationEndpoint}
                        hint="Pfad oder vollständige URL zum Authorization-Endpoint des Identity-Providers."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Endpunkt zum Erstellen des Tokens"
                        placeholder="https://auth.example.com/xyz oder /idp/xyz"
                        required
                        value={identityProvider.tokenEndpoint}
                        onChange={handleInputChange('tokenEndpoint')}
                        onBlur={handleInputBlur('tokenEndpoint')}
                        disabled={inputsDisabled || isSystemProvider}
                        error={errors.tokenEndpoint}
                        hint="Pfad oder vollständige URL zum Token-Endpoint, über den Access- bzw. ID-Tokens ausgegeben werden."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Endpunkt für Informationen über die Nutzer:in"
                        placeholder="https://auth.example.com/xyz oder /idp/xyz"
                        value={identityProvider.userinfoEndpoint ?? undefined}
                        onChange={val => {
                            handleInputChange('userinfoEndpoint')(val == null || val.length === 0 ? undefined : val);
                        }}
                        onBlur={handleInputBlur('userinfoEndpoint')}
                        disabled={inputsDisabled || isSystemProvider}
                        error={errors.userinfoEndpoint}
                        hint="Pfad oder vollständige URL zum Userinfo-Endpoint, um Benutzerinformationen abzufragen."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Endpunkt zum Beenden der Session"
                        placeholder="https://auth.example.com/xyz oder /idp/xyz"
                        value={identityProvider.endSessionEndpoint ?? undefined}
                        onChange={val => {
                            handleInputChange('endSessionEndpoint')(val == null || val.length === 0 ? undefined : val);
                        }}
                        onBlur={handleInputBlur('endSessionEndpoint')}
                        disabled={inputsDisabled || isSystemProvider}
                        error={errors.endSessionEndpoint}
                        hint="Pfad oder vollständige URL zum End-Session-Endpoint, über den Benutzer abgemeldet werden. Ohne diesen Endpoint bleibt die Single-Sign-On-Session bestehen und die Nutzer:inn kann sich mehrfach hintereinander authorisieren, ohne erneut Anmeldedaten eingeben zu müssen."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <TextFieldComponent
                        label="Client ID"
                        required
                        value={identityProvider.clientId}
                        onChange={handleInputChange('clientId')}
                        onBlur={handleInputBlur('clientId')}
                        disabled={inputsDisabled || isSystemProvider}
                        error={errors.clientId}
                        hint="ID des Clients unter dem der Nutzerkontenanbieter erreichbar ist."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <SelectFieldComponent
                        label="Client Secret"
                        value={identityProvider.clientSecretKey ?? undefined}
                        onChange={(value) => {
                            if (isStringNullOrEmpty(value)) {
                                handleInputChange('clientSecretKey')(undefined);
                            } else {
                                handleInputChange('clientSecretKey')(value);
                            }
                        }}
                        disabled={inputsDisabled || isSystemProvider}
                        options={
                            secrets
                                .map((secret) => ({
                                    value: secret.key,
                                    label: secret.name,
                                }))
                        }
                        hint={'Nur notwendig, wenn der Nutzerkontenanbieter dies erfordert.'}
                    />
                </Grid>
            </Grid>

            <StringListInput2
                label="Scopes"
                hint=""
                addLabel="Scope hinzufügen"
                noItemsHint="Keine Scopes vorhanden"
                value={identityProvider.defaultScopes}
                onChange={(value) => {
                    handleInputChange('defaultScopes')(value ?? []);
                }}
                allowEmpty={true}
                disabled={inputsDisabled || isSystemProvider}
                error={defaultScopesError}
                sx={{my: 4}}
            />

            <TableFieldComponent2<IdentityAdditionalParameter>
                label="Zusätzliche Parameter"
                fields={[
                    {
                        key: 'key',
                        label: 'Schlüssel',
                        type: 'string',
                        disabled: inputsDisabled || isSystemProvider,
                    },
                    {
                        key: 'value',
                        label: 'Wert',
                        type: 'string',
                        disabled: inputsDisabled || isSystemProvider,
                    },
                ]}
                createDefaultRow={() => ({key: '', value: ''})}
                value={identityProvider.additionalParams}
                onChange={(value) => {
                    handleInputChange('additionalParams')(value ?? []);
                }}
                disabled={inputsDisabled || isSystemProvider}
                error={additionalParamsError}
                sx={{my: 4}}
            />

            <Grid
                item
                xs={12}
            >
                <Typography
                    variant="h6"
                    sx={{mt: 4, mb: 0}}
                >
                    Attributszuweisungen
                </Typography>
            </Grid>

            <TableFieldComponent2<IdentityAttributeMapping>
                label="Attributszuweisungen"
                fields={[
                    {
                        key: 'label',
                        label: 'Titel',
                        type: 'string',
                        disabled: inputsDisabled || isSystemProvider,
                    },
                    {
                        key: 'description',
                        label: 'Beschreibung',
                        type: 'string',
                        disabled: inputsDisabled || isSystemProvider,
                    },
                    {
                        key: 'keyInData',
                        label: 'Feldname',
                        type: 'string',
                        disabled: inputsDisabled || isSystemProvider,
                    },
                    {
                        key: 'displayAttribute',
                        label: 'Anzeigeattribut',
                        type: 'boolean',
                        disabled: inputsDisabled || isSystemProvider,
                    },
                ]}
                hint="Geben Sie hier die Attributszuweisungen an, die für den Nutzerkontenanbieter gelten sollen."
                createDefaultRow={() => ({
                    label: '',
                    description: '',
                    keyInData: '',
                    displayAttribute: false,
                })}
                value={identityProvider.attributes}
                onChange={(value) => {
                    handleInputChange('attributes')(value ?? []);
                }}
                disabled={inputsDisabled || isSystemProvider}
                addTooltip="Attributszuweisung hinzufügen"
                deleteTooltip="Attributszuweisung löschen"
                noRowsPlaceholder="Keine Attributszuweisungen vorhanden"
                helpDialog={{
                    title: 'Attributszuweisungen',
                    content: (
                        <Box>
                            <Typography>
                                Hier können Sie die Attributszuweisungen (Claim-Zuordnung) für den Nutzerkontenanbieter hinterlegen. Bitte beachten Sie, dass diese Einstellungen nur für den ausgewählten Anbieter gelten.
                            </Typography>
                            <ul style={{marginTop: '1rem', paddingLeft: '1.1rem'}}>
                                <li>
                                    <strong>Titel</strong> – Anzeigename, der später in der Gover-Oberfläche
                                    erscheint (z.&nbsp;B. „E-Mail“ oder „Nachname“).
                                </li>

                                <li>
                                    <strong>Beschreibung</strong> – Kurze Erklärung, wofür das Attribut
                                    verwendet wird bzw. welche Daten es enthält.
                                </li>

                                <li>
                                    <strong>Feldname</strong> – Schlüssel in den Daten / Claim-Name
                                    (<code>email</code>, <code>given_name</code>, …), so wie er im <em>userinfo</em>-Response bzw. ID-Token vorkommt.
                                </li>

                                <li>
                                    <strong>Anzeigeattribut</strong> – Steuert, ob der Wert später
                                    zur Identifikation in Übersichten von Gover (z. B. in Anträgen) angezeigt wird.
                                </li>
                            </ul>
                        </Box>
                    ),
                }}
                error={attributesError}
                sx={{my: 4}}
            />

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
                    !isSystemProvider &&
                    <Tooltip title={'Aktualisieren Sie die Auswahllisten für z.B. Dateien und Geheimnisse, falls Sie diese nicht vorab hinterlegt haben.'}>
                        <Button
                            onClick={handleRefreshRelatedEntities}
                            disabled={isBusy}
                        >
                            Auswahllisten neu laden <HelpIconOutlined
                            fontSize="small"
                            sx={{ml: 1}}
                        />
                        </Button>
                    </Tooltip>
                }

                {
                    isStringNotNullOrEmpty(identityProvider.key) &&
                    !isSystemProvider &&
                    originalIdentityProvider != null &&
                    <Box
                        sx={{
                            marginLeft: 'auto',
                        }}
                    >
                        {
                            !originalIdentityProvider.isEnabled &&
                            <Button
                                variant="outlined"
                                onClick={checkAndHandleDelete}
                                disabled={isBusy}
                                color="error"

                                startIcon={<DeleteOutlinedIcon />}
                            >
                                Löschen
                            </Button>
                        }

                        {
                            originalIdentityProvider.isEnabled &&
                            <Tooltip title="Nur ein inaktiver Nutzerkontenanbieter kann gelöscht werden. Sie müssen den Anbieter zunächst deaktivieren und speichern, bevor Sie ihn löschen können.">
                                <span>
                                    <Button
                                        variant="outlined"
                                        disabled={true}
                                        color="error"
                                        startIcon={<DeleteOutlinedIcon />}
                                    >
                                        Löschen
                                    </Button>
                                </span>
                            </Tooltip>
                        }
                    </Box>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Nutzerkontenanbieter löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={identityProvider.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diesen Nutzerkontenanbieter wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Dieser Nutzerkontenanbieter kann nicht gelöscht werden, da er noch in Formularen verwendet wird."
                solutionText="Bitte ändern Sie die Einstellungen zur Einbindung von Nutzerkonten in diesen Formularen und versuchen Sie es erneut:"
                links={relatedEntities ?? undefined}
            />
        </Box>
    );
}

async function fetchRelatedEntities(api: Api): Promise<{
    assets: Asset[];
    secrets: SecretEntityResponseDTO[];
}> {
    const [assets, secrets] = await Promise
        .all([
            new AssetsApiService(api)
                .listAll({
                    contentType: 'image/',
                    isPrivate: false,
                }),
            new SecretsApiService(api)
                .listAll(),
        ]);
    return {
        assets: assets.content,
        secrets: secrets.content,
    };
}