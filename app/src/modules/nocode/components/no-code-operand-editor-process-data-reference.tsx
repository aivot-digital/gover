import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import {Grid, Stack} from '@mui/material';
import {
    isNoCodeNodeDataReference,
    isNoCodeProcessDataReference,
    NoCodeInstanceDataReference,
    NoCodeNodeDataReference,
    NoCodeProcessDataReference,
} from '../../../models/functions/no-code-expression';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {Actions} from '../../../components/actions/actions';
import {ProcessDataKeyInputComponent} from '../../../views/process-data-key-input-field-view';

interface NoCodeOperandEditorProcessDataReferenceProps {
    label: string;
    hint?: string;
    value: NoCodeProcessDataReference | NoCodeInstanceDataReference | NoCodeNodeDataReference;
    onChange: (value: NoCodeProcessDataReference | NoCodeInstanceDataReference | NoCodeNodeDataReference | undefined) => void;
    onAddEnclosingExpression: () => void;
}

export function NoCodeOperandEditorProcessDataReference(props: NoCodeOperandEditorProcessDataReferenceProps) {
    const {
        label,
        hint,
        value,
        onChange,
        onAddEnclosingExpression,
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
