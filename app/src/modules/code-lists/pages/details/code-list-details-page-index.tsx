import React, {useContext, useEffect, useEffectEvent, useMemo, useRef, useState} from 'react';
import {Box, Button, CircularProgress, Grid, Typography} from '@mui/material';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import Sync from '@aivot/mui-material-symbols-400-n25-outlined/Sync';
import {useNavigate} from 'react-router-dom';
import * as yup from 'yup';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Download from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType,
} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericDetailsSkeleton} from '../../../../components/generic-details-page/generic-details-skeleton';
import {TextFieldComponent} from '../../../../components/text-field/text-field-component';
import {SelectFieldComponent} from '../../../../components/select-field/select-field-component';
import {ConfirmDialog} from '../../../../dialogs/confirm-dialog/confirm-dialog';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useChangeBlocker} from '../../../../hooks/use-change-blocker';
import {useFormManager} from '../../../../hooks/use-form-manager';
import {showApiErrorSnackbar, showErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {CodeListsApiService} from '../../code-lists-api-service';
import {type CodeList, type CodeListSourceColumns, getDuplicateCodeListColumnError} from '../../models/code-list';
import {CodeListSourceType, CodeListSourceTypeLabels, isCodeListSyncable} from '../../enums/code-list-source-type';
import {CodeListStatus} from '../../enums/code-list-status';
import {AssetSelector} from '../../../assets/components/asset-selector';
import {isStringNotNullOrEmpty} from '../../../../utils/string-utils';
import {deepEquals} from '../../../../utils/equality-utils';
import {CodeListColumnMappingsInput} from '../../components/code-list-column-mappings-input';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';
import {previewSourceColumns} from '../../models/preview-source-columns';
import {StatusTable} from '../../../../components/status-table/status-table';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';
import {formatInstantInApplicationTimeZone} from '../../../../utils/temporal-utils';

const SourceTypeOptions = Object.values(CodeListSourceType).map((value) => ({
    value,
    label: CodeListSourceTypeLabels[value],
})).filter((option) => option.value !== CodeListSourceType.Plugin);

const XRepositoryMetadataHint = 'Geben Sie die versionsspezifische URN ein und rufen Sie die Metadaten ab, um die Spaltenstruktur der Codeliste zu laden.';
const CsvMetadataHint = 'Wählen Sie eine CSV-Datei aus. Die Metadaten werden automatisch abgerufen, um die Spaltenstruktur der Codeliste zu laden.';

const CodeListSchema = yup.object({
    key: yup.string().typeError('Bitte geben Sie einen Text ein.')
        .trim()
        .notOneOf(['new'], 'Der Schlüssel „new“ ist für die Anlage neuer Codelisten reserviert.')
        .matches(/^[^/]+$/, 'Der Schlüssel darf keinen Schrägstrich enthalten.')
        .max(255, 'Der Schlüssel darf maximal 255 Zeichen lang sein.')
        .required('Der Schlüssel ist ein Pflichtfeld.'),
    name: yup.string().typeError('Bitte geben Sie einen Text ein.')
        .trim()
        .min(3, 'Der Name muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name darf maximal 96 Zeichen lang sein.')
        .required('Der Name ist ein Pflichtfeld.'),
    description: yup.string().typeError('Bitte geben Sie einen Text ein.')
        .trim()
        .nullable()
        .max(500, 'Die Beschreibung darf maximal 500 Zeichen lang sein.'),
    sourceType: yup.string().typeError('Wählen Sie eine gültige Quelle für die Codeliste aus.')
        .required('Die Quelle ist ein Pflichtfeld.'),
    sourceRef: yup.string().nonNullable('Bitte geben Sie einen Text ein.').typeError('Bitte geben Sie einen Text ein.')
        .when('sourceType', {
            is: (sourceType: CodeListSourceType) => isCodeListSyncable(sourceType),
            then: (schema) => schema.trim().required('Die Quellenreferenz ist ein Pflichtfeld.'),
            otherwise: (schema) => schema,
        }),
    // Row checks are reported for the whole table, because the form manager shows errors per field.
    columnMappings: yup.array().nonNullable('Die Einträge haben ein ungültiges Format.').typeError('Die Einträge haben ein ungültiges Format.')
        .test('complete', 'Jede Spalte benötigt einen Spaltenschlüssel und einen Anzeigenamen.', (mappings) => (mappings ?? [])
            .every((mapping) => mapping.key?.trim() && mapping.name?.trim()))
        .test('unique-keys', 'Jeder Spaltenschlüssel darf nur einmal vergeben werden.', (mappings) => {
            const keys = (mappings ?? []).map((mapping) => mapping.key?.trim());
            return new Set(keys).size === keys.length;
        })
        .test('unique-assignments', function (mappings) {
            const error = getDuplicateCodeListColumnError(mappings ?? [], this.parent.columns);
            return error == null || this.createError({message: error});
        })
        .when('sourceType', {
            is: CodeListSourceType.Manual,
            then: (schema) => schema.min(1, 'Manuelle Codelisten benötigen mindestens eine Spalte.'),
            otherwise: (schema) => schema,
        }),
    keyColumnKey: yup.string().typeError('Wählen Sie eine gültige Schlüsselspalte aus.')
        .nullable()
        .test('bound-key', 'Bitte wählen Sie eine zugeordnete Spalte als Schlüsselspalte aus.', function (value) {
            const list = this.parent as CodeList;
            if (list.sourceType === CodeListSourceType.Plugin) return true;
            return value != null && list.columnMappings.some(mapping => mapping.key === value && mapping.index != null);
        }),
}).nonNullable('Die Angaben haben ein ungültiges Format.').typeError('Die Angaben haben ein ungültiges Format.');

function sanitizeCodeList(codeList: CodeList): CodeList {
    return {
        ...codeList,
        key: codeList.key.trim(),
        sourceRef: codeList.sourceType === CodeListSourceType.Manual ? '' : (codeList.sourceRef ?? '').trim(),
        description: codeList.description ?? '',
        columnMappings: codeList.columnMappings.map((mapping) => ({
            ...mapping,
            key: mapping.key.trim(),
            name: mapping.name.trim(),
        })),
        keyColumnKey: codeList.keyColumnKey?.trim() || null,
    };
}

function isValidXRepositoryUrn(value: string): boolean {
    const trimmedValue = value.trim();
    return trimmedValue.toLowerCase().startsWith('urn:') && trimmedValue.length > 4 && !/\s/.test(trimmedValue);
}

function isValidUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value.trim());
}

function isValidMetadataSourceRef(sourceType: CodeListSourceType, sourceRef: string): boolean {
    switch (sourceType) {
        case CodeListSourceType.Asset:
            return isValidUuid(sourceRef);
        case CodeListSourceType.XRepository:
            return isValidXRepositoryUrn(sourceRef);
        default:
            return false;
    }
}

function getMetadataRequestKey(sourceType: CodeListSourceType, sourceRef: string): string {
    return `${sourceType}:${sourceRef}`;
}

function formatSyncTimestamp(value: string | null | undefined): string {
    if (value == null || value.length === 0) {
        return 'Noch nicht synchronisiert';
    }

    const formatted = formatInstantInApplicationTimeZone(value, 'dd.MM.yyyy – HH:mm:ss');
    return formatted != null ? `${formatted} Uhr` : 'Zeitpunkt konnte nicht gelesen werden';
}

export function CodeListDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const {
        item,
        setItem,
        isNewItem,
        isExistingItem,
        isBusy,
        setIsBusy,
        isEditable,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<CodeList, void>;

    const {
        currentItem: codeList,
        errors,
        hasNotChanged,
        handleInputBlur,
        handleInputChange,
        handleInputPatch,
        validate,
        reset,
    } = useFormManager<CodeList>(item, CodeListSchema as any);

    const apiService = useMemo(() => new CodeListsApiService(), []);
    const changeBlocker = useChangeBlocker(item, codeList);
    const [showConfirmDelete, setShowConfirmDelete] = useState(false);
    const [isFetchingMetadata, setIsFetchingMetadata] = useState(false);
    const [metadataResult, setMetadataResult] = useState<{
        request: number;
        sourceKey: string;
        metadata: CodeListSourceColumns;
    } | null>(null);
    const requestId = useRef(0);
    const requireFreshMetadata = useRef(false);
    const previewSource = useRef<string | null>(null);
    const canDeleteCodeList = useHasSystemPermission(Permission.CODE_LIST_DELETE);

    useEffect(() => () => { requestId.current++; }, []);

    // Read the latest draft when a fetch completes so edits made in the meantime survive.
    const applyMetadata = useEffectEvent((result: NonNullable<typeof metadataResult>) => {
        if (result.request !== requestId.current || !codeList ||
            result.sourceKey !== getMetadataRequestKey(codeList.sourceType, codeList.sourceRef.trim())) return;
        const firstProposal = isNewItem && previewSource.current !== result.sourceKey && codeList.columnMappings.length === 0;
        const patch = firstProposal ? result.metadata : previewSourceColumns(codeList, result.metadata);
        if (!deepEquals(codeList.columns, patch.columns) || !deepEquals(codeList.columnMappings, patch.columnMappings)
            || codeList.keyColumnKey !== patch.keyColumnKey) {
            handleInputPatch(patch);
        }
        requireFreshMetadata.current = false;
        previewSource.current = result.sourceKey;
        setIsFetchingMetadata(false);
    });
    useEffect(() => {
        if (metadataResult) applyMetadata(metadataResult);
    }, [metadataResult]);

    const loadMetadata = async (type: CodeListSourceType, ref: string) => {
        const sourceRef = ref.trim();
        if (!isValidMetadataSourceRef(type, sourceRef)) return;
        const request = ++requestId.current;
        setIsFetchingMetadata(true);
        try {
            const metadata = type === CodeListSourceType.Asset
                ? await apiService.getAssetColumns(sourceRef)
                : await apiService.getXRepositoryColumns(sourceRef);
            if (request === requestId.current) {
                setMetadataResult({request, sourceKey: getMetadataRequestKey(type, sourceRef), metadata});
            }
        } catch (error) {
            if (request === requestId.current) {
                setIsFetchingMetadata(false);
                dispatch(showApiErrorSnackbar(error, 'Die Quellspalten konnten nicht geladen werden.'));
            }
        }
    };

    const invalidatePreview = () => {
        requestId.current++;
        previewSource.current = null;
        setIsFetchingMetadata(false);
    };

    if (codeList == null) {
        return <GenericDetailsSkeleton/>;
    }

    const editPermission = isNewItem ? Permission.CODE_LIST_CREATE : Permission.CODE_LIST_UPDATE;
    const saveDisabledByPermission = !isEditable;
    const saveDisabledTooltip = saveDisabledByPermission
        ? formatMissingPermissionTooltip(editPermission)
        : undefined;
    const deleteDisabledByPermission = !canDeleteCodeList;
    const deleteDisabledTooltip = deleteDisabledByPermission
        ? formatMissingPermissionTooltip(Permission.CODE_LIST_DELETE)
        : undefined;
    const isExistingSyncableCodeList = isExistingItem === true && isCodeListSyncable(codeList.sourceType);
    const statusTableItems = [
        {
            label: 'Letzte Synchronisierung',
            icon: <Sync/>,
            children: formatSyncTimestamp(item?.lastSync),
        },
    ];

    const handleSourceTypeChange = (value: string | null) => {
        const sourceType = (value ?? CodeListSourceType.Manual) as CodeListSourceType;
        invalidatePreview();
        requireFreshMetadata.current = isCodeListSyncable(sourceType);
        const defaultCodeList = apiService.initialize();
        // A new list takes the columns of its source when its metadata is fetched; manual lists start with defaults.
        const columnsPatch = isNewItem === true || codeList.columnMappings.length === 0
            ? (sourceType === CodeListSourceType.Manual
                ? {
                    columns: defaultCodeList.columns,
                    columnMappings: defaultCodeList.columnMappings,
                    keyColumnKey: defaultCodeList.keyColumnKey,
                }
                : {columns: [], columnMappings: [], keyColumnKey: null})
            : sourceType === CodeListSourceType.Manual && item != null
                ? previewSourceColumns(codeList, item)
                : {};

        handleInputPatch({
            sourceType,
            sourceRef: '',
            ...columnsPatch,
            status: isCodeListSyncable(sourceType) ? CodeListStatus.SyncPending : CodeListStatus.Synced,
        });
    };

    const handleSourceRefChange = (value: string | null) => {
        invalidatePreview();
        requireFreshMetadata.current = true;
        handleInputChange('sourceRef')(value ?? '');
        if (codeList.sourceType === CodeListSourceType.Asset && value) {
            void loadMetadata(codeList.sourceType, value);
        }
    };
    const metadataAction = (
        <Button
            variant="outlined"
            onClick={() => void loadMetadata(codeList.sourceType, codeList.sourceRef)}
            disabled={isBusy || !isEditable || isFetchingMetadata || !isValidMetadataSourceRef(codeList.sourceType, codeList.sourceRef.trim())}
            startIcon={isFetchingMetadata ? <CircularProgress size={18} color="inherit"/> : <Download/>}
        >
            Metadaten abrufen
        </Button>
    );

    const handleSave = () => {
        if (!isEditable || isFetchingMetadata) {
            return;
        }

        if (requireFreshMetadata.current && isCodeListSyncable(codeList.sourceType)) {
            dispatch(showErrorSnackbar('Rufen Sie die Metadaten der gewählten Quelle ab, bevor Sie die Codeliste speichern.'));
            return;
        }

        if (!validate()) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);
        const payload = sanitizeCodeList(codeList);

        if (isNewItem === true) {
            apiService
                .create(payload)
                .then((created) => {
                    setItem(created);
                    reset();
                    dispatch(showSuccessSnackbar('Neue Codeliste erfolgreich angelegt.'));
                    setTimeout(() => {
                        navigate(`/code-lists/${encodeURIComponent(created.key)}`, {replace: true});
                    }, 0);
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Die Codeliste konnte nicht gespeichert werden.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        } else {
            apiService
                .update(payload.key, payload, {
                    query: {refreshSource: previewSource.current === getMetadataRequestKey(payload.sourceType, payload.sourceRef) || undefined},
                })
                .then((updated) => {
                    invalidatePreview();
                    requireFreshMetadata.current = false;
                    setItem(updated);
                    reset();
                    dispatch(showSuccessSnackbar('Änderungen an der Codeliste erfolgreich gespeichert.'));
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Die Codeliste konnte nicht gespeichert werden.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const handleDelete = () => {
        setShowConfirmDelete(false);

        if (isNewItem === true || !canDeleteCodeList) {
            return;
        }

        setIsBusy(true);
        apiService
            .destroy(codeList.key)
            .then(() => {
                reset();
                navigate('/code-lists', {replace: true});
                dispatch(showSuccessSnackbar('Die Codeliste wurde erfolgreich gelöscht.'));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Löschen der Codeliste ist ein Fehler aufgetreten.'));
            })
            .finally(() => {
                setIsBusy(false);
            });
    };

    return (
        <Box>
            {
                isCodeListSyncable(codeList.sourceType) &&
                item?.status === CodeListStatus.SyncFailed &&
                item.statusMessage != null &&
                <AlertComponent
                    color="error"
                    title="Fehler bei der Synchronisierung der Codeliste"
                    sx={{
                        mt: 0,
                        mb: 2,
                    }}
                >
                    Beim Synchronisieren der Codeliste ist ein Fehler aufgetreten.
                    Die folgende Fehlermeldung wurde protokolliert:

                    <ExpandableCodeBlock
                        value={item.statusMessage}
                        sx={{
                            my: 2,
                        }}
                    />

                    Bitte prüfen Sie die konfigurierte Quelle der Codeliste und beheben Sie das Problem.
                    Starten Sie anschließend die Synchronisierung erneut, damit die Einträge aktualisiert und
                    der Fehlerstatus entfernt wird.
                </AlertComponent>
            }

            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Allgemeine Angaben
            </Typography>
            <Typography sx={{mb: 2, maxWidth: 900}}>
                Legen Sie Quelle, Beschreibung und Spalten der Codeliste fest. Welche Spalten als Wert und Beschriftung
                dienen, legen Sie in den Auswahlfeldern fest, die die Codeliste verwenden.
            </Typography>

            {
                isExistingSyncableCodeList &&
                <StatusTable
                    sx={{mt: 4, mb: 3}}
                    cardVariant="outlined"
                    items={statusTableItems}
                />
            }

            <Grid
                container
                columnSpacing={4}
                rowSpacing={2}
            >
                <Grid size={{xs: 12, lg: 6}}>
                    <TextFieldComponent
                        label="Schlüssel"
                        value={codeList.key}
                        onChange={handleInputChange('key')}
                        onBlur={handleInputBlur('key')}
                        required
                        maxCharacters={255}
                        error={errors.key}
                        disabled={isBusy || !isEditable || isExistingItem === true}
                    />
                </Grid>

                <Grid size={{xs: 12, lg: 6}}>
                    <TextFieldComponent
                        label="Name"
                        value={codeList.name}
                        onChange={handleInputChange('name')}
                        onBlur={handleInputBlur('name')}
                        required
                        minCharacters={3}
                        maxCharacters={96}
                        error={errors.name}
                        disabled={isBusy || !isEditable}
                    />
                </Grid>

                <Grid size={{xs: 12, lg: 6}}>
                    {
                        codeList.sourceType === CodeListSourceType.Plugin &&
                        <TextFieldComponent
                            label="Quelle der Einträge"
                            value="Erweiterung"
                            onChange={() => {
                            }}
                            error={errors.sourceType}
                            disabled={true}
                        />
                    }

                    {
                        codeList.sourceType !== CodeListSourceType.Plugin &&
                        <SelectFieldComponent
                            label="Quelle der Einträge"
                            value={codeList.sourceType}
                            onChange={handleSourceTypeChange}
                            options={SourceTypeOptions}
                            required
                            error={errors.sourceType}
                            disabled={isBusy || !isEditable}
                        />
                    }
                </Grid>

                <Grid size={{xs: 12, lg: 6}}>
                    <TextFieldComponent
                        label="Beschreibung"
                        value={codeList.description}
                        onChange={handleInputChange('description')}
                        onBlur={handleInputBlur('description')}
                        multiline
                        rows={3}
                        maxCharacters={500}
                        error={errors.description}
                        disabled={isBusy || !isEditable}
                    />
                </Grid>

                <Grid size={{xs: 12}}>
                    <AlertComponent color="info" sx={{mt: 2, mb: 3}}>
                        Einträge von Codelisten können in öffentlichen Formularen angezeigt werden. Hinterlegen Sie
                        daher keine vertraulichen Informationen.
                    </AlertComponent>
                </Grid>

                {
                    isCodeListSyncable(codeList.sourceType) &&
                    <Grid size={{xs: 12}}>
                        {
                            codeList.sourceType === CodeListSourceType.Asset &&
                            <AssetSelector
                                label="CSV-Datei"
                                selectLabel="CSV-Datei"
                                value={isStringNotNullOrEmpty(codeList.sourceRef) ? codeList.sourceRef : null}
                                onChange={handleSourceRefChange}
                                externalAction={metadataAction}
                                required={true}
                                error={errors.sourceRef}
                                hint={isFetchingMetadata ? 'Metadaten werden abgerufen...' : CsvMetadataHint}
                                disabled={isBusy || !isEditable}
                                isBusy={isFetchingMetadata}
                                mimetype="text/csv"
                            />
                        }

                        {
                            codeList.sourceType === CodeListSourceType.XRepository &&
                            <TextFieldComponent
                                label="XRepository-URN"
                                value={codeList.sourceRef}
                                onChange={handleSourceRefChange}
                                onBlur={handleInputBlur('sourceRef')}
                                required={true}
                                error={errors.sourceRef}
                                disabled={isBusy || !isEditable}
                                hint={isFetchingMetadata ? 'Metadaten werden abgerufen...' : XRepositoryMetadataHint}
                                externalAction={metadataAction}
                            />
                        }
                    </Grid>
                }

                <Grid size={{xs: 12}}>
                    <CodeListColumnMappingsInput
                        codeList={codeList}
                        savedColumnMappings={isExistingItem === true ? item?.columnMappings ?? [] : []}
                        onChange={handleInputPatch}
                        disabled={isBusy || !isEditable}
                        error={errors.columnMappings}
                        keyColumnError={errors.keyColumnKey}
                    />
                </Grid>
            </Grid>

            <Box
                sx={{
                    display: 'flex',
                    marginTop: 4,
                    gap: 2,
                }}
            >
                <DisabledTooltip
                    disabled={saveDisabledByPermission}
                    title={saveDisabledTooltip}
                >
                    <Button
                        onClick={handleSave}
                        disabled={isBusy || isFetchingMetadata || hasNotChanged || !isEditable}
                        variant="contained"
                        color="primary"
                        startIcon={<Save/>}
                    >
                        Speichern
                    </Button>
                </DisabledTooltip>
                <DisabledTooltip
                    disabled={saveDisabledByPermission}
                    title={saveDisabledTooltip}
                >
                    <Button
                        onClick={() => {
                            invalidatePreview();
                            requireFreshMetadata.current = false;
                            reset();
                        }}
                        disabled={isBusy || isFetchingMetadata || hasNotChanged || !isEditable}
                        color="error"
                    >
                        Zurücksetzen
                    </Button>
                </DisabledTooltip>
                {
                    isExistingItem === true &&
                    <DisabledTooltip
                        disabled={deleteDisabledByPermission}
                        title={deleteDisabledTooltip}
                        wrapperSx={{marginLeft: 'auto'}}
                    >
                        <Button
                            variant="outlined"
                            onClick={() => setShowConfirmDelete(true)}
                            disabled={isBusy || !isEditable || !canDeleteCodeList}
                            color="error"
                            startIcon={<Delete/>}
                        >
                            Löschen
                        </Button>
                    </DisabledTooltip>
                }
            </Box>

            {changeBlocker.dialog}

            <ConfirmDialog
                title="Codeliste löschen"
                onCancel={() => setShowConfirmDelete(false)}
                onConfirm={showConfirmDelete ? handleDelete : undefined}
                confirmationText={codeList.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Möchten Sie diese Codeliste wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
                <AlertComponent color="warning" sx={{mt: 2}}>
                    Das System kann nicht prüfen, ob diese Codeliste noch in Low-Code-Skripten verwendet wird.
                    Stellen Sie vor dem Löschen sicher, dass keine entsprechenden Verwendungen mehr bestehen.
                </AlertComponent>
            </ConfirmDialog>
        </Box>
    );
}
