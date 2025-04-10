import {TextFieldComponent} from '../components/text-field/text-field-component';
import {BaseViewProps} from './base-view';
import {TextFieldElement} from '../models/elements/form/input/text-field-element';
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function TextFieldView(props: BaseViewProps<TextFieldElement, string>) {
    const {
        element,
        setValue,
        value,
        error,
        isBusy: isGloballyDisabled,
        isDeriving,
        valueOverride,
    } = props;

    const {
        disabled
    } = element;

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const handleBlur = (val: string | null | undefined) => {
        if (valueOverride?.onBlur) {
            valueOverride.onBlur(element.id, val);
        }
    };

    return (
        <TextFieldComponent
            label={element.label ?? ''}
            autocomplete={element.autocomplete}
            placeholder={element.placeholder}
            error={error}
            hint={element.hint}
            multiline={element.isMultiline}
            required={element.required}
            disabled={isDisabled}
            busy={isBusy}
            maxCharacters={element.maxCharacters}
            minCharacters={element.minCharacters}
            value={value?.toString() ?? undefined}
            onChange={val => setValue(val)}
            onBlur={valueOverride?.onBlur ? handleBlur : undefined}
            debounce={1000}
        />
    );
}
