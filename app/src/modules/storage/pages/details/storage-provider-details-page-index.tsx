import {Box, Button, Grid, Typography} from '@mui/material';
import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {useGenericDetailsPageContext} from '../../../../components/generic-details-page/generic-details-page-context';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {useNavigate} from 'react-router-dom';
import {StorageProvidersApiService} from '../../storage-providers-api-service';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {type StorageProviderAdditionalData} from './storage-provider-details-page-additional-data';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import * as yup from 'yup';
import {goverSchemaToYup2 as goverSchemaToYup} from '../../../../utils/gover-schema-to-yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {addSnackbarMessage, removeSnackbarMessage, SnackbarSeverity, SnackbarType} from '../../../../slices/shell-slice';
import {type StorageProviderDefinition} from '../../entities/storage-provider-definition';
import {type StorageProviderEntity, StorageProviderMetadataAttribute} from '../../entities/storage-provider-entity';
import {ElementDerivationContext} from '../../../elements/components/element-derivation-context';
import {StorageProviderType, StorageProviderTypeLabels, StorageProviderTypes} from '../../enums/storage-provider-type';
import Tooltip from '@mui/material/Tooltip';
import HelpIconOutlined from '@mui/icons-material/HelpOutline';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';
import {NumberFieldComponent} from '../../../../components/number-field/number-field-component';
import {TableFieldComponent2} from '../../../../components/table-field/table-field-component-2';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';

function getIndexedFieldError(
    errors: Record<string, any> | undefined,
    fieldName: string,
    message: string,
): string | undefined {
    if (!errors) return undefined;

    return Object.keys(errors).some(k => k.startsWith(`${fieldName}[`))
        ? message
        : undefined;
}

export const _StorageProviderSchema = {
    name: yup.string()
        .trim()
        .min(3, 'Der Name des Speicheranbieters muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name des Speicheranbieters darf maximal 96 Zeichen lang sein.')
        .required('Der Name des Speicheranbieters ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .min(10, 'Die Beschreibung muss mindestens 10 Zeichen lang sein.')
        .max(500, 'Die Beschreibung darf maximal 500 Zeichen lang sein.')
        .required('Die Beschreibung des Speicheranbieters ist ein Pflichtfeld.'),
    storageProviderDefinitionKey: yup.string()
        .trim()
        .required('Der Anbieter des Speicheranbieters ist ein Pflichtfeld.'),
    storageProviderDefinitionVersion: yup.number()
        .min(1, 'Die Version des Speicheranbieters muss mindestens 1 sein.')
        .required('Die Version des Speicheranbieters ist ein Pflichtfeld.'),
    type: yup.string()
        .trim()
        .required('Der Typ des Speicheranbieters ist ein Pflichtfeld.'),
    maxFileSizeInBytes: yup.number()
        .min(0, 'Die maximale Dateigröße muss mindestens 0 Bytes betragen.')
        .required('Die maximale Dateigröße ist ein Pflichtfeld.'),
    metadataAttributes: yup.array()
        .of(
            yup.object({
                label: yup.string().trim(),
                description: yup.string().trim(),
                key: yup.string().trim(),
            }).test('row-completeness', 'Bitte geben Sie mind. Titel und Feldname an oder löschen Sie die Zeile.', function (row) {
                if (!row) return true;

                const {label, description, key} = row;

                const isAnyFilled = !!label?.trim() || !!key?.trim();
                const areAllFilled = !!label?.trim() && !!key?.trim();

                if (!isAnyFilled) {
                    return this.createError({message: 'Bitte füllen Sie die Felder aus oder löschen Sie die Zeile.'});
                }

                if (!areAllFilled) {
                    return this.createError({message: 'Bitte geben Sie mind. Titel und Feldname an oder löschen Sie die Zeile.'});
                }

                return true;
            }),
        ),
};

export function StorageProviderDetailsPageIndex(): ReactNode {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

    const [storageProviderSchema, setStorageProviderSchema] = useState<any>(_StorageProviderSchema);

    const {
        item,
        setItem,
        additionalData,
        setAdditionalData,
        isBusy,
        setIsBusy,
        isEditable,
        isExistingItem,
    } = useGenericDetailsPageContext<StorageProviderEntity, StorageProviderAdditionalData>();

    useEffect(() => {
        if (isEditable) {
            return;
        }

        dispatch(addSnackbarMessage({
            key: 'storage-provider-no-access',
            message: 'Dieser Zahlungsdienstleister kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.',
            severity: SnackbarSeverity.Warning,
            type: SnackbarType.Dismissable,
        }));

        return () => {
            dispatch(removeSnackbarMessage('payment-provider-no-access'));
        };
    }, [isEditable]);

    const {
        currentItem: storageProvider,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<StorageProviderEntity>(item, yup.object(storageProviderSchema) as any, true);

    const {
        storageProviderDefinitionKey,
        storageProviderDefinitionVersion,
    } = storageProvider ?? {
        storageProviderDefinitionKey: '',
        storageProviderDefinitionVersion: 0,
    };

    const definitions: StorageProviderDefinition[] = useMemo(() => {
        return additionalData?.definitions ?? [];
    }, [additionalData]);

    const definition: StorageProviderDefinition | undefined = useMemo(() => {
        return definitions.find((def) => (
            def.key === storageProviderDefinitionKey &&
            def.version === storageProviderDefinitionVersion
        ));
    }, [definitions, storageProviderDefinitionKey, storageProviderDefinitionVersion]);

    useEffect(() => {
        if (definition?.providerConfigLayout != null) {
            setStorageProviderSchema({
                ..._StorageProviderSchema,
                configuration: yup.object().shape(
                    goverSchemaToYup(definition.providerConfigLayout),
                ),
            });
        } else {
            setStorageProviderSchema(_StorageProviderSchema);
        }
    }, [definition]);

    const changeBlocker = useChangeBlocker(item, storageProvider, undefined, undefined, true);
    const [showConfirmDialog, setShowConfirmDialog] = useState(false);

    if (storageProvider == null) {
        return (
            <GenericDetailsSkeleton />
        );
    }

    const handleSave = async (): Promise<void> => {
        if (storageProvider != null) {
            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                // TODO: Move errors from errors to configuration object
                return;
            }

            setIsBusy(true);

            const apiService = new StorageProvidersApiService();

            try {
                if (storageProvider.id === 0) {
                    const newProvider = await apiService.create(storageProvider);

                    setItem(newProvider);
                    reset();

                    dispatch(showSuccessSnackbar('Neuer Speicheranbieter erfolgreich angelegt.'));

                    setTimeout(() => {
                        navigate(`/storage-providers/${newProvider.id}`, {replace: true});
                    }, 0);
                } else {
                    const updatedProvider = await apiService.update(storageProvider.id, storageProvider);

                    setItem(updatedProvider);
                    reset();

                    dispatch(showSuccessSnackbar('Änderungen am Speicheranbieter erfolgreich gespeichert.'));
                }
            } catch (err) {
                dispatch(showApiErrorSnackbar(err, 'Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
            } finally {
                setIsBusy(false);
            }
        }
    };

    const handleDelete = async (): Promise<void> => {
        if (storageProvider.id === 0) {
            return;
        }

        setIsBusy(true);

        try {
            await new StorageProvidersApiService().destroy(storageProvider.id);
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, 'Beim Löschen des Speicheranbieters ist ein Fehler aufgetreten.'));
        }

        reset(); // prevent change blocker by resetting unsaved changes
        navigate('/storage-providers', {
            replace: true,
        });
        dispatch(showSuccessSnackbar('Der Speicheranbieter wurde erfolgreich gelöscht.'));
    };

    const handleRefreshDefinitions = async () => {
        setIsBusy(true);
        try {
            const updatedDefinitions = await new StorageProvidersApiService().listDefinitions();
            setAdditionalData({
                ...additionalData,
                definitions: updatedDefinitions,
            });
            dispatch(showSuccessSnackbar('Auswahllisten wurden erfolgreich neu geladen.'));
        } catch (error) {
            console.error('Fehler beim Aktualisieren der Auswahllisten', error);
            dispatch(showErrorSnackbar('Fehler beim Aktualisieren der Auswahllisten.'));
        } finally {
            setIsBusy(false);
        }
    };
    const inputsDisabled = storageProvider.readOnly;
    const attributesError = getIndexedFieldError(
        errors,
        'metadataAttributes',
        'Bitte füllen Sie alle Metadaten-Attribute mit mindestens dem Titel und Feldnamen aus.',
    );
    return (
        <Box>
            {
                storageProvider.statusMessage != null &&
                <AlertComponent
                    color="error"
                    title="Fehler bei der Synchronisation des Speicheranbieters"
                    sx={{
                        mt: 0,
                        mb: 2,
                    }}
                >
                    Während der Synchronisation mit dem Speicheranbieter ist ein Fehler aufgetreten.
                    Die folgende Fehlermeldung wurde protokolliert:

                    <ExpandableCodeBlock
                        value={storageProvider.statusMessage}
                        sx={{
                            my: 2,
                        }}
                    />

                    Bitte beheben Sie das Problem mit dem Speicheranbieter, damit eine ordnungsgemäße Funktion gewährleistet ist. Bitte starten Sie nach der Behebung des Problems manuell die Synchronisation, damit die Verbindung geprüft und der Fehlerstatus entfernt wird.
                </AlertComponent>
            }

            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Speicheranbieter konfigurieren
            </Typography>
            <Typography sx={{mb: 3, maxWidth: 900}}>
                Konfigurieren Sie den Speicheranbieter, um ihn für die Ablage von Dateien zum angegebenen Verwendungszweck (Typ) nutzen zu können. Sie können die meisten Einstellungen jederzeit anpassen – bitte beachten Sie jedoch, dass bestehende Dateien bei einer Änderung des Speicherortes nicht automatisch migriert werden.
            </Typography>

            <Grid
                container={true}
                spacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        md: 4,
                    }}
                >
                    <SelectFieldComponent
                        label="Speicheranbieter"
                        required={true}
                        value={storageProvider.storageProviderDefinitionKey}
                        onChange={handleInputChange('storageProviderDefinitionKey')}
                        options={definitions.map((def) => ({
                            value: def.key,
                            label: def.name,
                            subLabel: def.description,
                        }))}
                        disabled={isExistingItem}
                        error={errors.storageProviderDefinitionKey}
                        hint="Dies kann nach der Erstellung nicht mehr geändert werden."
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        md: 4,
                    }}
                >
                    <SelectFieldComponent
                        label="Version"
                        required={true}
                        value={storageProvider.storageProviderDefinitionVersion.toString()}
                        onChange={(val) => {
                            if (val == null) {
                                handleInputChange('storageProviderDefinitionVersion')(0);
                            } else {
                                handleInputChange('storageProviderDefinitionVersion')(parseInt(val));
                            }
                        }}
                        options={definitions.filter((def) => {
                            return def.key === storageProvider.storageProviderDefinitionKey;
                        }).map((def) => ({
                            value: def.version.toString(),
                            label: `Version ${def.version.toString()}`,
                        }))}
                        error={errors.storageProviderDefinitionVersion}
                        hint="Bestimmt, welche Version der Konfigurationsoberfläche und Einstellungsmöglichkeiten angezeigt werden."
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        md: 4,
                    }}
                >
                    <SelectFieldComponent
                        label="Typ (Verwendungszweck)"
                        required={true}
                        value={storageProvider.type}
                        onChange={(val) => {
                            if (val == null) {
                                handleInputChange('type')(StorageProviderType.Assets);
                            } else {
                                handleInputChange('type')(val as StorageProviderType);
                            }
                        }}
                        options={StorageProviderTypes.map((type) => ({
                            value: type,
                            label: StorageProviderTypeLabels[type],
                        }))}
                        disabled={isExistingItem}
                        error={errors.type}
                        hint="Bestimmt die Art der gespeicherten Daten. Dies kann nach der Erstellung nicht mehr geändert werden."
                    />
                </Grid>
            </Grid>

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
                    <TextFieldComponent
                        label="Name"
                        required
                        value={storageProvider.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        disabled={isBusy || !isEditable}
                        error={errors.name}
                        hint="Dient der Identifizierung des Speicheranbieters."
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >

                    <NumberFieldComponent
                        label="Maximale Dateigröße (in Bytes)"
                        required
                        value={storageProvider.maxFileSizeInBytes ?? undefined}
                        onChange={handleInputChange('maxFileSizeInBytes')}
                        onBlur={handleInputBlur('maxFileSizeInBytes')}
                        disabled={isBusy || !isEditable}
                        error={errors.maxFileSizeInBytes}
                        suffix="Bytes"
                        hint='Max. Größe für abzulegende Dateien. Geben Sie 0 ein, wenn keine Beschränkung existiert.'
                    />
                </Grid>

                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    <TextFieldComponent
                        label="Interne Beschreibung"
                        required
                        value={storageProvider.description}
                        onChange={handleInputChange('description')}
                        onBlur={handleInputBlur('description')}
                        multiline={true}
                        disabled={isBusy || !isEditable}
                        error={errors.description}
                        hint="Interne Beschreibung des Speicheranbieters zur besseren Identifizierbarkeit."
                    />
                </Grid>
            </Grid>

            {
                definition != null &&
                definition.providerConfigLayout != null &&
                <ElementDerivationContext
                    element={definition.providerConfigLayout}
                    elementData={storageProvider.configuration}
                    onElementDataChange={handleInputChange('configuration')}
                />
            }

            {
                definition != null &&
                definition.supportsMetadataAttributes &&
                <TableFieldComponent2<StorageProviderMetadataAttribute>
                    label="Metadaten-Attribute"
                    fields={[
                        {
                            key: 'label',
                            label: 'Titel',
                            type: 'string',
                            disabled: inputsDisabled,
                        },
                        {
                            key: 'description',
                            label: 'Beschreibung (optional)',
                            type: 'string',
                            disabled: inputsDisabled,
                        },
                        {
                            key: 'key',
                            label: 'Feldname',
                            type: 'string',
                            disabled: inputsDisabled,
                        },
                    ]}
                    hint="Geben Sie hier die optionale Metadaten-Attribute ein, die für Dateien mitgegeben werden können."
                    createDefaultRow={() => ({
                        label: '',
                        key: '',
                        description: '',
                    })}
                    value={storageProvider.metadataAttributes}
                    onChange={(value) => {
                        handleInputChange('metadataAttributes')(value ?? []);
                    }}
                    disabled={inputsDisabled}
                    addTooltip="Metadaten-Attribut hinzufügen"
                    deleteTooltip="Metadaten-Attribut löschen"
                    noRowsPlaceholder="Keine Metadaten-Attribute vorhanden"
                    helpDialog={{
                        title: 'Metadaten-Attribute',
                        content: (
                            <Box>
                                <Typography>
                                    Hier können Sie optionale Metadaten-Attribute definieren, die für Dateien mitgegeben werden können.
                                    Diese Attribute können beispielsweise in der Dateiansicht oder bei der Suche nach Dateien verwendet werden.
                                    Bitte beachten Sie, dass diese Einstellungen nur für den ausgewählten Anbieter gelten.
                                </Typography>
                                <ul style={{marginTop: '1rem', paddingLeft: '1.1rem'}}>
                                    <li>
                                        <strong>Titel</strong>
                                        – Anzeigename, der später in der Gover-Oberfläche
                                        erscheint (z.&nbsp;B. „E-Mail“ oder „Ersteller:in“).
                                    </li>

                                    <li>
                                        <strong>Feldname</strong>
                                        – Schlüssel in den Daten
                                        (z.&nbsp;B.{' '}
                                        <code>email</code>
                                        , <code>creator</code>, …)
                                    </li>
                                </ul>
                            </Box>
                        ),
                    }}
                    error={attributesError}
                    sx={{my: 4}}
                />
            }

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
                    startIcon={<SaveOutlinedIcon />}
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
                    storageProvider.id !== 0 &&
                    item != null &&
                    <Button
                        variant={'outlined'}
                        onClick={() => setShowConfirmDialog(true)}
                        disabled={isBusy || !isEditable}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<Delete />}
                    >
                        Löschen
                    </Button>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Speicheranbieter löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={storageProvider.name ?? ''}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diesen Speicheranbieter wirklich löschen? Diese Aktion kann nicht rückgängig gemacht
                    werden.
                </Typography>
                <AlertComponent color={'warning'}>
                    Wenn der Speicheranbieter bereits für die Ablage von Dateien genutzt wurde, können diese Dateien nach dem Löschen des Speicheranbieters nicht mehr erreicht werden. Bitte stellen Sie sicher, dass Sie alle Dateien migriert oder gelöscht haben, bevor Sie fortfahren.
                </AlertComponent>
            </ConfirmDialog>
        </Box>
    );
}
