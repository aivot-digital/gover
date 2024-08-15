import {Box, Button, FormHelperText, FormLabel} from '@mui/material';
import {TableFieldElement} from '../../models/elements/form/input/table-field-element';
import {DataGrid, GridColumns, GridRenderCellParams, GridSelectionModel} from '@mui/x-data-grid';
import React, {useCallback, useState} from 'react';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {BaseViewProps} from "../../views/base-view";
import {ConfirmDialog} from "../../dialogs/confirm-dialog/confirm-dialog";

export function TableFieldComponentView({
                                            element,
                                            value,
                                            error,
                                            setValue,
                                        }: BaseViewProps<TableFieldElement, any[]>) {
    const [selectionModel, setSelectionModel] = useState<GridSelectionModel>();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    const handleAddRow = useCallback(() => {
        if (element.id != null) {
            const newRow = (element.fields ?? []).reduce((acc, val) => {
                acc[val.label] = '';
                return acc;
            }, {} as {[key: string]: string});

            setValue([
                ...(value ?? []),
                newRow,
            ]);
        }
    }, [element.fields, element.id, setValue, value]);

    const handleSelectionModelChange = useCallback((model) => {
        setSelectionModel(model);
    }, []);

    const handleDelete = useCallback(() => {
        if (element.id != null && selectionModel != null) {
            const updatedRows = (value ?? []).filter((_: any, index: number) => !selectionModel.includes(index));
            setValue(updatedRows.length > 0 ? updatedRows : undefined);
            setSelectionModel([]);
            setConfirmDelete(undefined);
        }
    }, [element.id, selectionModel, setValue, value]);

    const handleCellEdit = useCallback((params) => {
        if (element.id) {
            const updatedValues = [...(value ?? [])];
            updatedValues[params.id] = {
                ...updatedValues[params.id],
                [params.field]: params.value,
            };
            setValue(updatedValues);
        }
    }, [element.id, setValue, value]);

    const columns: GridColumns = (element.fields ?? []).map(field => ({
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

    const rows = (value ?? []).map((data: any, index: number) => ({
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
                    error={error != null}
                >
                    {element.label} {element.required && ' *'}
                </FormLabel>

                {
                    !element.disabled &&
                    <Box>
                        <Button
                            onClick={handleAddRow}
                            disabled={element.maximumRows != null && rows.length >= element.maximumRows}
                        >
                            Hinzufügen
                        </Button>
                        <Button
                            color="error"
                            onClick={() => setConfirmDelete(() => handleDelete)}
                            disabled={!hasSelectedRows}
                            sx={{ml: 1}}
                        >
                            Löschen
                        </Button>
                    </Box>
                }
            </Box>

            <div style={{
                width: '100%',
            }}>
                <DataGrid
                    rows={rows}
                    columns={columns}
                    pageSize={8}
                    rowsPerPageOptions={[8, 16, 32]}
                    autoHeight

                    checkboxSelection={!element.disabled}
                    disableSelectionOnClick
                    onSelectionModelChange={handleSelectionModelChange}
                    selectionModel={selectionModel}

                    onCellEditCommit={handleCellEdit}

                    disableColumnSelector
                    disableColumnFilter
                />
            </div>

            {
                (error || element.hint) &&
                <FormHelperText
                    sx={{mt: 1}}
                    error={error != null}
                >
                    {
                        error == null ?
                            element.hint :
                            error
                    }
                </FormHelperText>
            }

            <ConfirmDialog
                title="Möchten Sie die ausgewählten Einträge wirklich löschen?"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Dieser Vorgang kann nicht rückgängig gemacht werden. Wenn Sie die Daten löschen, müssen Sie diese bei Bedarf erneut eingeben. Möchten Sie die Daten wirklich löschen?
            </ConfirmDialog>
        </>
    );
}
