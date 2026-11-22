import {NoCodeParameter} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeOperand} from '../../../models/functions/no-code-expression';
import {getNoCodeNumberValidationError} from './no-code-number-validation';

/** A relationship supplied at runtime cannot describe a known sentence while authoring. */
export function resolveNoCodeExpressionPresentation(
    parameters: readonly NoCodeParameter[],
    operands: readonly (NoCodeOperand | null | undefined)[],
): {parameters: NoCodeParameter[]; useSentence: boolean} {
    const presentedParameters = parameters.map(parameter => resolveNoCodeParameterPresentation(parameter, operands));
    const useSentence = presentedParameters.every((parameter, index) => {
        if (parameter.optionsAsSentencePart !== true) return true;
        const operand = operands[index];
        return operand?.type === 'NoCodeStaticValue' && operand.value != null && operand.value !== ''
            && parameter.options.some(option => option.value === String(operand.value));
    });

    return {
        parameters: useSentence ? presentedParameters : presentedParameters.map(parameter => ({
            ...parameter,
            sentencePhrase: null,
            sentenceSuffix: null,
        })),
        useSentence,
    };
}

/** Only a known, authored quantity may change a unit's label; references and functions stay symbolic. */
export function resolveNoCodeParameterPresentation(
    parameter: NoCodeParameter,
    operands: readonly (NoCodeOperand | null | undefined)[],
): NoCodeParameter {
    if (parameter.quantityParameterIndex == null) return parameter;
    const quantity = operands[parameter.quantityParameterIndex];
    if (quantity?.type !== 'NoCodeStaticValue'
        || getNoCodeNumberValidationError(quantity.value, '', {integerOnly: true}) != null
        || Math.abs(Number(quantity.value)) !== 1) return parameter;

    return {
        ...parameter,
        options: parameter.options.map(option => ({
            ...option,
            label: option.singularLabel ?? option.label,
        })),
    };
}
