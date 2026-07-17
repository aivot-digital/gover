import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import {Grid, Stack} from '@mui/material';
import {
    isNoCodeNodeDataReference,
    isNoCodeProcessDataReference,
    NoCodeInstanceDataReference,
    NoCodeNodeDataReference, NoCodeOperandError,
    NoCodeProcessDataReference,
} from '../../../models/functions/no-code-expression';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {Actions} from '../../../components/actions/actions';
import {ProcessDataKeyInputComponent} from '../../../views/process-data-key-input-field-view';
import Typography from '@mui/material/Typography';

interface NoCodeOperandEditorProcessDataReferenceProps {
    label: string;
    hint?: string;
    value: NoCodeProcessDataReference | NoCodeInstanceDataReference | NoCodeNodeDataReference;
    onChange: (value: NoCodeProcessDataReference | NoCodeInstanceDataReference | NoCodeNodeDataReference | undefined) => void;
    onAddEnclosingExpression: () => void;
    operandError?: NoCodeOperandError;
}

export function NoCodeOperandEditorProcessDataReference(props: NoCodeOperandEditorProcessDataReferenceProps) {
    const {
        label,
        hint,
        value,
        onChange,
        onAddEnclosingExpression,
        operandError,
    } = props;

    const isProcessDataReference = isNoCodeProcessDataReference(value);
    const isNodeDataReference = isNoCodeNodeDataReference(value);
    const sourceLabel = isProcessDataReference
        ? 'Vorgangsdaten ($)'
        : isNodeDataReference
            ? 'Pfad in den Elementdaten'
            : 'Geschützte Vorgangsdaten ($$)';

    const startIcon = isProcessDataReference
        ? '$.'
        : isNodeDataReference
            ? `_.${value.nodeDataKey}.`
            : '$$.';

    const referenceActions = [
        {
            icon: <Delete/>,
            tooltip: 'Diesen Vorgangsdaten-Verweis löschen',
            onClick: () => onChange(undefined),
        },
        {
            tooltip: 'Diesen Verweis mit einem Ausdruck verknüpfen',
            icon: <Functions/>,
            onClick: onAddEnclosingExpression,
        },
    ];

    return (
        <Grid
            container
            spacing={2}
        >
            {
                isNodeDataReference &&
                <Grid size={4}>
                    <TextFieldComponent
                        label="Datenschlüssel des Prozesselementes"
                        hint="Der Datenschlüssel des Prozesselementes, aus dessen Ergebnis gelesen werden soll."
                        value={value.nodeDataKey ?? undefined}
                        onChange={(nodeDataKey) => onChange({
                            ...value,
                            nodeDataKey: nodeDataKey ?? undefined,
                        })}
                        muiPassTroughProps={{margin: 'none'}}
                        startIcon="_."
                        error={operandError?.error ?? undefined}
                    />
                </Grid>
            }

            <Grid size={isNodeDataReference ? 8 : 12}>
                <Stack
                    direction="row"
                    alignItems="flex-start"
                >
                    {
                        isProcessDataReference
                            ? (
                                <ProcessDataKeyInputComponent
                                    label={`${label ?? ''} — (${sourceLabel})`}
                                    hint={hint}
                                    value={value.path ?? undefined}
                                    onChange={(path) => {
                                        onChange({
                                            ...value,
                                            path: path ?? undefined,
                                        });
                                    }}
                                    disableWildCards={true}
                                    error={operandError?.error ?? undefined}
                                />
                            )
                            : (
                                <TextFieldComponent
                                    label={`${label ?? ''} — (${sourceLabel})`}
                                    hint={hint}
                                    value={value.path ?? undefined}
                                    onChange={(path) => {
                                        onChange({
                                            ...value,
                                            path: path ?? undefined,
                                        });
                                    }}
                                    startIcon={startIcon}
                                    endAction={isNodeDataReference ? undefined : referenceActions}
                                    muiPassTroughProps={{margin: 'none'}}
                                    error={operandError?.error ?? undefined}
                                />
                            )
                    }

                    {
                        isProcessDataReference &&
                        <Actions
                            size="small"
                            dense={true}
                            color="inherit"
                            actions={referenceActions}
                            sx={{
                                mt: 1.5,
                                ml: 1,
                                opacity: 0.66,
                            }}
                        />
                    }

                    {
                        isNodeDataReference &&
                        <Actions
                            size="small"
                            dense={true}
                            color="inherit"
                            actions={referenceActions}
                            sx={{
                                mt: 1.5,
                                ml: 1,
                                opacity: 0.66,
                            }}
                        />
                    }
                </Stack>
            </Grid>
        </Grid>
    );
}
