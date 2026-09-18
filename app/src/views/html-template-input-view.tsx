import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {
    HtmlTemplateInputElement,
    HtmlTemplateInputValue,
} from '../models/elements/form/input/html-template-input-element';
import {HtmlTemplateInputComponent} from '../components/html-template-input/html-template-input-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function HtmlTemplateInputView(
    props: BaseViewProps<HtmlTemplateInputElement, HtmlTemplateInputValue>
) {
    const {
        element,
        setValue,
        value,
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
        <HtmlTemplateInputComponent
            label={element.label ?? ''}
            hint={element.hint}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required}
            disabled={Boolean(isDisabled) || isBusy}
            value={value}
            onChange={setValue}
        />
    );
}
