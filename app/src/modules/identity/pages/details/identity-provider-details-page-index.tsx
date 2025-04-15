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
    authorizationEndpoint: yup.string()
        .trim()
        .min(1, 'Der Autorisierungsendpunkt ist ein Pflichtfeld.')
        .max(255, 'Der Autorisierungsendpunkt darf maximal 255 Zeichen lang sein.')
        .url('Der Autorisierungsendpunkt muss eine gültige URL sein.')
        .required('Der Autorisierungsendpunkt ist ein Pflichtfeld.'),
    tokenEndpoint: yup.string()
        .trim()
        .min(1, 'Der Tokenendpunkt ist ein Pflichtfeld.')
        .max(255, 'Der Tokenendpunkt darf maximal 255 Zeichen lang sein.')
        .url('Der Tokenendpunkt muss eine gültige URL sein.')
        .required('Der Tokenendpunkt ist ein Pflichtfeld.'),
    userinfoEndpoint: yup.string()
        .trim()
        .max(255, 'Der Userinfoendpunkt darf maximal 255 Zeichen lang sein.')
        .url('Der Userinfoendpunkt muss eine gültige URL sein.')
        .nullable(),
    endSessionEndpoint: yup.string()
        .trim()
        .max(255, 'Der End-Session-Endpunkt darf maximal 255 Zeichen lang sein.')
        .url('Der End-Session-Endpunkt muss eine gültige URL sein.')
        .nullable(),
    clientId: yup.string()
        .trim()
        .min(1, 'Die Client-ID ist ein Pflichtfeld.')
        .max(128, 'Die Client-ID darf maximal 128 Zeichen lang sein.')
        .required('Die Client-ID ist ein Pflichtfeld.'),
    metadataIdentifier: yup.string()
        .trim()
        .min(1, 'Der Metadaten-Identifikator ist ein Pflichtfeld.')
        .max(64, 'Der Metadaten-Identifikator darf maximal 64 Zeichen lang sein.')
        .required('Der Metadaten-Identifikator ist ein Pflichtfeld.'),
});

export function IdentityProviderDetailsPageIndex() {
    useAdminGuard();

    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const api = useApi();

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

    const {
        currentItem: identityProvider,
        errors,
        hasNotChanged,
        handleInputPatch,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<IdentityProviderDetailsDTO>(originalIdentityProvider, formSchema as any);

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
        isBusy || identityProvider == null || identityProvider.type !== IdentityProviderType.Custom
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
                    console.error(err);
                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
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
                    console.error(err);
                    dispatch(showErrorSnackbar('Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
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
                    // identityProvider: identityProvider.key, TODO: Fix this check
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

    return (
        <Box>
            {
                isNewItem &&
                <>
                    <Box
                        sx={{
                            display: 'flex',
                            gap: '1rem',
                            alignItems: 'center',
                        }}
                    >
                        <TextFieldComponent
                            label="Aus Konfiguration laden"
                            placeholder="https://example.com/.well-known/openid-configuration"
                            value={endpointConfigUrl}
                            onChange={val => setEndpointConfigUrl(val ?? '')}
                            hint="Über den Link zur OpenID Endpoint Konfiguration können Sie die Konfiguration des Nutzerkontenanbieters automatisch laden."
                            error={endpointConfigUrlError}
                        />

                        <Button
                            onClick={handlePrepareEntity}
                            sx={{
                                flexShrink: 0,
                            }}
                        >
                            Konfiguration laden
                        </Button>
                    </Box>

                    <Divider sx={{my: 4}} />
                </>
            }

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
                        disabled={inputsDisabled}
                        error={errors.name}
                        hint="Name des Nutzerkontenanbieters. Sichtbar auch für Antragsteller:innen im Formular."
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
                    <TextFieldComponent
                        label="Interne Beschreibung"
                        placeholder="z.B. Keycloak für Testumgebung"
                        required
                        value={identityProvider.description}
                        onChange={handleInputChange('description')}
                        onBlur={handleInputBlur('description')}
                        multiline={true}
                        disabled={inputsDisabled}
                        error={errors.description}
                        hint="Interne Beschreibung des Nutzerkontenanbieters zur besseren Identifizierbarkeit. Sichtbar nur für Mitarbeiter:innen."
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
                    <SelectFieldComponent
                        label="Logo"
                        value={identityProvider.iconAssetKey ?? undefined}
                        onChange={(value) => {
                            if (isStringNullOrEmpty(value)) {
                                handleInputChange('iconAssetKey')(undefined);
                            } else {
                                handleInputChange('iconAssetKey')(value);
                            }
                        }}
                        disabled={inputsDisabled}
                        options={
                            assets
                                .map((secret) => ({
                                    value: secret.key,
                                    label: secret.filename,
                                }))
                        }
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                    md={6}
                >
                    <IdentityProviderIcon
                        name={identityProvider.name}
                        type={identityProvider.type}
                        iconAssetKey={identityProvider.iconAssetKey}
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Host"
                        placeholder="https://keycloak.example.com"
                        required
                        value={identityProvider.authorizationEndpoint}
                        onChange={handleInputChange('authorizationEndpoint')}
                        onBlur={handleInputBlur('authorizationEndpoint')}
                        disabled={inputsDisabled}
                        error={errors.authorizationEndpoint}
                        hint="Host unter dem der Nutzerkontenanbieter erreichbar ist. Sichtbar auch für Antragsteller:innen im Formular."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Realm"
                        placeholder="z.B. master"
                        required
                        value={identityProvider.tokenEndpoint}
                        onChange={handleInputChange('tokenEndpoint')}
                        onBlur={handleInputBlur('tokenEndpoint')}
                        disabled={inputsDisabled}
                        error={errors.tokenEndpoint}
                        hint="Realm unter dem der Nutzerkontenanbieter erreichbar ist. Sichtbar auch für Antragsteller:innen im Formular."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Realm"
                        placeholder="z.B. master"
                        required
                        value={identityProvider.userinfoEndpoint ?? undefined}
                        onChange={val => {
                            handleInputChange('userinfoEndpoint')(val == null || val.length === 0 ? undefined : val);
                        }}
                        onBlur={val => {
                            handleInputChange('userinfoEndpoint')(val == null || val.length === 0 ? undefined : val);
                        }}
                        disabled={inputsDisabled}
                        error={errors.userinfoEndpoint}
                        hint="Realm unter dem der Nutzerkontenanbieter erreichbar ist. Sichtbar auch für Antragsteller:innen im Formular."
                    />
                </Grid>

                <Grid
                    item
                    xs={12}
                >
                    <TextFieldComponent
                        label="Realm"
                        placeholder="z.B. master"
                        required
                        value={identityProvider.endSessionEndpoint ?? undefined}
                        onChange={val => {
                            handleInputChange('endSessionEndpoint')(val == null || val.length === 0 ? undefined : val);
                        }}
                        onBlur={val => {
                            handleInputChange('endSessionEndpoint')(val == null || val.length === 0 ? undefined : val);
                        }}
                        disabled={inputsDisabled}
                        error={errors.endSessionEndpoint}
                        hint="Realm unter dem der Nutzerkontenanbieter erreichbar ist. Sichtbar auch für Antragsteller:innen im Formular."
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
                        disabled={inputsDisabled}
                        error={errors.clientId}
                        hint="ID des Clients unter dem der Nutzerkontenanbieter erreichbar ist. Sichtbar auch für Antragsteller:innen im Formular."
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
                        disabled={inputsDisabled}
                        options={
                            secrets
                                .map((secret) => ({
                                    value: secret.key,
                                    label: secret.name,
                                }))
                        }
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
                disabled={inputsDisabled}
            />

            <TableFieldComponent2<IdentityAdditionalParameter>
                label="Zusätzliche Parameter"
                fields={[
                    {
                        key: 'key',
                        label: 'Schlüssel',
                        type: 'string',
                        disabled: inputsDisabled,
                    },
                    {
                        key: 'value',
                        label: 'Wert',
                        type: 'string',
                        disabled: inputsDisabled,
                    },
                ]}
                createDefaultRow={() => ({key: '', value: ''})}
                value={identityProvider.additionalParams}
                onChange={(value) => {
                    handleInputChange('additionalParams')(value ?? []);
                }}
                disabled={inputsDisabled}
            />

            <TableFieldComponent2<IdentityAttributeMapping>
                label="Attributszuweisungen"
                fields={[
                    {
                        key: 'label',
                        label: 'Titel',
                        type: 'string',
                        disabled: inputsDisabled,
                    },
                    {
                        key: 'description',
                        label: 'Beschreibung',
                        type: 'string',
                        disabled: inputsDisabled,
                    },
                    {
                        key: 'keyInData',
                        label: 'Feldname',
                        type: 'string',
                        disabled: inputsDisabled,
                    },
                ]}
                hint="Geben Sie hier die Attributszuweisungen an, die für den Nutzerkontenanbieter gelten sollen."
                createDefaultRow={() => ({label: '', description: '', keyInData: ''})}
                value={identityProvider.attributes}
                onChange={(value) => {
                    handleInputChange('attributes')(value ?? []);
                }}
                disabled={inputsDisabled}
                addTooltip="Attributszuweisung hinzufügen"
                deleteTooltip="Attributszuweisung löschen"
                noRowsPlaceholder="Keine Attributszuweisungen vorhanden"
                helpDialog={{
                    title: 'Attributszuweisungen',
                    content: (
                        <Box>
                            <Typography>
                                Hier können Sie die Attributszuweisungen für den Nutzerkontenanbieter anpassen.
                            </Typography>
                            <Typography>
                                Bitte beachten Sie, dass diese Einstellungen nur für den ausgewählten Anbieter gelten.
                            </Typography>
                        </Box>
                    ),
                }}
            />

            <CheckboxFieldComponent
                label="Es handelt sich um eine vorproduktive Konfiguration"
                value={identityProvider.isTestProvider}
                onChange={handleInputChange('isTestProvider')}
                variant="switch"
                error={errors.isTestProvider}
                hint="Gibt an, ob diese Konfiguration für eine Testinstanz bestimmt ist. Das System verhindert den Einsatz von Testkonfigurationen in der Live-Umgebung, um Fehlkonfigurationen zu vermeiden."
                disabled={inputsDisabled}
            />

            <CheckboxFieldComponent
                label="Aktiv"
                value={identityProvider.isEnabled}
                onChange={handleInputChange('isEnabled')}
                variant="switch"
                error={errors.isEnabled}
                hint="Gibt an, ob diese Konfiguration für eine Testinstanz bestimmt ist. Das System verhindert den Einsatz von Testkonfigurationen in der Live-Umgebung, um Fehlkonfigurationen zu vermeiden."
                disabled={inputsDisabled}
            />

            <TextFieldComponent
                label="Metadaten-Identifikator"
                required
                value={identityProvider.metadataIdentifier}
                onChange={handleInputChange('metadataIdentifier')}
                onBlur={handleInputBlur('metadataIdentifier')}
                disabled={inputsDisabled}
                error={errors.metadataIdentifier}
                hint="Technisch"
            />

            {
                identityProvider.type === IdentityProviderType.Custom &&
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

                    <Tooltip title={'Aktualisieren Sie die Auswahllisten für z.B. Zertifikatsdateien und Geheimnisse, falls Sie diese nicht vorab hinterlegt haben.'}>
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

                    {
                        isStringNotNullOrEmpty(identityProvider.key) &&
                        <Button
                            variant="outlined"
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
                title="Zahlungsdienstleister löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={identityProvider.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diesen Zahlungsdienstleister wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>

            <ConstraintDialog
                open={showConstraintDialog}
                onClose={() => setShowConstraintDialog(false)}
                message="Dieser Zahlungsdienstleister kann nicht gelöscht werden, da er noch in Formularen verwendet wird."
                solutionText="Bitte ändern Sie die Einstellungen für Online-Zahlungen dieser Formulare und versuchen Sie es erneut:"
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