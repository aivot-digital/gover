import {validateNoCodeNodeDataPath} from './no-code-node-data-path';
import {AnyElement} from '../../../models/elements/any-element';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {NoCodeOperand, NoCodeOperandError} from '../../../models/functions/no-code-expression';
import {findMatchingNoCodeSignature, NoCodeOperatorDetailsDTO, NoCodeParameter, resolveNoCodeSignature} from '../../../models/dtos/no-code-operator-details-dto';
import {getNoCodeNumberValidationError} from './no-code-number-validation';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {isValidProcessDataPath} from '../../../utils/process-data-path';
import {getFormElementOptions, readsFormElementState} from './no-code-options';

/** Check structure and explicitly declared literal constraints; dynamic values remain the evaluator's responsibility. */
export function validateNoCode(
    operand: NoCodeOperand | null | undefined,
    operators: NoCodeOperatorDetailsDTO[],
    elements: ElementWithParents[],
    required = true,
    parameter?: NoCodeParameter,
): NoCodeOperandError {
    let error: string | null = null;
    let subErrors: NoCodeOperandError[] = [];
    if (operand == null) {
        error = required ? 'Legen Sie einen Ausdruck fest.' : null;
    } else {
        switch (operand.type) {
            case 'NoCodeStaticValue':
                // Unconstrained nodes still represent intentional values, including empty values, false or zero.
                error = getNoCodeNumberValidationError(operand.value, parameter?.label ?? 'Wert', parameter?.numberConstraints) ?? null;
                break;
            case 'NoCodeReference':
                if (!operand.elementId) error = 'Wählen Sie ein Formularelement aus.';
                else if (!elements.some(({element}) => element.id === operand.elementId)) error = 'Das ausgewählte Formularelement ist nicht verfügbar.';
                break;
            case 'NoCodeExpression': {
                const operator = operators.find((candidate) => candidate.identifier === operand.operatorIdentifier);
                if (!operand.operatorIdentifier) error = 'Wählen Sie eine Funktion aus.';
                else if (operator == null) error = 'Die ausgewählte Funktion ist nicht verfügbar.';
                const operands = operand.operands ?? [];
                if (operator != null && findMatchingNoCodeSignature(operator, operands.length) == null) {
                    error = 'Die Anzahl der Parameter passt nicht zur Funktion.';
                }
                const parameters = operator == null ? [] : resolveNoCodeSignature(operator, {operandCount: operands.length}).parameters;
                subErrors = operands.map((child, index) => validateNoCode(child, operators, elements, true, parameters[index]));
                // An unknown element would always count as visible at runtime.
                const target = operands[0];
                const formElements = readsFormElementState(operand.operatorIdentifier) ? getFormElementOptions(elements) : null;
                if (formElements != null && subErrors[0] != null && target?.type === 'NoCodeStaticValue' && typeof target.value === 'string' &&
                    target.value.trim() !== '' && !formElements.some(({value}) => value === target.value)) {
                    subErrors[0] = {
                        ...subErrors[0],
                        error: 'Das ausgewählte Formularelement ist nicht verfügbar.',
                        isValid: false,
                    };
                }
                break;
            }
            case 'NoCodeNodeDataReference':
                error = validateNoCodeNodeDataPath(operand) ?? null;
                break;
            case 'NoCodeProcessDataReference':
            case 'NoCodeInstanceDataReference':
                if (!operand.path || !isValidProcessDataPath(operand.path, operand.type === 'NoCodeProcessDataReference')) {
                    error = operand.type === 'NoCodeProcessDataReference'
                        ? 'Geben Sie einen Datenpfad wie person.name, items[0].name oder items[*].name an.'
                        : 'Geben Sie einen Datenpfad wie person.name oder items[0].name an.';
                }
                break;
            default:
                error = 'Diese Ausdrucksart wird nicht unterstützt.';
        }
    }
    return {
        operand: operand ?? null,
        error,
        subErrors,
        isValid: error == null && subErrors.every((child) => child.isValid),
    };
}

export interface ElementNoCodeFunction {
    section: 'visibility' | 'value' | 'validation';
    operand: NoCodeOperand | null | undefined;
}

/** Include selected-but-empty functions so switching tabs cannot bypass the apply check. */
export function elementNoCodeFunctions(element: AnyElement): ElementNoCodeFunction[] {
    const functions: ElementNoCodeFunction[] = [];
    if (element.visibility?.type === 'NoCode') {
        functions.push({
            section: 'visibility',
            operand: element.visibility.noCode,
        });
    }
    if (isAnyInputElement(element)) {
        if (element.value?.type === 'NoCode') functions.push({
            section: 'value',
            operand: element.value.noCode,
        });
        if (element.validation?.type === 'NoCode') {
            const validations = element.validation.noCodeList ?? [];
            if (validations.length === 0) functions.push({
                section: 'validation',
                operand: null,
            });
            else validations.forEach(({noCode}) => functions.push({
                section: 'validation',
                operand: noCode,
            }));
        }
    }
    return functions;
}
