import {BaseViewProps} from './base-view';
import {PhoneNumberFieldElement} from '../models/elements/form/input/phone-number-field-element';
import {PhoneNumberFieldComponent} from '../components/phone-number-field/phone-number-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function PhoneNumberFieldView(props: BaseViewProps<PhoneNumberFieldElement, string>) {
    const {element, value, setValue, onBlur, errors} = props;
    const fieldProps = props.inputModeLiteralContext?.fieldProps;
    return (
        <PhoneNumberFieldComponent
            {...fieldProps}
            label={fieldProps?.label ?? element.label ?? ''}
            value={value}
            onChange={setValue}
            onBlur={onBlur == null ? undefined : value => onBlur(value, [element.id])}
            placeholder={element.placeholder ?? undefined}
            hint={fieldProps?.hint ?? element.hint ?? undefined}
            error={fieldProps?.error ?? errors?.join(' ')}
            required={fieldProps?.required ?? element.required ?? undefined}
            disabled={fieldProps?.disabled ?? (element.disabled || props.isBusy)}
            busy={fieldProps?.busy ?? (props.isDeriving && hasDerivableAspects(element))}
            defaultCountry={element.defaultCountry ?? 'DE'}
            preferredCountries={element.preferredCountries ?? undefined}
            readonly={fieldProps?.readOnly}
            muiPassTroughProps={{autoComplete: element.autocomplete ?? 'tel'}}
        />
    );
}
