import {RadioFieldComponent} from '../components/radio-field/radio-field-component';
import React, {useEffect, useMemo} from 'react';
import {type BaseViewProps} from './base-view';
import {type SelectFieldElement, type SelectFieldElementOption} from '../models/elements/form/input/select-field-element';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {resolveElementState, resolveValue} from '../utils/element-data-utils';
import {ElementType} from '../data/element-type/element-type';
import {isStringNullOrEmpty} from '../utils/string-utils';
import {
    resolveViewDispatcherValueScope,
    useViewDispatcherContext,
} from '../components/view-dispatcher/view-dispatcher.context';
import {getDisabledFieldBackground} from '../theming/field-state-colors';
import {SelectFieldPresentation} from '../models/elements/form/input/select-field-presentation';

export function SelectFieldView(props: BaseViewProps<SelectFieldElement, string>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const viewDispatcherContext = useViewDispatcherContext();
    const {
        allElements,
    } = viewDispatcherContext;

    const {
        options: baseOptions,
        disabled,
        dependsOnSelectFieldId,
    } = element;

    const allOptions = useMemo(() => {
        if (baseOptions == null) {
            return [];
        }

        return baseOptions
            .map((option: string | SelectFieldElementOption) => {
                if (typeof option === 'string') {
                    return {
                        value: option,
                        label: option,
                    };
                }

                return option;
            });
    }, [baseOptions]);

    const dependencyElement = useMemo(() => {
        if (isStringNullOrEmpty(dependsOnSelectFieldId)) {
            return undefined;
        }

        return allElements.find((candidate) => (
            candidate.id === dependsOnSelectFieldId &&
            candidate.type === ElementType.Select
        ));
    }, [allElements, dependsOnSelectFieldId]);

    const dependencyValue = useMemo(() => {
        if (dependencyElement == null) {
            return undefined;
        }

        // As in the backend derivation, the referenced select counts only in the row, surrounding row or form level
        // that contains it, and only while it is visible.
        const scope = resolveViewDispatcherValueScope(viewDispatcherContext, dependencyElement.id);
        if (scope == null || resolveElementState(dependencyElement, scope.derivedData)?.visible === false) {
            return undefined;
        }

        const referencedValue = resolveValue(dependencyElement, scope.authoredElementValues, scope.derivedData);
        return referencedValue != null ? `${referencedValue}` : undefined;
    }, [dependencyElement, viewDispatcherContext]);

    const options = useMemo(() => {
        if (dependencyElement == null || isStringNullOrEmpty(dependsOnSelectFieldId)) {
            return allOptions;
        }

        return allOptions.filter((option) => {
            if (isStringNullOrEmpty(option.group)) {
                return true;
            }

            return option.group === dependencyValue;
        });
    }, [allOptions, dependencyElement, dependencyValue, dependsOnSelectFieldId]);

    useEffect(() => {
        if (value == null || dependencyElement == null || isStringNullOrEmpty(dependsOnSelectFieldId)) {
            return;
        }

        const matchingOption = allOptions.find((option) => option.value === value);
        if (matchingOption == null) {
            return;
        }

        const isStillVisible = options.some((option) => option.value === value);
        if (isStillVisible) {
            return;
        }

        setValue(null, [dependencyElement.id]);
    }, [allOptions, dependencyElement, dependsOnSelectFieldId, options, setValue, value]);

    const isDisabled = useMemo(() => {
        return disabled || isGloballyDisabled;
    }, [disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const emptyStatePlaceholder = useMemo(() => {
        if (dependencyElement == null || isStringNullOrEmpty(dependsOnSelectFieldId)) {
            return undefined;
        }

        if (options.length > 0) {
            return undefined;
        }

        if (isStringNullOrEmpty(dependencyValue)) {
            return 'Bitte wählen Sie zuerst im übergeordneten Auswahlfeld eine Option aus';
        }

        return 'Für die aktuelle Auswahl sind keine Optionen verfügbar';
    }, [dependencyElement, dependencyValue, dependsOnSelectFieldId, options.length]);
    const inputModeFieldProps = props.inputModeLiteralContext?.fieldProps;

    if (element.presentation === SelectFieldPresentation.Radio) {
        return <RadioFieldComponent
            {...inputModeFieldProps}
            label={inputModeFieldProps?.label ?? element.label ?? ''}
            value={value}
            onChange={setValue}
            options={options}
            required={inputModeFieldProps?.required ?? element.required}
            disabled={inputModeFieldProps?.disabled ?? isDisabled}
            busy={inputModeFieldProps?.busy ?? isBusy}
            readOnly={inputModeFieldProps?.readOnly}
            error={inputModeFieldProps?.error ?? (errors != null ? errors.join(' ') : undefined)}
            hint={inputModeFieldProps?.hint ?? element.hint ?? emptyStatePlaceholder}
            displayInline={element.displayInline}
            toggleButtons={element.toggleButtons}
        />;
    }

    return (
        <SelectFieldComponent
            {...inputModeFieldProps}
            label={inputModeFieldProps?.label ?? element.label ?? ''}
            autocomplete={element.autocomplete ?? undefined}
            error={inputModeFieldProps?.error ?? (errors != null ? errors.join(' ') : undefined)}
            hint={inputModeFieldProps?.hint ?? element.hint ?? undefined}
            placeholder={element.placeholder ?? undefined}
            value={value}
            onChange={setValue}
            disabled={inputModeFieldProps?.disabled ?? isDisabled}
            required={inputModeFieldProps?.required ?? element.required ?? undefined}
            options={options}
            presentation={element.presentation ?? SelectFieldPresentation.Dropdown}
            emptyStatePlaceholder={emptyStatePlaceholder}
            controlSx={{
                backgroundColor: isBusy ? getDisabledFieldBackground : undefined,
                cursor: isBusy ? 'not-allowed' : undefined,
            }}
            readOnly={inputModeFieldProps?.readOnly ?? isBusy}
        />
    );
}
