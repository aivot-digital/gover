import {useContext, useMemo, useState} from 'react';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {EndAction} from '../../../components/text-field/text-field-component-props';
import {VariablePickerDialog} from '../../../components/input-mode-field/input-mode-field';
import {ViewDispatcherContext} from '../../../components/view-dispatcher/view-dispatcher.context';
import {NoCodeInstanceDataReference} from '../../../models/functions/no-code-expression';
import {InputVariableSource} from '../../../models/input-mode';
import {
    useOptionalProcessNodeEditorContext,
} from '../../process/pages/details/components/process-node-editor/process-node-editor-context';

const SOURCES = [InputVariableSource.ProtectedProcessData];

export function NoCodeProtectedDataReferenceInput(props: {
    label: string;
    hint?: string;
    value: NoCodeInstanceDataReference;
    onChange: (value: NoCodeInstanceDataReference) => void;
    error?: string;
    endAction: EndAction[];
}) {
    const view = useContext(ViewDispatcherContext);
    const process = useOptionalProcessNodeEditorContext();
    const [open, setOpen] = useState(false);
    const variables = useMemo(() => (view?.inputModeVariables ?? process?.incomingMetadata?.inputVariables ?? [])
            .filter(variable => variable.source === InputVariableSource.ProtectedProcessData),
        [view?.inputModeVariables, process?.incomingMetadata?.inputVariables]);
    const selectedReference = useMemo(() => ({
        source: InputVariableSource.ProtectedProcessData,
        path: props.value.path ?? '',
    }), [props.value.path]);
    return <>
        <TextFieldComponent required
                            label={props.label}
                            hint={props.hint}
                            value={props.value.path ?? ''}
                            onChange={(path) => props.onChange({
                                ...props.value,
                                path: path ?? '',
                            })}
                            startIcon="$$."
                            copyable
                            copyValueTemplate="$$.{value}"
                            error={props.error}
                            margin="none"
                            endAction={[{
                                icon: <DataObject/>,
                                tooltip: 'Geschützte Vorgangsdaten auswählen',
                                onClick: () => setOpen(true),
                            }, ...props.endAction]}/>
        {open && <VariablePickerDialog open
                                       title="Geschützte Vorgangsdaten auswählen"
                                       variables={variables}
                                       allowedSources={SOURCES}
                                       selectedReference={selectedReference}
                                       allowClear={false}
                                       readOnly={false}
                                       onClose={() => setOpen(false)}
                                       onClear={() => undefined}
                                       onSelect={(reference) => {
                                           props.onChange({
                                               ...props.value,
                                               path: reference.path,
                                           });
                                           setOpen(false);
                                       }}/>}
    </>;
}
