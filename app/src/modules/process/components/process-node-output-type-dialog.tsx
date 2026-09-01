import React, {type ReactNode} from 'react';
import {
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    Typography,
} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {ExpandableCodeBlock} from '../../../components/expandable-code-block/expandable-code-block';
import {useRetainedDialogValue} from '../../../hooks/use-retained-dialog-value';
import {type ProcessNodeOutput} from '../services/process-node-provider-api-service';

export interface ProcessNodeOutputTypeDialogProps {
    open: boolean;
    output: ProcessNodeOutput | null;
    onClose: () => void;
}

export function ProcessNodeOutputTypeDialog(props: ProcessNodeOutputTypeDialogProps): ReactNode {
    const renderOutput = useRetainedDialogValue(props.open, props.output);

    return (
        <Dialog
            open={props.open && renderOutput != null}
            onClose={props.onClose}
            fullWidth
            maxWidth="sm"
            scroll="paper"
        >
            <DialogTitleWithClose onClose={props.onClose}>
                TypeScript-Typdefinition
            </DialogTitleWithClose>

            {
                renderOutput != null &&
                <DialogContent>
                    <Typography variant="h6">
                        {renderOutput.label}
                    </Typography>
                    <Typography
                        variant="body2"
                        sx={{
                            mt: 0.25,
                            color: 'text.secondary',
                            fontFamily: 'monospace',
                            overflowWrap: 'anywhere',
                        }}
                    >
                        {renderOutput.key}
                    </Typography>

                    <Typography
                        variant="body2"
                        sx={{
                            mt: 1.5,
                            color: 'text.secondary',
                        }}
                    >
                        {renderOutput.description}
                    </Typography>

                    <Box sx={{mt: 2}}>
                        <ExpandableCodeBlock
                            value={renderOutput.typeDefinition}
                            language="typescript"
                            sx={{mb: 0}}
                        />
                    </Box>
                </DialogContent>
            }

            <DialogActions sx={{justifyContent: 'flex-end'}}>
                <Button onClick={props.onClose}>
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
