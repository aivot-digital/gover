import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import Functions from '@aivot/mui-material-symbols-400-n25-outlined/Functions';
import {
    isNoCodeNodeDataReference,
    isNoCodeProcessDataReference,
    NoCodeInstanceDataReference,
    NoCodeNodeDataReference, NoCodeOperandError,
    NoCodeProcessDataReference,
} from '../../../models/functions/no-code-expression';
import {NoCodeProtectedDataReferenceInput} from './no-code-protected-data-reference-input';
import {NoCodeNodeDataReferenceInput} from './no-code-node-data-reference-input';
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
            ? 'Elementdaten (_)'
            : 'Geschützte Vorgangsdaten ($$)';

    const referenceActions = [
        {
            color: 'error' as const,
            icon: <Delete/>,
            tooltip: 'Diesen Verweis löschen',
            onClick: () => onChange(undefined),
        },
        {
            tooltip: 'Diesen Verweis mit einem Ausdruck verknüpfen',
            icon: <Functions/>,
            onClick: onAddEnclosingExpression,
        },
    ];

    if (isNodeDataReference) {
        return <NoCodeNodeDataReferenceInput
            label={`${label} — (${sourceLabel})`}
            hint={hint}
            value={value}
            onChange={onChange}
            error={operandError?.error ?? undefined}
            endAction={referenceActions}
        />;
    }

    return isProcessDataReference ? <ProcessDataKeyInputComponent
        required
        label={`${label} — (${sourceLabel})`}
        hint={hint}
        value={value.path ?? undefined}
        onChange={(path) => onChange({
            ...value,
            path: path ?? undefined,
        })}
        disableWildCards={true}
        error={operandError?.error ?? undefined}
        endAction={referenceActions}
        margin="none"
    /> : <NoCodeProtectedDataReferenceInput
        label={`${label} — (${sourceLabel})`}
        hint={hint}
        value={value}
        onChange={onChange}
        endAction={referenceActions}
        error={operandError?.error ?? undefined}
    />;
}
