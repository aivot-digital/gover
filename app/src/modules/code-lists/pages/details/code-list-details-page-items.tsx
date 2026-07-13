import React, {useCallback, useContext, useMemo, useRef, useState} from 'react';
import {Alert, Box, Button, Typography} from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
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

export function CodeListDetailsPageItems() {
    const dispatch = useAppDispatch();
    const showConfirm = useConfirm();
    const listControlRef = useRef<ListControlRef | null>(null);

    const {
        item: codeList,
        isEditable,
    } = useContext(GenericDetailsPageContext) as GenericDetailsPageContextType<CodeList, void>;

    const [dialogItem, setDialogItem] = useState<CodeListItem | null>(null);
    const [showDialog, setShowDialog] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    const isManual = codeList?.sourceType === CodeListSourceType.Manual;
    const canManageItems = isEditable && isManual;

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
        if (!canManageItems) {
            return undefined;
        }

        return [
            <Button
                key="add"
                variant="contained"
                startIcon={<AddOutlinedIcon/>}
                onClick={handleOpenCreateDialog}
                disabled={(codeList?.columns.length ?? 0) === 0}
            >
                Eintrag hinzufügen
            </Button>,
        ];
    }, [canManageItems, codeList?.columns.length]);

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
                Verwalten Sie die Werte dieser Code-Liste.
            </Typography>

            {
                !isManual &&
                <Alert
                    severity="info"
                    sx={{mb: 2}}
                >
                    Einträge synchronisierter Code-Listen werden aus der Quelle gelesen und können hier nicht direkt
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
                        description={isManual ? 'Fügen Sie die ersten Werte für diese Code-Liste hinzu.' : 'Bei der nächsten Synchronisierung werden Einträge aus der Quelle geladen.'}
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
