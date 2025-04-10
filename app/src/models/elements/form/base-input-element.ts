import {type BaseFormElement} from './base-form-element';
import {type ElementType} from '../../../data/element-type/element-type';
import {type Function} from '../../functions/function';
import {JavascriptCode} from '../../functions/javascript-code';
import {NoCodeExpression, ValidationExpressionWrapper} from '../../functions/no-code-expression';

export interface BaseInputElement<V, T extends ElementType> extends BaseFormElement<T> {
    label?: string;
    hint?: string;
    required?: boolean;
    technical?: boolean;
    disabled?: boolean;

    validate?: Function;
    computeValue?: Function;

    valueCode?: JavascriptCode;
    valueExpression?: NoCodeExpression;

    validationCode?: JavascriptCode;
    validationExpressions?: ValidationExpressionWrapper[];

    destinationKey?: string | null;

    validationReferencedIds?: string[];
    valueReferencedIds?: string[];
}
