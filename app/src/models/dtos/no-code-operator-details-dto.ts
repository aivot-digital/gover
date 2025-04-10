import {NoCodeDataType} from '../../data/no-code-data-type';

export interface NoCodeOperatorDetailsDTO {
    identifier: string;
    packageName: string;
    label: string;
    description: string;
    abstractDescription: string;
    parameters: NoCodeParameter[];
    returnType: NoCodeDataType;
}

export interface NoCodeParameter {
    type: NoCodeDataType;
    label: string;
    options: NoCodeParameterOption[];
}

export interface NoCodeParameterOption {
    label: string;
    value: string;
}