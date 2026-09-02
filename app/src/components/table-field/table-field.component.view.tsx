import {TableFieldElement} from '../../models/elements/form/input/table-field-element';
import {DataGrid, GridColDef, GridPaginationModel, GridRenderCellParams, GridRowId, GridRowSelectionModel, GridValidRowModel} from '@mui/x-data-grid';
import React, {useMemo, useState} from 'react';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {BaseViewProps} from '../../views/base-view';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {parseGermanNumber} from '../../utils/parse-german-numbers';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {getSelectedRowIds, hasSelectedGridRows} from './table-field-selection';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';
import AddIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {type Action} from '../actions/actions-props';
import {TableFieldLayout} from './table-field-layout';
import {TableFieldColumnHeader} from './table-field-column-header';

// TODO: Unify with table-field-component.tsx
export function TableFieldComponentView(props: BaseViewProps<TableFieldElement, { [key: string]: string | number | null }[]>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        id,
        fields,
        disabled,
    } = element;

    const [selectionModel, setSelectionModel] = useState<GridRowSelectionModel>();
    const [confirmDelete, setConfirmDelete] = useState<() => void>();
    const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({
        pageSize: 8,
        page: 0,
    });

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
            acc[val.key ?? ''] = null;
            return acc;
        }, {} as { [key: string]: string | number | null });

        setValue([
            ...(value ?? []),
            newRow,
        ]);
    };


    const handleDelete = () => {
        if (element.id != null && selectionModel != null) {
            const selectedIds = new Set(getSelectedRowIds(selectionModel, rows.map((row) => row._id)));
            const updatedRows = (value ?? []).filter((_: any, index: number) => !selectedIds.has(index));
            setValue(updatedRows.length > 0 ? updatedRows : null);
            setSelectionModel({
                type: 'include',
                ids: new Set(),
            });
            setConfirmDelete(undefined);
        }
    };

    const handleCellEdit = (newRow: GridValidRowModel, oldRow: GridValidRowModel, params: {
        rowId: GridRowId;
    }) => {
        const processedRow: Record<string, string | number | null> = {};
        for (const field of fields ?? []) {
            if (field.key == null) {
                continue;
            }

            let cellValue = newRow[field.key];

            if (cellValue == null) {
                cellValue = null;
            } else if (field.datatype === 'number') {
                if (typeof cellValue === 'string' && isNaN(Number(cellValue))) {
                    cellValue = parseGermanNumber(cellValue);
                }

                cellValue = parseFloat(Number(cellValue).toFixed(field.decimalPlaces ?? 0));
            } else if (field.datatype === 'string') {
                if (isStringNullOrEmpty(cellValue)) {
                    cellValue = null;
                } else {
                    cellValue = cellValue.toString();
                }
            }

            processedRow[field.key] = cellValue;
        }

        const updatedValues = [
            ...(value ?? []),
        ];

        updatedValues[params.rowId as number] = processedRow;

        setValue(updatedValues);

        return {
            ...processedRow,
            _id: params.rowId,
        }
    };

    const columns: GridColDef[] = useMemo(() => {
        if (fields == null) {
            return [];
        }

        return fields
            .map((field) => ({
                field: field.key ?? '',
                headerName: field.label ?? '',
                renderHeader: () => (
                    <TableFieldColumnHeader
                        label={field.label ?? ''}
                        optional={field.optional === true}
                    />
                ),
                editable: !field.disabled && !isDisabled && !isBusy && !isDeriving,
                flex: 1,
                type: field.datatype ?? 'string',
                renderCell: (params: GridRenderCellParams<any>) => (
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
                _id: index,
                ...data,
            }));
    }, [value]);

    const hasSelectedRows = useMemo(() => hasSelectedGridRows(selectionModel, rows.map((row) => row._id)), [rows, selectionModel]);
    const fieldError = errors != null && errors.length > 0 ? errors.join(' ') : undefined;
    const tableActions: Action[] = [
        {
            icon: <AddIcon/>,
            iconPosition: 'start',
            label: 'Hinzufügen',
            tooltip: 'Tabellenzeile hinzufügen',
            ariaLabel: 'Tabellenzeile hinzufügen',
            onClick: handleAddRow,
            disabled: isDisabled || isBusy || (element.maximumRows != null && rows.length >= element.maximumRows),
        },
        {
            icon: <Delete/>,
            iconPosition: 'start',
            label: 'Löschen',
            tooltip: 'Ausgewählte Tabellenzeilen löschen',
            ariaLabel: 'Ausgewählte Tabellenzeilen löschen',
            onClick: () => setConfirmDelete(() => handleDelete),
            disabled: isDisabled || isBusy || !hasSelectedRows,
            color: 'error',
        },
    ];

    return (
        <>
            <TableFieldLayout
                label={element.label ?? ''}
                hint={element.hint ?? undefined}
                error={fieldError}
                required={element.required ?? undefined}
                disabled={isDisabled}
                busy={isBusy}
                actions={tableActions}
            >
                {(fieldContext) => (
                    <DataGrid
                        rows={rows}
                        getRowId={row => row._id}
                        columns={columns}
                        paginationModel={paginationModel}
                        pageSizeOptions={[8, 16, 32]}
                        onPaginationModelChange={(newPaginationModel) => setPaginationModel(newPaginationModel)}
                        autoHeight

                        // Keep the selection column mounted while deriving to avoid a layout shift.
                        checkboxSelection={!element.disabled}
                        disableRowSelectionExcludeModel

                        disableRowSelectionOnClick
                        onRowSelectionModelChange={isDisabled || isBusy ? undefined : setSelectionModel}
                        rowSelectionModel={selectionModel}

                        processRowUpdate={isDisabled || isBusy ? undefined : handleCellEdit}

                        disableColumnSelector
                        disableColumnFilter

                        aria-labelledby={fieldContext.labelId}
                        aria-describedby={fieldContext.describedBy}
                        aria-invalid={fieldContext.invalid || undefined}
                        aria-busy={fieldContext.busy || undefined}
                        sx={{
                            backgroundColor: isBusy ? getDisabledFieldBackground : undefined,
                            borderBottom: '1px solid',
                            borderBottomColor: 'divider',
                            cursor: (isBusy || isDisabled) ? 'not-allowed' : undefined,
                            pointerEvents: isBusy ? 'none' : 'auto',
                        }}
                    />
                )}
            </TableFieldLayout>

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
