import {NoCodeDataType} from '../../../data/no-code-data-type';
import {
    NoCodeOperatorDetailsDTO,
    NoCodeParameter,
    NoCodeParameterOption,
    resolveNoCodeSignature,
} from '../../../models/dtos/no-code-operator-details-dto';
import {NoCodeExpression, NoCodeOperand} from '../../../models/functions/no-code-expression';
import {getInputVariableReference, InputVariableSource} from '../../../models/input-mode';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {formatNoCodeValue} from './format-no-code-value';
import {humanizeNoCode, NoCodePresentationContext} from './humanize-no-code';
import {getNoCodeParameterSuggestions, getReferencedElementOptions} from './no-code-options';
import {getNoCodeReadableTemplate, resolveNoCodeExpressionPresentation} from './no-code-parameter-presentation';

export interface NoCodeSummaryDetail {
    label: string;
    value: string;
}

export interface NoCodeSummary {
    text: string;
    details: NoCodeSummaryDetail[];
}

/** Describe the authored expression without evaluating it or changing its grouping. */
export function summarizeNoCode(
    operand: NoCodeOperand | null,
    allElements: ElementWithParents[],
    allOperators: NoCodeOperatorDetailsDTO[],
    context: NoCodePresentationContext = {},
): NoCodeSummary {
    const details = new Map<string, NoCodeSummaryDetail>();
    const referenceIdentities = new Map<string, Set<string>>();
    const selectionValues = new Map<string, Set<string>>();

    function register(map: Map<string, Set<string>>, label: string, identity: string) {
        const identities = map.get(label) ?? new Set<string>();
        identities.add(identity);
        map.set(label, identities);
    }

    allElements.forEach(({element}) => register(referenceIdentities, generateComponentTitle(element), `field:${element.id}`));
    context.variables?.forEach(variable => register(referenceIdentities, variable.label, `variable:${getInputVariableReference(variable)}`));

    function addDetail(label: string, value: string) {
        details.set(JSON.stringify([label, value]), {label, value});
    }

    const preparedExpressions = new Map<NoCodeExpression, {
        operator: NoCodeOperatorDetailsDTO | undefined;
        parameters: NoCodeParameter[];
        options: NoCodeParameterOption[][];
        useSentence: boolean;
    }>();

    function prepare(node: NoCodeExpression) {
        const cached = preparedExpressions.get(node);
        if (cached != null) return cached;

        const operands = node.operands ?? [];
        const operator = allOperators.find(candidate => candidate.identifier === node.operatorIdentifier);
        const signature = operator == null ? undefined : resolveNoCodeSignature(operator, {operandCount: operands.length});
        const {parameters, useSentence} = resolveNoCodeExpressionPresentation(
            Array.from({length: Math.max(signature?.parameters.length ?? 0, operands.length)}, (_, i) =>
                signature?.parameters[i] ?? {
                    label: `Parameter ${i + 1}`,
                    type: NoCodeDataType.Runtime,
                    description: null,
                    options: [],
                }), operands);
        const referenceOptions = operands.map(child => {
            if (child?.type === 'NoCodeProcessDataReference') return context.processOptions?.get(child.path ?? '') ?? [];
            const element = child?.type === 'NoCodeReference' ? allElements.find(({element}) => element.id === child.elementId)?.element : undefined;
            return element == null ? [] : getReferencedElementOptions(element, context.codeListOptions ?? new Map());
        });
        const suggestions = getNoCodeParameterSuggestions(parameters, referenceOptions);
        const result = {
            operator,
            parameters,
            useSentence,
            options: parameters.map((parameter, i) => parameter.options.length ? parameter.options : suggestions[i]),
        };
        preparedExpressions.set(node, result);
        return result;
    }

    // Collect labels before rendering so equal labels with different stored values remain distinguishable.
    function collectSelections(node: NoCodeOperand | null | undefined) {
        if (node?.type !== 'NoCodeExpression') return;
        const {options} = prepare(node);
        node.operands?.forEach((child, i) => {
            if (child?.type === 'NoCodeStaticValue' && child.value != null && child.value !== '') {
                const selected = options[i]?.find(option => option.value === String(child.value));
                if (selected != null) {
                    options[i].filter(option => option.label === selected.label)
                        .forEach(option => register(selectionValues, option.label, option.value));
                }
            }
            collectSelections(child);
        });
    }

    function describe(node: NoCodeOperand | null | undefined, parameter?: NoCodeParameter, options: NoCodeParameterOption[] = []): string {
        if (node == null) return parameter == null ? 'Kein Ausdruck definiert' : `Fehlender Parameter „${parameter.label}“`;
        switch (node.type) {
            case 'NoCodeStaticValue': {
                const selected = node.value == null || node.value === '' ? undefined : options.find(option => option.value === String(node.value));
                if (selected == null) return formatNoCodeValue(node.value, parameter?.type ?? context.type);
                const value = String(node.value);
                addDetail(selected.label, value);
                const type = parameter?.type ?? context.type;
                // A textual selection does not become Boolean just because its stored value is "true" or "false".
                const isBoolean = typeof node.value === 'boolean'
                    ? type == null || type === NoCodeDataType.Runtime || type === NoCodeDataType.Boolean
                    : type === NoCodeDataType.Boolean && (node.value === 'true' || node.value === 'false');
                const isUnit = parameter?.quantityParameterIndex != null && parameter.options.some(option => option.value === value);
                const isSentencePart = parameter?.optionsAsSentencePart === true && parameter.options.some(option => option.value === value);
                const label = isBoolean || isUnit || isSentencePart ? selected.label : `„${selected.label}“`;
                return label + ((selectionValues.get(selected.label)?.size ?? 0) > 1 ? ` (${value})` : '');
            }
            case 'NoCodeReference': {
                const element = allElements.find(({element}) => element.id === node.elementId)?.element;
                if (element == null) return humanizeNoCode(node, allElements, allOperators, context);
                const label = generateComponentTitle(element);
                addDetail(`Formularelement „${label}“`, element.id);
                return `Wert von „${label}“${(referenceIdentities.get(label)?.size ?? 0) > 1 ? ` (ID: ${element.id})` : ''}`;
            }
            case 'NoCodeProcessDataReference':
            case 'NoCodeInstanceDataReference':
            case 'NoCodeNodeDataReference': {
                const source = node.type === 'NoCodeProcessDataReference' ? InputVariableSource.ProcessData
                    : node.type === 'NoCodeInstanceDataReference' ? InputVariableSource.ProtectedProcessData : InputVariableSource.ElementData;
                const reference = getInputVariableReference({
                    source,
                    nodeDataKey: node.type === 'NoCodeNodeDataReference' ? node.nodeDataKey : undefined,
                    path: node.path ?? '',
                });
                const variable = context.variables?.find(candidate => getInputVariableReference(candidate) === reference);
                if (variable == null || variable.label.trim() === '') {
                    const text = humanizeNoCode(node, allElements, allOperators, {...context, variables: undefined});
                    if (node.path && (node.type !== 'NoCodeNodeDataReference' || node.nodeDataKey)) addDetail(text, reference);
                    return text;
                }
                addDetail(variable.label, reference);
                return `„${variable.label}“${(referenceIdentities.get(variable.label)?.size ?? 0) > 1 ? ` (${reference})` : ''}`;
            }
            case 'NoCodeExpression': {
                const {operator, parameters, options, useSentence} = prepare(node);
                const operands = node.operands ?? [];
                const argument = (index: number) => {
                    const text = describe(operands[index], parameters[index], options[index]);
                    return operands[index]?.type === 'NoCodeExpression' ? `(${text})` : text;
                };
                const template = operator == null ? null : getNoCodeReadableTemplate(operator, operands.length);
                const used = new Set(Array.from(template?.matchAll(/#(\d+)/g) ?? [], match => Number(match[1])));
                // A partial template can misstate a variadic operation; use one complete fallback instead.
                const completeTemplate = useSentence && template != null && template.trim() !== '' && used.size === parameters.length
                    && Array.from(used).every(index => index < parameters.length);
                if (completeTemplate) {
                    // Replace once: authored strings containing placeholders must never be parsed again.
                    return template.replace(/„#(\d+)“|“#(\d+)”|"#(\d+)"|'#(\d+)'|#(\d+)/g,
                        (_match, ...groups) => argument(Number(groups.slice(0, 5).find(group => group != null))));
                }
                const label = operator?.label ?? (node.operatorIdentifier ? `Unbekannte Funktion (${node.operatorIdentifier})` : 'Keine Funktion ausgewählt');
                return label + (parameters.length ? `: ${parameters.map((parameter, i) => `${parameter.label}: ${argument(i)}`).join('; ')}` : '');
            }
            default:
                return 'Unbekannte Ausdrucksart';
        }
    }

    collectSelections(operand);
    return {text: describe(operand), details: Array.from(details.values())};
}
