import {useRef} from 'react';
import {useTemplateVariablePicker} from '../dynamic-text/use-template-variable-picker';
import {
    RichTextInputComponent,
    type RichTextInputComponentMethods,
    type RichTextInputComponentProps,
} from './rich-text-input-component';

export type RichTextInputWithVariablePickerProps = Omit<
    RichTextInputComponentProps,
    'dynamicText' | 'dynamicTextVariableMetadata' | 'endAction'
> & {
    /** Set when the backend renders the template as Markdown, so `{! !}` keeps a value's formatting. */
    offerFormattedInsertion?: boolean;
};

export function RichTextInputWithVariablePicker(props: RichTextInputWithVariablePickerProps) {
    const {offerFormattedInsertion, ...inputProps} = props;
    const inputRef = useRef<RichTextInputComponentMethods | null>(null);
    const picker = useTemplateVariablePicker(inputRef, {
        offerFormattedInsertion,
        readOnly: Boolean(inputProps.disabled || inputProps.readOnly || inputProps.busy),
    });

    return <>
        <RichTextInputComponent
            {...inputProps}
            ref={inputRef}
            dynamicText
            dynamicTextVariableMetadata={picker.variableMetadata}
            endAction={picker.endAction}
        />

        {picker.pickerDialog}
    </>;
}
