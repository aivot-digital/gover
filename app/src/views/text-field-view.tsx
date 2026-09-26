import {TextFieldComponent} from '../components/text-field/text-field-component';
import {BaseViewProps} from './base-view';
import {TextFieldElement} from '../models/elements/form/input/text-field-element';
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {TextFieldComponentProps} from '../components/text-field/text-field-component-props';
import Autocomplete from '@mui/material/Autocomplete';
import {isStringNullOrEmpty} from '../utils/string-utils';
import {DynamicTextInputField} from '../components/dynamic-text/dynamic-text-field';

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
        prefix,
        copyable,
        copyValueTemplate,
    } = element;

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const handleBlur = (val: string | null) => {
        if (onBlur != null) {
            onBlur(val, [element.id]);
        }
    };
    const inputModeFieldProps = props.inputModeLiteralContext?.fieldProps;
    const dynamicTextContext = props.inputModeLiteralContext?.dynamicText;

    const textFieldProps: TextFieldComponentProps = useMemo(() => ({
        ...inputModeFieldProps,
        label: inputModeFieldProps?.label ?? label ?? '',
        autocomplete: autocomplete ?? undefined,
        placeholder: placeholder ?? undefined,
        error: inputModeFieldProps?.error ?? (errors != null ? errors.join(' ') : undefined),
        hint: inputModeFieldProps?.hint ?? hint ?? undefined,
        multiline: isMultiline ?? undefined,
        required: inputModeFieldProps?.required ?? required ?? undefined,
        disabled: inputModeFieldProps?.disabled ?? isDisabled,
        busy: inputModeFieldProps?.busy ?? isBusy,
        maxCharacters: maxCharacters ?? undefined,
        minCharacters: minCharacters ?? undefined,
        value: value == null ? value : value.toString(),
        onChange: val => setValue(val),
        onBlur: onBlur != null ? handleBlur : undefined,
        debounce: 1000,
        startIcon: isStringNullOrEmpty(prefix) ? undefined : prefix,
        copyable: copyable ?? false,
        copyValueTemplate: copyValueTemplate ?? undefined,
    }), [label, autocomplete, placeholder, errors, hint, isMultiline, required, isDisabled, isBusy, maxCharacters, minCharacters, value, setValue, onBlur, element.id, prefix, copyable, copyValueTemplate, inputModeFieldProps]);

    if (dynamicTextContext != null) {
        // TODO(input-modes): Add suggestions, prefix/copy adornments, and character counters to the token editor
        // before enabling dynamic text on fields that use those specialized TextField capabilities.
        return <DynamicTextInputField
            ref={dynamicTextContext.inputRef}
            {...inputModeFieldProps}
            label={inputModeFieldProps?.label ?? label ?? ''}
            hint={inputModeFieldProps?.hint ?? hint ?? undefined}
            error={inputModeFieldProps?.error ?? (errors != null ? errors.join(' ') : undefined)}
            required={inputModeFieldProps?.required ?? required ?? undefined}
            disabled={inputModeFieldProps?.disabled ?? isDisabled}
            readOnly={inputModeFieldProps?.readOnly}
            busy={inputModeFieldProps?.busy ?? isBusy}
            multiline={isMultiline ?? undefined}
            placeholder={placeholder ?? undefined}
            value={value == null ? null : value.toString()}
            onChange={setValue}
            onBlur={onBlur != null ? handleBlur : undefined}
            debounce={1000}
            endAction={props.inputModeLiteralContext?.variableInsertAction}
            variableMetadata={dynamicTextContext.variableMetadata}
        />;
    }

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
