import {useState} from 'react';
import {Alert, Box, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeEditorExpressionNode} from '../utils/no-code-editor-store';

interface NoCodeOperatorChangeDialogProps {
    operator: NoCodeOperatorDetailsDTO;
    node: NoCodeEditorExpressionNode;
    onClose: () => void;
    onApply: (parameterCount: number) => void;
}

/** The persisted format selects signatures by operand count, not by a separate signature ID. */
export function NoCodeOperatorChangeDialog({
                                               operator,
                                               node,
                                               onClose,
                                               onApply,
                                           }: NoCodeOperatorChangeDialogProps) {
    const counts = [...new Set(operator.signatures.map((signature) => signature.parameters.length))];
    const [count, setCount] = useState(counts.find((candidate) => candidate >= node.operands.length) ?? counts[0]);
    const removed = node.operands.slice(count).filter((operand) => operand != null).length;

    return (
        <Dialog open
                onClose={onClose}
                fullWidth
                maxWidth="sm">
            <DialogTitleWithClose onClose={onClose}>Operator ändern</DialogTitleWithClose>
            <DialogContent>
                <Typography sx={{mb: 2}}>Neuer Operator: {operator.label}</Typography>
                <SelectFieldComponent
                    label="Anzahl der Parameter"
                    required
                    includeEmptyOption={false}
                    value={String(count)}
                    options={counts.map((value) => ({
                        value: String(value),
                        label: String(value),
                    }))}
                    onChange={(value) => {
                        if (value != null) setCount(Number(value));
                    }}
                />
                <Box sx={{mt: 2}}>
                    {removed > 0
                        ? <Alert severity="warning">
                            {removed === 1 ? 'Ein belegter Parameter wird' : `${removed} belegte Parameter werden`} einschließlich
                                                                                                                    aller
                                                                                                                    untergeordneten
                                                                                                                    Ausdrücke
                                                                                                                    entfernt. {count === 0
                            ? 'Es bleiben keine bisherigen Parameter erhalten.'
                            : count === 1 ? 'Der erste Parameter bleibt erhalten.' : `Die ersten ${count} Parameter bleiben erhalten.`}
                            {' '}Sie können die Änderung anschließend rückgängig machen.
                        </Alert>
                        : <Typography>Vorhandene Parameter bleiben erhalten. Zusätzliche Parameter beginnen
                                      leer.</Typography>}
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Abbrechen</Button>
                <Button variant="contained"
                        onClick={() => onApply(count)}>Operator ändern</Button>
            </DialogActions>
        </Dialog>
    );
}
