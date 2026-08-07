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
import {useViewDispatcherContext} from '../components/view-dispatcher/view-dispatcher.context';

const noCodeReturnTypeMap: Record<NoCodeInputFieldReturnType, NoCodeDataType> = {
    [NoCodeInputFieldReturnType.RUNTIME]: NoCodeDataType.Runtime,
    [NoCodeInputFieldReturnType.BOOLEAN]: NoCodeDataType.Boolean,
    [NoCodeInputFieldReturnType.NUMBER]: NoCodeDataType.Number,
    [NoCodeInputFieldReturnType.DATE]: NoCodeDataType.Date,
    [NoCodeInputFieldReturnType.STRING]: NoCodeDataType.String,
    [NoCodeInputFieldReturnType.DATETIME]: NoCodeDataType.DateTime,
    [NoCodeInputFieldReturnType.TIME]: NoCodeDataType.Time,
};

function mapReturnTypeToNoCodeDataType(returnType: NoCodeInputFieldReturnType | null | undefined): NoCodeDataType {
    return returnType == null
        ? NoCodeDataType.Runtime
        : noCodeReturnTypeMap[returnType] ?? NoCodeDataType.Runtime;
}

export function NoCodeInputFieldView(props: BaseViewProps<NoCodeInputFieldElement, NoCodeInputFieldElementItem>) {
    const {
        element,
        value,
        setValue,
        errors,
        errorDetails,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const {
        rootElement,
    } = useViewDispatcherContext();

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
            errorDetails={errorDetails}
            required={element.required}
            disabled={Boolean(isDisabled) || isBusy}
            value={value}
            desiredReturnType={desiredReturnType}
            onChange={setValue}
        />
    );
}
