import {TextFieldComponent} from '../text-field/text-field-component';
import {TextFieldComponentProps} from '../text-field/text-field-component-props';
import {normalizeEmailAddress, validateEmailAddress} from '../../utils/email-address-utils';

export interface EmailFieldComponentProps extends Omit<TextFieldComponentProps, 'multiline' | 'type' | 'validate' | 'normalizeInput'> {
    allowLocalDomains?: boolean;
}

export function EmailFieldComponent({allowLocalDomains = false, onChange, onBlur, ...props}: EmailFieldComponentProps) {
    const htmlInputProps = props.muiPassTroughProps?.slotProps?.htmlInput;
    return <TextFieldComponent
        {...props}
        autocomplete={props.autocomplete ?? 'email'}
        // Native type=email rejects some IDN addresses. Our shared validation defines the supported syntax.
        type="text"
        muiPassTroughProps={{...props.muiPassTroughProps, slotProps: {
            ...props.muiPassTroughProps?.slotProps,
            htmlInput: ownerState => ({
                ...(typeof htmlInputProps === 'function' ? htmlInputProps(ownerState) : htmlInputProps),
                inputMode: 'email', autoCapitalize: 'none', spellCheck: false,
            }),
        }}}
        validate={value => validateEmailAddress(value, allowLocalDomains)}
        onChange={value => { if (!props.disabled && !props.readonly && !props.busy) onChange(value); }}
        onBlur={value => {
            if (props.disabled || props.readonly || props.busy) return;
            const normalized = value == null ? null : normalizeEmailAddress(value, allowLocalDomains) ?? value.trim();
            if (normalized !== value) onChange(normalized);
            onBlur?.(normalized);
        }}
    />;
}
