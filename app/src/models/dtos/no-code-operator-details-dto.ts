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