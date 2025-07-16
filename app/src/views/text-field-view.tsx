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
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
        onBlur,
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
        if (onBlur != null) {
            onBlur(val, [element.id]);
        }
    };

    return (
        <TextFieldComponent
            label={element.label ?? ''}
            autocomplete={element.autocomplete ?? undefined}
            placeholder={element.placeholder ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            hint={element.hint ?? undefined}
            multiline={element.isMultiline ?? undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            busy={isBusy}
            maxCharacters={element.maxCharacters ?? undefined}
            minCharacters={element.minCharacters ?? undefined}
            value={value?.toString() ?? undefined}
            onChange={val => setValue(val)}
            onBlur={onBlur != null ? handleBlur : undefined}
            debounce={1000}
        />
    );
}
