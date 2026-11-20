import {isNoCodeExpression, NoCodeOperand} from '../../../models/functions/no-code-expression';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {
    NoCodeOperatorDetailsDTO,
    NoCodeParameter,
    NoCodeParameterOption,
    resolveNoCodeSignature,
} from '../../../models/dtos/no-code-operator-details-dto';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {getInputVariableReference, InputVariableSuggestion, InputVariableSource} from '../../../models/input-mode';
import {formatNoCodeValue} from './format-no-code-value';
import {getNoCodeParameterSuggestions, getReferencedElementOptions} from './no-code-options';

export interface NoCodePresentationContext {
    variables?: InputVariableSuggestion[];
    codeListOptions?: ReadonlyMap<string, NoCodeParameterOption[]>;
    processOptions?: ReadonlyMap<string, NoCodeParameterOption[]>;
    type?: NoCodeDataType;
}

export function humanizeNoCode(operand: NoCodeOperand | null, allElements: ElementWithParents[], allOperators: NoCodeOperatorDetailsDTO[], context: NoCodePresentationContext = {}): string {
    function describe(node: NoCodeOperand | null | undefined, parameter?: NoCodeParameter, suggestions: NoCodeParameterOption[] = []): string {
        if (node == null) return parameter == null ? 'Kein Ausdruck definiert' : `Fehlender Parameter „${parameter.label}“`;
        switch (node.type) {
            case 'NoCodeStaticValue':
                return formatNoCodeValue(node.value, parameter?.type ?? context.type, parameter?.options.length ? parameter.options : suggestions);
            case 'NoCodeReference': {
                const element = allElements.find(({element}) => element.id === node.elementId)?.element;
                return element != null ? `Wert von „${generateComponentTitle(element)}“`
                    : node.elementId ? `Unbekanntes Element (ID: ${node.elementId})` : 'Kein Element ausgewählt';
            }
            case 'NoCodeProcessDataReference':
            case 'NoCodeInstanceDataReference':
            case 'NoCodeNodeDataReference': {
                const source = node.type === 'NoCodeProcessDataReference' ? InputVariableSource.ProcessData
                    : node.type === 'NoCodeInstanceDataReference' ? InputVariableSource.ProtectedProcessData : InputVariableSource.ElementData;
                const key = node.type === 'NoCodeNodeDataReference' ? node.nodeDataKey : undefined;
                const variable = context.variables?.find((v) => getInputVariableReference(v) === getInputVariableReference({
                    source,
                    nodeDataKey: key,
                    path: node.path ?? '',
                }));
                if (variable != null) return `„${variable.label}“ (${getInputVariableReference(variable)})`;
                const label = source === InputVariableSource.ProcessData ? 'Vorgangsdaten' : source === InputVariableSource.ProtectedProcessData ? 'Geschützte Vorgangsdaten' : `Elementdaten (${key || 'kein Datenschlüssel'})`;
                return `${label} → ${node.path || 'Kein Pfad angegeben'}`;
            }
            case 'NoCodeExpression': {
                const operator = allOperators.find((candidate) => candidate.identifier === node.operatorIdentifier);
                const operands = node.operands ?? [];
                if (operator == null) return `Unbekannter Operator (${node.operatorIdentifier || 'keine Auswahl'})${operands.length ? `: ${operands.map((child) => describe(child)).join('; ')}` : ''}`;
                const {parameters} = resolveNoCodeSignature(operator, {operandCount: operands.length || undefined});
                const referenceOptions = operands.map((child) => {
                    if (child?.type === 'NoCodeProcessDataReference') return context.processOptions?.get(child.path ?? '') ?? [];
                    const element = child?.type === 'NoCodeReference' ? allElements.find(({element}) => element.id === child.elementId)?.element : null;
                    return element == null ? [] : getReferencedElementOptions(element, context.codeListOptions ?? new Map());
                });
                const suggestions = getNoCodeParameterSuggestions(parameters, referenceOptions);
                const argument = (index: number) => {
                    const text = describe(operands[index], parameters[index] ?? {
                        label: `Parameter ${index + 1}`,
                        type: NoCodeDataType.Runtime,
                        description: null,
                        options: [],
                    }, suggestions[index]);
                    return isNoCodeExpression(operands[index]) ? `(${text})` : text;
                };
                if (operator.humanReadableTemplate != null) {
                    // Replace once, including legacy quotes around a placeholder. Inserted values are never parsed again.
                    const text = operator.humanReadableTemplate.replace(/„#(\d+)“|“#(\d+)”|"#(\d+)"|'#(\d+)'|#(\d+)/g,
                        (_match, ...groups) => argument(Number(groups.slice(0, 5).find((group) => group != null))));
                    const used = new Set(Array.from(operator.humanReadableTemplate.matchAll(/#(\d+)/g), (match) => Number(match[1])));
                    const remaining = Array.from({length: Math.max(parameters.length, operands.length)}, (_, i) => i).filter((i) => !used.has(i));
                    return text + (remaining.length ? ` [Weitere Parameter: ${remaining.map(argument).join('; ')}]` : '');
                }
                const args = parameters.map((param, i) => [param.sentencePhrase ?? param.label, argument(i), param.sentenceSuffix].filter(Boolean).join(' '));
                for (let i = parameters.length; i < operands.length; i++) args.push(argument(i));
                return `${operator.label}${operator.sentenceLead ? ` ${operator.sentenceLead}` : ''}${args.length ? `: ${args.join('; ')}` : ''}`;
            }
            default:
                return 'Unbekannte Ausdrucksart';
        }
    }

    return describe(operand);
}

/** Continue the editor's sentence without repeating the operator that is already visible in its pill. */
export function summarizeNoCodeParameters(operand: NoCodeOperand, allElements: ElementWithParents[], allOperators: NoCodeOperatorDetailsDTO[], context: NoCodePresentationContext = {}): string {
    if (operand.type !== 'NoCodeExpression') return humanizeNoCode(operand, allElements, allOperators, context);
    const operator = allOperators.find(candidate => candidate.identifier === operand.operatorIdentifier);
    const operands = operand.operands ?? [];
    const parameters = operator ? resolveNoCodeSignature(operator, {operandCount: operands.length}).parameters : [];
    const referenceOptions = operands.map(child => {
        if (child?.type === 'NoCodeProcessDataReference') return context.processOptions?.get(child.path ?? '') ?? [];
        const element = child?.type === 'NoCodeReference' ? allElements.find(({element}) => element.id === child.elementId)?.element : undefined;
        return element ? getReferencedElementOptions(element, context.codeListOptions ?? new Map()) : [];
    });
    const suggestions = getNoCodeParameterSuggestions(parameters, referenceOptions);
    return Array.from({length: Math.max(operands.length, parameters.length)}, (_, i) => {
        const parameter = parameters[i];
        const child = operands[i];
        const label = parameter?.label ?? `Parameter ${i + 1}`;
        let description: string;
        if (!child) description = `Fehlender Parameter „${label}“`;
        else if (child.type === 'NoCodeExpression') {
            const childOperator = allOperators.find(candidate => candidate.identifier === child.operatorIdentifier);
            description = `(${[childOperator?.label ?? 'Unbekannter Operator', childOperator?.sentenceLead,
                summarizeNoCodeParameters(child, allElements, allOperators, context)].filter(Boolean).join(' ')})`;
        } else if (child.type === 'NoCodeStaticValue') {
            description = formatNoCodeValue(child.value, parameter?.type, parameter?.options.length ? parameter.options : suggestions[i]);
        } else description = humanizeNoCode(child, allElements, allOperators, context);
        return [parameter?.sentencePhrase ?? `${label}:`, description, parameter?.sentenceSuffix].filter(Boolean).join(' ');
    }).join(' ');
}
