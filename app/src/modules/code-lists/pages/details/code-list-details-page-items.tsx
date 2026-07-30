import React, {useCallback, useContext, useMemo, useRef, useState} from 'react';
import {Alert, AlertTitle, Box, Button, Typography} from '@mui/material';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import CloudUpload from '@aivot/mui-material-symbols-400-n25-outlined/CloudUpload';
import Edit from '@aivot/mui-material-symbols-400-n25-outlined/Edit';
import FileDownload from '@aivot/mui-material-symbols-400-n25-outlined/Download';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {GridColDef} from '@mui/x-data-grid';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType,
} from '../../../../components/generic-details-page/generic-details-page-context';
import {GenericList} from '../../../../components/generic-list/generic-list';
import {GenericListPropsFetchOptions, ListControlRef} from '../../../../components/generic-list/generic-list-props';
import {EmptyDataListPlaceholder} from '../../../../components/empty-data-list-placeholder/empty-data-list-placeholder';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {useConfirm} from '../../../../providers/confirm-provider';
import {showApiErrorSnackbar, showSuccessSnackbar} from '../../../../slices/snackbar-slice';
import {CodeListsApiService} from '../../code-lists-api-service';
import {CodeList} from '../../models/code-list';
import {CodeListItem} from '../../models/code-list-item';
import {CodeListSourceType} from '../../enums/code-list-source-type';
import {CodeListItemDialog} from '../../dialogs/code-list-item-dialog';
import {downloadBlobFile} from '../../../../utils/download-utils';
import {useHasSystemPermission} from '../../../permissions/hooks/use-permissions';
import {Permission} from '../../../../data/permissions/permission';
import {formatMissingPermissionTooltip} from '../../../permissions/utils/permission-utils';
import {DisabledTooltip} from '../../../../components/disabled-tooltip/disabled-tooltip';

function selectCsvFile(): Promise<File | null> {
    return new Promise((resolve) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '.csv,text/csv';
        input.addEventListener('change', () => {
            resolve(input.files?.[0] ?? null);
            input.remove();
        });
        input.addEventListener('cancel', () => {
            resolve(null);
            input.remove();
        });
        input.click();
    });
}

export function CodeListDetailsPageItems() {
    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();
    const listControlRef = useRef<ListControlRef | null>(null);

    const {
        item: codeList,
        setItem,
        isEditable,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<CodeList, void>;

    const [dialogItem, setDialogItem] = useState<CodeListItem | null>(null);
    const [showDialog, setShowDialog] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [isCsvBusy, setIsCsvBusy] = useState(false);
    const canExportCodeList = useHasSystemPermission(Permission.CODE_LIST_EXPORT);

    const isManual = codeList?.sourceType === CodeListSourceType.Manual;
    const canManageItems = isEditable && isManual;
    const isSavedCodeList = codeList != null && codeList.key.length > 0;
    const updateDisabledTooltip = !isEditable
        ? formatMissingPermissionTooltip(Permission.CODE_LIST_UPDATE)
        : undefined;
    const exportDisabledTooltip = !canExportCodeList
        ? formatMissingPermissionTooltip(Permission.CODE_LIST_EXPORT)
        : undefined;
    const addItemDisabledTooltip = updateDisabledTooltip ??
        ((codeList?.columns.length ?? 0) === 0 ? 'Für diese Codeliste sind noch keine Spalten definiert.' : undefined);

    const fetchItems = useCallback((options: GenericListPropsFetchOptions<CodeListItem>) => {
        if (codeList == null) {
            return Promise.resolve({
                content: [],
                page: {
                    size: 0,
                    number: 0,
                    totalElements: 0,
                    totalPages: 0,
                },
            });
        }

        return new CodeListsApiService()
            .listItems(
                codeList.key,
                options.page,
                options.size,
                options.sort,
                options.order,
            );
    }, [codeList]);

    const columnDefinitions = useMemo<Array<GridColDef<CodeListItem>>>(() => {
        const columns = (codeList?.columns ?? []).map((column, index) => ({
            field: `column_${index}`,
            headerName: column,
            flex: 1,
            sortable: false,
            valueGetter: (_: any, row: CodeListItem) => row.columns[index] ?? '',
        }));

        if (columns.length > 0) {
            return columns;
        }

        return [
            {
                field: 'label',
                headerName: 'Beschriftung',
                flex: 1,
            },
            {
                field: 'value',
                headerName: 'Wert',
                flex: 1,
            },
        ];
    }, [codeList?.columns]);

    const getRowIdentifier = useCallback((row: CodeListItem) => row.id.toString(), []);

    const handleOpenCreateDialog = useCallback(() => {
        if (!canManageItems) {
            return;
        }

        setDialogItem(null);
        setShowDialog(true);
    }, [canManageItems]);

    const handleSaveItem = (columns: string[]) => {
        if (codeList == null || !canManageItems) {
            return;
        }

        setIsSaving(true);

        const request = dialogItem == null
            ? new CodeListsApiService().createItem(codeList.key, columns)
            : new CodeListsApiService().updateItem(codeList.key, dialogItem.id, columns);

        request
            .then(() => {
                setShowDialog(false);
                setDialogItem(null);
                listControlRef.current?.refresh();
                dispatch(showSuccessSnackbar(dialogItem == null ? 'Eintrag wurde angelegt.' : 'Eintrag wurde gespeichert.'));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Eintrag konnte nicht gespeichert werden.'));
            })
            .finally(() => {
                setIsSaving(false);
            });
    };

    const handleDeleteItem = useCallback((item: CodeListItem) => {
        if (codeList == null || !canManageItems) {
            return;
        }

        showConfirm({
            title: 'Eintrag löschen',
            children: (
                <Typography>
                    Möchten Sie den Eintrag <strong>{item.label || item.value || item.id}</strong> wirklich löschen?
                </Typography>
            ),
            confirmButtonText: 'Eintrag löschen',
        })
            .then((confirmed) => {
                if (!confirmed) {
                    return;
                }

                return new CodeListsApiService()
                    .deleteItem(codeList.key, item.id)
                    .then(() => {
                        listControlRef.current?.refresh();
                        dispatch(showSuccessSnackbar('Eintrag wurde gelöscht.'));
                    });
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Eintrag konnte nicht gelöscht werden.'));
            });
    }, [canManageItems, codeList, dispatch, showConfirm]);

    const handleExportCsv = useCallback(() => {
        if (codeList == null || codeList.key.length === 0 || !canExportCodeList) {
            return;
        }

        setIsCsvBusy(true);

        new CodeListsApiService()
            .exportCsv(codeList.key)
            .then((blob) => {
                downloadBlobFile(`code-list-${codeList.key}.csv`, blob);
                dispatch(showSuccessSnackbar('CSV-Export wurde gestartet.'));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die CSV-Datei konnte nicht exportiert werden.'));
            })
            .finally(() => {
                setIsCsvBusy(false);
            });
    }, [canExportCodeList, codeList, dispatch]);

    const handleImportCsv = useCallback(async () => {
        if (codeList == null || codeList.key.length === 0 || !canManageItems) {
            return;
        }

        const file = await selectCsvFile();
        if (file == null) {
            return;
        }

        const confirmed = await showConfirm({
            title: 'CSV importieren',
            children: (
                <Typography>
                    Beim Import werden alle bestehenden Einträge dieser Codeliste ersetzt.
                </Typography>
            ),
            confirmButtonText: 'CSV importieren',
        });
        if (!confirmed) {
            return;
        }

        setIsCsvBusy(true);

        new CodeListsApiService()
            .importCsv(codeList.key, file)
            .then((updatedCodeList) => {
                setItem(updatedCodeList);
                listControlRef.current?.refresh();
                dispatch(showSuccessSnackbar('CSV-Datei wurde importiert.'));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die CSV-Datei konnte nicht importiert werden.'));
            })
            .finally(() => {
                setIsCsvBusy(false);
            });
    }, [canManageItems, codeList, dispatch, setItem, showConfirm]);

    const rowActions = useCallback((item: CodeListItem) => {
        if (!isManual) {
            return [];
        }

        return [
            {
                icon: <Edit/>,
                onClick: () => {
                    if (!canManageItems) {
                        return;
                    }

                    setDialogItem(item);
                    setShowDialog(true);
                },
                tooltip: 'Eintrag bearbeiten',
                disabled: !canManageItems,
                disabledTooltip: updateDisabledTooltip,
            },
            {
                icon: <Delete/>,
                onClick: () => handleDeleteItem(item),
                tooltip: 'Eintrag löschen',
                disabled: !canManageItems,
                disabledTooltip: updateDisabledTooltip,
            },
        ];
    }, [canManageItems, handleDeleteItem, isManual, updateDisabledTooltip]);

    const preSearchElements = useMemo(() => {
        if (!isSavedCodeList) {
            return undefined;
        }

        return [
            <Box
                key="actions"
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 1,
                    flexWrap: 'wrap',
                    width: '100%',
                }}
            >
                {
                    isManual &&
                    <Box
                        sx={{
                            display: 'flex',
                            gap: 1,
                            flexWrap: 'wrap',
                        }}
                    >
                        <DisabledTooltip
                            disabled={!canManageItems || (codeList?.columns.length ?? 0) === 0}
                            title={addItemDisabledTooltip}
                        >
                            <Button
                                variant="contained"
                                startIcon={<Add/>}
                                onClick={handleOpenCreateDialog}
                                disabled={!canManageItems || (codeList?.columns.length ?? 0) === 0}
                            >
                                Eintrag hinzufügen
                            </Button>
                        </DisabledTooltip>
                    </Box>
                }

                <Box
                    sx={{
                        display: 'flex',
                        gap: 1,
                        flexWrap: 'wrap',
                        ml: 'auto',
                    }}
                >
                    <DisabledTooltip
                        disabled={!canExportCodeList}
                        title={exportDisabledTooltip}
                    >
                        <Button
                            variant="outlined"
                            startIcon={<FileDownload/>}
                            onClick={handleExportCsv}
                            disabled={isCsvBusy || !canExportCodeList}
                        >
                            CSV exportieren
                        </Button>
                    </DisabledTooltip>
                    {
                        isManual &&
                        <DisabledTooltip
                            disabled={!canManageItems}
                            title={updateDisabledTooltip}
                        >
                            <Button
                                variant="outlined"
                                startIcon={<CloudUpload/>}
                                onClick={handleImportCsv}
                                disabled={isCsvBusy || !canManageItems}
                            >
                                CSV importieren
                            </Button>
                        </DisabledTooltip>
                    }
                </Box>
            </Box>,
        ];
    }, [
        addItemDisabledTooltip,
        canExportCodeList,
        canManageItems,
        codeList?.columns.length,
        exportDisabledTooltip,
        handleExportCsv,
        handleImportCsv,
        handleOpenCreateDialog,
        isCsvBusy,
        isManual,
        isSavedCodeList,
        updateDisabledTooltip,
    ]);

    if (codeList == null) {
        return null;
    }

    return (
        <Box>
            <Typography
                variant="h5"
                sx={{mt: 1.5, mb: 1}}
            >
                Einträge
            </Typography>
            <Typography sx={{mb: 2, maxWidth: 900}}>
                Prüfen und verwalten Sie die Auswahlwerte dieser Codeliste.
            </Typography>

            <Alert
                severity="info"
                sx={{mb: 2}}
            >
                <AlertTitle>
                    {isManual ? 'Öffentliche Auswahlwerte' : 'Synchronisierte Auswahlwerte'}
                </AlertTitle>

                {
                    isManual
                        ? 'Die Auswahlwerte dieser Codeliste können in öffentlichen Formularen verwendet und über die öffentliche Codelisten-API ohne Anmeldung abgerufen werden. Erfassen Sie keine vertraulichen Informationen.'
                        : 'Diese Auswahlwerte werden aus der Quelle synchronisiert und können hier nicht direkt bearbeitet werden. Nach der Synchronisierung sind sie in öffentlichen Formularen und über die öffentliche Codelisten-API ohne Anmeldung abrufbar. Die Quelle darf daher keine vertraulichen Informationen enthalten.'
                }
            </Alert>

            <GenericList<CodeListItem>
                controlRef={listControlRef}
                disableFullWidthToggle
                sx={{
                    mx: '-16px',
                    mb: '-16px',
                }}
                columnDefinitions={columnDefinitions}
                fetch={fetchItems}
                getRowIdentifier={getRowIdentifier}
                rowActionsCount={isManual ? 2 : 0}
                rowActions={isManual ? rowActions : undefined}
                defaultSortField="id"
                rowMenuItems={[]}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Einträge vorhanden"
                        description={isManual ? 'Fügen Sie die ersten Werte für diese Codeliste hinzu.' : 'Bei der nächsten Synchronisierung werden Einträge aus der Quelle geladen.'}
                        addText={isManual ? 'Eintrag hinzufügen' : undefined}
                        onAdd={isManual ? handleOpenCreateDialog : undefined}
                        addDisabled={!canManageItems || (codeList?.columns.length ?? 0) === 0}
                        addDisabledTooltip={addItemDisabledTooltip}
                    />
                }
                loadingPlaceholder="Lade Einträge..."
                noSearchResultsPlaceholder="Keine Einträge gefunden"
                preSearchElements={preSearchElements}
            />

            <CodeListItemDialog
                open={showDialog}
                codeList={codeList}
                item={dialogItem}
                isBusy={isSaving}
                onClose={() => {
                    setShowDialog(false);
                    setDialogItem(null);
                }}
                onSave={handleSaveItem}
            />
        </Box>
    );
}
