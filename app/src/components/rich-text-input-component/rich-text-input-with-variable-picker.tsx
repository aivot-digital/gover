import {useMemo, useRef, useState} from 'react';
import DataObject from '@aivot/mui-material-symbols-400-n25-outlined/DataObject';
import {
    getInputModeVariableCategoryLabel,
    getInputModeVariableReference,
    type InputModeVariable,
    VariablePickerDialog,
} from '../input-mode-field/input-mode-field';
import {getInputVariableReference, InputVariableSource} from '../../models/input-mode';
import {useViewDispatcherContext} from '../view-dispatcher/view-dispatcher.context';
import {
    RichTextInputComponent,
    type RichTextInputComponentMethods,
    type RichTextInputComponentProps,
} from './rich-text-input-component';

const VARIABLE_SOURCES: InputVariableSource[] = [
    InputVariableSource.ElementMetadata,
    InputVariableSource.ProcessData,
    InputVariableSource.ProtectedProcessData,
];

export type RichTextInputWithVariablePickerProps = Omit<
    RichTextInputComponentProps,
    'dynamicText' | 'dynamicTextVariableMetadata' | 'endAction'
>;

export function RichTextInputWithVariablePicker(props: RichTextInputWithVariablePickerProps) {
    const {inputModeVariables} = useViewDispatcherContext();
    const inputRef = useRef<RichTextInputComponentMethods | null>(null);
    const [pickerOpen, setPickerOpen] = useState(false);

    const variables = useMemo<InputModeVariable[]>(() => (
        (inputModeVariables ?? [])
            .filter((variable) => VARIABLE_SOURCES.includes(variable.source))
    ), [inputModeVariables]);
    const variableMetadata = useMemo(() => variables.map((variable) => ({
        reference: getInputModeVariableReference(variable),
        label: variable.label,
        category: getInputModeVariableCategoryLabel(variable.source),
        origin: typeof variable.origin === 'string' ? variable.origin : variable.origin?.name ?? undefined,
        description: variable.description ?? undefined,
    })), [variables]);
    const isReadOnly = Boolean(props.disabled || props.readOnly || props.busy);

    return <>
        <RichTextInputComponent
            {...props}
            ref={inputRef}
            dynamicText
            dynamicTextVariableMetadata={variableMetadata}
            endAction={{
                icon: <DataObject/>,
                tooltip: 'Variable referenzieren',
                onClick: () => setPickerOpen(true),
            }}
        />

        <VariablePickerDialog
            open={pickerOpen}
            variables={variables}
            allowedSources={VARIABLE_SOURCES}
            selectedReference={null}
            allowClear={false}
            readOnly={isReadOnly}
            title="Variable referenzieren"
            onClose={() => setPickerOpen(false)}
            onClear={() => undefined}
            onSelect={(reference) => {
                setPickerOpen(false);

                const insertReference = () => inputRef.current?.insertVariableReference(
                    getInputVariableReference(reference),
                );
                if (typeof requestAnimationFrame === 'function') {
                    requestAnimationFrame(insertReference);
                } else {
                    insertReference();
                }
            }}
        />
    </>;
}
