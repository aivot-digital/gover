import {Box, Button, Typography} from '@mui/material';
import React, {useContext, useEffect, useMemo, useState} from 'react';
import {GenericDetailsPageContext, GenericDetailsPageContextType} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useApi} from '../../../../hooks/use-api';
import {useNavigate} from 'react-router-dom';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../../../utils/string-utils';
import {useSelector} from 'react-redux';
import {selectUser} from '../../../../slices/user-slice';
import {isAdmin} from '../../../../utils/is-admin';
import {PaymentProvidersApiService} from '../../../../modules/payment/payment-providers-api-service';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {ViewDispatcherComponent} from '../../../../components/view-dispatcher.component';
import {flattenElements} from '../../../../utils/flatten-elements';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {PaymentProviderAdditionalData} from './payment-provider-details-page-additional-data';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import {PaymentProviderResponseDTO} from '../../../../modules/payment/dtos/payment-provider-response-dto';
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import {useFormManager} from "../../../../hooks/use-form-manager";
import {useChangeBlocker} from "../../../../hooks/use-change-blocker";
import { ConstraintDialog } from '../../../../dialogs/constraint-dialog/constraint-dialog';
import {ConfirmDialog} from "../../../../dialogs/confirm-dialog/confirm-dialog";
import {FormsApiService} from "../../../../modules/forms/forms-api-service";
import {ConstraintLinkProps} from "../../../../dialogs/constraint-dialog/constraint-link-props";
import HelpIconOutlined from "@mui/icons-material/HelpOutline";
import Tooltip from "@mui/material/Tooltip";
import * as yup from "yup";
import {goverSchemaToYup} from '../../../../utils/gover-schema-to-yup';
import {PaymentProviderDefinitionResponseDTO} from '../../../../modules/payment/dtos/payment-provider-definition-response-dto';
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";

export const _PaymentProviderSchema = {
    name: yup.string()
        .trim()
        .min(3, "Der Name des Zahlungsdienstleisters muss mindestens 3 Zeichen lang sein.")
        .max(96, "Der Name des Zahlungsdienstleisters darf maximal 96 Zeichen lang sein.")
        .required("Der Name des Zahlungsdienstleisters ist ein Pflichtfeld."),
    description: yup.string()
        .trim()
        .min(10, "Die Beschreibung muss mindestens 10 Zeichen lang sein.")
        .max(500, "Die Beschreibung darf maximal 500 Zeichen lang sein.")
        .required("Die Beschreibung des Zahlungsdienstleisters ist ein Pflichtfeld."),
    providerKey: yup.string()
        .trim()
        .required("Der Anbieter des Zahlungsdienstleisters ist ein Pflichtfeld."),
    isTestProvider: yup.boolean()
        .default(false),
};

export function PaymentProviderDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const user = useSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    const api = useApi();
    const apiService = useMemo(() => new PaymentProvidersApiService(api), [api]);

    const [paymentProviderSchema, setPaymentProviderSchema] = useState<any>(_PaymentProviderSchema);

    const {
        item,
        setItem,
        isNewItem,
        additionalData,
        isBusy,
        setIsBusy,
        setAdditionalData,
    } = useContext<GenericDetailsPageContextType<PaymentProviderResponseDTO, PaymentProviderAdditionalData>>(GenericDetailsPageContext);

    const {
        currentItem: paymentProvider,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<PaymentProviderResponseDTO>(item, yup.object(paymentProviderSchema) as any);

    const definitions = useMemo(() => {
        return additionalData?.definitions ?? [];
    }, [additionalData]);

    const definition: PaymentProviderDefinitionResponseDTO | undefined = useMemo(() => {
        return definitions.find(def => def.key === paymentProvider?.providerKey);
    }, [definitions, paymentProvider?.providerKey]);

    useEffect(() => {
        if (definition != null && definition.configLayout != null) {
            setPaymentProviderSchema({
                ..._PaymentProviderSchema,
                config: yup.object().shape(
                    goverSchemaToYup(definition.configLayout)
                )
            });
        } else {
            setPaymentProviderSchema(_PaymentProviderSchema);
        }
     }, [definition]);

    const changeBlocker = useChangeBlocker(item, paymentProvider);

    const [showConfirmDialog, setShowConfirmDialog] = useState(false);
    const [showConstraintDialog, setShowConstraintDialog] = useState(false);
    const [relatedEntities, setRelatedEntities] = useState<ConstraintLinkProps[] | null>(null);

    if (paymentProvider == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleRefreshDefinitions = async () => {
        setIsBusy(true);
        try {
            const updatedDefinitions = await new PaymentProvidersApiService(api).listDefinitions();
            setAdditionalData({
                ...additionalData,
                definitions: updatedDefinitions,
            });
            dispatch(showSuccessSnackbar("Auswahllisten wurden erfolgreich neu geladen."));
        } catch (error) {
            console.error("Fehler beim Aktualisieren der Auswahllisten", error);
            dispatch(showErrorSnackbar("Fehler beim Aktualisieren der Auswahllisten."));
        } finally {
            setIsBusy(false);
        }
    };

    const handleSave = () => {
        if (paymentProvider != null) {

            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar("Bitte überprüfen Sie Ihre Eingaben."));
                return;
            }

            setIsBusy(true);

            if (isStringNullOrEmpty(paymentProvider.key)) {
                apiService
                    .create(paymentProvider)
                    .then((newPaymentProvider) => {
                        setItem(newPaymentProvider);
                        reset();

                        dispatch(showSuccessSnackbar('Neuer Zahlungsdienstleister erfolgreich angelegt.'));

                        // use setTimeout instead of useEffect to prevent unnecessary rerender
                        setTimeout(() => {
                            navigate(`/payment-providers/${newPaymentProvider.key}`, {replace: true});
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
                    .update(paymentProvider.key, paymentProvider)
                    .then((updatedPaymentProvider) => {
                        setItem(updatedPaymentProvider);
                        reset();

                        dispatch(showSuccessSnackbar('Änderungen am Zahlungsdienstleister erfolgreich gespeichert.'));
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
        if (isStringNullOrEmpty(paymentProvider.key)) return;

        setIsBusy(true);
        try {
            const formsApi = new FormsApiService(api);
            const relatedForms = await formsApi.list(0, 999, undefined, undefined, { paymentProvider: paymentProvider.key });

            if (relatedForms.content.length > 0) {
                const maxVisibleLinks = 5;
                let processedLinks = relatedForms.content.slice(0, maxVisibleLinks).map(f => ({
                    label: f.title,
                    to: `/forms/${f.id}`
                }));

                if (relatedForms.content.length > maxVisibleLinks) {
                    processedLinks.push({
                        label: "Weitere Formulare anzeigen…",
                        to: `/payment-provider/${paymentProvider.key}/forms`
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
        if (isStringNotNullOrEmpty(paymentProvider.key)) {
            setIsBusy(true);

            apiService
                .destroy(paymentProvider.key)
                .then(() => {
                    navigate('/payment-providers', {
                        replace: true,
                    });
                    dispatch(showSuccessSnackbar('Der Zahlungsdienstleister wurde erfolgreich gelöscht.'));
                })
                .catch(err => {
                    console.error(err);
                    dispatch(showErrorSnackbar('Beim Löschen des Zahlungsdienstleisters ist ein Fehler aufgetreten.'));
                    setIsBusy(false);
                });
        }
    };

    const configErrors = Object.keys(errors).reduce((acc, key) => {
        if (key.startsWith("config.")) {
            acc[key.replace("config.", "")] = (errors as Record<string, string>)[key];
        }
        return acc;
    }, {} as Record<string, string>);

    return (
        <Box>
            {
                isNewItem ?
                <SelectFieldComponent
                    label="Zahlungsdienstleister"
                    required
                    value={paymentProvider.providerKey}
                    onChange={handleInputChange("providerKey")}
                    options={definitions.map(def => ({
                        value: def.key,
                        label: def.name,
                    }))}
                    error={errors.providerKey}
                    hint="Bestimmt, welche Konfigurationsoberfläche nach der Auswahl des Zahlungsdienstleisters eingeblendet wird. Der Name des Anbieters ist gegenüber antragstellenden Personen sichtbar."
                /> :
                <TextFieldComponent
                    label="Zahlungsdienstleister"
                    value={paymentProvider.providerKey}
                    onChange={handleInputChange("providerKey")}
                    disabled={true}
                />
            }

            <TextFieldComponent
                label="Name"
                required
                value={paymentProvider.name}
                onChange={handleInputChange("name")}
                onBlur={handleInputBlur("name")}
                disabled={isBusy || !userIsAdmin}
                error={errors.name}
                hint="Dient der internen Identifizierung des Zahlungsdienstleisters."
            />

            <TextFieldComponent
                label="Interne Beschreibung"
                required
                value={paymentProvider.description}
                onChange={handleInputChange("description")}
                onBlur={handleInputBlur("description")}
                multiline={true}
                disabled={isBusy || !userIsAdmin}
                error={errors.description}
                hint="Interne Beschreibung des Zahlungsdienstleisters zur besseren Identifizierbarkeit. Sichtbar nur für Mitarbeiter:innen."
            />

            {
                definition != null &&
                definition.configLayout != null &&
                <ViewDispatcherComponent
                    allElements={flattenElements(definition.configLayout)}
                    element={definition.configLayout}
                    isBusy={isBusy}
                    isDeriving={false}
                    valueOverride={{
                        values: paymentProvider.config,
                        onChange: (key, value) => {
                            handleInputChange("config")({
                                ...paymentProvider.config,
                                [key]: value,
                            });
                        },
                    }}
                    errorsOverride={configErrors}
                    mode="viewer"
                />
            }

            <CheckboxFieldComponent
                label="Es handelt sich um eine vorproduktive Konfiguration"
                value={paymentProvider.isTestProvider}
                onChange={handleInputChange("isTestProvider")}
                variant="switch"
                error={errors.isTestProvider}
                hint="Gibt an, ob diese Konfiguration für eine Testinstanz bestimmt ist. Das System verhindert den Einsatz von Testkonfigurationen in der Live-Umgebung, um Fehlkonfigurationen zu vermeiden."
            />

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

                    {/*
                        isStringNotNullOrEmpty(paymentProvider.key) &&
                        <Button
                            onClick={() => {
                                reset();
                            }}
                            disabled={isBusy || hasNotChanged}
                            color="error"
                        >
                            Zurücksetzen
                        </Button>
                    */}

                    <Tooltip title={"Aktualisieren Sie die Auswahllisten für z.B. Zertifikatsdateien und Geheimnisse, falls Sie diese nicht vorab hinterlegt haben."}>
                        <Button
                            onClick={handleRefreshDefinitions}
                            disabled={isBusy}
                        >
                            Auswahllisten neu laden <HelpIconOutlined fontSize="small" sx={{ml:1}}/>
                        </Button>
                    </Tooltip>

                    {
                        isStringNotNullOrEmpty(paymentProvider.key) &&
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
                title="Zahlungsdienstleister löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={paymentProvider.name}
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