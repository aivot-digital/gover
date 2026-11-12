import {useRef} from 'react';
import {
    DynamicTextInputField,
    type DynamicTextFieldMethods,
    type DynamicTextInputFieldProps,
} from './dynamic-text-field';
import {useTemplateVariablePicker} from './use-template-variable-picker';

export type DynamicTextInputWithVariablePickerProps = Omit<DynamicTextInputFieldProps, 'endAction' | 'variableMetadata'>;

/** A text field for templates that only show text, such as payment texts, with the variable picker. */
export function DynamicTextInputWithVariablePicker(props: DynamicTextInputWithVariablePickerProps) {
    const inputRef = useRef<DynamicTextFieldMethods | null>(null);
    const picker = useTemplateVariablePicker(inputRef, {
        readOnly: Boolean(props.disabled || props.readOnly || props.busy),
    });

    return <>
        <DynamicTextInputField
            {...props}
            ref={inputRef}
            endAction={picker.endAction}
            variableMetadata={picker.variableMetadata}
        />

        {picker.pickerDialog}
    </>;
}
