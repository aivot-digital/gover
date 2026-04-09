import {Box, Button, Grid, Typography} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useNavigate} from 'react-router-dom';
import {isStringNullOrEmpty} from '../../../../utils/string-utils';
import {PaymentProvidersApiService} from '../../payment-providers-api-service';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {PaymentProviderAdditionalData} from './payment-provider-details-page-additional-data';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import {PaymentProviderResponseDTO} from '../../dtos/payment-provider-response-dto';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker-2';
import {ConstraintDialog} from '../../../../dialogs/constraint-dialog/constraint-dialog';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {ConstraintLinkProps} from '../../../../dialogs/constraint-dialog/constraint-link-props';
import HelpIconOutlined from '@mui/icons-material/HelpOutline';
import Tooltip from '@mui/material/Tooltip';
import * as yup from 'yup';
import {goverSchemaToYup, mapFormManagerErrorsToComputedErrors} from '../../../../utils/gover-schema-to-yup';
import {PaymentProviderDefinitionResponseDTO} from '../../dtos/payment-provider-definition-response-dto';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {useConfirm} from '../../../../providers/confirm-provider';
import {VFormVersionWithDetailsService} from '../../../forms/services/v-form-version-with-details-api-service';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {
    ElementDerivationContext,
} from '../../../elements/components/element-derivation-context';
import {ComputedElementErrors, DerivedRuntimeElementData} from '../../../../models/element-data';

type PaymentProviderEditableFields =
    'name' |
    'description' |
    'providerKey' |
    'providerVersion' |
    'isEnabled' |
    'isTestProvider' |
    'config';

type PaymentProviderEditable = Pick<PaymentProviderResponseDTO, PaymentProviderEditableFields>;

type PaymentProviderYupSchemaType = yup.ObjectSchema<PaymentProviderEditable>;

export const BasePaymentProviderYupSchema: PaymentProviderYupSchemaType = yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name des Zahlungsdienstleisters muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name des Zahlungsdienstleisters darf maximal 96 Zeichen lang sein.')
        .required('Der Name des Zahlungsdienstleisters ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .min(10, 'Die Beschreibung muss mindestens 10 Zeichen lang sein.')
        .max(500, 'Die Beschreibung darf maximal 500 Zeichen lang sein.')
        .required('Die Beschreibung des Zahlungsdienstleisters ist ein Pflichtfeld.'),
    providerKey: yup.string()
        .trim()
        .required('Der Anbieter des Zahlungsdienstleisters ist ein Pflichtfeld.'),
    providerVersion: yup.number()
        .min(1, 'Die Version des Zahlungsdienstleisters muss mindestens 1 sein.')
        .required('Die Version des Zahlungsdienstleisters ist ein Pflichtfeld.'),
    isEnabled: yup.boolean()
        .default(false),
    isTestProvider: yup.boolean()
        .default(false),
    config: yup.object()
        .required('Die Konfiguration des Zahlungsdienstleisters ist ein Pflichtfeld.'),
});

export function PaymentProviderDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const showConfirm = useConfirm();

    const [derivedRuntimeConfigData, setDerivedRuntimeConfigData] = useState<DerivedRuntimeElementData | null>(null);
    const [paymentProviderSchema, setPaymentProviderSchema] = useState<PaymentProviderYupSchemaType>(BasePaymentProviderYupSchema);
    const [clientSideValidationErrors, setClientSideValidationErrors] = useState<ComputedElementErrors | null>(null);

    const {
        item: originalPaymentProvider,
        setItem: setOriginalPaymentProvider,
        isNewItem: isNewPaymentProvider,
        additionalData = {
            definitions: [],
        } as PaymentProviderAdditionalData,
        setAdditionalData,
        isBusy,
        setIsBusy,
        isEditable,
    } = useGenericDetailsPageContext<PaymentProviderResponseDTO, PaymentProviderAdditionalData>();

    const {
        definitions: availablePaymentProviderDefinitions,
    } = additionalData;

    const {
        currentItem: editedPaymentProvider,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate: validateFormManager,
        reset: resetFormManager,
    } = useFormManager<Pick<PaymentProviderResponseDTO, PaymentProviderEditableFields>>(
        originalPaymentProvider,
        paymentProviderSchema,
        true,
    );

    const selectedPaymentProviderDefinition: PaymentProviderDefinitionResponseDTO | undefined = useMemo(() => {
        return availablePaymentProviderDefinitions.find(def => (
            def.key === editedPaymentProvider?.providerKey &&
            def.version === editedPaymentProvider?.providerVersion
        ));
    }, [availablePaymentProviderDefinitions, editedPaymentProvider?.providerKey, editedPaymentProvider?.providerVersion]);

    const changeBlocker = useChangeBlocker({
        original: originalPaymentProvider,
        edited: editedPaymentProvider,
        useDeepEquals: true,
    });

    const [showConfirmDialog, setShowConfirmDialog] = useState(false);
    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [relatedEntities, setRelatedEntities] = useState<ConstraintLinkProps[] | null>(null);

    useEffect(() => {
        if (selectedPaymentProviderDefinition?.configLayout == null) {
            setPaymentProviderSchema(BasePaymentProviderYupSchema);
            return;
        }

        if (derivedRuntimeConfigData == null) {
            setPaymentProviderSchema(BasePaymentProviderYupSchema);
            return;
        }

        const configSchemaFields = goverSchemaToYup(
            selectedPaymentProviderDefinition.configLayout,
            derivedRuntimeConfigData.elementStates,
        );
        const configSchema: yup.ObjectSchema<{
            config: Record<string, any>;
        }> = yup.object({
            config: yup.object(configSchemaFields),
        });

        const newSchema: PaymentProviderYupSchemaType = BasePaymentProviderYupSchema
            .concat(configSchema);

        setPaymentProviderSchema(newSchema);
    }, [selectedPaymentProviderDefinition?.configLayout, derivedRuntimeConfigData]);

    useEffect(() => {
        if (
            selectedPaymentProviderDefinition?.configLayout == null ||
            editedPaymentProvider == null ||
            Object.keys(errors).length === 0
        ) {
            setClientSideValidationErrors(null);
            return;
        }

        const computedErrors = mapFormManagerErrorsToComputedErrors(
            selectedPaymentProviderDefinition.configLayout,
            editedPaymentProvider.config ?? {},
            errors,
            {rootPath: 'config'},
        );

        setClientSideValidationErrors(Object.keys(computedErrors).length === 0 ? null : computedErrors);
    }, [editedPaymentProvider, errors, selectedPaymentProviderDefinition?.configLayout]);

    if (originalPaymentProvider == null || editedPaymentProvider == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const handleRefreshDefinitions = () => {
        setIsBusy(true);

        new PaymentProvidersApiService()
            .listDefinitions()
            .then((definitions) => {
                setAdditionalData({
                    ...additionalData,
                    definitions: definitions,
                });
                dispatch(showSuccessSnackbar('Auswahllisten wurden erfolgreich neu geladen.'));
            })
            .catch((error) => {
                dispatch(showApiErrorSnackbar(error, 'Fehler beim Laden der Auswahllisten'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    const handleSave = () => {
        const validationResult = validateFormManager();

        if (!validationResult) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);

        const apiService = new PaymentProvidersApiService();

        if (isNewPaymentProvider) {
            apiService
                .create(editedPaymentProvider)
                .then((createdPaymentProvider) => {
                    setOriginalPaymentProvider(createdPaymentProvider);
                    resetFormManager();

                    dispatch(showSuccessSnackbar('Neuer Zahlungsdienstleister erfolgreich angelegt.'));

                    // use setTimeout instead of useEffect to prevent unnecessary rerender
                    setTimeout(() => {
                        navigate(`/payment-providers/${createdPaymentProvider.key}`, {replace: true});
                    }, 0);
                })
                .catch(err => {
                    dispatch(showApiErrorSnackbar(err, 'Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        } else {
            apiService
                .update(originalPaymentProvider.key, editedPaymentProvider)
                .then((updatedPaymentProvider) => {
                    setOriginalPaymentProvider(updatedPaymentProvider);
                    resetFormManager();

                    dispatch(showSuccessSnackbar('Änderungen am Zahlungsdienstleister erfolgreich gespeichert.'));
                })
                .catch(err => {
                    if (err.status === 409) {
                        handleInputChange('isEnabled')(true);
                        dispatch(showApiErrorSnackbar(err, 'Es existieren noch veröffentlichte Formulare, die diesen Zahlungsdienstleister verwenden'));
                    } else {
                        dispatch(showApiErrorSnackbar(err, 'Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                    }
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const checkAndHandleDelete = async () => {
        // New payment providers cannot be deleted
        if (isNewPaymentProvider) {
            return;
        }

        setIsBusy(true);
        try {
            const formsApi = new VFormVersionWithDetailsService();
            const relatedForms = await formsApi.listAll({
                paymentProviderKey: originalPaymentProvider.key,
            });

            if (relatedForms.content.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = relatedForms.content.slice(0, maxVisibleLinks).map(f => ({
                    label: f.internalTitle,
                    to: `/forms/${f.id}`,
                }));

                if (relatedForms.content.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: 'Weitere Formulare anzeigen…',
                        to: `/payment-provider/${originalPaymentProvider.key}/forms`,
                    });
                }

                setRelatedEntities(processedLinks);
                setShowConstraintDialog(true);
            } else {
                setShowConfirmDialog(true);
            }
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Fehler beim Prüfen der Löschbarkeit.'));
        } finally {
            setIsBusy(false);
        }
    };

    const handleDelete = () => {
        // New payment providers cannot be deleted
        if (isNewPaymentProvider) {
            return;
        }

        setIsBusy(true);

        new PaymentProvidersApiService()
            .destroy(originalPaymentProvider.key)
            .then(() => {
                resetFormManager(); // prevent change blocker by resetting unsaved changes
                navigate('/payment-providers', {
                    replace: true,
                });
                dispatch(showSuccessSnackbar('Der Zahlungsdienstleister wurde erfolgreich gelöscht.'));
            })
            .catch(err => {
                dispatch(showApiErrorSnackbar(err, 'Beim Löschen des Zahlungsdienstleisters ist ein Fehler aufgetreten.'));
                setIsBusy(false);
            });
    };

    const handleStatusChange = async (newValue: boolean) => {
        // Bei Deaktivierung
        if (newValue === false) {
            const confirmed = await showConfirm({
                title: 'Deaktivierung bestätigen',
                confirmButtonText: 'Ja, Zahlungsdienstleister deaktivieren',
                children: (
                    <>
                        <Typography gutterBottom>
                            Wenn Sie den Zahlungsdienstleister deaktivieren, wird der Zahlungsdienstleister automatisch
                            aus Formularen mit dem Status "In Bearbeitung" entfernt.
                        </Typography>
                        <Typography gutterBottom>
                            Bitte beachten Sie, dass Sie den Zahlungsdienstleister speichern müssen, um diese Änderung
                            zu übernehmen.
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

    return (
        <Box>
            <Grid
                container={true}
                spacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    {
                        isNewPaymentProvider ?
                            <SelectFieldComponent
                                label="Zahlungsdienstleister"
                                required
                                value={editedPaymentProvider.providerKey}
                                onChange={handleInputChange('providerKey')}
                                options={availablePaymentProviderDefinitions.map(def => ({
                                    value: def.key,
                                    label: def.name,
                                    subLabel: def.description,
                                }))}
                                disabled={isBusy || !isEditable}
                                error={errors.providerKey}
                                hint="Bestimmt, welche Konfigurationsoberfläche nach der Auswahl des Zahlungsdienstleisters eingeblendet wird. Der Name des Anbieters ist gegenüber antragstellenden Personen sichtbar."
                            /> :
                            <SelectFieldComponent
                                label="Zahlungsdienstleister"
                                required
                                value={editedPaymentProvider.providerKey}
                                onChange={handleInputChange('providerKey')}
                                options={availablePaymentProviderDefinitions.map(def => ({
                                    value: def.key,
                                    label: def.name,
                                    subLabel: def.description,
                                }))}
                                disabled={true}
                                error={errors.providerKey}
                                hint="Bestimmt, welche Konfigurationsoberfläche nach der Auswahl des Zahlungsdienstleisters eingeblendet wird. Der Name des Anbieters ist gegenüber antragstellenden Personen sichtbar."
                            />
                    }
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    {
                        isNewPaymentProvider ?
                            <SelectFieldComponent
                                label="Version"
                                required
                                value={editedPaymentProvider.providerVersion.toString()}
                                onChange={(val) => {
                                    if (val == null) {
                                        handleInputChange('providerVersion')(0);
                                    } else {
                                        handleInputChange('providerVersion')(parseInt(val));
                                    }
                                }}
                                options={availablePaymentProviderDefinitions.filter(def => {
                                    return def.key === editedPaymentProvider.providerKey;
                                }).map(def => ({
                                    value: def.version.toString(),
                                    label: `Version ${def.version.toString()}`,
                                }))}
                                disabled={isBusy || !isEditable || isStringNullOrEmpty(editedPaymentProvider.providerKey)}
                                error={errors.providerVersion}
                                hint="Bestimmt, welche Version der Konfigurationsoberfläche und Einstellungsmöglichkeiten angezeigt werden."
                            /> :
                            <TextFieldComponent
                                label="Version"
                                value={originalPaymentProvider.providerVersion > 0 ? `Version ${originalPaymentProvider.providerVersion}` : ''}
                                onChange={() => undefined}
                                disabled={true}
                            />
                    }
                </Grid>
            </Grid>

            <TextFieldComponent
                label="Name"
                required
                value={editedPaymentProvider.name}
                onChange={handleInputChange('name')}
                onBlur={handleInputBlur('name')}
                disabled={isBusy || !isEditable}
                error={errors.name}
                hint="Dient der internen Identifizierung des Zahlungsdienstleisters."
            />

            <TextFieldComponent
                label="Interne Beschreibung"
                required
                value={editedPaymentProvider.description}
                onChange={handleInputChange('description')}
                onBlur={handleInputBlur('description')}
                multiline={true}
                disabled={isBusy || !isEditable}
                error={errors.description}
                hint="Interne Beschreibung des Zahlungsdienstleisters zur besseren Identifizierbarkeit. Sichtbar nur für Mitarbeiter:innen."
            />

            {
                selectedPaymentProviderDefinition != null &&
                selectedPaymentProviderDefinition.configLayout != null &&
                <ElementDerivationContext
                    element={selectedPaymentProviderDefinition.configLayout}
                    authoredElementValues={editedPaymentProvider.config}
                    onAuthoredElementValuesChange={handleInputChange('config')}
                    disabled={isBusy || !isEditable}
                    onDerivationFinished={setDerivedRuntimeConfigData}
                    computedErrors={clientSideValidationErrors}
                    suppressErrors={hasNotChanged}
                />
            }

            <CheckboxFieldComponent
                label="Aktiv (kann in konfigurierten Formularen genutzt werden)"
                value={editedPaymentProvider.isEnabled}
                onChange={handleStatusChange}
                variant="switch"
                error={errors.isEnabled}
                hint="Gibt an, ob diese Konfiguration aktiviert ist. Bei temporären technischen Problemen o.Ä. kann der Dienstleister deaktiviert werden, ohne die Konfiguration zu verlieren."
                disabled={isBusy || !isEditable}
            />

            <CheckboxFieldComponent
                label="Es handelt sich um eine vorproduktive Konfiguration"
                value={editedPaymentProvider.isTestProvider}
                onChange={handleInputChange('isTestProvider')}
                variant="switch"
                error={errors.isTestProvider}
                disabled={isBusy || !isEditable}
                hint="Gibt an, ob diese Konfiguration für eine Testinstanz bestimmt ist. Das System verhindert den Einsatz von Testkonfigurationen in der Live-Umgebung, um Fehlkonfigurationen zu vermeiden."
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
                    disabled={isBusy || hasNotChanged || !isEditable}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon/>}
                >
                    Speichern
                </Button>

                <Tooltip title={'Aktualisieren Sie die Auswahllisten für z.B. Zertifikatsdateien und Geheimnisse, falls Sie diese nicht vorab hinterlegt haben.'}>
                    <Button
                        onClick={handleRefreshDefinitions}
                        disabled={isBusy || !isEditable}
                    >
                        Auswahllisten neu laden <HelpIconOutlined
                        fontSize="small"
                        sx={{ml: 1}}
                    />
                    </Button>
                </Tooltip>

                {
                    !isNewPaymentProvider &&
                    !originalPaymentProvider.isEnabled &&
                    <Button
                        variant={'outlined'}
                        onClick={checkAndHandleDelete}
                        disabled={isBusy || !isEditable}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<Delete/>}
                    >
                        Löschen
                    </Button>
                }

                {
                    !isNewPaymentProvider &&
                    originalPaymentProvider.isEnabled &&
                    <Tooltip title="Zum Löschen muss der Zahlungsdienstleister zuerst deaktiviert und gespeichert werden.">
                        <Box sx={{ml: 'auto'}}>
                            <Button
                                variant="outlined"
                                disabled={true}
                                color="error"
                                startIcon={<Delete/>}
                            >
                                Löschen
                            </Button>
                        </Box>
                    </Tooltip>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Zahlungsdienstleister löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={editedPaymentProvider.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diesen Zahlungsdienstleister wirklich löschen? Diese Aktion kann nicht rückgängig
                    gemacht werden.
                </Typography>

                <Typography
                    sx={{
                        mt: 2,
                    }}
                >
                    <strong>Hinweis:</strong>
                    Sofern noch ausstehende Transaktionen bestehen, werden diese ebenfalls gelöscht.
                    Eine Liste mit ausstehenden Transaktionen können Sie im
                    Reiter <strong>Transaktionen</strong> einsehen.
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
