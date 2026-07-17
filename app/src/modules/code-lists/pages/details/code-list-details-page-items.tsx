import React, {useCallback, useContext, useMemo, useRef, useState} from 'react';
import {Alert, Box, Button, Typography} from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
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

    const isManual = codeList?.sourceType === CodeListSourceType.Manual;
    const canManageItems = isEditable && isManual;
    const isSavedCodeList = codeList != null && codeList.id !== 0;

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
                codeList.id,
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
                field: 'value',
                headerName: 'Wert',
                flex: 1,
            },
            {
                field: 'label',
                headerName: 'Beschriftung',
                flex: 1,
            },
        ];
    }, [codeList?.columns]);

    const getRowIdentifier = useCallback((row: CodeListItem) => row.id.toString(), []);

    const handleOpenCreateDialog = () => {
        setDialogItem(null);
        setShowDialog(true);
    };

    const handleSaveItem = (columns: string[]) => {
        if (codeList == null || !canManageItems) {
            return;
        }

        setIsSaving(true);

        const request = dialogItem == null
            ? new CodeListsApiService().createItem(codeList.id, columns)
            : new CodeListsApiService().updateItem(codeList.id, dialogItem.id, columns);

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

    const handleDeleteItem = (item: CodeListItem) => {
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
                    .deleteItem(codeList.id, item.id)
                    .then(() => {
                        listControlRef.current?.refresh();
                        dispatch(showSuccessSnackbar('Eintrag wurde gelöscht.'));
                    });
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Der Eintrag konnte nicht gelöscht werden.'));
            });
    };

    const handleExportCsv = useCallback(() => {
        if (codeList == null || codeList.id === 0) {
            return;
        }

        setIsCsvBusy(true);

        new CodeListsApiService()
            .exportCsv(codeList.id)
            .then((blob) => {
                downloadBlobFile(`code-list-${codeList.id}.csv`, blob);
                dispatch(showSuccessSnackbar('CSV-Export wurde gestartet.'));
            })
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Die CSV-Datei konnte nicht exportiert werden.'));
            })
            .finally(() => {
                setIsCsvBusy(false);
            });
    }, [codeList, dispatch]);

    const handleImportCsv = useCallback(async () => {
        if (codeList == null || codeList.id === 0 || !canManageItems) {
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
                    Der Import ersetzt alle bestehenden Einträge dieser Codeliste.
                </Typography>
            ),
            confirmButtonText: 'CSV importieren',
        });
        if (!confirmed) {
            return;
        }

        setIsCsvBusy(true);

        new CodeListsApiService()
            .importCsv(codeList.id, file)
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
        if (!canManageItems) {
            return [];
        }

        return [
            {
                icon: <EditOutlinedIcon/>,
                onClick: () => {
                    setDialogItem(item);
                    setShowDialog(true);
                },
                tooltip: 'Eintrag bearbeiten',
            },
            {
                icon: <Delete/>,
                onClick: () => handleDeleteItem(item),
                tooltip: 'Eintrag löschen',
            },
        ];
    }, [canManageItems]);

    const preSearchElements = useMemo(() => {
        if (!isSavedCodeList) {
            return undefined;
        }

        return [
            <Box
                key="actions"
                sx={{
                    display: 'flex',
                    gap: 1,
                    flexWrap: 'wrap',
                }}
            >
                <Button
                    variant="outlined"
                    startIcon={<FileDownloadOutlinedIcon/>}
                    onClick={handleExportCsv}
                    disabled={isCsvBusy}
                >
                    CSV exportieren
                </Button>
                {
                    canManageItems &&
                    <Button
                        variant="outlined"
                        startIcon={<CloudUploadOutlinedIcon/>}
                        onClick={handleImportCsv}
                        disabled={isCsvBusy}
                    >
                        CSV importieren
                    </Button>
                }
                {
                    canManageItems &&
                    <Button
                        variant="contained"
                        startIcon={<AddOutlinedIcon/>}
                        onClick={handleOpenCreateDialog}
                        disabled={(codeList?.columns.length ?? 0) === 0}
                    >
                        Eintrag hinzufügen
                    </Button>
                }
            </Box>,
        ];
    }, [canManageItems, codeList?.columns.length, handleExportCsv, handleImportCsv, isCsvBusy, isSavedCodeList]);

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
                Verwalten Sie die Werte dieser Codeliste.
            </Typography>

            <Alert
                severity="info"
                sx={{mb: 2}}
            >
                Alle Codelisten sind öffentlich verfügbar und dürfen keine vertraulichen Informationen enthalten.
            </Alert>

            {
                !isManual &&
                <Alert
                    severity="info"
                    sx={{mb: 2}}
                >
                    Einträge synchronisierter Codelisten werden aus der Quelle gelesen und können hier nicht direkt
                    bearbeitet werden.
                </Alert>
            }

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
                rowActionsCount={canManageItems ? 2 : 0}
                rowActions={canManageItems ? rowActions : undefined}
                defaultSortField="id"
                rowMenuItems={[]}
                noDataPlaceholder={
                    <EmptyDataListPlaceholder
                        title="Keine Einträge vorhanden"
                        description={isManual ? 'Fügen Sie die ersten Werte für diese Codeliste hinzu.' : 'Bei der nächsten Synchronisierung werden Einträge aus der Quelle geladen.'}
                        addText={canManageItems ? 'Eintrag hinzufügen' : undefined}
                        onAdd={canManageItems ? handleOpenCreateDialog : undefined}
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
