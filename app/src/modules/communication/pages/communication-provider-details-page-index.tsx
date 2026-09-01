import {Box, Button, Grid, Typography} from '@mui/material';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import SaveOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import * as yup from 'yup';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {DisabledTooltip} from '../../../components/disabled-tooltip/disabled-tooltip';
import {GenericDetailsSkeleton} from '../../../components/generic-details-page/generic-details-skeleton';
import {useGenericDetailsPageContext} from '../../../components/generic-details-page/generic-details-page-context';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {Permission} from '../../../data/permissions/permission';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useChangeBlocker} from '../../../hooks/use-change-blocker-2';
import {useFormManager} from '../../../hooks/use-form-manager';
import {useConfirm} from '../../../providers/confirm-provider';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {ElementDerivationContext} from '../../elements/components/element-derivation-context';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import {formatMissingPermissionTooltip} from '../../permissions/utils/permission-utils';
import {CommunicationProvidersApiService} from '../communication-providers-api-service';
import {
    type CommunicationConfigurationLayout,
    type CommunicationProvider,
    type CommunicationProviderRequest,
} from '../models';
import {type CommunicationProviderAdditionalData} from './communication-provider-details-page-additional-data';

const communicationProviderSchema = yup.object({
    id: yup.number().required(),
    communicationProviderDefinitionKey: yup.string()
        .trim()
        .required('Die Definition ist ein Pflichtfeld.'),
    communicationProviderDefinitionVersion: yup.number()
        .min(1, 'Die Version muss mindestens 1 sein.')
        .required('Die Version ist ein Pflichtfeld.'),
    name: yup.string()
        .trim()
        .max(64, 'Der Name darf maximal 64 Zeichen lang sein.')
        .required('Der Name ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .max(255, 'Die Beschreibung darf maximal 255 Zeichen lang sein.')
        .required('Die Beschreibung ist ein Pflichtfeld.'),
    configuration: yup.object().required(),
    isEnabled: yup.boolean().required(),
    isTestProvider: yup.boolean().required(),
}) as yup.ObjectSchema<CommunicationProvider>;

export function CommunicationProviderDetailsPageIndex() {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const canDelete = useHasSystemPermission(Permission.COMMUNICATION_PROVIDER_DELETE);
    const [layout, setLayout] = useState<CommunicationConfigurationLayout | null>(null);

    const {
        item: originalProvider,
        setItem: setOriginalProvider,
        isNewItem,
        additionalData,
        isBusy,
        setIsBusy,
        isEditable,
    } = useGenericDetailsPageContext<CommunicationProvider, CommunicationProviderAdditionalData>();

    const definitions = additionalData?.definitions ?? [];
    const {
        currentItem: provider,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        handleInputPatch,
        validate,
        reset,
    } = useFormManager<CommunicationProvider>(originalProvider, communicationProviderSchema, true);

    const definitionOptions = useMemo(() => {
        const uniqueDefinitions = new Map<string, (typeof definitions)[number]>();
        definitions.forEach((definition) => {
            if (!uniqueDefinitions.has(definition.key)) {
                uniqueDefinitions.set(definition.key, definition);
            }
        });

        return Array.from(uniqueDefinitions.values()).map(definition => ({
            value: definition.key,
            label: definition.name,
            subLabel: definition.description,
        }));
    }, [definitions]);

    const selectedDefinition = useMemo(() => definitions.find(definition => (
        definition.key === provider?.communicationProviderDefinitionKey &&
        definition.version === provider?.communicationProviderDefinitionVersion
    )), [definitions, provider?.communicationProviderDefinitionKey, provider?.communicationProviderDefinitionVersion]);

    useEffect(() => {
        const definitionKey = provider?.communicationProviderDefinitionKey;
        const definitionVersion = provider?.communicationProviderDefinitionVersion ?? 0;
        let isActive = true;

        setLayout(null);
        if (!definitionKey || definitionVersion < 1) {
            return () => {
                isActive = false;
            };
        }

        new CommunicationProvidersApiService()
            .getProviderConfigurationLayout(definitionKey, definitionVersion)
            .then((loadedLayout) => {
                if (isActive) setLayout(loadedLayout);
            })
            .catch(error => {
                if (isActive) {
                    dispatch(showApiErrorSnackbar(error, 'Konfigurationsoberfläche konnte nicht geladen werden.'));
                }
            });

        return () => {
            isActive = false;
        };
    }, [dispatch, provider?.communicationProviderDefinitionKey, provider?.communicationProviderDefinitionVersion]);

    const changeBlocker = useChangeBlocker({
        original: originalProvider,
        edited: provider,
        useDeepEquals: true,
        isActive: originalProvider != null && provider != null,
    });

    if (originalProvider == null || provider == null) {
        return <GenericDetailsSkeleton/>;
    }

    const editPermission = isNewItem === true
        ? Permission.COMMUNICATION_PROVIDER_CREATE
        : Permission.COMMUNICATION_PROVIDER_UPDATE;
    const editDisabledTooltip = !isEditable
        ? formatMissingPermissionTooltip(editPermission)
        : undefined;
    const deleteDisabledTooltip = !canDelete
        ? formatMissingPermissionTooltip(Permission.COMMUNICATION_PROVIDER_DELETE)
        : originalProvider.isEnabled || provider.isEnabled
            ? 'Zum Löschen muss der Kommunikationsanbieter zuerst deaktiviert und gespeichert werden.'
            : undefined;
    const deleteDisabled = isBusy || !canDelete || originalProvider.isEnabled || provider.isEnabled;

    const handleSave = async () => {
        if (!validate()) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        const request: CommunicationProviderRequest = {
            communicationProviderDefinitionKey: provider.communicationProviderDefinitionKey,
            communicationProviderDefinitionVersion: provider.communicationProviderDefinitionVersion,
            name: provider.name.trim(),
            description: provider.description.trim(),
            configuration: provider.configuration ?? {},
            isEnabled: provider.isEnabled,
            isTestProvider: provider.isTestProvider,
        };

        setIsBusy(true);
        try {
            const api = new CommunicationProvidersApiService();
            const savedProvider = isNewItem
                ? await api.createProvider(request)
                : await api.updateProvider(originalProvider.id, request);

            setOriginalProvider(savedProvider);
            reset();
            dispatch(showSuccessSnackbar(isNewItem
                ? 'Kommunikationsanbieter wurde angelegt.'
                : 'Kommunikationsanbieter wurde gespeichert.'));

            if (isNewItem) {
                setTimeout(() => {
                    void navigate(`/communication-providers/${savedProvider.id}`, {replace: true});
                }, 0);
            }
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbieter konnte nicht gespeichert werden.'));
        } finally {
            setIsBusy(false);
        }
    };

    const handleDelete = async () => {
        if (isNewItem || deleteDisabled) return;

        const confirmed = await confirm({
            title: 'Kommunikationsanbieter löschen',
            confirmButtonText: 'Endgültig löschen',
            isDestructive: true,
            children: <Typography>Möchten Sie „{provider.name}“ endgültig löschen?</Typography>,
        });
        if (!confirmed) return;

        setIsBusy(true);
        try {
            await new CommunicationProvidersApiService().deleteProvider(originalProvider.id);
            reset();
            dispatch(showSuccessSnackbar('Kommunikationsanbieter wurde gelöscht.'));
            setTimeout(() => {
                void navigate('/communication-providers', {replace: true});
            }, 0);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Kommunikationsanbieter konnte nicht gelöscht werden.'));
            setIsBusy(false);
        }
    };

    return (
        <Box>
            <Grid container spacing={2}>
                <Grid size={{xs: 12, md: 8}}>
                    <SelectFieldComponent
                        label="Definition"
                        required
                        value={provider.communicationProviderDefinitionKey || undefined}
                        options={definitionOptions}
                        onChange={(value) => {
                            const definition = definitions.find(candidate => candidate.key === value);
                            handleInputPatch({
                                communicationProviderDefinitionKey: value ?? '',
                                communicationProviderDefinitionVersion: definition?.version ?? 0,
                                configuration: {},
                            });
                        }}
                        disabled={isBusy || !isEditable || isNewItem !== true}
                        error={errors.communicationProviderDefinitionKey}
                    />
                </Grid>
                <Grid size={{xs: 12, md: 4}}>
                    <SelectFieldComponent
                        label="Version"
                        required
                        value={provider.communicationProviderDefinitionVersion > 0
                            ? String(provider.communicationProviderDefinitionVersion)
                            : undefined}
                        options={definitions
                            .filter(definition => definition.key === provider.communicationProviderDefinitionKey)
                            .map(definition => ({
                                value: String(definition.version),
                                label: `Version ${definition.version}`,
                            }))}
                        onChange={(value) => handleInputPatch({
                            communicationProviderDefinitionVersion: Number(value ?? 0),
                            configuration: {},
                        })}
                        disabled={isBusy || !isEditable || isNewItem !== true || !provider.communicationProviderDefinitionKey}
                        error={errors.communicationProviderDefinitionVersion}
                    />
                </Grid>
            </Grid>

            <TextFieldComponent
                label="Name"
                required
                value={provider.name}
                onChange={handleInputChange('name')}
                onBlur={handleInputBlur('name')}
                disabled={isBusy || !isEditable}
                error={errors.name}
                maxCharacters={64}
            />
            <TextFieldComponent
                label="Interne Beschreibung"
                required
                multiline
                value={provider.description}
                onChange={handleInputChange('description')}
                onBlur={handleInputBlur('description')}
                disabled={isBusy || !isEditable}
                error={errors.description}
                maxCharacters={255}
            />

            {layout != null && (
                <ElementDerivationContext
                    element={layout}
                    authoredElementValues={provider.configuration ?? {}}
                    onAuthoredElementValuesChange={handleInputChange('configuration')}
                    disabled={isBusy || !isEditable}
                />
            )}

            <CheckboxFieldComponent
                label="Aktiv"
                hint="Nur aktive Anbieter stehen in aktiven Kommunikationsanbindungen zur Verfügung."
                variant="switch"
                value={provider.isEnabled}
                onChange={handleInputChange('isEnabled')}
                disabled={isBusy || !isEditable || selectedDefinition?.supportedIdentityProviderTypes.length === 0}
                error={errors.isEnabled}
            />
            <CheckboxFieldComponent
                label="Vorproduktive Konfiguration"
                hint="Vorproduktive Kommunikationsanbieter können nur mit vorproduktiven Nutzerkontenanbietern verbunden werden."
                variant="switch"
                value={provider.isTestProvider}
                onChange={handleInputChange('isTestProvider')}
                disabled={isBusy || !isEditable || isNewItem !== true}
                error={errors.isTestProvider}
            />

            <Box sx={{display: 'flex', gap: 2, mt: 3}}>
                <DisabledTooltip
                    title={editDisabledTooltip}
                    disabled={isBusy || !isEditable || hasNotChanged}
                >
                    <Button
                        variant="contained"
                        startIcon={<SaveOutlinedIcon/>}
                        onClick={() => void handleSave()}
                        disabled={isBusy || !isEditable || hasNotChanged}
                    >
                        Speichern
                    </Button>
                </DisabledTooltip>

                {isNewItem !== true && (
                    <DisabledTooltip
                        title={deleteDisabledTooltip}
                        disabled={deleteDisabled}
                        wrapperSx={{ml: 'auto'}}
                    >
                        <Button
                            color="error"
                            variant="outlined"
                            startIcon={<Delete/>}
                            onClick={() => void handleDelete()}
                            disabled={deleteDisabled}
                        >
                            Löschen
                        </Button>
                    </DisabledTooltip>
                )}
            </Box>

            {changeBlocker.dialog}
        </Box>
    );
}
