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
> & {
    /** Set when the backend renders the template as Markdown, so `{! !}` keeps a value's formatting. */
    offerFormattedInsertion?: boolean;
};

export function RichTextInputWithVariablePicker(props: RichTextInputWithVariablePickerProps) {
    const {offerFormattedInsertion, ...inputProps} = props;
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
    const isReadOnly = Boolean(inputProps.disabled || inputProps.readOnly || inputProps.busy);

    return <>
        <RichTextInputComponent
            {...inputProps}
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
            offerFormattedInsertion={offerFormattedInsertion}
            readOnly={isReadOnly}
            title="Variable referenzieren"
            onClose={() => setPickerOpen(false)}
            onClear={() => undefined}
            onSelect={(reference, _suggestion, insertion) => {
                setPickerOpen(false);

                const insertReference = () => inputRef.current?.insertVariableReference(
                    getInputVariableReference(reference),
                    insertion,
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
