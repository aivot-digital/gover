import React, {useContext, useEffect, useMemo, useRef, useState} from 'react';
import {Alert, Box, Button, CircularProgress, Grid, Stack, Typography} from '@mui/material';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
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
import {CodeList} from '../../models/code-list';
import {CodeListSourceType, CodeListSourceTypeLabels, isCodeListSyncable} from '../../enums/code-list-source-type';
import {CodeListStatus} from '../../enums/code-list-status';
import {AssetSelector} from '../../../assets/components/asset-selector';
import {isStringNotNullOrEmpty} from '../../../../utils/string-utils';
import {StringListInput2} from '../../../../components/string-list-input/string-list-input-2';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';
import {withAsyncWrapper} from '../../../../utils/with-async-wrapper';

const SourceTypeOptions = Object.values(CodeListSourceType).map((value) => ({
    value,
    label: CodeListSourceTypeLabels[value],
})).filter((option) => option.value !== CodeListSourceType.Plugin);

const DefaultManualCodeListColumns = ['Beschriftung', 'Wert'];
const DefaultManualCodeListValueColumnIndex = 1;
const DefaultManualCodeListLabelColumnIndex = 0;
const CodeListLabelColumnHint = 'Der Anzeigename, der für Benutzer:innen angezeigt wird.';
const CodeListValueColumnHint = 'Der technische Schlüssel, der im Hintergrund gespeichert und an nachfolgende Prozessschritte oder Systeme übertragen wird.';
const XRepositoryMetadataHint = 'Geben Sie die versionsspezifische URN ein und rufen Sie die Metadaten ab, um die Spaltenstruktur der Codeliste zu laden.';
const CsvMetadataHint = 'Wählen Sie eine CSV-Datei aus. Die Metadaten werden automatisch abgerufen, um die Spaltenstruktur der Codeliste zu laden.';
const MetadataFetchMinRuntime = 600;

type MetadataFetchResult = {
    success: true;
    columns: string[];
} | {
    success: false;
    error: unknown;
};

const CodeListSchema = yup.object({
    name: yup.string()
        .trim()
        .min(3, 'Der Name muss mindestens 3 Zeichen lang sein.')
        .max(96, 'Der Name darf maximal 96 Zeichen lang sein.')
        .required('Der Name ist ein Pflichtfeld.'),
    description: yup.string()
        .trim()
        .max(500, 'Die Beschreibung darf maximal 500 Zeichen lang sein.'),
    sourceType: yup.string()
        .required('Die Quelle ist ein Pflichtfeld.'),
    sourceRef: yup.string()
        .when('sourceType', {
            is: (sourceType: CodeListSourceType) => isCodeListSyncable(sourceType),
            then: (schema) => schema.trim().required('Die Quellenreferenz ist ein Pflichtfeld.'),
            otherwise: (schema) => schema,
        }),
    columns: yup.array()
        .of(yup.string().trim().required())
        .when('sourceType', {
            is: CodeListSourceType.Manual,
            then: (schema) => schema.min(1, 'Manuelle Codelisten benötigen mindestens eine Spalte.'),
            otherwise: (schema) => schema,
        }),
    valueColumnIndex: yup.number()
        .min(0, 'Die Wert-Spalte ist ungültig.')
        .required('Die Wert-Spalte ist ein Pflichtfeld.'),
    labelColumnIndex: yup.number()
        .min(0, 'Die Beschriftungs-Spalte ist ungültig.')
        .required('Die Beschriftungs-Spalte ist ein Pflichtfeld.'),
});

function parseColumns(value: string | null | undefined): string[] {
    return (value ?? '')
        .split('\n')
        .map((line) => line.trim())
        .filter((line) => line.length > 0);
}

function sanitizeCodeList(codeList: CodeList): CodeList {
    const columns = codeList.columns ?? [];
    const lastIndex = Math.max(columns.length - 1, 0);

    return {
        ...codeList,
        sourceRef: codeList.sourceType === CodeListSourceType.Manual ? '' : (codeList.sourceRef ?? ''),
        description: codeList.description ?? '',
        columns,
        valueColumnIndex: columns.length === 0 ? 0 : Math.min(codeList.valueColumnIndex ?? 0, lastIndex),
        labelColumnIndex: columns.length === 0 ? 0 : Math.min(codeList.labelColumnIndex ?? 0, lastIndex),
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

function getMetadataColumnsPatch(
    codeList: CodeList,
    columns: string[],
): Pick<CodeList, 'columns' | 'valueColumnIndex' | 'labelColumnIndex'> {
    const lastIndex = Math.max(columns.length - 1, 0);

    return {
        columns,
        valueColumnIndex: columns.length === 0 ? 0 : Math.min(codeList.valueColumnIndex, lastIndex),
        labelColumnIndex: columns.length === 0 ? 0 : Math.min(codeList.labelColumnIndex, lastIndex),
    };
}

async function fetchMetadataColumns(
    apiService: CodeListsApiService,
    sourceType: CodeListSourceType,
    sourceRef: string,
): Promise<MetadataFetchResult> {
    try {
        const columns = sourceType === CodeListSourceType.Asset
            ? await apiService.getAssetColumns(sourceRef)
            : await apiService.getXRepositoryColumns(sourceRef);

        return {
            success: true,
            columns,
        };
    } catch (error) {
        return {
            success: false,
            error,
        };
    }
}

export function CodeListDetailsPageIndex() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const {
        item,
        setItem,
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
    const [metadataFetchTrigger, setMetadataFetchTrigger] = useState(0);
    const [isFetchingMetadata, setIsFetchingMetadata] = useState(false);
    const metadataFetchIdRef = useRef(0);
    const lastMetadataRequestKeyRef = useRef<string | null>(null);

    const columnOptions = useMemo(() => (codeList?.columns ?? []).map((column, index) => ({
        value: index.toString(),
        label: column,
    })), [codeList?.columns]);

    const runMetadataFetch = (
        sourceType: CodeListSourceType,
        sourceRef: string,
        options: {
            onError?: () => void;
            successMessage?: string;
        } = {},
    ): boolean => {
        if (codeList == null) {
            return false;
        }

        const fetchId = metadataFetchIdRef.current + 1;
        metadataFetchIdRef.current = fetchId;
        setIsFetchingMetadata(true);

        withAsyncWrapper<void, MetadataFetchResult>({
            desiredMinRuntime: MetadataFetchMinRuntime,
            // Keep the loading indicator visible long enough to avoid flicker on fast metadata responses.
            main: async () => fetchMetadataColumns(apiService, sourceType, sourceRef),
        })
            .then((result) => {
                if (metadataFetchIdRef.current !== fetchId) {
                    // Ignore stale responses after the user selected another source.
                    return;
                }

                if (!result.success) {
                    options.onError?.();
                    dispatch(showApiErrorSnackbar(result.error, 'Beim Abrufen der Metadaten ist ein Fehler aufgetreten.'));
                    return;
                }

                handleInputPatch(getMetadataColumnsPatch(codeList, result.columns));

                if (options.successMessage != null) {
                    dispatch(showSuccessSnackbar(options.successMessage));
                }
            })
            .catch((err) => {
                if (metadataFetchIdRef.current !== fetchId) {
                    return;
                }

                options.onError?.();
                dispatch(showApiErrorSnackbar(err, 'Beim Abrufen der Metadaten ist ein Fehler aufgetreten.'));
            })
            .finally(() => {
                if (metadataFetchIdRef.current === fetchId) {
                    setIsFetchingMetadata(false);
                }
            });

        return true;
    };

    useEffect(() => {
        if (
            codeList == null ||
            metadataFetchTrigger === 0 ||
            !isEditable ||
            isBusy
        ) {
            return;
        }

        const sourceRef = (codeList.sourceRef ?? '').trim();
        const sourceType = codeList.sourceType;
        const requestKey = getMetadataRequestKey(sourceType, sourceRef);

        if (sourceType !== CodeListSourceType.Asset) {
            setIsFetchingMetadata(false);
            return;
        }

        if (
            sourceRef.length === 0 ||
            !isValidMetadataSourceRef(sourceType, sourceRef)
        ) {
            setIsFetchingMetadata(false);
            return;
        }

        if (lastMetadataRequestKeyRef.current === requestKey) {
            return;
        }

        lastMetadataRequestKeyRef.current = requestKey;
        runMetadataFetch(sourceType, sourceRef, {
            onError: () => {
                lastMetadataRequestKeyRef.current = null;
            },
        });

        return () => {
            metadataFetchIdRef.current += 1;
        };
    }, [
        apiService,
        codeList?.sourceRef,
        codeList?.sourceType,
        metadataFetchTrigger,
        isEditable,
        isBusy,
    ]);

    if (codeList == null) {
        return <GenericDetailsSkeleton/>;
    }

    const handleColumnsChange = (value: string | null) => {
        const columns = parseColumns(value);
        handleInputPatch(getMetadataColumnsPatch(codeList, columns));
    };

    const handleSourceTypeChange = (value: string | null) => {
        const sourceType = (value ?? CodeListSourceType.Manual) as CodeListSourceType;
        const useDefaultManualColumns = sourceType === CodeListSourceType.Manual && codeList.columns.length === 0;

        handleInputPatch({
            sourceType,
            sourceRef: '',
            columns: useDefaultManualColumns ? [...DefaultManualCodeListColumns] : codeList.columns,
            valueColumnIndex: useDefaultManualColumns
                ? DefaultManualCodeListValueColumnIndex
                : codeList.valueColumnIndex,
            labelColumnIndex: useDefaultManualColumns
                ? DefaultManualCodeListLabelColumnIndex
                : codeList.labelColumnIndex,
            status: isCodeListSyncable(sourceType) ? CodeListStatus.SyncPending : CodeListStatus.Synced,
        });
    };

    const handleAssetSourceRefChange = (value: string | null) => {
        lastMetadataRequestKeyRef.current = null;
        handleInputChange('sourceRef')(value ?? '');
        setMetadataFetchTrigger((current) => current + 1);
    };

    const handleFetchXRepositoryMetadata = () => {
        const sourceRef = (codeList.sourceRef ?? '').trim();

        if (
            !isValidMetadataSourceRef(CodeListSourceType.XRepository, sourceRef) ||
            isFetchingMetadata ||
            isBusy ||
            !isEditable
        ) {
            return;
        }

        runMetadataFetch(CodeListSourceType.XRepository, sourceRef, {
            successMessage: 'XRepository-Metadaten wurden erfolgreich abgerufen.',
        });
    };

    const handleSave = () => {
        if (!validate()) {
            dispatch(showErrorSnackbar('Bitte überprüfen Sie Ihre Eingaben.'));
            return;
        }

        setIsBusy(true);
        const payload = sanitizeCodeList(codeList);

        if (payload.id === 0) {
            apiService
                .create(payload)
                .then((created) => {
                    setItem(created);
                    reset();
                    dispatch(showSuccessSnackbar('Neue Codeliste erfolgreich angelegt.'));
                    setTimeout(() => {
                        navigate(`/code-lists/${created.id}`, {replace: true});
                    }, 0);
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        } else {
            apiService
                .update(payload.id, payload)
                .then((updated) => {
                    setItem(updated);
                    reset();
                    dispatch(showSuccessSnackbar('Änderungen an der Codeliste erfolgreich gespeichert.'));
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Speichern fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const handleDelete = () => {
        setShowConfirmDelete(false);

        if (codeList.id === 0) {
            return;
        }

        setIsBusy(true);
        apiService
            .destroy(codeList.id)
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
            {!isEditable && (
                <Alert
                    severity="warning"
                    sx={{mb: 3}}
                >
                    Diese Codeliste kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.
                </Alert>
            )}

            {
                codeList.status == CodeListStatus.SyncFailed &&
                codeList.statusMessage != null &&
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
                        value={codeList.statusMessage}
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
                Legen Sie Quelle, Spalten und die Zuordnung von Beschriftung und Wert fest.
            </Typography>

            <Grid
                container
                columnSpacing={4}
                rowSpacing={2}
            >
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
                            label="Quelle"
                            value="Plugin"
                            onChange={() => {
                            }}
                            readonly={true}
                            error={errors.sourceType}
                            disabled={isBusy || !isEditable}
                        />
                    }

                    {
                        codeList.sourceType !== CodeListSourceType.Plugin &&
                        <SelectFieldComponent
                            label="Quelle"
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

                <Grid size={{xs: 12, lg: 6}}>
                    <Alert severity="info">
                        Auswahlwerte von Codelisten können in öffentlichen Formularen verwendet und über die
                        öffentliche Codelisten-API ohne Anmeldung abgerufen werden. Hinterlegen Sie daher keine
                        vertraulichen Informationen.
                    </Alert>
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
                                onChange={handleAssetSourceRefChange}
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
                            <Stack
                                direction={{xs: 'column', md: 'row'}}
                                spacing={2}
                            >
                                <Box sx={{flex: 1, minWidth: 0}}>
                                    <TextFieldComponent
                                        label="XRepository-URN"
                                        value={codeList.sourceRef}
                                        onChange={handleInputChange('sourceRef')}
                                        onBlur={handleInputBlur('sourceRef')}
                                        required={true}
                                        error={errors.sourceRef}
                                        disabled={isBusy || !isEditable}
                                        hint={isFetchingMetadata ? 'Metadaten werden abgerufen...' : XRepositoryMetadataHint}
                                    />
                                </Box>

                                <Box>
                                    <Button
                                        variant="outlined"
                                        onClick={handleFetchXRepositoryMetadata}
                                        disabled={
                                            isBusy ||
                                            !isEditable ||
                                            isFetchingMetadata ||
                                            !isValidMetadataSourceRef(CodeListSourceType.XRepository, codeList.sourceRef ?? '')
                                        }
                                        startIcon={
                                            isFetchingMetadata
                                                ? <CircularProgress size={18} color="inherit"/>
                                                : <Download/>
                                        }
                                        sx={{
                                            mt: {xs: 0, md: 3},
                                        }}
                                    >
                                        Metadaten abrufen
                                    </Button>
                                </Box>
                            </Stack>
                        }
                    </Grid>
                }

                <Grid size={{xs: 12}}>
                    <StringListInput2
                        label="Spalten"
                        hint="Die Spalten dieser Codeliste"
                        addLabel="Spalte hinzufügen"
                        disabled={isBusy || !isEditable || codeList.sourceType !== CodeListSourceType.Manual}
                        noItemsHint="Noch keine Spalten definiert."
                        value={codeList.columns ?? []}
                        onChange={(val) => {
                            handleInputChange('columns')(val ?? []);
                        }}
                        allowEmpty={false}
                    />
                </Grid>


                <Grid size={{xs: 12, lg: 6}}>
                    <SelectFieldComponent
                        label="Beschriftungs-Spalte"
                        value={codeList.labelColumnIndex.toString()}
                        onChange={(value) => handleInputChange('labelColumnIndex')(Number.parseInt(value ?? '0', 10))}
                        options={columnOptions}
                        required={codeList.columns.length > 0}
                        hint={CodeListLabelColumnHint}
                        error={errors.labelColumnIndex}
                        disabled={isBusy || !isEditable || codeList.columns.length === 0}
                    />
                </Grid>
                <Grid size={{xs: 12, lg: 6}}>
                    <SelectFieldComponent
                        label="Wert-Spalte"
                        value={codeList.valueColumnIndex.toString()}
                        onChange={(value) => handleInputChange('valueColumnIndex')(Number.parseInt(value ?? '0', 10))}
                        options={columnOptions}
                        required={codeList.columns.length > 0}
                        hint={CodeListValueColumnHint}
                        error={errors.valueColumnIndex}
                        disabled={isBusy || !isEditable || codeList.columns.length === 0}
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
                <Button
                    onClick={handleSave}
                    disabled={isBusy || hasNotChanged || !isEditable}
                    variant="contained"
                    color="primary"
                    startIcon={<Save/>}
                >
                    Speichern
                </Button>
                <Button
                    onClick={reset}
                    disabled={isBusy || hasNotChanged || !isEditable}
                    color="error"
                >
                    Zurücksetzen
                </Button>
                {
                    codeList.id !== 0 &&
                    <Button
                        variant="outlined"
                        onClick={() => setShowConfirmDelete(true)}
                        disabled={isBusy || !isEditable}
                        color="error"
                        startIcon={<Delete/>}
                        sx={{marginLeft: 'auto'}}
                    >
                        Löschen
                    </Button>
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
