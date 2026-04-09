import {Box, FormHelperText, FormLabel, SxProps} from '@mui/material';
import {DataGrid, GridColDef, GridFooter, GridFooterContainer, GridPaginationModel, GridRowId, GridRowSelectionModel, GridValidRowModel} from '@mui/x-data-grid';
import React, {ReactNode, useMemo, useState} from 'react';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {GridColType} from '@mui/x-data-grid/models/colDef/gridColType';
import {Actions} from '../actions/actions';
import AddIcon from '@mui/icons-material/Add';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import {Action} from '../actions/actions-props';
import {InfoDialog} from '../../dialogs/info-dialog/info-dialog';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {getSelectedRowIds, hasSelectedGridRows} from './table-field-selection';

interface TableField<T extends GridValidRowModel, K extends keyof T & string> {
    key: K;
    label: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    type?: GridColType;
    renderCell?: (value: T[K]) => ReactNode;
}

interface TableFieldComponentProps<T extends GridValidRowModel> {
    label: string;
    hint?: string;
    error?: string;
    noRowsPlaceholder?: string;
    fields: TableField<T, keyof T & string>[];
    createDefaultRow: () => T;
    value?: T[];
    onChange: (value: T[] | undefined) => void;
    disabled?: boolean;
    required?: boolean;
    rowsHaveIds?: boolean;
    maximumRows?: number;
    helpDialog?: {
        title: string;
        content: ReactNode;
    };
    addTooltip?: string;
    deleteTooltip?: string;
    actions?: Action[];
    sx?: SxProps;
}

// TODO: Unify with table-field.component.view.tsx
export function TableFieldComponent2<T extends GridValidRowModel>(props: TableFieldComponentProps<T>) {
    const {
        label,
        hint,
        error,
        noRowsPlaceholder,
        fields,
        createDefaultRow,
        value: originalValue,
        onChange,
        disabled,
        required,
        rowsHaveIds,
        maximumRows,
        helpDialog,
        addTooltip,
        deleteTooltip,
        actions,
        sx,
    } = props;

    // Store the currently selected rows in this state to be able to delete them later
    const [selectionModel, setSelectionModel] = useState<GridRowSelectionModel>();

    // Store the confirm delete function in this state to signal the confirm dialog that the deletion is about to happen
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    // Store the currently selected page and size in this state to be able to change it later
    const [paginationModel, setPaginationModel] = useState<GridPaginationModel>({
        pageSize: 8,
        page: 0,
    });

    // Store the help dialog state in this state to signal the help dialog that the deletion is about to happen
    const [showHelpDialog, setShowHelpDialog] = useState(false);

    // Normalize the value to always be an empty list. This makes working with the value alot easier later on.
    const value = useMemo(() => {
        return originalValue ?? [];
    }, [originalValue]);

    // Adding a row is only adding a new Record with empty values to the list of rows
    const handleAddRow = () => {
        onChange([
            ...(value ?? []),
            createDefaultRow(),
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
            .filter((row: T, index: number) => {
                const rowId: GridRowId = rowsHaveIds ? (row as T & { id: GridRowId }).id : index;
                return !selectedIds.has(rowId);
            });

        // Propagate the change. If no rows are left, propagate undefined to signal that the field is empty
        onChange(updatedRows.length > 0 ? updatedRows : undefined);

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
        onChange(updatedValues);

        return { ...newRow, updatedAt: new Date() };
    };

    // Determine the columns for the data grid based on the fields of the element
    const columns: GridColDef[] = useMemo(() => {
        return fields.map((field) => ({
            field: field.key,
            headerName: field.label + (field.required ? ' *' : ''),
            editable: !field.disabled,
            flex: 1,
            type: field.type,
            /*renderCell: (params: GridRenderCellParams<any>) => {
                if (field.renderCell != null) {
                    return field.renderCell(params.value);
                }

                if (field.type === 'number') {
                    return formatNumStringToGermanNum(params.value);
                }

                return params.value;
            },
             */
        }));
    }, [fields]);

    const rows: Array<T & { id: any }> = useMemo(() => {
        if (rowsHaveIds) {
            return value as Array<T & { id: any }>;
        }
        return value.map((data: T, index: number) => ({
            id: index,
            ...data,
        }));
    }, [value, rowsHaveIds]);

    const hasSelectedRows = useMemo(() => {
        return hasSelectedGridRows(selectionModel, rows.map((row) => row.id));
    }, [rows, selectionModel]);

    return (
        <>
            <DataGrid
                sx={sx}
                rows={rows}
                columns={columns}
                paginationModel={paginationModel}
                onPaginationModelChange={setPaginationModel}
                pageSizeOptions={[8, 16, 32]}
                autoHeight
                showToolbar

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

                slots={{
                    noRowsOverlay: () => (
                        <Box
                            sx={{
                                display: 'flex',
                                justifyContent: 'center',
                                alignItems: 'center',
                                height: '100%',
                                opacity: 0.75,
                            }}
                        >
                            {noRowsPlaceholder ?? 'Keine Einträge vorhanden'}
                        </Box>
                    ),
                    toolbar: () => (
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                my: 2,
                                mx: 2,
                            }}
                        >
                            <FormLabel
                                error={error != null}
                            >
                                {label} {required && ' *'}
                            </FormLabel>

                            {
                                <Actions
                                    sx={{
                                        ml: 'auto',
                                    }}
                                    actions={[
                                        {
                                            icon: <AddIcon />,
                                            label: 'Hinzufügen',
                                            tooltip: addTooltip,
                                            onClick: handleAddRow,
                                            disabled: maximumRows != null && rows.length >= maximumRows,
                                            visible: !disabled,
                                        },
                                        {
                                            icon: <Delete />,
                                            label: 'Löschen',
                                            tooltip: deleteTooltip,
                                            onClick: () => setConfirmDelete(() => handleDeleteRows),
                                            disabled: !hasSelectedRows,
                                            visible: !disabled,
                                        },
                                        {
                                            icon: <HelpOutlineOutlinedIcon />,
                                            tooltip: 'Hilfe',
                                            onClick: () => {
                                                setShowHelpDialog(true);
                                            },
                                            visible: helpDialog != null,
                                        },
                                        ...(actions ?? []),
                                    ]}
                                />
                            }
                        </Box>
                    ),
                    footer: () => (
                        <GridFooterContainer>
                            {
                                (error ?? hint) != null &&
                                <Box
                                    sx={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        p: 2,
                                    }}
                                >
                                    {
                                        (error || hint) &&
                                        <FormHelperText
                                            sx={{mt: 1}}
                                            error={error != null}
                                        >
                                            {error ?? hint}
                                        </FormHelperText>
                                    }
                                </Box>
                            }

                            <GridFooter
                                sx={{
                                    ml: 'auto',
                                    borderTop: 'none',
                                }}
                            />
                        </GridFooterContainer>
                    ),
                }}
            />

            <ConfirmDialog
                title="Möchten Sie die ausgewählten Einträge wirklich löschen?"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Dieser Vorgang kann nicht rückgängig gemacht werden. Wenn Sie die Daten löschen, müssen Sie diese bei Bedarf erneut eingeben.
                Möchten Sie die Daten wirklich löschen?
            </ConfirmDialog>

            {
                helpDialog != null &&
                <InfoDialog
                    open={showHelpDialog}
                    title={helpDialog.title}
                    severity="info"
                    onClose={() => {
                        setShowHelpDialog(false);
                    }}
                >
                    {helpDialog.content}
                </InfoDialog>
            }
        </>
    );
}
