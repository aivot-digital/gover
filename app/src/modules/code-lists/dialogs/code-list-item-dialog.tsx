import React, {useEffect, useState} from 'react';
import {Alert, Dialog, DialogActions, DialogContent, DialogTitle, Button, Grid} from '@mui/material';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {CodeList} from '../models/code-list';
import {CodeListItem} from '../models/code-list-item';

interface CodeListItemDialogProps {
    open: boolean;
    codeList: CodeList;
    item: CodeListItem | null;
    isBusy: boolean;
    onClose: () => void;
    onSave: (columns: string[]) => void;
}

const CodeListLabelColumnHint = 'Der Anzeigename, der für Benutzer:innen angezeigt wird.';
const CodeListValueColumnHint = 'Der technische Schlüssel, der im Hintergrund gespeichert und an nachfolgende Prozessschritte oder Systeme übertragen wird.';

function getCodeListColumnHint(
    index: number,
    labelColumnIndex: number,
    valueColumnIndex: number,
): string | undefined {
    if (index === labelColumnIndex) {
        return CodeListLabelColumnHint;
    }

    if (index === valueColumnIndex) {
        return CodeListValueColumnHint;
    }

    return undefined;
}

export function CodeListItemDialog(props: CodeListItemDialogProps) {
    const {
        open,
        codeList,
        item,
        isBusy,
        onClose,
        onSave,
    } = props;

    const [columns, setColumns] = useState<string[]>([]);

    useEffect(() => {
        if (!open) {
            return;
        }

        setColumns(item?.columns ?? codeList.columns.map(() => ''));
    }, [codeList.columns, item, open]);

    const handleColumnChange = (index: number) => (value: string | null) => {
        setColumns((current) => {
            const next = current.slice();
            next[index] = value ?? '';
            return next;
        });
    };

    const hasColumns = codeList.columns.length > 0;

    return (
        <Dialog
            open={open}
            onClose={isBusy ? undefined : onClose}
            fullWidth
            maxWidth="md"
        >
            <DialogTitle>
                {item == null ? 'Eintrag hinzufügen' : 'Eintrag bearbeiten'}
            </DialogTitle>
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
                            codeList.columns.map((column, index) => (
                                <Grid
                                    key={`${column}-${index}`}
                                    size={{xs: 12, md: 6}}
                                >
                                    <TextFieldComponent
                                        label={column}
                                        value={columns[index] ?? ''}
                                        onChange={handleColumnChange(index)}
                                        hint={getCodeListColumnHint(
                                            index,
                                            codeList.labelColumnIndex,
                                            codeList.valueColumnIndex,
                                        )}
                                        disabled={isBusy}
                                    />
                                </Grid>
                            ))
                        }
                    </Grid>
                }
            </DialogContent>
            <DialogActions sx={{justifyContent: 'flex-start'}}>
                <Button
                    onClick={() => onSave(columns)}
                    disabled={isBusy || !hasColumns || columns.length !== codeList.columns.length}
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
