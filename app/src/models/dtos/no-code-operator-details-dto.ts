import {NoCodeDataType} from '../../data/no-code-data-type';

export interface NoCodeOperatorDetailsDTO {
    identifier: string;
    packageName: string;
    label: string;
    description: string;
    abstractDescription: string;
    humanReadableTemplate: string | null;
    /** Text after the operator that starts the sentence its parameters continue, such as „ergibt wahr“. */
    sentenceLead?: string | null;
    tags: string[];
    variableArgumentCount?: boolean;
    signatures: NoCodeSignature[];
}

export interface NoCodeSignature {
    returnType: NoCodeDataType;
    parameters: NoCodeParameter[];
}

export interface NoCodeParameter {
    type: NoCodeDataType;
    label: string;
    description: string | null | undefined;
    /** Text before the operand. Null falls back to the label, an empty text shows nothing. */
    sentencePhrase?: string | null;
    /** Text after the operand. */
    sentenceSuffix?: string | null;
    numberConstraints?: NoCodeNumberConstraints | null;
    dateInput?: NoCodeDateInput | null;
    /** A freely entered text value uses its leading and trailing whitespace literally. */
    preserveWhitespace?: boolean | null;
    /** Marks fixed options as units and points to the sibling operand providing their quantity. */
    quantityParameterIndex?: number | null;
    /** A fixed choice supplies a sentence part; unknown choices require a named-input presentation. */
    optionsAsSentencePart?: boolean | null;
    options: NoCodeParameterOption[];
}

export enum NoCodeDateInput {
    Calendar = 'Calendar',
    CalendarOrDateTime = 'CalendarOrDateTime',
}

export interface NoCodeNumberConstraints {
    integerOnly: boolean;
    minimum?: number | null;
    maximum?: number | null;
    allowEmpty?: boolean;
}

export interface NoCodeParameterOption {
    label: string;
    value: string;
    singularLabel?: string | null;
}

/** These core functions repeat their second input and allow adding further inputs in the editor. */
export function getNoCodeVariadicInputKind(operator: NoCodeOperatorDetailsDTO): 'text' | 'number' | undefined {
    if (!operator.variableArgumentCount || operator.signatures.length !== 1 || operator.signatures[0].parameters.length !== 2) return undefined;
    switch (operator.identifier) {
        case 'concat':
            return 'text';
        case 'add':
        case 'subtract':
        case 'multiply':
        case 'divide':
            return 'number';
        default:
            return undefined;
    }
}

export function matchesDesiredNoCodeReturnType(
    operator: NoCodeOperatorDetailsDTO,
    desiredReturnType: NoCodeDataType,
): boolean {
    if (desiredReturnType === NoCodeDataType.Runtime) {
        return true;
    }

    return operator.signatures.some((signature) => (
        signature.returnType === desiredReturnType ||
        signature.returnType === NoCodeDataType.Runtime
    ));
}

/** Matches arity accepted by the evaluator, independently of fallback signatures used to display invalid data. */
export function findMatchingNoCodeSignature(operator: NoCodeOperatorDetailsDTO, operandCount: number): NoCodeSignature | undefined {
    const exact = operator.signatures.find(candidate => candidate.parameters.length === operandCount);
    if (exact != null || !operator.variableArgumentCount) return exact;
    return operator.signatures
        .filter(candidate => candidate.parameters.length < operandCount)
        .sort((a, b) => b.parameters.length - a.parameters.length)[0];
}

export function resolveNoCodeSignature(
    operator: NoCodeOperatorDetailsDTO,
    options?: {
        operandCount?: number;
        desiredReturnType?: NoCodeDataType;
    },
): NoCodeSignature {
    const signatures = operator.signatures ?? [];
    if (signatures.length === 0) {
        return {
            returnType: options?.desiredReturnType ?? NoCodeDataType.Runtime,
            parameters: [],
        };
    }

    const operandCount = options?.operandCount;
    let signature: NoCodeSignature | undefined;

    if (operandCount != null) {
        signature = findMatchingNoCodeSignature(operator, operandCount);

        if (signature == null) {
            signature = signatures
                .filter((candidate) => candidate.parameters.length >= operandCount)
                .sort((a, b) => a.parameters.length - b.parameters.length)[0];
        }
    }

    if (signature == null && options?.desiredReturnType != null) {
        signature = signatures.find((candidate) => (
            candidate.returnType === options.desiredReturnType ||
            candidate.returnType === NoCodeDataType.Runtime
        ));
    }

    signature ??= signatures[0];

    if (operandCount == null || operandCount <= signature.parameters.length || signature.parameters.length === 0) {
        return signature;
    }

    const parameters = [...signature.parameters];
    const repeatedTemplate = signature.parameters[signature.parameters.length - 1];
    // A second parameter such as „Teiler 1“ numbers only the divisors, excluding the initial number.
    const numberedFrom = getNoCodeVariadicInputKind(operator) == null ? undefined : repeatedTemplate.label.match(/\d+$/)?.[0];
    let labelIndex = Number(numberedFrom ?? signature.parameters.length);
    while (parameters.length < operandCount) {
        labelIndex++;
        parameters.push({
            ...repeatedTemplate,
            label: createRepeatedParameterLabel(repeatedTemplate.label, labelIndex),
            options: [...repeatedTemplate.options],
        });
    }

    return {
        ...signature,
        parameters,
    };
}

function createRepeatedParameterLabel(label: string, index: number): string {
    const numberedLabelMatch = label.match(/^(.*?)(\d+)$/);
    if (numberedLabelMatch != null) {
        return `${numberedLabelMatch[1]}${index}`;
    }

    return `${label} ${index}`;
}
