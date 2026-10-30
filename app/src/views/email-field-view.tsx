import {BaseViewProps} from './base-view';
import {EmailFieldElement} from '../models/elements/form/input/email-field-element';
import {EmailFieldComponent} from '../components/email-field/email-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function EmailFieldView(props: BaseViewProps<EmailFieldElement, string>) {
    const {element, value, setValue, onBlur, errors} = props;
    const fieldProps = props.inputModeLiteralContext?.fieldProps;
    return (
        <EmailFieldComponent
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
            readonly={fieldProps?.readOnly}
            allowLocalDomains={element.allowLocalDomains ?? false}
            autocomplete={element.autocomplete ?? 'email'}
        />
    );
}
