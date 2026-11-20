import {NoCodeDataType} from '../../../data/no-code-data-type';
import {ElementType} from '../../../data/element-type/element-type';
import {NoCodeParameter, NoCodeParameterOption} from '../../../models/dtos/no-code-operator-details-dto';
import {OptionsSourceType} from '../../../models/elements/form/input/options-source-type';
import {ElementWithParents} from '../../../utils/flatten-elements';

export const NO_CODE_BOOLEAN_OPTIONS = [{
    label: 'Wahr',
    value: 'true',
}, {
    label: 'Falsch',
    value: 'false',
}];

export function getNoCodeParameterSuggestions(parameters: NoCodeParameter[], referenceOptions: NoCodeParameterOption[][]): NoCodeParameterOption[][] {
    return parameters.map((parameter) => {
        // Options defined by the operator, such as the units of a date calculation, are the only valid values.
        if (parameter.options.length > 0) {
            return [];
        }

        // Suggest the options of referenced values only for parameters of a compatible type, so a number of days
        // referenced in a date calculation does not become a suggestion for the date.
        const optionsByValue = new Map<string, NoCodeParameterOption>();
        referenceOptions.forEach((options, operandIndex) => {
            const referencingParameter = parameters[operandIndex];
            if (referencingParameter == null || !areCompatibleParameterTypes(parameter.type, referencingParameter.type)) {
                return;
            }

            for (const option of options) {
                if (!optionsByValue.has(option.value)) {
                    optionsByValue.set(option.value, option);
                }
            }
        });

        return Array.from(optionsByValue.values());
    });
}

export function getReferencedElementOptions(
    element: ElementWithParents['element'],
    codeListOptions: ReadonlyMap<string, NoCodeParameterOption[]>,
): NoCodeParameterOption[] {
    switch (element.type) {
        case ElementType.Checkbox:
            return NO_CODE_BOOLEAN_OPTIONS;
        case ElementType.Select:
            if ((element.optionsSource ?? OptionsSourceType.Manual) === OptionsSourceType.Manual && element.options != null) {
                return element.options.map((option) => (
                    typeof option === 'string' ?
                        {
                            value: option,
                            label: option,
                        } :
                        option
                ));
            }
            return codeListOptions.get(element.id) ?? [];
        case ElementType.MultiCheckbox:
            if ((element.optionsSource ?? OptionsSourceType.Manual) === OptionsSourceType.Manual && element.options != null) {
                return element.options;
            }
            return codeListOptions.get(element.id) ?? [];
        case ElementType.ChipInput:
            if ((element.optionsSource ?? OptionsSourceType.Manual) === OptionsSourceType.Manual && element.suggestions != null) {
                return element.suggestions.map((value) => ({
                    value,
                    label: value,
                }));
            }
            return codeListOptions.get(element.id) ?? [];
        default:
            return [];
    }
}

/**
 * Operators that accept values of any type, such as the comparison operators, use runtime parameters.
 */
function areCompatibleParameterTypes(first: NoCodeDataType, second: NoCodeDataType): boolean {
    return first === second || first === NoCodeDataType.Runtime || second === NoCodeDataType.Runtime;
}
