import {BaseViewProps} from './base-view';
import {
    NoCodeInputFieldElement,
    NoCodeInputFieldElementItem,
    NoCodeInputFieldReturnType
} from '../models/elements/form/input/no-code-input-field-element';
import {NoCodeDataType} from '../data/no-code-data-type';
import {NoCodeInputFieldComponent} from '../components/no-code-input-field/no-code-input-field-component';
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

function mapReturnTypeToNoCodeDataType(returnType: NoCodeInputFieldReturnType | null | undefined): NoCodeDataType {
    switch (returnType) {
        case NoCodeInputFieldReturnType.RUNTIME:
            return NoCodeDataType.Runtime;
        case NoCodeInputFieldReturnType.BOOLEAN:
            return NoCodeDataType.Boolean;
        case NoCodeInputFieldReturnType.NUMBER:
            return NoCodeDataType.Number;
        case NoCodeInputFieldReturnType.DATE:
            return NoCodeDataType.Date;
        case NoCodeInputFieldReturnType.STRING:
            return NoCodeDataType.String;
        case NoCodeInputFieldReturnType.DATETIME:
            return NoCodeDataType.Date;
        default:
            return NoCodeDataType.Runtime;
    }
}

export function NoCodeInputFieldView(props: BaseViewProps<NoCodeInputFieldElement, NoCodeInputFieldElementItem>) {
    const {
        rootElement,
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

    const desiredReturnType = useMemo(() => {
        return mapReturnTypeToNoCodeDataType(element.returnType ?? NoCodeInputFieldReturnType.BOOLEAN);
    }, [element.returnType]);

    return (
        <NoCodeInputFieldComponent
            rootElement={rootElement}
            label={element.label ?? ''}
            hint={element.hint}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required}
            disabled={Boolean(isDisabled) || isBusy}
            value={value ?? undefined}
            desiredReturnType={desiredReturnType}
            onChange={setValue}
        />
    );
}
