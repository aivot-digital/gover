import {Box, Button, FormHelperText, FormLabel} from '@mui/material';
import {DataGrid, GridColDef, GridPaginationModel, GridRenderCellParams, GridRowSelectionModel} from '@mui/x-data-grid';
import React, {useMemo, useState} from 'react';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {TableFieldComponentProps} from './table-field-component-props';
import {getSelectedRowIds, hasSelectedGridRows} from './table-field-selection';

/**
 * @deprecated use TableFieldComponent2 instead
 * @param props
 * @constructor
 */
export function TableFieldComponent(props: TableFieldComponentProps) {
    // Store the currently selected rows in this state to be able to delete them later
    const [selectionModel, setSelectionModel] = useState<GridRowSelectionModel>();

    // Store the confirm delete function in this state to signal the confirm dialog that the deletion is about to happen
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    // Store the currently selected page and size in this state to be able to change it later
    const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({
        pageSize: 8,
        page: 0,
    });

    // Normalize the value to always be an empty list. This makes working with the value alot easier later on.
    const value = useMemo(() => {
        return props.value ?? [];
    }, [props.value]);

    // Adding a row is only adding a new Record with empty values to the list of rows
    const handleAddRow = () => {
        props.onChange([
            ...value,
            {},
        ]);
    };

    const handleDeleteRows = () => {
        if (selectionModel == null) {
            // No rows selected so no deletion required
            return;
        }

        const selectedIds = new Set(getSelectedRowIds(selectionModel, rows.map((row) => row.id)));

        // Filter out the rows that are selected
        const updatedRows = value
            .filter((_: any, index: number) => !selectedIds.has(index));

        // Propagate the change. If no rows are left, propagate undefined to signal that the field is empty
        props.onChange(updatedRows.length > 0 ? updatedRows : undefined);

        // Reset the selection model and the confirm dialog
        setSelectionModel({ type: 'include', ids: new Set() });
        setConfirmDelete(undefined);
    };

    const handleRowUpdate = (newRow: any, oldRow: any) => {
        // Extract the index of the currently edited row
        let rowIndex = newRow.id;
        if (typeof rowIndex === 'string') {
            rowIndex = parseInt(rowIndex, 10);
        }

        // Create a new updated value array with the updated value
        const updatedValues = [...value];
        updatedValues[rowIndex] = newRow;

        // Propagate the change
        props.onChange(updatedValues);

        return { ...newRow, updatedAt: new Date() };
    };

    // Determine the columns for the data grid based on the fields of the element
    const columns: GridColDef[] = useMemo(() => {
        return props.fields
            .map((field) => ({
                field: field.label,
                headerName: field.label + (field.optional ? '' : ' *'),
                editable: !field.disabled,
                flex: 1,
                type: field.datatype,
                renderCell: (params: GridRenderCellParams) => (
                    (params.value == null || params.value.length === 0) &&
                    field.placeholder != null &&
                    field.placeholder.length > 0 ?
                        (
                            <i style={{opacity: '0.65'}}>
                                {field.placeholder}
                            </i>
                        ) : (
                            field.datatype === 'number' ? formatNumStringToGermanNum(params.value, field.decimalPlaces) : params.value
                        )
                ),
            }));
    }, [props.fields]);

    const rows = value.map((data: any, index: number) => ({
        id: index,
        ...data,
    }));

    const hasSelectedRows = useMemo(() => {
        return hasSelectedGridRows(selectionModel, rows.map((row) => row.id));
    }, [rows, selectionModel]);

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    mb: 1,
                }}
            >
                <FormLabel
                    error={props.error != null}
                >
                    {props.label} {props.required && ' *'}
                </FormLabel>

                {
                    !props.disabled &&
                    <Box>
                        <Button
                            onClick={handleAddRow}
                            disabled={props.maximumRows != null && rows.length >= props.maximumRows}
                        >
                            Hinzufügen
                        </Button>
                        <Button
                            color="error"
                            onClick={() => setConfirmDelete(() => handleDeleteRows)}
                            disabled={!hasSelectedRows}
                            sx={{ml: 1}}
                        >
                            Löschen
                        </Button>
                    </Box>
                }
            </Box>

            <div
                style={{
                    width: '100%',
                }}
            >
                <DataGrid
                    rows={rows}
                    columns={columns}
                    paginationModel={paginationModel}
                    onPaginationModelChange={setPaginationModel}
                    pageSizeOptions={[8, 16, 32]}
                    autoHeight

                    checkboxSelection={!props.disabled}
                    disableRowSelectionExcludeModel
                    onRowClick={(params, event) => {
                        event.defaultMuiPrevented = true;
                    }}

                    rowSelectionModel={selectionModel}
                    onRowSelectionModelChange={(newModel) => setSelectionModel(newModel)}

                    processRowUpdate={handleRowUpdate}

                    disableColumnSelector
                    disableColumnFilter
                />
            </div>

            {
                (props.error || props.hint) &&
                <FormHelperText
                    sx={{mt: 1}}
                    error={props.error != null}
                >
                    {
                        props.error == null ?
                            props.hint :
                            props.error
                    }
                </FormHelperText>
            }

            <ConfirmDialog
                title="Möchten Sie die ausgewählten Einträge wirklich löschen?"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Dieser Vorgang kann nicht rückgängig gemacht werden. Wenn Sie die Daten löschen, müssen Sie diese bei Bedarf erneut eingeben.
                Möchten Sie die Daten wirklich löschen?
            </ConfirmDialog>
        </>
    );
}
