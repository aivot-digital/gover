import {BaseViewProps} from "./base-view";
import {RichTextInputElement} from "../models/elements/form/input/rich-text-input-element";
import {RichTextInputComponent} from "../components/rich-text-input-component/rich-text-input-component";
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function RichTextView(props: BaseViewProps<RichTextInputElement, string>) {
    const {
        element,
        value,
        setValue,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <RichTextInputComponent
            label={element.label ?? ''}
            hint={element.hint}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required}
            disabled={isDisabled}
            readOnly={isBusy}
            reducedMode={element.reducedMode ?? false}
            value={value}
            onChange={setValue}
        />
    );
}
