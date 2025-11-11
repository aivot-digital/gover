import {isNoCodeExpression, isNoCodeReference, isNoCodeStaticValue, NoCodeOperand} from '../../../models/functions/no-code-expression';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {NoCodeOperatorDetailsDTO} from '../../../models/dtos/no-code-operator-details-dto';
import {generateComponentTitle} from '../../../utils/generate-component-title';

export function humanizeNoCode(operand: NoCodeOperand, allElements: ElementWithParents[], allOperators: NoCodeOperatorDetailsDTO[]): string {
    if (isNoCodeStaticValue(operand)) {
        return `„${operand.value}”`;
    }

    if (isNoCodeReference(operand)) {
        const element = allElements
            .find(el => el.element.id === operand.elementId);

        const elementTitle = element != null
            ? generateComponentTitle(element.element)
            : `Unbekanntes Element (ID: ${operand.elementId})`;

        return `Wert von „${elementTitle}”`;
    }

    if (isNoCodeExpression(operand)) {
        const operator = allOperators
            .find(op => op.identifier === operand.operatorIdentifier);

        if (operator == null) {
            return `Der Ausdruck mit dem unbekannten No-Code Operand (ID: ${operand.operatorIdentifier})`;
        }

        const parameterHumanized: (string | null)[] = (operand.operands ?? [])
            .map(op => op != null ? humanizeNoCode(op, allElements, allOperators) : null);

        if (operator.humanReadableTemplate != null) {
            let template = operator.humanReadableTemplate;

            operator.signatures[0].parameters.forEach((param, index) => {
                const placeholder = `#${index}`;
                const insert = parameterHumanized[index] ?? `„${param.label}”`;
                template = template.replaceAll(placeholder, insert);
            });

            return template;
        } else {
            return `Das Ergebnis der Operation „${operator.label}” mit den Parametern: ${parameterHumanized.join(', ')}`;
        }
    }

    return 'Unbekannter No-Code Operand';
}