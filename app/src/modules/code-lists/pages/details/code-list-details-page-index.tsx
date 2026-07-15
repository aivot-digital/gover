import React, {useContext, useMemo, useState} from 'react';
import {Alert, Box, Button, Grid, Stack, Typography} from '@mui/material';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import {useNavigate} from 'react-router-dom';
import * as yup from 'yup';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
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
import {Actions} from '../../../../components/actions/actions';
import Download from '@aivot/mui-material-symbols-400-outlined/dist/download/Download';
import {StringListInput2} from '../../../../components/string-list-input/string-list-input-2';
import {StorageProviderStatus} from '../../../storage/enums/storage-provider-status';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';

const SourceTypeOptions = Object.values(CodeListSourceType).map((value) => ({
    value,
    label: CodeListSourceTypeLabels[value],
})).filter((option) => option.value !== CodeListSourceType.Plugin);

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
            then: (schema) => schema.min(1, 'Manuelle Code-Listen benoetigen mindestens eine Spalte.'),
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

    const columnOptions = useMemo(() => (codeList?.columns ?? []).map((column, index) => ({
        value: index.toString(),
        label: column,
    })), [codeList?.columns]);

    if (codeList == null) {
        return <GenericDetailsSkeleton/>;
    }

    const handleColumnsChange = (value: string | null) => {
        const columns = parseColumns(value);
        const lastIndex = Math.max(columns.length - 1, 0);

        handleInputPatch({
            columns,
            valueColumnIndex: columns.length === 0 ? 0 : Math.min(codeList.valueColumnIndex, lastIndex),
            labelColumnIndex: columns.length === 0 ? 0 : Math.min(codeList.labelColumnIndex, lastIndex),
        });
    };

    const handleSourceTypeChange = (value: string | null) => {
        const sourceType = (value ?? CodeListSourceType.Manual) as CodeListSourceType;

        handleInputPatch({
            sourceType,
            sourceRef: '',
            columns: sourceType === CodeListSourceType.Manual && codeList.columns.length === 0 ? ['value', 'label'] : codeList.columns,
            status: isCodeListSyncable(sourceType) ? CodeListStatus.SyncPending : CodeListStatus.Synced,
        });
    };

    const handleSave = () => {
        if (!validate()) {
            dispatch(showErrorSnackbar('Bitte ueberpruefen Sie Ihre Eingaben.'));
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
                    dispatch(showSuccessSnackbar('Neue Code-Liste erfolgreich angelegt.'));
                    setTimeout(() => {
                        navigate(`/code-lists/${created.id}`, {replace: true});
                    }, 0);
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Speichern fehlgeschlagen. Bitte ueberpruefen Sie Ihre Eingaben.'));
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
                    dispatch(showSuccessSnackbar('Aenderungen an der Code-Liste erfolgreich gespeichert.'));
                })
                .catch((err) => {
                    dispatch(showApiErrorSnackbar(err, 'Speichern fehlgeschlagen. Bitte ueberpruefen Sie Ihre Eingaben.'));
                })
                .finally(() => {
                    setIsBusy(false);
                });
        }
    };

    const handleDelete = () => {
        if (codeList.id === 0) {
            return;
        }

        setIsBusy(true);
        apiService
            .destroy(codeList.id)
            .then(() => {
                reset();
                navigate('/code-lists', {replace: true});
                dispatch(showSuccessSnackbar('Die Code-Liste wurde erfolgreich geloescht.'));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Loeschen der Code-Liste ist ein Fehler aufgetreten.'));
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
                    Diese Code-Liste kann nur von Administrator:innen bearbeitet werden. Sie haben Lesezugriff.
                </Alert>
            )}

            {
                codeList.status == CodeListStatus.SyncFailed &&
                codeList.statusMessage != null &&
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
                        value={codeList.statusMessage}
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
                Allgemeine Angaben
            </Typography>
            <Typography sx={{mb: 2, maxWidth: 900}}>
                Legen Sie Quelle, Spalten und die Zuordnung von Wert und Beschriftung fest.
            </Typography>

            <Alert
                severity="info"
                sx={{mb: 2}}
            >
                Alle Code-Listen sind öffentlich verfügbar und dürfen keine vertraulichen Informationen enthalten.
            </Alert>

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

                <Grid size={{xs: 12}}>
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
                    {
                        codeList.sourceType === CodeListSourceType.Asset &&
                        <Stack
                            direction="row"
                            spacing={2}
                        >
                            <AssetSelector
                                label="CSV-Datei"
                                selectLabel="CSV-Datei"
                                value={isStringNotNullOrEmpty(codeList.sourceRef) ? codeList.sourceRef : null}
                                onChange={handleInputChange('sourceRef')}
                                required={true}
                                error={errors.sourceRef}
                                disabled={isBusy || !isEditable}
                            />

                            <Actions
                                actions={[
                                    {
                                        icon: <Download/>,
                                        tooltip: 'Metadaten abrufen',
                                        onClick: () => {
                                            new CodeListsApiService()
                                                .getAssetColumns(codeList.sourceRef ?? '')
                                                .then((columns) => {
                                                    handleInputChange('columns')(columns);
                                                })
                                                .catch((err) => {
                                                    dispatch(showApiErrorSnackbar(err, 'Beim Abrufen der Spalten ist ein Fehler aufgetreten.'))
                                                });
                                        },
                                    },
                                ]}
                            />
                        </Stack>
                    }

                    {
                        codeList.sourceType === CodeListSourceType.XRepository &&
                        <Stack
                            direction="row"
                            spacing={2}
                        >
                            <TextFieldComponent
                                label="XRepository-URN"
                                value={codeList.sourceRef}
                                onChange={handleInputChange('sourceRef')}
                                onBlur={handleInputBlur('sourceRef')}
                                required={true}
                                error={errors.sourceRef}
                                disabled={isBusy || !isEditable}
                                hint="Verwenden Sie die spezifische Versionskennung der XRepository-Code-Liste."
                            />

                            <Actions
                                actions={[
                                    {
                                        icon: <Download/>,
                                        tooltip: 'Metadaten abrufen',
                                        onClick: () => {
                                            new CodeListsApiService()
                                                .getXRepositoryColumns(codeList.sourceRef ?? '')
                                                .then((columns) => {
                                                    handleInputChange('columns')(columns);
                                                })
                                                .catch((err) => {
                                                    dispatch(showApiErrorSnackbar(err, 'Beim Abrufen der Spalten ist ein Fehler aufgetreten.'))
                                                });
                                        },
                                    },
                                ]}
                            />
                        </Stack>
                    }
                </Grid>

                <Grid size={{xs: 12}}>
                    <StringListInput2
                        label="Spalten"
                        hint="Die Spalten dieser Code-Liste"
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
                        label="Wert-Spalte"
                        value={codeList.valueColumnIndex.toString()}
                        onChange={(value) => handleInputChange('valueColumnIndex')(Number.parseInt(value ?? '0', 10))}
                        options={columnOptions}
                        required={codeList.columns.length > 0}
                        error={errors.valueColumnIndex}
                        disabled={isBusy || !isEditable || codeList.columns.length === 0}
                    />
                </Grid>
                <Grid size={{xs: 12, lg: 6}}>
                    <SelectFieldComponent
                        label="Beschriftungs-Spalte"
                        value={codeList.labelColumnIndex.toString()}
                        onChange={(value) => handleInputChange('labelColumnIndex')(Number.parseInt(value ?? '0', 10))}
                        options={columnOptions}
                        required={codeList.columns.length > 0}
                        error={errors.labelColumnIndex}
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
                    startIcon={<SaveOutlinedIcon/>}
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
                title="Code-Liste löschen"
                onCancel={() => setShowConfirmDelete(false)}
                onConfirm={undefined}
                confirmationText={codeList.name}
                isDestructive
                confirmButtonText="Ja, endgültig löschen"
            >
                <Typography>
                    Moechten Sie diese Code-Liste wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
                </Typography>
            </ConfirmDialog>
        </Box>
    );
}
