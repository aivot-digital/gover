import {type ReactNode, useId} from 'react';
import {
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    Divider,
    Stack,
    Typography,
} from '@mui/material';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {ExpandableCodeBlock} from '../../../components/expandable-code-block/expandable-code-block';
import {useRetainedDialogValue} from '../../../hooks/use-retained-dialog-value';
import {type ProcessNodeOutput} from '../services/process-node-provider-api-service';
import {ProcessNodeOutputReference} from './process-node-output-reference';

export interface ProcessNodeOutputTypeDialogProps {
    open: boolean;
    output: ProcessNodeOutput | null;
    /**
     * Complete element-data path for a configured element. Provider information
     * only has the output key because it has no element data key.
     */
    dataPath?: string;
    onClose: () => void;
}

export function ProcessNodeOutputTypeDialog(props: ProcessNodeOutputTypeDialogProps): ReactNode {
    const dialogId = useId();
    const outputTitleId = `${dialogId}-output-title`;
    const descriptionId = `${dialogId}-description`;
    const accessTitleId = `${dialogId}-access-title`;
    const typeTitleId = `${dialogId}-type-title`;
    // Retain both together so the close transition cannot mix a previous output
    // with a new element's path.
    const {output: renderOutput, dataPath: renderDataPath} = useRetainedDialogValue(props.open, {
        output: props.output,
        dataPath: props.dataPath,
    });

    return (
        <Dialog
            open={props.open && renderOutput != null}
            onClose={props.onClose}
            fullWidth
            maxWidth="sm"
            scroll="paper"
            aria-describedby={descriptionId}
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Details zu den Ausgangsdaten
            </DialogTitleWithClose>

            {
                renderOutput != null &&
                <DialogContent sx={{pb: 3}}>
                    <Stack spacing={2} divider={<Divider/>}>
                        <Box component="section" aria-labelledby={outputTitleId}>
                            <Typography id={outputTitleId} variant="h5" component="h3" sx={{overflowWrap: 'anywhere'}}>
                                {renderOutput.label}
                            </Typography>
                            <Typography id={descriptionId} variant="body2" color="text.secondary" sx={{mt: 1}}>
                                {renderOutput.description}
                            </Typography>
                        </Box>

                        <Box component="section" aria-labelledby={accessTitleId}>
                            <Typography id={accessTitleId} variant="h6" component="h3">
                                Zugriff auf die Daten
                            </Typography>
                            <Typography variant="body2" color="text.secondary" sx={{mt: 1}}>
                                {renderDataPath != null
                                    ? 'Über diesen Pfad greifen Sie in weiteren Prozessschritten direkt auf den Wert in den Elementdaten zu. Bei einer Zuordnung zu den Vorgangsdaten steht der Wert zusätzlich unter dem eingetragenen Zielpfad bereit.'
                                    : 'Der Schlüssel bezeichnet diesen Wert innerhalb der Ausgangsdaten. Für den vollständigen Datenpfad wird er mit dem Datenschlüssel des Prozesselements kombiniert.'}
                            </Typography>
                            <Box sx={{mt: 1.5, px: 1, py: 0.5, bgcolor: 'action.hover', borderRadius: 1}}>
                                <ProcessNodeOutputReference
                                    value={renderDataPath ?? renderOutput.key}
                                    kind={renderDataPath != null ? 'path' : 'key'}
                                />
                            </Box>
                        </Box>

                        <Box component="section" aria-labelledby={typeTitleId}>
                            <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                                <Typography id={typeTitleId} variant="h6" component="h3">
                                    Datentyp
                                </Typography>
                                <Chip label="TypeScript" size="small" variant="outlined"/>
                            </Box>
                            <Typography variant="body2" color="text.secondary" sx={{mt: 1, mb: 1.5}}>
                                Die Definition beschreibt die Art und den Aufbau der Werte, z. B. Text, Zahlen oder ein
                                Objekt mit mehreren Feldern.
                            </Typography>
                            <ExpandableCodeBlock
                                value={renderOutput.typeDefinition}
                                language="typescript"
                                sx={{mb: 0}}
                            />
                        </Box>
                    </Stack>
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
