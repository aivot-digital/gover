import {BaseViewProps} from "./base-view";
import {RichTextInputElement} from "../models/elements/form/input/rich-text-input-element";
import {RichTextInputComponent} from "../components/rich-text-input-component/rich-text-input-component";
import {useMemo} from 'react';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {ElementType} from '../data/element-type/element-type';
import {useViewDispatcherContext} from '../components/view-dispatcher/view-dispatcher.context';

export function RichTextView(props: BaseViewProps<RichTextInputElement, string>) {
    const {
        element,
        value,
        setValue,
        errors,
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

    const isProcessConfigRoot = useMemo(() => {
        return (rootElement as { type: ElementType }).type === ElementType.ConfigLayout;
    }, [rootElement]);
    const inputModeFieldProps = props.inputModeLiteralContext?.fieldProps;
    const dynamicTextContext = props.inputModeLiteralContext?.dynamicText;

    return (
        <RichTextInputComponent
            ref={dynamicTextContext?.inputRef}
            {...inputModeFieldProps}
            label={inputModeFieldProps?.label ?? element.label ?? ''}
            hint={inputModeFieldProps?.hint ?? element.hint}
            error={inputModeFieldProps?.error ?? (errors != null ? errors.join(' ') : undefined)}
            required={inputModeFieldProps?.required ?? element.required}
            disabled={inputModeFieldProps?.disabled ?? isDisabled}
            readOnly={inputModeFieldProps?.readOnly ?? isBusy}
            reducedMode={isProcessConfigRoot ? true : element.reducedMode}
            value={value}
            onChange={setValue}
            endAction={props.inputModeLiteralContext?.variableInsertAction}
            dynamicText={dynamicTextContext != null}
            dynamicTextVariableMetadata={dynamicTextContext?.variableMetadata}
        />
    );
}
