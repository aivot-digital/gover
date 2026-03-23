import React, {useEffect, useMemo} from 'react';
import {type BaseViewProps} from './base-view';
import {type SelectFieldElement, type SelectFieldElementOption} from '../models/elements/form/input/select-field-element';
import {SelectFieldComponent} from '../components/select-field/select-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {resolveValue} from '../utils/element-data-utils';
import {ElementType} from '../data/element-type/element-type';
import {isStringNullOrEmpty} from '../utils/string-utils';


export function SelectFieldView(props: BaseViewProps<SelectFieldElement, string>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
        allElements,
        authoredElementValues,
        derivedData,
        rootAuthoredElementValues,
        rootDerivedData,
    } = props;

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

        const localValue = resolveValue(dependencyElement, authoredElementValues, derivedData);
        if (localValue != null) {
            return `${localValue}`;
        }

        const rootValue = resolveValue(dependencyElement, rootAuthoredElementValues, rootDerivedData);
        if (rootValue != null) {
            return `${rootValue}`;
        }

        return undefined;
    }, [authoredElementValues, dependencyElement, derivedData, rootAuthoredElementValues, rootDerivedData]);

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

        setValue(undefined, [dependencyElement.id]);
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

    return (
        <SelectFieldComponent
            label={element.label ?? ''}
            autocomplete={element.autocomplete ?? undefined}
            error={errors != null ? errors.join(' ') : undefined}
            hint={element.hint ?? undefined}
            placeholder={element.placeholder ?? undefined}
            value={value ?? undefined}
            onChange={setValue}
            disabled={isDisabled}
            required={element.required ?? undefined}
            options={options}
            emptyStatePlaceholder={emptyStatePlaceholder}

            sx={{
                backgroundColor: isBusy ? '#F8F8F8' : undefined,
                cursor: isBusy ? 'not-allowed' : undefined,
            }}
            readOnly={isBusy}
        />
    );
}
