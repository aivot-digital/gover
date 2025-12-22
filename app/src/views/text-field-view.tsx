import {TextFieldComponent} from '../components/text-field/text-field-component';
import {BaseViewProps} from './base-view';
import {TextFieldElement} from '../models/elements/form/input/text-field-element';
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {TextFieldComponentProps} from '../components/text-field/text-field-component-props';
import Autocomplete from '@mui/material/Autocomplete';
import {CodeEditor} from "../components/code-editor/code-editor";
import {RichTextEditorComponentView} from "../components/richt-text-editor/rich-text-editor.component.view";

export function TextFieldView(props: BaseViewProps<TextFieldElement, string>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
        onBlur,
    } = props;

    const {
        label,
        autocomplete,
        placeholder,
        hint,
        isMultiline,
        required,
        disabled,
        maxCharacters,
        minCharacters,
        suggestions,
    } = element;

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const handleBlur = (val: string | null | undefined) => {
        if (onBlur != null) {
            onBlur(val, [element.id]);
        }
    };

    const textFieldProps: TextFieldComponentProps = useMemo(() => ({
        label: label ?? '',
        autocomplete: autocomplete ?? undefined,
        placeholder: placeholder ?? undefined,
        error: errors != null ? errors.join(' ') : undefined,
        hint: hint ?? undefined,
        multiline: isMultiline ?? undefined,
        required: required ?? undefined,
        disabled: isDisabled,
        busy: isBusy,
        maxCharacters: maxCharacters ?? undefined,
        minCharacters: minCharacters ?? undefined,
        value: value?.toString() ?? undefined,
        onChange: val => setValue(val),
        onBlur: onBlur != null ? handleBlur : undefined,
        debounce: 1000,
    }), [label, autocomplete, placeholder, errors, hint, isMultiline, required, isDisabled, isBusy, maxCharacters, minCharacters, value, setValue, onBlur]);

    if (suggestions != null) {
        return (
            <Autocomplete
                freeSolo={true}
                disablePortal={false}
                options={suggestions.map(s => ({
                    label: s,
                }))}
                renderInput={(params) => (
                    <TextFieldComponent
                        {...textFieldProps}
                        muiPassTroughProps={params}
                    />
                )}
            />
        );
    }

    return (
        <TextFieldComponent
            {...textFieldProps}
        />
    );
}
