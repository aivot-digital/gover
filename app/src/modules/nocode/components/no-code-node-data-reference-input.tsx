import {useContext, useMemo, useState} from 'react';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {EndAction} from '../../../components/text-field/text-field-component-props';
import {VariablePickerDialog} from '../../../components/input-mode-field/input-mode-field';
import {ViewDispatcherContext} from '../../../components/view-dispatcher/view-dispatcher.context';
import {NoCodeNodeDataReference} from '../../../models/functions/no-code-expression';
import {InputVariableSource} from '../../../models/input-mode';
import {
    useOptionalProcessNodeEditorContext,
} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';
import {
    formatNoCodeNodeDataPath,
    splitNoCodeNodeDataPath,
    validateNoCodeNodeDataPath,
} from '../utils/no-code-node-data-path';

const ELEMENT_DATA_SOURCES = [InputVariableSource.ElementData];

interface NoCodeNodeDataReferenceInputProps {
    label: string;
    hint?: string;
    value: NoCodeNodeDataReference;
    onChange: (value: NoCodeNodeDataReference) => void;
    error?: string;
    endAction: EndAction[];
}

export function NoCodeNodeDataReferenceInput(props: NoCodeNodeDataReferenceInputProps) {
    const view = useContext(ViewDispatcherContext);
    const process = useOptionalProcessNodeEditorContext();
    const [pickerOpen, setPickerOpen] = useState(false);
    const variables = useMemo(() => (view?.inputModeVariables ?? process?.incomingMetadata?.inputVariables ?? [])
            .filter((variable) => variable.source === InputVariableSource.ElementData && validateNoCodeNodeDataPath(variable) == null),
        [view?.inputModeVariables, process?.incomingMetadata?.inputVariables]);
    const selectedReference = useMemo(() => ({
        source: InputVariableSource.ElementData,
        nodeDataKey: props.value.nodeDataKey,
        path: props.value.path ?? '',
    }), [props.value.nodeDataKey, props.value.path]);

    return <>
        <TextFieldComponent
            required
            label={props.label}
            hint={props.hint}
            value={formatNoCodeNodeDataPath(props.value)}
            onChange={(path) => props.onChange({...props.value, ...splitNoCodeNodeDataPath(path ?? '')})}
            startIcon="_."
            copyable
            copyValueTemplate="_.{value}"
            validate={(path) => validateNoCodeNodeDataPath(splitNoCodeNodeDataPath(path ?? ''))}
            error={props.error}
            margin="none"
            endAction={[{
                icon: <DataObject/>,
                tooltip: 'Elementdaten auswählen',
                onClick: () => setPickerOpen(true),
            }, ...props.endAction]}
        />
        {pickerOpen && <VariablePickerDialog
            open
            title="Elementdaten auswählen"
            variables={variables}
            allowedSources={ELEMENT_DATA_SOURCES}
            selectedReference={selectedReference}
            allowClear={false}
            readOnly={false}
            onClose={() => setPickerOpen(false)}
            onClear={() => undefined}
            onSelect={(reference) => {
                props.onChange({
                    ...props.value,
                    nodeDataKey: reference.nodeDataKey,
                    path: reference.path,
                });
                setPickerOpen(false);
            }}
        />}
    </>;
}
