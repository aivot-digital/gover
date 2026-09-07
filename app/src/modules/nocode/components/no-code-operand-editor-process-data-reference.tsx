import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import {Grid} from '@mui/material';
import {
    isNoCodeNodeDataReference,
    isNoCodeProcessDataReference,
    NoCodeInstanceDataReference,
    NoCodeNodeDataReference, NoCodeOperandError,
    NoCodeProcessDataReference,
} from '../../../models/functions/no-code-expression';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {ProcessDataKeyInputComponent} from '../../../views/process-data-key-input-field-view';

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
                {isProcessDataReference ? (
                    <ProcessDataKeyInputComponent
                        label={`${label ?? ''} — (${sourceLabel})`}
                        hint={hint}
                        value={value.path ?? undefined}
                        onChange={(path) => onChange({...value, path: path ?? undefined})}
                        disableWildCards={true}
                        error={operandError?.error ?? undefined}
                        endAction={referenceActions}
                        margin="none"
                    />
                ) : (
                    <TextFieldComponent
                        label={`${label ?? ''} — (${sourceLabel})`}
                        hint={hint}
                        value={value.path ?? undefined}
                        onChange={(path) => onChange({...value, path: path ?? undefined})}
                        startIcon={startIcon}
                        endAction={referenceActions}
                        margin="none"
                        error={operandError?.error ?? undefined}
                    />
                )}
            </Grid>
        </Grid>
    );
}
