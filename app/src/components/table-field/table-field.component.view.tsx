import {Box, Button, FormHelperText, FormLabel} from '@mui/material';
import {TableFieldElement} from '../../models/elements/form/input/table-field-element';
import {DataGrid, GridCellEditCommitParams, GridColumns, GridRenderCellParams, GridSelectionModel} from '@mui/x-data-grid';
import React, {useMemo, useState} from 'react';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {BaseViewProps} from '../../views/base-view';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {parseGermanNumber} from '../../utils/parse-german-numbers';
import {isStringNullOrEmpty} from '../../utils/string-utils';

// TODO: Unify with table-field-component.tsx
export function TableFieldComponentView(props: BaseViewProps<TableFieldElement, { [key: string]: string | number | null }[]>) {
    const {
        element,
        setValue,
        value,
        error,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        id,
        fields,
        disabled,
    } = element;

    const [selectionModel, setSelectionModel] = useState<GridSelectionModel>();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();
    const [pageSize, setPageSize] = useState(8);

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const handleAddRow = () => {
        if (id == null) {
            return;
        }

        const newRow = (fields ?? []).reduce((acc, val) => {
            acc[val.label] = null;
            return acc;
        }, {} as { [key: string]: string | number | null });

        setValue([
            ...(value ?? []),
            newRow,
        ]);
    };


    const handleDelete = () => {
        if (element.id != null && selectionModel != null) {
            const updatedRows = (value ?? []).filter((_: any, index: number) => !selectionModel.includes(index));
            setValue(updatedRows.length > 0 ? updatedRows : undefined);
            setSelectionModel([]);
            setConfirmDelete(undefined);
        }
    };

    const handleCellEdit = (params: GridCellEditCommitParams) => {
        if (fields == null) {
            return;
        }

        const field = fields
            .find(field => field.label === params.field);

        if (field == null) {
            return;
        }

        let cellValue = params.value;

        if (cellValue == null) {
            cellValue = undefined;
        } else if (field.datatype === 'number') {
            if (typeof cellValue === 'string' && isNaN(Number(cellValue))) {
                cellValue = parseGermanNumber(cellValue);
            }

            cellValue = parseFloat(Number(cellValue).toFixed(field.decimalPlaces ?? 0));
        } else if (field.datatype === 'string') {
            if (isStringNullOrEmpty(cellValue)) {
                cellValue = undefined;
            }

            cellValue = cellValue.toString();
        }

        const updatedValues = [
            ...(value ?? []),
        ];

        updatedValues[params.id as number] = {
            ...updatedValues[params.id as number],
            [params.field]: cellValue,
        };

        setValue(updatedValues);
    };

    const columns: GridColumns = useMemo(() => {
        if (fields == null) {
            return [];
        }

        return fields
            .map((field) => ({
                field: field.label,
                headerName: field.label + (field.optional ? '' : ' *'),
                editable: !field.disabled && !isDisabled && !isBusy && !isDeriving,
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
                            field.datatype === 'number' ?
                                formatNumStringToGermanNum(params.value, field.decimalPlaces) :
                                params.value
                        )
                ),
            }));
    }, [fields, isDisabled, isBusy, isDeriving]);

    const rows = useMemo(() => {
        if (value == null) {
            return [];
        }

        return value
            .map((data: any, index: number) => ({
                id: index,
                ...data,
            }));
    }, [value]);

    const hasSelectedRows = useMemo(() => selectionModel != null && selectionModel.length > 0, [selectionModel]);

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
                            disabled={isDisabled || isBusy || (element.maximumRows != null && rows.length >= element.maximumRows)}
                        >
                            Hinzufügen
                        </Button>
                        <Button
                            color="error"
                            onClick={() => setConfirmDelete(() => handleDelete)}
                            disabled={isDisabled || isBusy || !hasSelectedRows}
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

                    // can stay active on isBusy because pointerEvents are blocked via CSS and to prevent layout shift
                    checkboxSelection={!element.disabled}

                    disableSelectionOnClick={true}
                    onSelectionModelChange={!isBusy ? setSelectionModel : undefined}
                    selectionModel={selectionModel}

                    onCellEditCommit={!isBusy ? handleCellEdit : undefined}

                    disableColumnSelector
                    disableColumnFilter

                    sx={{
                        backgroundColor: isBusy ? '#F8F8F8' : undefined,
                        cursor: isBusy ? 'not-allowed' : undefined,
                        pointerEvents: isBusy ? 'none' : 'auto',
                    }}
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
