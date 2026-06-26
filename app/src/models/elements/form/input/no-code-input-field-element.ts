import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {NoCodeOperand} from '../../../functions/no-code-expression';

export interface NoCodeInputFieldElement extends BaseInputElement<ElementType.NoCodeInput> {
    returnType: NoCodeInputFieldReturnType | null;
}

export enum NoCodeInputFieldReturnType {
    RUNTIME = 'RUNTIME',
    STRING = 'STRING',
    NUMBER = 'NUMBER',
    BOOLEAN = 'BOOLEAN',
    DATE = 'DATE',
    DATETIME = 'DATETIME',
}

export interface NoCodeInputFieldElementItem {
    noCode: NoCodeOperand | null;
}
