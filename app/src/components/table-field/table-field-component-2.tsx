import {Box, type SxProps, type Theme} from '@mui/material';
import {DataGrid, GridColDef, GridPaginationModel, GridRowId, GridRowSelectionModel, GridValidRowModel} from '@mui/x-data-grid';
import React, {ReactNode, useMemo, useState} from 'react';
import {ConfirmDialog} from '../../dialogs/confirm-dialog/confirm-dialog';
import AddIcon from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import HelpOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Help';
import {Action} from '../actions/actions-props';
import {InfoDialog} from '../../dialogs/info-dialog/info-dialog';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {getSelectedRowIds, hasSelectedGridRows} from './table-field-selection';
import {type FormFieldGroupLayoutProps} from '../form-field';
import {TableFieldLayout} from './table-field-layout';
import {TableFieldColumnHeader} from './table-field-column-header';
import {getDisabledFieldBackground} from '../../theming/field-state-colors';

interface TableField<T extends GridValidRowModel, K extends keyof T & string> {
    key: K;
    label: string;
    placeholder?: string;
    required?: boolean;
    disabled?: boolean;
    type?: GridColDef['type'];
    renderCell?: (value: T[K]) => ReactNode;
}

interface TableFieldComponentProps<T extends GridValidRowModel> extends FormFieldGroupLayoutProps {
    label: string;
    hint?: string;
    error?: string;
    noRowsPlaceholder?: string;
    fields: TableField<T, keyof T & string>[];
    createDefaultRow: () => T;
    value?: T[] | null;
    onChange: (value: T[] | null) => void;
    disabled?: boolean;
    busy?: boolean;
    readOnly?: boolean;
    required?: boolean;
    rowsHaveIds?: boolean;
    maximumRows?: number;
    helpDialog?: {
        title: string;
        content: ReactNode;
    };
    addTooltip?: string;
    addLabel?: string;
    deleteTooltip?: string;
    actions?: Action[];
    controlSx?: SxProps<Theme>;
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
        busy,
        readOnly,
        required,
        rowsHaveIds,
        maximumRows,
        helpDialog,
        addTooltip,
        addLabel,
        deleteTooltip,
        actions,
        controlSx,
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
    const isInteractionDisabled = Boolean(disabled || busy || readOnly);

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

        // Propagate the change. If no rows are left, propagate an explicit clear.
        onChange(updatedRows.length > 0 ? updatedRows : null);

        // Reset the selection model and the confirm dialog
        setSelectionModel({ type: 'include', ids: new Set() });
        setConfirmDelete(undefined);
    };

    const handleRowUpdate = (newRow: GridValidRowModel, oldRow: GridValidRowModel) => {
        const rowIndex = rows.findIndex((row) => row.id === newRow.id);
        if (rowIndex < 0) {
            return oldRow;
        }

        // DataGrid adds an index-based ID when domain rows have no ID. Keep that implementation
        // detail out of the authored value, while preserving genuine domain IDs when configured.
        const {id: _generatedId, ...rowWithoutGeneratedId} = newRow;
        const updatedRow = (rowsHaveIds ? newRow : rowWithoutGeneratedId) as T;

        // Create a new updated value array with the updated value
        const updatedValues = [...value];
        updatedValues[rowIndex] = updatedRow;

        // Propagate the change
        onChange(updatedValues);

        return newRow;
    };

    // Determine the columns for the data grid based on the fields of the element
    const columns: GridColDef[] = useMemo(() => {
        return fields.map((field) => ({
            field: field.key,
            headerName: field.label,
            renderHeader: () => (
                <TableFieldColumnHeader
                    label={field.label}
                    optional={!field.required}
                />
            ),
            editable: !field.disabled && !isInteractionDisabled,
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
    }, [fields, isInteractionDisabled]);

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

    const tableActions: Action[] = [
        {
            icon: <AddIcon/>,
            iconPosition: 'start',
            label: addLabel ?? 'Hinzufügen',
            tooltip: addTooltip ?? addLabel ?? 'Eintrag hinzufügen',
            ariaLabel: addTooltip ?? addLabel ?? 'Eintrag hinzufügen',
            onClick: handleAddRow,
            disabled: isInteractionDisabled || (maximumRows != null && rows.length >= maximumRows),
        },
        {
            icon: <Delete/>,
            iconPosition: 'start',
            label: 'Löschen',
            tooltip: deleteTooltip ?? 'Ausgewählte Einträge löschen',
            ariaLabel: deleteTooltip ?? 'Ausgewählte Einträge löschen',
            onClick: () => setConfirmDelete(() => handleDeleteRows),
            disabled: isInteractionDisabled || !hasSelectedRows,
            color: 'error',
        },
        {
            icon: <HelpOutlineOutlinedIcon/>,
            iconPosition: 'start',
            label: 'Hilfe',
            tooltip: 'Hilfe',
            ariaLabel: `Hilfe zu ${label}`,
            onClick: () => setShowHelpDialog(true),
            visible: helpDialog != null,
            ignoreBusy: true,
        },
        ...(actions ?? []),
    ];

    return (
        <>
            <TableFieldLayout
                id={props.id}
                label={label}
                ariaDescribedBy={props.ariaDescribedBy}
                labelAction={props.labelAction}
                hint={hint}
                error={error}
                required={required}
                disabled={disabled}
                busy={busy}
                readOnly={readOnly}
                margin={props.margin ?? 'normal'}
                sx={props.sx}
                showOptionalIndicator={props.showOptionalIndicator}
                actions={tableActions}
            >
                {(fieldContext) => (
                    <DataGrid
                        sx={[
                            {
                                backgroundColor: busy ? getDisabledFieldBackground : undefined,
                                borderBottom: '1px solid',
                                borderBottomColor: 'divider',
                                cursor: busy ? 'not-allowed' : undefined,
                                pointerEvents: busy ? 'none' : undefined,
                            },
                            ...(Array.isArray(controlSx) ? controlSx : [controlSx]),
                        ]}
                        rows={rows}
                        columns={columns}
                        paginationModel={paginationModel}
                        onPaginationModelChange={setPaginationModel}
                        pageSizeOptions={[8, 16, 32]}
                        autoHeight
                        checkboxSelection={!disabled && !readOnly}
                        disableRowSelectionExcludeModel
                        onRowClick={(params, event) => {
                            event.defaultMuiPrevented = true;
                        }}

                        rowSelectionModel={selectionModel}
                        onRowSelectionModelChange={isInteractionDisabled ? undefined : setSelectionModel}

                        processRowUpdate={isInteractionDisabled ? undefined : handleRowUpdate}

                        disableColumnSelector
                        disableColumnFilter

                        aria-labelledby={fieldContext.labelId}
                        aria-describedby={fieldContext.describedBy}
                        aria-invalid={fieldContext.invalid || undefined}
                        aria-busy={fieldContext.busy || undefined}
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
                        }}
                    />
                )}
            </TableFieldLayout>

            <ConfirmDialog
                title="Möchten Sie die ausgewählten Einträge wirklich löschen?"
                onConfirm={confirmDelete}
                onCancel={() => setConfirmDelete(undefined)}
            >
                Dieser Vorgang kann nicht rückgängig gemacht werden. Wenn Sie die Daten löschen, müssen Sie diese bei Bedarf erneut eingeben.
                Möchten Sie die Daten wirklich löschen?
            </ConfirmDialog>

            {helpDialog != null && (
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
            )}
        </>
    );
}
