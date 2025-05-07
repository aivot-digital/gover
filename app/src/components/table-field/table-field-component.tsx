import {Box, Button, FormHelperText, FormLabel} from '@mui/material';
import {DataGrid, GridCellEditCommitParams, GridColumns, GridRenderCellParams, GridSelectionModel} from '@mui/x-data-grid';
import React, {useMemo, useState} from 'react';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {TableFieldComponentProps} from './table-field-component-props';

/**
 * @deprecated use TableFieldComponent2 instead
 * @param props
 * @constructor
 */
export function TableFieldComponent(props: TableFieldComponentProps) {
    // Store the currently selected rows in this state to be able to delete them later
    const [selectionModel, setSelectionModel] = useState<GridSelectionModel>();

    // Store the confirm delete function in this state to signal the confirm dialog that the deletion is about to happen
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    // Store the currently selected page size in this state to be able to change it later
    const [pageSize, setPageSize] = useState(8);

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

        // Filter out the rows that are selected
        const updatedRows = value
            .filter((_: any, index: number) => !selectionModel.includes(index));

        // Propagate the change. If no rows are left, propagate undefined to signal that the field is empty
        props.onChange(updatedRows.length > 0 ? updatedRows : undefined);

        // Reset the selection model and the confirm dialog
        setSelectionModel([]);
        setConfirmDelete(undefined);
    };

    const handleCellEdit = (params: GridCellEditCommitParams) => {
        // Extract the index of the currently edited row
        let rowIndex = params.id;
        if (typeof rowIndex === 'string') {
            rowIndex = parseInt(rowIndex, 10);
        }

        // Create a new updated value array with the updated value
        const updatedValues = [...value];
        updatedValues[rowIndex] = {
            ...updatedValues[rowIndex],
            [params.field]: params.value,
        };

        // Propagate the change
        props.onChange(updatedValues);
    };

    // Determine the columns for the data grid based on the fields of the element
    const columns: GridColumns = useMemo(() => {
        return props.fields
            .map((field) => ({
                field: field.label,
                headerName: field.label + (field.optional ? '' : ' *'),
                editable: !field.disabled,
                flex: 1,
                type: field.datatype,
                renderCell: (params: GridRenderCellParams<string>) => (
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

    const hasSelectedRows = selectionModel != null && selectionModel.length > 0;

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
                    pageSize={pageSize}
                    rowsPerPageOptions={[8, 16, 32]}
                    onPageSizeChange={(newPageSize) => setPageSize(newPageSize)}
                    autoHeight

                    checkboxSelection={!props.disabled}
                    disableSelectionOnClick
                    onSelectionModelChange={setSelectionModel}
                    selectionModel={selectionModel}

                    onCellEditCommit={handleCellEdit}

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
