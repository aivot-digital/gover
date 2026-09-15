import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';
import {JavascriptCode} from "../../../functions/javascript-code";
import {ConditionSet} from "../../../functions/conditions/condition-set";
import {NoCodeOperand} from "../../../functions/no-code-expression";

export interface FunctionInputElement extends BaseInputElement<ElementType.FunctionInput> {
    returnType: FunctionInputElementReturnType | null;
}

export enum FunctionInputElementReturnType {
    STRING = 'STRING',
    NUMBER = 'NUMBER',
    BOOLEAN = 'BOOLEAN',
    DATE = 'DATE',
    DATETIME = 'DATETIME',
}

export interface FunctionInputElementItem {
    type: FunctionInputElementItemType | null;
    code: JavascriptCode | null;
    conditionSet: ConditionSet | null;
    noCode: NoCodeOperand | null;
}

export enum FunctionInputElementItemType {
    NoCode = 'NoCode',
    ConditionSet = 'ConditionSet',
    Javascript = 'Javascript',
}