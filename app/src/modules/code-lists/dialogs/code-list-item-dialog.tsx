import React, {useEffect, useMemo, useState} from 'react';
import {Alert, Dialog, DialogActions, DialogContent, Button, Grid} from '@mui/material';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {CodeList, getAvailableColumnMappings} from '../models/code-list';
import {CodeListItem} from '../models/code-list-item';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';

interface CodeListItemDialogProps {
    open: boolean;
    codeList: CodeList;
    item: CodeListItem | null;
    isBusy: boolean;
    onClose: () => void;
    onSave: (columns: string[]) => void;
}

const KeyColumnHint = 'Die Werte der Schlüsselspalte identifizieren die Einträge der Codeliste.';

export function CodeListItemDialog(props: CodeListItemDialogProps) {
    const {
        open,
        codeList,
        item,
        isBusy,
        onClose,
        onSave,
    } = props;

    // The cells follow the stored column order; columns without a mapping keep their cells unchanged.
    const [columns, setColumns] = useState<string[]>([]);
    const [showValidationErrors, setShowValidationErrors] = useState(false);
    const columnMappings = useMemo(() => getAvailableColumnMappings(codeList), [codeList]);

    useEffect(() => {
        if (!open) {
            return;
        }

        setColumns(codeList.columns.map((_, index) => item?.columns[index] ?? ''));
        setShowValidationErrors(false);
    }, [codeList.columns, item, open]);

    const handleColumnChange = (index: number) => (value: string | null) => {
        setColumns((current) => {
            const next = current.slice();
            next[index] = value ?? '';
            return next;
        });
    };

    const hasColumns = columnMappings.length > 0;
    const handleClose = isBusy ? () => undefined : onClose;
    const keyColumn = columnMappings.find((mapping) => mapping.key === codeList.keyColumnKey);
    const keyColumnError = keyColumn != null && !columns[(keyColumn.index ?? 0) - 1]?.trim()
        ? `Bitte geben Sie für „${keyColumn.name}“ einen Wert ein.`
        : undefined;
    const handleSave = () => {
        setShowValidationErrors(true);
        if (isBusy || !hasColumns || keyColumnError != null) {
            return;
        }

        onSave(columns);
    };

    return (
        <Dialog
            open={open}
            onClose={isBusy ? undefined : onClose}
            fullWidth
            maxWidth="md"
        >
            <DialogTitleWithClose onClose={handleClose}>
                {item == null ? 'Eintrag hinzufügen' : 'Eintrag bearbeiten'}
            </DialogTitleWithClose>
            <DialogContent>
                {
                    !hasColumns &&
                    <Alert severity="warning">
                        Für diese Codeliste sind noch keine Spalten definiert.
                    </Alert>
                }
                {
                    hasColumns &&
                    <Grid
                        container
                        columnSpacing={3}
                        rowSpacing={2}
                        sx={{mt: 0.5}}
                    >
                        {
                            columnMappings.map((mapping) => {
                                const index = (mapping.index ?? 0) - 1;
                                const isKeyColumn = mapping === keyColumn;
                                return (
                                    <Grid
                                        key={mapping.key}
                                        size={{xs: 12, md: 6}}
                                    >
                                        <TextFieldComponent
                                            label={mapping.name}
                                            value={columns[index] ?? ''}
                                            onChange={handleColumnChange(index)}
                                            required={isKeyColumn}
                                            error={showValidationErrors && isKeyColumn ? keyColumnError : undefined}
                                            hint={isKeyColumn ? KeyColumnHint : undefined}
                                            disabled={isBusy}
                                        />
                                    </Grid>
                                );
                            })
                        }
                    </Grid>
                }
            </DialogContent>
            <DialogActions sx={{justifyContent: 'flex-start'}}>
                <Button
                    onClick={handleSave}
                    disabled={isBusy || !hasColumns}
                    variant="contained"
                    startIcon={<Save/>}
                >
                    Speichern
                </Button>
                <Button
                    onClick={onClose}
                    disabled={isBusy}
                    variant="outlined"
                >
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
