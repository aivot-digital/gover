import {NoCodeDataType} from '../../data/no-code-data-type';

export interface NoCodeOperatorDetailsDTO {
    identifier: string;
    packageName: string;
    label: string;
    description: string;
    abstractDescription: string;
    humanReadableTemplate: string | null;
    tags: string[];
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
    options: NoCodeParameterOption[];
}

export interface NoCodeParameterOption {
    label: string;
    value: string;
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
        signature = signatures.find((candidate) => candidate.parameters.length === operandCount);

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
    for (let i = signature.parameters.length; i < operandCount; i++) {
        parameters.push({
            ...repeatedTemplate,
            label: createRepeatedParameterLabel(repeatedTemplate.label, i + 1),
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
