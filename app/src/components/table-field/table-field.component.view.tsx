import {Box, Button, FormHelperText, FormLabel} from '@mui/material';
import {TableFieldElement} from '../../models/elements/form/input/table-field-element';
import {DataGrid, GridColumns, GridRenderCellParams, GridSelectionModel} from '@mui/x-data-grid';
import {useCallback, useState} from 'react';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {BaseViewProps} from "../../views/base-view";

export function TableFieldComponentView({element, value, error, setValue}: BaseViewProps<TableFieldElement, any[]>) {
    const [selectionModel, setSelectionModel] = useState<GridSelectionModel>();

    const handleAddRow = useCallback(() => {
        if (element.id != null) {
            const newRow = (element.fields ?? []).reduce((acc, val) => {
                acc[val.label] = '';
                return acc;
            }, {} as { [key: string]: string })

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
            setSelectionModel(undefined);
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
                    <i>
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
                            onClick={handleDelete}
                            disabled={!hasSelectedRows}
                            sx={{ml: 1}}
                        >
                            Löschen
                        </Button>
                    </Box>
                }
            </Box>

            <div style={{height: 550, width: '100%'}}>
                <DataGrid
                    rows={rows}
                    columns={columns}
                    pageSize={8}
                    rowsPerPageOptions={[8, 16, 32]}

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
        </>
    );
}
