import {Box, Button, Grid, Stack, Typography} from '@mui/material';
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
import {useChangeBlocker} from '../../../../hooks/use-change-blocker-2';
import * as yup from 'yup';
import {goverSchemaToYup, mapFormManagerErrorsToComputedErrors} from '../../../../utils/gover-schema-to-yup';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {
    addSnackbarMessage,
    removeSnackbarMessage,
    SnackbarSeverity,
    SnackbarType,
} from '../../../../slices/shell-slice';
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
import {bytesToMegabytes, megabytesToBytes} from '../../../../utils/bytes-megabytes-conversion';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {CheckboxFieldComponent} from '../../../../components/checkbox-field/checkbox-field-component';
import {format} from 'date-fns';
import {StatusTable} from '../../../../components/status-table/status-table';
import Sync from '@aivot/mui-material-symbols-400-outlined/dist/sync/Sync';
import {ComputedElementErrors, DerivedRuntimeElementData} from '../../../../models/element-data';
import {Hint} from '../../../../components/hint/hint';
import {StorageProviderStatus} from '../../enums/storage-provider-status';

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
    readOnlyStorage: yup.boolean()
        .optional(),
    testProvider: yup.boolean()
        .default(false),
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
    const [derivedElementData, setDerivedElementData] = useState<DerivedRuntimeElementData | null>(null);
    const [clientSideValidationErrors, setClientSideValidationErrors] = useState<ComputedElementErrors | null>(null);

    const {
        item: originalStorageProvider,
        setItem: setOriginalStorageProvider,
        additionalData,
        setAdditionalData,
        isBusy,
        setIsBusy,
        isEditable,
        isExistingItem,
    } = useGenericDetailsPageContext<StorageProviderEntity, StorageProviderAdditionalData>();

    // Extract the id of the storage provider for later usage.
    const {
        id: storageProviderId,
    } = originalStorageProvider ?? {
        id: null,
    };

    // Store the state of the initial derivation to prevent the change blocker from firing after the first derivation.
    const [initialDerivationDone, setInitialDerivationDone] = useState(false);

    // Reset the initial derivation done, when the id of the storage provider is changed.
    useEffect(() => {
        setInitialDerivationDone(false);
    }, [storageProviderId]);

    useEffect(() => {
        if (isEditable) {
            return;
        }

        dispatch(addSnackbarMessage({
            key: 'storage-provider-no-access',
            message: 'Dieser Speicheranbieter kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.',
            severity: SnackbarSeverity.Warning,
            type: SnackbarType.Dismissable,
        }));

        return () => {
            dispatch(removeSnackbarMessage('storage-provider-no-access'));
        };
    }, [isEditable]);

    const {
        currentItem: editedStorageProvider,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        validate,
        reset,
    } = useFormManager<StorageProviderEntity>(originalStorageProvider, yup.object(storageProviderSchema) as any, true);

    const {
        storageProviderDefinitionKey,
        storageProviderDefinitionVersion,
    } = editedStorageProvider ?? {
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
                    goverSchemaToYup(definition.providerConfigLayout, derivedElementData?.elementStates ?? {}),
                ),
            });
        } else {
            setStorageProviderSchema(_StorageProviderSchema);
        }
    }, [definition, derivedElementData?.elementStates]);

    useEffect(() => {
        if (
            definition?.providerConfigLayout == null ||
            editedStorageProvider == null ||
            Object.keys(errors).length === 0
        ) {
            setClientSideValidationErrors(null);
            return;
        }

        const computedErrors = mapFormManagerErrorsToComputedErrors(
            definition.providerConfigLayout,
            editedStorageProvider.configuration ?? {},
            errors,
            {rootPath: 'configuration'},
        );

        setClientSideValidationErrors(Object.keys(computedErrors).length === 0 ? null : computedErrors);
    }, [definition?.providerConfigLayout, editedStorageProvider, errors]);

    const {
        dialog: changeBlockerDialog,
    } = useChangeBlocker({
        original: originalStorageProvider,
        edited: editedStorageProvider,
        useDeepEquals: true,
    });

    const [showConfirmDialog, setShowConfirmDialog] = useState(false);

    if (editedStorageProvider == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    const handleSave = async (): Promise<void> => {
        if (editedStorageProvider != null) {
            const validationResult = validate();

            if (!validationResult) {
                dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
                return;
            }

            setIsBusy(true);

            const apiService = new StorageProvidersApiService();

            try {
                if (editedStorageProvider.id === 0) {
                    const newProvider = await apiService.create(editedStorageProvider);

                    setOriginalStorageProvider(newProvider);
                    reset();

                    dispatch(showSuccessSnackbar('Neuer Speicheranbieter erfolgreich angelegt.'));

                    setTimeout(() => {
                        navigate(`/storage-providers/${newProvider.id}`, {replace: true});
                    }, 0);
                } else {
                    const updatedProvider = await apiService.update(editedStorageProvider.id, editedStorageProvider);

                    setOriginalStorageProvider(updatedProvider);
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
        if (editedStorageProvider.id === 0) {
            return;
        }

        setIsBusy(true);

        try {
            await new StorageProvidersApiService().destroy(editedStorageProvider.id);
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
    const inputsDisabled = editedStorageProvider.systemProvider || isBusy || !isEditable;
    const attributesError = getIndexedFieldError(
        errors,
        'metadataAttributes',
        'Bitte füllen Sie alle Metadaten-Attribute mit mindestens dem Titel und Feldnamen aus.',
    );

    // Build StatusTable items array in a type-safe way
    const statusTableItems = [];
    if (editedStorageProvider.systemProvider) {
        statusTableItems.push({
            label: 'Systemanbieter',
            icon: <HelpIconOutlined color="primary"/>,
            children: 'Dieser Speicheranbieter ist ein Systemanbieter und kann nicht verändert werden.',
        });
    }
    statusTableItems.push({
        label: 'Zuletzt synchronisiert',
        icon: <Sync/>,
        children: editedStorageProvider.lastSync
            ? format(new Date(editedStorageProvider.lastSync), 'dd.MM.yyyy – HH:mm:ss') + ' Uhr'
            : 'Noch nicht synchronisiert',
    });

    return (
        <Box>
            {
                editedStorageProvider.status == StorageProviderStatus.SyncFailed &&
                editedStorageProvider.statusMessage != null &&
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
                        value={editedStorageProvider.statusMessage}
                        sx={{
                            my: 2,
                        }}
                    />

                    Bitte beheben Sie das Problem mit dem Speicheranbieter, damit eine ordnungsgemäße Funktion
                    gewährleistet ist. Bitte starten Sie nach der Behebung des Problems manuell die Synchronisation,
                    damit die Verbindung geprüft und der Fehlerstatus entfernt wird.
                </AlertComponent>
            }

            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Speicheranbieter konfigurieren
            </Typography>
            <Typography sx={{mb: 3, maxWidth: 900}}>
                Konfigurieren Sie den Speicheranbieter, um ihn für die Ablage von Dateien zum angegebenen
                Verwendungszweck (Typ) nutzen zu können. Sie können die meisten Einstellungen jederzeit anpassen – bitte
                beachten Sie jedoch, dass bestehende Dateien bei einer Änderung des Speicherortes nicht automatisch
                migriert werden.
            </Typography>

            {isExistingItem &&
                <StatusTable
                    sx={{mt: 4, mb: 3}}
                    cardVariant="outlined"
                    items={statusTableItems}
                />
            }

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
                    <SelectFieldComponent
                        label="Speichertyp"
                        required={true}
                        value={editedStorageProvider.storageProviderDefinitionKey}
                        onChange={handleInputChange('storageProviderDefinitionKey')}
                        options={definitions.map((def) => ({
                            value: def.key,
                            label: def.name,
                            subLabel: def.description,
                        }))}
                        disabled={isExistingItem}
                        error={errors.storageProviderDefinitionKey}
                        hint="Diese Einstellung kann nach der Erstellung nicht mehr geändert werden."
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    <SelectFieldComponent
                        label="Version"
                        required={true}
                        value={editedStorageProvider.storageProviderDefinitionVersion.toString()}
                        onChange={(val) => {
                            if (val == null) {
                                handleInputChange('storageProviderDefinitionVersion')(0);
                            } else {
                                handleInputChange('storageProviderDefinitionVersion')(parseInt(val));
                            }
                        }}
                        options={definitions.filter((def) => {
                            return def.key === editedStorageProvider.storageProviderDefinitionKey;
                        }).map((def) => ({
                            value: def.version.toString(),
                            label: `Version ${def.version.toString()}`,
                        }))}
                        disabled={inputsDisabled}
                        error={errors.storageProviderDefinitionVersion}
                        hint="Bestimmt, welche Version der Konfigurationsoberfläche und Einstellungsmöglichkeiten angezeigt werden."
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
                        label="Name des Speicheranbieters"
                        required
                        value={editedStorageProvider.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        disabled={inputsDisabled}
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
                    <SelectFieldComponent
                        label="Verwendungszweck"
                        required={true}
                        value={editedStorageProvider.type}
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
                        hint="Bestimmt die Art der gespeicherten Daten bzw. wo im System dieser Anbieter verwendet werden kann. Diese Einstellung kann nach der Erstellung nicht mehr geändert werden."
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
                        value={editedStorageProvider.description}
                        onChange={handleInputChange('description')}
                        onBlur={handleInputBlur('description')}
                        multiline={true}
                        disabled={inputsDisabled}
                        error={errors.description}
                        hint="Interne Beschreibung des Speicheranbieters zur besseren Identifizierbarkeit."
                        rows={6}
                    />
                </Grid>

                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    <CheckboxFieldComponent
                        label="Es handelt sich um einen read-only Speicher, von welchem Dateien nur gelesen, aber nicht geschrieben werden können."
                        value={editedStorageProvider.readOnlyStorage}
                        onChange={handleInputChange('readOnlyStorage')}
                        variant="switch"
                        error={errors.readOnlyStorage}
                        disabled={inputsDisabled}
                    />

                    <CheckboxFieldComponent
                        label="Es handelt sich um eine vorproduktive Konfiguration"
                        value={editedStorageProvider.testProvider}
                        onChange={handleInputChange('testProvider')}
                        variant="switch"
                        error={errors.testProvider}
                        disabled={inputsDisabled}
                        hint="Gibt an, ob diese Konfiguration für eine Testinstanz bestimmt ist. Das System verhindert den Einsatz von Testkonfigurationen in der Live-Umgebung, um Fehlkonfigurationen zu vermeiden."
                    />
                </Grid>
                <Grid
                    size={{
                        xs: 12,
                        md: 6,
                    }}
                >
                    <Stack
                        direction="row"
                        gap={1}
                    >
                        <NumberFieldComponent
                            label="Maximale Dateigröße (in MB)"
                            value={bytesToMegabytes(editedStorageProvider.maxFileSizeInBytes)}
                            onChange={(mb) => handleInputChange('maxFileSizeInBytes')(megabytesToBytes(mb) as any)}
                            onBlur={(mb) => handleInputBlur('maxFileSizeInBytes')(megabytesToBytes(mb) as any)}
                            disabled={inputsDisabled}
                            error={errors.maxFileSizeInBytes}
                            decimalPlaces={2}
                            suffix="MB"
                            hint="Die maximale Dateigröße die pro Datei an diesen Speicheranbieter übertragen werden kann."
                        />

                        <Hint
                            sx={{
                                mt: 3,
                            }}
                            summary="Die maximale Dateigröße die pro Datei an diesen Speicheranbieter übertragen werden kann."
                            detailsTitle="Maximale Dateigröße"
                            details={
                                <>
                                    <Typography
                                        marginBottom={2}
                                    >
                                        Speicheranbieter können in der Übertragung verschiedene Limitierungen haben.
                                        Um diesen Limitierungen gerecht zu werden, kann pro Speicheranbieter eine
                                        maximale Dateigröße spezifiziert werden.
                                        Die Angabe der maximalen Dateigröße erfolgt in Megabyte* und gilt pro Datei,
                                        welche an den Speicheranbieter von Gover übertragen wird.
                                    </Typography>

                                    <Typography
                                        marginBottom={2}
                                    >
                                        Die maximale Dateigröße wird unter Anderem an den folgenden Stellen berücksichtigt:

                                        <ul>
                                            <li>Das Hochladen von Dateien &amp; Medien</li>
                                            <li>Das Übertragen von Anlagen an einen Prozess</li>
                                            <li>Das Erstellen von Anlagen in einem Vorgang z.B. durch eine PDF-Generierung</li>
                                            <li>Das Importieren von Dateien als Anlagen in einen Vorgang</li>
                                            <li>Das Übertragen von Anlagen aus einem Vorgang in einen Speicheranbieter</li>
                                        </ul>
                                    </Typography>

                                    <Typography
                                        variant="caption"
                                        color="textSecondary"
                                    >
                                        *1 Megabyte entspricht 1.000 Kilobytes oder 1.000.000 Bytes.
                                    </Typography>
                                </>
                            }
                        />
                    </Stack>
                </Grid>
            </Grid>

            {
                definition != null &&
                definition.providerConfigLayout != null &&
                <ElementDerivationContext
                    element={definition.providerConfigLayout}
                    authoredElementValues={editedStorageProvider.configuration}
                    onAuthoredElementValuesChange={handleInputChange('configuration')}
                    disabled={inputsDisabled}
                    computedErrors={clientSideValidationErrors}
                    onDerivationFinished={(derivedElementData) => {
                        setInitialDerivationDone(true);
                        setDerivedElementData(derivedElementData);
                    }}
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
                    value={editedStorageProvider.metadataAttributes}
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
                                    Hier können Sie optionale Metadaten-Attribute definieren, die für Dateien mitgegeben
                                    werden können.
                                    Diese Attribute können beispielsweise in der Dateiansicht oder bei der Suche nach
                                    Dateien verwendet werden.
                                    Bitte beachten Sie, dass diese Einstellungen nur für den ausgewählten Anbieter
                                    gelten.
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
                    marginTop: 4,
                    gap: 2,
                }}
            >
                <Button
                    onClick={handleSave}
                    disabled={inputsDisabled || hasNotChanged}
                    variant="contained"
                    color="primary"
                    startIcon={<SaveOutlinedIcon/>}
                >
                    Speichern
                </Button>

                <Tooltip title={'Aktualisieren Sie die Auswahllisten für z.B. Zertifikatsdateien und Geheimnisse, falls Sie diese nicht vorab hinterlegt haben.'}>
                    <Button
                        onClick={handleRefreshDefinitions}
                        disabled={inputsDisabled}
                    >
                        Auswahllisten neu laden <HelpIconOutlined
                        fontSize="small"
                        sx={{ml: 1}}
                    />
                    </Button>
                </Tooltip>

                {
                    editedStorageProvider.id !== 0 &&
                    originalStorageProvider != null &&
                    <Button
                        variant={'outlined'}
                        onClick={() => setShowConfirmDialog(true)}
                        disabled={inputsDisabled}
                        color="error"
                        sx={{
                            marginLeft: 'auto',
                        }}
                        startIcon={<Delete/>}
                    >
                        Löschen
                    </Button>
                }
            </Box>

            {changeBlockerDialog}

            <ConfirmDialog
                title="Speicheranbieter löschen"
                onCancel={() => setShowConfirmDialog(false)}
                onConfirm={showConfirmDialog ? handleDelete : undefined}
                confirmationText={editedStorageProvider.name ?? ''}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diesen Speicheranbieter wirklich löschen? Diese Aktion kann nicht rückgängig gemacht
                    werden.
                </Typography>
                <AlertComponent color={'warning'}>
                    Wenn der Speicheranbieter bereits für die Ablage von Dateien genutzt wurde, können diese Dateien
                    nach dem Löschen des Speicheranbieters nicht mehr erreicht werden. Bitte stellen Sie sicher, dass
                    Sie alle Dateien migriert oder gelöscht haben, bevor Sie fortfahren.
                </AlertComponent>
            </ConfirmDialog>
        </Box>
    );
}
