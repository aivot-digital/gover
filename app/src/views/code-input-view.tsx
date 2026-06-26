import {BaseViewProps} from './base-view';
import {CodeInputElement, CodeInputFieldLanguage} from '../models/elements/form/input/code-input-element';
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {CodeInputFieldComponent} from '../components/code-input-field/code-input-field-component';

export function CodeInputView(props: BaseViewProps<CodeInputElement, string>) {
    const {
        element,
        value,
        setValue,
        onBlur,
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

    const editorHeight = useMemo(() => {
        if (element.editorHeight == null) {
            return undefined;
        }

        return `${Math.min(1200, Math.max(200, Math.round(element.editorHeight)))}px`;
    }, [element.editorHeight]);

    return (
        <CodeInputFieldComponent
            label={element.label ?? ''}
            hint={element.hint}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required}
            disabled={isDisabled}
            readOnly={isBusy}
            value={value}
            onChange={setValue}
            onBlur={onBlur != null ? (nextValue) => onBlur(nextValue, [element.id]) : undefined}
            language={element.language ?? CodeInputFieldLanguage.Javascript}
            height={editorHeight}
            wordWrap={element.wordWrap ?? false}
        />
    );
}
