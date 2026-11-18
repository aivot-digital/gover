import {BaseViewProps} from './base-view';
import {DurationFieldElement} from '../models/elements/form/input/duration-field-element';
import {DurationFieldComponent} from '../components/duration-field/duration-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function DurationFieldView(props: BaseViewProps<DurationFieldElement, number>) {
    const {element, value, setValue, onBlur, errors} = props;
    const fieldProps = props.inputModeLiteralContext?.fieldProps;
    return (
        <DurationFieldComponent
            {...fieldProps}
            label={fieldProps?.label ?? element.label ?? ''}
            value={value}
            onChange={setValue}
            onBlur={onBlur}
            precision={element.precision}
            largestUnit={element.largestUnit}
            minDuration={element.minDuration}
            maxDuration={element.maxDuration}
            hint={fieldProps?.hint ?? element.hint ?? undefined}
            error={fieldProps?.error ?? errors?.join(' ')}
            required={fieldProps?.required ?? element.required ?? undefined}
            disabled={fieldProps?.disabled ?? (element.disabled || props.isBusy)}
            readOnly={fieldProps?.readOnly}
            busy={fieldProps?.busy ?? (props.isDeriving && hasDerivableAspects(element))}
        />
    );
}
