import {Box, Button, FormHelperText, FormLabel, SxProps} from '@mui/material';
import {DataGrid, GridCellEditCommitParams, GridColumns, GridFooter, GridFooterContainer, GridRenderCellParams, GridSelectionModel, GridValidRowModel} from '@mui/x-data-grid';
import React, {ReactNode, useMemo, useState} from 'react';
import {formatNumStringToGermanNum} from '../../utils/format-german-numbers';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import {GridColType} from '@mui/x-data-grid/models/colDef/gridColType';
import {Actions} from '../actions/actions';
import AddIcon from '@mui/icons-material/Add';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import {Action} from '../actions/actions-props';
import {InfoDialog} from '../../dialogs/info-dialog/info-dialog';

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
    const [selectionModel, setSelectionModel] = useState<GridSelectionModel>();

    // Store the confirm delete function in this state to signal the confirm dialog that the deletion is about to happen
    const [confirmDelete, setConfirmDelete] = useState<() => void>();

    // Store the currently selected page size in this state to be able to change it later
    const [pageSize, setPageSize] = useState(8);

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

        // Filter out the rows that are selected
        const updatedRows = value
            .filter((_: any, index: number) => !selectionModel.includes(index));

        // Propagate the change. If no rows are left, propagate undefined to signal that the field is empty
        onChange(updatedRows.length > 0 ? updatedRows : undefined);

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
        onChange(updatedValues);
    };

    // Determine the columns for the data grid based on the fields of the element
    const columns: GridColumns = useMemo(() => {
        return fields.map((field) => ({
            field: field.key,
            headerName: field.label + (field.required ? ' *' : ''),
            editable: !field.disabled,
            flex: 1,
            type: field.type,
            renderCell: (params: GridRenderCellParams<any>) => {
                if (field.renderCell != null) {
                    return field.renderCell(params.value);
                }

                if (field.type === 'number') {
                    return formatNumStringToGermanNum(params.value);
                }

                return params.value;
            },
        }));
    }, [fields]);

    const rows: Array<T & { id: any }> = useMemo(() => {
        if (rowsHaveIds) {
            return rows;
        }
        return value.map((data: T, index: number) => ({
            id: index,
            ...data,
        }));
    }, [value, rowsHaveIds]);

    const hasSelectedRows = useMemo(() => selectionModel != null && selectionModel.length > 0, [selectionModel]);

    return (
        <>
            <DataGrid
                sx={sx}
                rows={rows}
                columns={columns}
                pageSize={pageSize}
                rowsPerPageOptions={[8, 16, 32]}
                onPageSizeChange={(newPageSize) => setPageSize(newPageSize)}
                autoHeight

                checkboxSelection={!disabled}
                disableSelectionOnClick
                onSelectionModelChange={setSelectionModel}
                selectionModel={selectionModel}

                onCellEditCommit={handleCellEdit}

                disableColumnSelector
                disableColumnFilter

                components={{
                    NoRowsOverlay: () => (
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
                    Toolbar: () => (
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                mt: 2,
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
                                            icon: <DeleteForeverOutlinedIcon />,
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
                    Footer: () => (
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
